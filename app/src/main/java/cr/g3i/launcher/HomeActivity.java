package cr.g3i.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spanned;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HomeActivity extends Activity {
    private static final String DIPLAY = "com.shihab.diplay";
    private static final String HEADUNIT = "com.andrerinas.headunitrevived";
    private static final String DIPLAY_RELEASES = "https://github.com/shihabal3amri/DiPlay/releases";
    private static final String HEADUNIT_RELEASES = "https://github.com/andreknieriem/open-headunit/releases";
    private static final int MAX_SHORTCUTS = 4;
    private static final int PICK_PHOTO = 1;
    private static final int PICK_LOGO = 2;
    private static final int BACKGROUND = 0xff090f18;
    private static final int SURFACE = 0xff141e2d;
    private static final int CONTROL = 0xff22334a;
    private static final int ACCENT = 0xff7cead9;
    private static final int SECONDARY = 0xffb0bfd2;
    private SharedPreferences preferences;
    private final Map<String, String> phones = new HashMap<>();
    private TextView connectionTitle;
    private TextView connectionDetail;
    private TextView bluetoothStatus;
    private TextView networkStatus;
    private TextView phoneStatus;
    private TextView hotspotStatus;
    private String hotspotMessage = "";
    private ConnectionDial connectionDial;
    private final BroadcastReceiver connectionChanges = new BroadcastReceiver() {
        // targetSdk 28 uses the legacy BLUETOOTH permission; unavailable metadata is handled below.
        @android.annotation.SuppressLint("MissingPermission")
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)
                    && intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                phones.clear();
            }
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)
                    || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    try {
                        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                            phones.remove(device.getAddress());
                        } else {
                            BluetoothClass deviceClass = device.getBluetoothClass();
                            if (deviceClass != null && deviceClass.getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE) {
                                String name = device.getName();
                                phones.put(device.getAddress(), name == null || name.isEmpty() ? "Teléfono" : name);
                            }
                        }
                    } catch (SecurityException ignored) {
                        // The firmware may withhold access to Bluetooth device metadata.
                    }
                }
            }
            refreshConnection();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("home", MODE_PRIVATE);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectionChanges, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(connectionChanges);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        G3iHotspot.ensure(this, status -> {
            if (isDestroyed()) return;
            hotspotMessage = status;
            if (hotspotStatus != null) hotspotStatus.setText(status);
        });
    }

    private void render() {
        boolean projection = preferences.getBoolean("projection", false);
        Configuration screen = getResources().getConfiguration();
        boolean wide = screen.orientation == Configuration.ORIENTATION_LANDSCAPE
                && screen.screenWidthDp >= 650;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout shell = column();
        shell.setBackgroundColor(BACKGROUND);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout home = column();
        home.setPadding(dp(18), dp(14), dp(18), dp(20));
        scroll.addView(home);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand = column();
        ImageView logo = new ImageView(this);
        logo.setContentDescription("XPENG");
        if (loadImage(logo, "logo")) {
            logo.setScaleType(ImageView.ScaleType.FIT_START);
            brand.addView(logo, new LinearLayout.LayoutParams(dp(112), dp(36)));
        } else {
            logo.setImageResource(R.drawable.xpeng_mark);
            logo.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout wordmarkRow = new LinearLayout(this);
            wordmarkRow.setGravity(Gravity.CENTER_VERTICAL);
            wordmarkRow.addView(logo, new LinearLayout.LayoutParams(dp(34), dp(20)));
            TextView wordmark = text("XPENG", 22, Color.WHITE, true);
            wordmark.setLetterSpacing(0.21f);
            LinearLayout.LayoutParams wordmarkParams = new LinearLayout.LayoutParams(-2, -2);
            wordmarkParams.setMarginStart(dp(9));
            wordmarkRow.addView(wordmark, wordmarkParams);
            brand.addView(wordmarkRow);
        }
        brand.addView(text("G3i   /   " + (projection ? "PROYECCIÓN" : "CABINA"), 11, ACCENT, true));
        boolean clockBelowBrand = getResources().getConfiguration().screenWidthDp < 600;
        if (clockBelowBrand) header.setOrientation(LinearLayout.VERTICAL);
        header.addView(brand, clockBelowBrand ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1));
        TextClock clock = new TextClock(this);
        clock.setTextSize(72);
        clock.setTextColor(Color.WHITE);
        LinearLayout dateAndTime = column();
        dateAndTime.setGravity(Gravity.END);
        dateAndTime.addView(clock);
        TextClock date = new TextClock(this);
        String dateFormat = android.text.format.DateFormat.getBestDateTimePattern(
                java.util.Locale.getDefault(), "EEEdMMM");
        date.setFormat12Hour(dateFormat);
        date.setFormat24Hour(dateFormat);
        date.setTextSize(36);
        date.setTextColor(SECONDARY);
        dateAndTime.addView(date);
        if (clockBelowBrand) {
            dateAndTime.setGravity(Gravity.CENTER_HORIZONTAL);
            date.setGravity(Gravity.CENTER);
            header.addView(dateAndTime, new LinearLayout.LayoutParams(-1, -2));
        } else {
            header.addView(dateAndTime);
        }
        home.addView(header);

        LinearLayout modes = new LinearLayout(this);
        modes.setPadding(0, dp(12), 0, dp(4));
        Button cabinMode = action("Cabina", () -> selectMode(false));
        Button projectionMode = action("Proyección", () -> selectMode(true));
        (projection ? projectionMode : cabinMode).setTextColor(ACCENT);
        (projection ? projectionMode : cabinMode).setSelected(true);
        modes.addView(cabinMode, new LinearLayout.LayoutParams(0, -2, 1));
        modes.addView(projectionMode, new LinearLayout.LayoutParams(0, -2, 1));
        home.addView(modes);
        home.addView(shortcutShelf());
        home.addView(connectionPanel());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        FrameLayout vehicle = new FrameLayout(this);
        GradientDrawable heroBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff31566a, 0xff142338, 0xff0c1524});
        heroBackground.setCornerRadius(dp(22));
        vehicle.setBackground(heroBackground);
        vehicle.setClipToOutline(true);
        ImageView car = new ImageView(this);
        boolean hasPhoto = loadImage(car, "photo");
        if (!hasPhoto) {
            car.setImageResource(R.drawable.g3i_illustration);
            car.setPadding(dp(20), dp(68), dp(20), dp(24));
        }
        car.setContentDescription("XPENG G3i");
        car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int heroHeight = projection ? 176 : Math.min(360, Math.max(246, screen.screenWidthDp * 3 / 5));
        vehicle.addView(car, new FrameLayout.LayoutParams(-1, dp(heroHeight)));
        LinearLayout vehicleLabel = column();
        vehicleLabel.setPadding(dp(20), dp(13), dp(20), dp(20));
        vehicleLabel.addView(text("MI VEHÍCULO", 11, ACCENT, true));
        vehicleLabel.addView(text("G3i", projection ? 34 : 54, Color.WHITE, true));
        if (!hasPhoto) vehicle.addView(vehicleLabel, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout connect = column();
        connect.addView(text("CONECTA TU TELÉFONO", 12, SECONDARY, true));
        boolean split = !projection && !wide && screen.screenWidthDp >= 330;
        LinearLayout receivers = new LinearLayout(this);
        receivers.setOrientation(split ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        addTile(receivers, receiverTile("iPhone", "Apple CarPlay", DIPLAY, "DiPlay", 0xff173c49), split, 1);
        addTile(receivers, receiverTile("Android", "Android Auto", HEADUNIT, "Open Headunit", 0xff263559), split, 1);
        connect.addView(receivers);

        if (wide && projection) {
            LinearLayout side = column();
            addTile(side, vehicle, false, 1);
            addTile(content, connect, true, 2);
            addTile(content, side, true, 1);
        } else {
            addTile(content, projection ? connect : vehicle, wide, 1);
            addTile(content, projection ? vehicle : connect, wide, 1.25f);
        }
        home.addView(content, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(15), dp(5), dp(15), dp(7));
        actions.setBackgroundColor(SURFACE);
        actions.addView(action("Apps", this::showApps), new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(action("Ajustes", this::showSettings), new LinearLayout.LayoutParams(0, -2, 1));
        shell.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        setContentView(shell);
        refreshConnection();
    }

    private View shortcutShelf() {
        android.widget.HorizontalScrollView shelf = new android.widget.HorizontalScrollView(this);
        shelf.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        shelf.addView(row);
        for (String key : shortcutKeys()) {
            ComponentName component = ComponentName.unflattenFromString(key);
            if (component == null) continue;
            Intent launch = Intent.makeMainActivity(component);
            ResolveInfo app = getPackageManager().resolveActivity(launch, 0);
            if (app == null) continue;
            Button shortcut = action(app.loadLabel(getPackageManager()).toString(),
                    () -> open(launch, "Aplicación no disponible"));
            android.graphics.drawable.Drawable icon = app.loadIcon(getPackageManager()).mutate();
            icon.setBounds(0, 0, dp(30), dp(30));
            shortcut.setCompoundDrawables(null, icon, null, null);
            shortcut.setCompoundDrawablePadding(dp(4));
            shortcut.setSingleLine(true);
            shortcut.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(shortcut, new LinearLayout.LayoutParams(dp(110), dp(74)));
        }
        row.addView(action("＋ Agregar app", () -> showAppPicker(true)));
        return shelf;
    }

    private View connectionPanel() {
        LinearLayout panel = column();
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(12));
        panel.setBackground(rounded(SURFACE));
        LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(-1, -2);
        outer.setMargins(0, dp(9), 0, dp(10));
        panel.setLayoutParams(outer);
        connectionDial = new ConnectionDial();
        panel.addView(connectionDial, new LinearLayout.LayoutParams(dp(145), dp(145)));
        connectionTitle = text("", 24, Color.WHITE, true);
        connectionTitle.setGravity(Gravity.CENTER);
        connectionTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        panel.addView(connectionTitle);
        connectionDetail = text("", 14, SECONDARY, false);
        connectionDetail.setGravity(Gravity.CENTER);
        panel.addView(connectionDetail);
        panel.addView(action("Vincular teléfono", () -> open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                "Ajustes de Bluetooth no disponibles")));

        LinearLayout status = new LinearLayout(this);
        bluetoothStatus = addStatus(status, "BLUETOOTH");
        networkStatus = addStatus(status, "RED");
        phoneStatus = addStatus(status, "TELÉFONO");
        panel.addView(status, new LinearLayout.LayoutParams(-1, -2));
        if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.P) {
            hotspotStatus = text(hotspotMessage, 12, SECONDARY, false);
            hotspotStatus.setGravity(Gravity.CENTER);
            panel.addView(hotspotStatus);
        }
        return panel;
    }

    private TextView addStatus(LinearLayout row, String label) {
        LinearLayout tile = column();
        tile.setPadding(dp(5), dp(5), dp(5), dp(5));
        tile.addView(text(label, 10, SECONDARY, true));
        TextView value = text("", 13, ACCENT, true);
        tile.addView(value);
        row.addView(tile, new LinearLayout.LayoutParams(0, -2, 1));
        return value;
    }

    private void refreshConnection() {
        if (connectionTitle == null) return;
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        String bluetooth = "No disponible";
        if (adapter != null) {
            try {
                bluetooth = adapter.isEnabled() ? "Activado" : "Apagado";
            } catch (SecurityException ignored) {
                bluetooth = "Sin acceso";
            }
        }
        boolean linked = !phones.isEmpty() && "Activado".equals(bluetooth);
        bluetoothStatus.setText(bluetooth);
        phoneStatus.setText(linked ? "Por Bluetooth" : "Sin confirmar");
        connectionTitle.setText(linked ? phones.values().iterator().next() : "Conecta tu teléfono");
        connectionDetail.setText(linked ? "Enlace Bluetooth detectado" : "Vincula el teléfono o abre tu app de proyección");
        connectionDial.linked = linked;
        connectionDial.invalidate();

        ConnectivityManager connectivity = getSystemService(ConnectivityManager.class);
        NetworkCapabilities network = connectivity == null ? null
                : connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        String transport = "Sin red activa";
        if (network != null) {
            if (network.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) transport = "Wi-Fi";
            else if (network.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) transport = "Ethernet";
            else if (network.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) transport = "Datos móviles";
            else transport = "Otra red";
        }
        networkStatus.setText(transport);
    }

    private final class ConnectionDial extends View {
        private final android.graphics.Paint pen = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private boolean linked;

        ConnectionDial() {
            super(HomeActivity.this);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            float center = Math.min(getWidth(), getHeight()) / 2f;
            canvas.save();
            canvas.translate(getWidth() / 2f, getHeight() / 2f);
            pen.setStyle(android.graphics.Paint.Style.STROKE);
            pen.setStrokeWidth(dp(1));
            pen.setColor(0xff53687c);
            for (int angle = 0; angle < 360; angle += 10) {
                canvas.drawLine(0, center * .83f, 0, center * .92f, pen);
                canvas.rotate(10);
            }
            float radius = center * .72f;
            pen.setStrokeWidth(dp(3));
            pen.setColor(linked ? ACCENT : 0xff627c94);
            canvas.drawArc(-radius, -radius, radius, radius, 135, 270, false, pen);
            pen.setColor(linked ? ACCENT : SECONDARY);
            canvas.drawRoundRect(-center * .17f, -center * .25f, center * .17f, center * .25f,
                    dp(4), dp(4), pen);
            canvas.drawLine(-center * .055f, center * .15f, center * .055f, center * .15f, pen);
            canvas.restore();
        }
    }

    private void selectMode(boolean projection) {
        preferences.edit().putBoolean("projection", projection).apply();
        render();
    }

    private Button receiverTile(String platform, String title, String packageName, String receiver, int color) {
        String availability = "Ver versiones de " + receiver;
        if (getPackageManager().getLaunchIntentForPackage(packageName) != null) {
            availability = "Abrir " + receiver;
            try {
                String version = getPackageManager().getPackageInfo(packageName, 0).versionName;
                if (version != null && !version.isEmpty()) availability = receiver + " " + version;
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
                // The app may be removed while this screen is being drawn.
            }
        }
        Button tile = action("", () -> openReceiver(packageName, receiver));
        android.text.SpannableString label = new android.text.SpannableString(
                platform + "\n" + title + "\n" + availability);
        int titleStart = platform.length() + 1;
        label.setSpan(new android.text.style.ForegroundColorSpan(ACCENT), 0, platform.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        label.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), titleStart, titleStart + title.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        label.setSpan(new android.text.style.ForegroundColorSpan(SECONDARY), titleStart + title.length() + 1,
                label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tile.setText(label);
        tile.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tile.setMinHeight(dp(112));
        tile.setPadding(dp(14), dp(12), dp(12), dp(12));
        tile.setBackground(new RippleDrawable(ColorStateList.valueOf(0x447cead9), rounded(color), null));
        return tile;
    }

    private void showSettings() {
        String[] choices = {"Cabina", "Proyección", "Ajustes de red de Android", "Elegir foto del G3i",
                "Elegir logo XPENG", "Permitir hotspot automático", "Versiones de DiPlay",
                "Versiones de Open Headunit"};
        new AlertDialog.Builder(this).setTitle("Ajustes del Home").setItems(choices, (dialog, index) -> {
            if (index == 2) {
                open(new Intent(Settings.ACTION_WIRELESS_SETTINGS), "Ajustes de red no disponibles");
                return;
            }
            if (index == 5) {
                openHotspotPermission();
                return;
            }
            if (index >= 6) {
                openReceiverReleases(index == 6 ? DIPLAY : HEADUNIT);
                return;
            }
            if (index == 3 || index == 4) {
                Intent picker = new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("image/*")
                        .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*"})
                        .addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(picker, index == 3 ? PICK_PHOTO : PICK_LOGO);
                } catch (ActivityNotFoundException exception) {
                    if (android.os.Build.VERSION.SDK_INT == 28) {
                        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 30);
                        } else {
                            render();
                            toast("Coloca xpeng-logo.png y xpeng-g3i.png en Descargas/XPENG");
                        }
                    } else {
                        toast("Selector de imágenes no disponible");
                    }
                }
                return;
            }
            selectMode(index == 1);
        }).show();
    }

    private void openHotspotPermission() {
        if (Settings.System.canWrite(this)) {
            toast("Permiso para hotspot ya concedido");
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
            return;
        } catch (ActivityNotFoundException ignored) {
            // Some builds only expose the general settings list.
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
            new AlertDialog.Builder(this).setTitle("Permiso de hotspot por ADB")
                    .setMessage("Este firmware no muestra la pantalla. Ejecuta en ADB:\n\n"
                            + "adb shell appops set " + getPackageName() + " WRITE_SETTINGS allow"
                            + "\n\nDespués vuelve a abrir G3i Home.")
                    .setPositiveButton("Entendido", null).show();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if ((request != PICK_PHOTO && request != PICK_LOGO) || result != RESULT_OK
                || data == null || data.getData() == null) return;

        String key = request == PICK_PHOTO ? "photo" : "logo";
        android.util.AtomicFile destination = new android.util.AtomicFile(imageFile(key));
        java.io.FileOutputStream stream = null;
        try {
            android.graphics.Bitmap bitmap = android.graphics.ImageDecoder.decodeBitmap(
                    android.graphics.ImageDecoder.createSource(getContentResolver(), data.getData()),
                    (decoder, info, source) -> {
                        decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                        limitImageSize(decoder, info);
                    });
            try {
                stream = destination.startWrite();
                if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw new java.io.IOException("Could not encode selected image");
                }
                destination.finishWrite(stream);
                stream = null;
            } finally {
                bitmap.recycle();
            }
            render();
        } catch (java.io.IOException | RuntimeException error) {
            if (stream != null) destination.failWrite(stream);
            toast("No se pudo importar la imagen seleccionada");
        }
    }

    private java.io.File imageFile(String key) {
        return new java.io.File(getFilesDir(), key + ".png");
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == 30 && grants.length > 0
                && grants[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) render();
    }

    private boolean loadImage(ImageView image, String key) {
        java.io.File imported = imageFile(key);
        if (imported.isFile() && decodeImage(image, android.graphics.ImageDecoder.createSource(imported))) {
            return true;
        }
        String saved = preferences.getString(key, null);
        if (saved != null && decodeImage(image, android.graphics.ImageDecoder.createSource(
                getContentResolver(), Uri.parse(saved)))) return true;
        if (android.os.Build.VERSION.SDK_INT != 28
                || checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
        java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS);
        String filename = "logo".equals(key) ? "xpeng-logo.png" : "xpeng-g3i.png";
        java.io.File file = new java.io.File(downloads, "XPENG/" + filename);
        return file.isFile() && decodeImage(image, android.graphics.ImageDecoder.createSource(file));
    }

    private boolean decodeImage(ImageView image, android.graphics.ImageDecoder.Source source) {
        try {
            image.setImageDrawable(android.graphics.ImageDecoder.decodeDrawable(source,
                    (decoder, info, src) -> limitImageSize(decoder, info)));
            return true;
        } catch (java.io.IOException | SecurityException exception) {
            return false;
        }
    }

    private static void limitImageSize(android.graphics.ImageDecoder decoder,
                                       android.graphics.ImageDecoder.ImageInfo info) {
        int longestSide = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
        decoder.setTargetSampleSize(Math.max(1, (int) Math.ceil(longestSide / 1600.0)));
    }

    private void showApps() {
        new AlertDialog.Builder(this).setTitle("Aplicaciones instaladas")
                .setItems(new String[]{"Abrir aplicación", "Editar accesos directos"},
                        (dialog, index) -> showAppPicker(index == 1)).show();
    }

    private void showAppPicker(boolean edit) {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<>(getPackageManager().queryIntentActivities(launcher, 0));
        apps.removeIf(app -> getPackageName().equals(app.activityInfo.packageName));
        apps.sort(new ResolveInfo.DisplayNameComparator(getPackageManager()));

        List<String> selected = shortcutKeys();
        String[] labels = new String[apps.size()];
        boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo app = apps.get(i);
            labels[i] = app.loadLabel(getPackageManager()).toString();
            checked[i] = selected.contains(componentOf(app).flattenToString());
        }
        AlertDialog.Builder picker = new AlertDialog.Builder(this);
        if (!edit) {
            picker.setTitle("Abrir aplicación").setItems(labels, (dialog, index) ->
                    open(Intent.makeMainActivity(componentOf(apps.get(index))), "Aplicación no disponible"));
        } else {
            picker.setTitle("Accesos directos · máximo 4")
                    .setMultiChoiceItems(labels, checked, (dialog, index, enabled) -> {
                        String key = componentOf(apps.get(index)).flattenToString();
                        if (!enabled) {
                            selected.remove(key);
                        } else if (selected.size() < MAX_SHORTCUTS) {
                            selected.add(key);
                        } else {
                            ((AlertDialog) dialog).getListView().setItemChecked(index, false);
                            toast("Puedes fijar hasta cuatro accesos");
                        }
                    })
                    .setPositiveButton("Guardar", (dialog, index) -> {
                        preferences.edit().putString("shortcuts", String.join("\n", selected)).apply();
                        render();
                    })
                    .setNegativeButton("Cancelar", null);
        }
        picker.show();
    }

    private List<String> shortcutKeys() {
        List<String> keys = new ArrayList<>();
        String saved = preferences.getString("shortcuts", "");
        for (String key : saved.split("\n")) {
            ComponentName component = ComponentName.unflattenFromString(key);
            if (component != null && getPackageManager().resolveActivity(
                    Intent.makeMainActivity(component), 0) != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    private ComponentName componentOf(ResolveInfo app) {
        return new ComponentName(app.activityInfo.packageName, app.activityInfo.name);
    }

    private void openReceiver(String packageName, String label) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) {
            openReceiverReleases(packageName);
            return;
        }
        open(launch, "No se pudo abrir " + label);
    }

    private void openReceiverReleases(String packageName) {
        String url = DIPLAY.equals(packageName) ? DIPLAY_RELEASES : HEADUNIT_RELEASES;
        open(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No hay navegador para abrir GitHub");
    }

    private void open(Intent intent, String message) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            toast(message);
        }
    }

    private void addTile(LinearLayout row, android.view.View tile, boolean wide, float weight) {
        LinearLayout.LayoutParams params = wide
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
                : new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), wide ? dp(8) : 0, dp(4));
        row.addView(tile, params);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String label, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(3), 0, dp(3));
        return view;
    }

    private Button action(String label, Runnable click) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setMinHeight(dp(52));
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        android.graphics.drawable.InsetDrawable background = new android.graphics.drawable.InsetDrawable(
                new RippleDrawable(ColorStateList.valueOf(0x447cead9), rounded(CONTROL), null), dp(3));
        button.setBackground(background);
        button.setOnClickListener(view -> click.run());
        return button;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(19));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
