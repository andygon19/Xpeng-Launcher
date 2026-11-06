package cr.g3i.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
    private static final int BACKGROUND = Color.rgb(16, 22, 32);
    private static final int SURFACE = Color.rgb(30, 41, 56);
    private static final int CONTROL = Color.rgb(44, 58, 77);
    private static final int ACCENT = Color.rgb(126, 228, 211);
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("home", MODE_PRIVATE);
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
        home.setPadding(dp(18), dp(14), dp(18), dp(14));
        scroll.addView(home);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("XPENG   /   G3i HOME", 19, ACCENT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextClock clock = new TextClock(this);
        clock.setTextSize(19);
        clock.setTextColor(Color.WHITE);
        header.addView(clock);
        home.addView(header);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        LinearLayout vehicle = panel("MI VEH�CULO", "XPENG G3i");
        ImageView car = new ImageView(this);
        car.setImageResource(R.drawable.g3i_illustration);
        car.setContentDescription("Ilustraci�n del XPENG G3i");
        car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        vehicle.addView(car, new LinearLayout.LayoutParams(-1, dp(projection ? 100 : 176)));
        LinearLayout connect = panel("PROYECCI�N", "Conecta tu tel�fono");
        Button carPlay = action("Apple CarPlay\nDiPlay � iPhone", () -> openReceiver(DIPLAY, "DiPlay"));
        Button androidAuto = action("Android Auto\nOpen Headunit � Android",
                () -> openReceiver(HEADUNIT, "Open Headunit"));
        if (projection) {
            carPlay.setMinHeight(dp(84));
            androidAuto.setMinHeight(dp(84));
        }
        connect.addView(carPlay);
        connect.addView(androidAuto);
        LinearLayout widgets = panel("WIDGETS", "Multimedia");
        widgets.addView(text("Integraci�n pendiente", 14, ACCENT, false));
        widgets.addView(text("Micr�fono", 20, Color.WHITE, true));
        widgets.addView(text("Integraci�n pendiente", 14, ACCENT, false));

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
                        () -> open(Intent.makeMainActivity(component), "Aplicaci�n no disponible")),
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
        String[] choices = {"Cabina", "Proyecci�n", "Ajustes de red de Android"};
        new AlertDialog.Builder(this).setTitle("Ajustes del Home").setItems(choices, (dialog, index) -> {
            if (index == 2) {
                open(new Intent(Settings.ACTION_WIRELESS_SETTINGS), "Ajustes de red no disponibles");
                return;
            }
            preferences.edit().putBoolean("projection", index == 1).apply();
            render();
        }).show();
    }

    private void showApps() {
        new AlertDialog.Builder(this).setTitle("Aplicaciones instaladas")
                .setItems(new String[]{"Abrir aplicaci�n", "Editar accesos directos"},
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
            picker.setTitle("Abrir aplicaci�n").setItems(labels, (dialog, index) ->
                    open(Intent.makeMainActivity(componentOf(apps.get(index))), "Aplicaci�n no disponible"));
        } else {
            picker.setTitle("Accesos directos � m�ximo 4")
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
            toast(label + " no est� instalado o no tiene pantalla de inicio");
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

    private void addTile(LinearLayout row, LinearLayout tile, boolean wide, float weight) {
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
                rounded(CONTROL), dp(3));
        button.setBackground(background);
        button.setOnClickListener(view -> click.run());
        return button;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(13));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
