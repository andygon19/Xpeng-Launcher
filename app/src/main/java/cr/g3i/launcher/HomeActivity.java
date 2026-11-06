package cr.g3i.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class HomeActivity extends Activity {
    private static final String DIPLAY = "com.shihab.diplay";
    private static final String HEADUNIT = "com.andrerinas.headunitrevived";
    private static final int MAX_SHORTCUTS = 4;
    private static final int PICK_PHOTO = 1;
    private static final int PICK_LOGO = 2;
    private static final int BACKGROUND = 0xff090f18;
    private static final int SURFACE = 0xff141e2d;
    private static final int CONTROL = 0xff22334a;
    private static final int ACCENT = 0xff7cead9;
    private static final int SECONDARY = 0xffb0bfd2;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("home", MODE_PRIVATE);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        boolean projection = preferences.getBoolean("projection", false);
        Configuration screen = getResources().getConfiguration();
        boolean wide = screen.orientation == Configuration.ORIENTATION_LANDSCAPE
                && screen.screenWidthDp >= 650;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

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
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextClock clock = new TextClock(this);
        clock.setTextSize(24);
        clock.setTextColor(Color.WHITE);
        header.addView(clock);
        home.addView(header);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        FrameLayout vehicle = new FrameLayout(this);
        vehicle.setBackground(rounded(SURFACE));
        vehicle.setClipToOutline(true);
        ImageView car = new ImageView(this);
        boolean hasPhoto = loadImage(car, "photo");
        if (!hasPhoto) {
            car.setImageResource(R.drawable.g3i_illustration);
            car.setPadding(dp(24), dp(24), dp(24), dp(38));
        }
        car.setContentDescription("XPENG G3i");
        car.setScaleType(hasPhoto ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        vehicle.addView(car, new FrameLayout.LayoutParams(-1, dp(projection ? 168 : 260)));
        TextView vehicleLabel = text("XPENG G3i", 23, Color.WHITE, true);
        vehicleLabel.setPadding(dp(16), dp(24), dp(16), dp(12));
        vehicleLabel.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, 0xee090f18}));
        vehicle.addView(vehicleLabel, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        LinearLayout connect = panel("CONECTIVIDAD", "Proyección");
        Button carPlay = action("Apple CarPlay\nDiPlay · iPhone", () -> openReceiver(DIPLAY, "DiPlay"));
        Button androidAuto = action("Android Auto\nOpen Headunit · Android",
                () -> openReceiver(HEADUNIT, "Open Headunit"));
        carPlay.setMinHeight(dp(projection ? 84 : 66));
        androidAuto.setMinHeight(dp(projection ? 84 : 66));
        carPlay.setTextColor(ACCENT);
        connect.addView(carPlay);
        connect.addView(androidAuto);
        LinearLayout widgets = panel("WIDGETS", "Multimedia");
        widgets.addView(text("Integración pendiente", 14, SECONDARY, false));
        widgets.addView(text("Micrófono", 20, Color.WHITE, true));
        widgets.addView(text("Integración pendiente", 14, SECONDARY, false));

        if (wide && projection) {
            LinearLayout side = column();
            addTile(side, vehicle, false, 1);
            addTile(side, widgets, false, 1);
            addTile(content, connect, true, 2);
            addTile(content, side, true, 1);
        } else {
            addTile(content, projection ? connect : vehicle, wide, 1);
            addTile(content, projection ? vehicle : connect, wide, 1.25f);
            addTile(content, widgets, wide, 0.8f);
        }
        home.addView(content, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout dock = column();
        LinearLayout dockRow = new LinearLayout(this);
        for (String key : shortcutKeys()) {
            ComponentName component = ComponentName.unflattenFromString(key);
            if (component == null) continue;
            ResolveInfo resolved = getPackageManager().resolveActivity(
                    Intent.makeMainActivity(component), 0);
            if (resolved != null) {
                if (dockRow.getChildCount() == 0) dock.addView(dockRow);
                dockRow.addView(action(resolved.loadLabel(getPackageManager()).toString(),
                        () -> open(Intent.makeMainActivity(component), "Aplicación no disponible")),
                        new LinearLayout.LayoutParams(0, -2, 1));
                if (dockRow.getChildCount() == 2) dockRow = new LinearLayout(this);
            }
        }
        home.addView(dock);
        LinearLayout actions = new LinearLayout(this);
        actions.addView(action("Apps", this::showApps), new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(action("Ajustes", this::showSettings), new LinearLayout.LayoutParams(0, -2, 1));
        home.addView(actions);
        setContentView(scroll);
    }

    private void showSettings() {
        String[] choices = {"Cabina", "Proyección", "Ajustes de red de Android", "Elegir foto del G3i", "Elegir logo XPENG"};
        new AlertDialog.Builder(this).setTitle("Ajustes del Home").setItems(choices, (dialog, index) -> {
            if (index == 2) {
                open(new Intent(Settings.ACTION_WIRELESS_SETTINGS), "Ajustes de red no disponibles");
                return;
            }
            if (index >= 3) {
                Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
                try {
                    startActivityForResult(picker, index == 3 ? PICK_PHOTO : PICK_LOGO);
                } catch (ActivityNotFoundException exception) {
                    toast("Selector de imágenes no disponible");
                }
                return;
            }
            preferences.edit().putBoolean("projection", index == 1).apply();
            render();
        }).show();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if ((request != PICK_PHOTO && request != PICK_LOGO) || result != RESULT_OK
                || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String key = request == PICK_PHOTO ? "photo" : "logo";
            String previous = preferences.getString(key, null);
            preferences.edit().putString(key, uri.toString()).apply();
            String other = preferences.getString(request == PICK_PHOTO ? "logo" : "photo", null);
            if (previous != null && !previous.equals(uri.toString()) && !previous.equals(other)) {
                try {
                    getContentResolver().releasePersistableUriPermission(Uri.parse(previous),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // The previous image provider may already have revoked access.
                }
            }
        } catch (SecurityException exception) {
            toast("No se pudo conservar el acceso a la imagen");
        }
    }

    private boolean loadImage(ImageView image, String key) {
        String saved = preferences.getString(key, null);
        if (saved == null) return false;
        try {
            android.graphics.ImageDecoder.Source source =
                    android.graphics.ImageDecoder.createSource(getContentResolver(), Uri.parse(saved));
            image.setImageDrawable(android.graphics.ImageDecoder.decodeDrawable(source, (decoder, info, src) -> {
                int longestSide = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                decoder.setTargetSampleSize(Math.max(1, (int) Math.ceil(longestSide / 1600.0)));
            }));
            return true;
        } catch (java.io.IOException | SecurityException exception) {
            return false;
        }
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
            toast(label + " no está instalado o no tiene pantalla de inicio");
            return;
        }
        open(launch, "No se pudo abrir " + label);
    }

    private void open(Intent intent, String message) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            toast(message);
        }
    }

    private LinearLayout panel(String label, String title) {
        LinearLayout panel = column();
        panel.setPadding(dp(14), dp(13), dp(14), dp(13));
        panel.setBackground(rounded(SURFACE));
        panel.addView(text(label, 12, ACCENT, true));
        panel.addView(text(title, 24, Color.WHITE, true));
        return panel;
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
