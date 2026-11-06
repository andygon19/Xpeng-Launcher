package cr.g3i.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class G3iHotspot {
    private static final String TAG = "G3iHotspot";
    private static final String IFACE = "wlan0";
    private static final String AP_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final int AP_DISABLING = 10;
    private static final int AP_DISABLED = 11;
    private static final int AP_ENABLING = 12;
    private static final int AP_ENABLED = 13;
    private static final int AP_FAILED = 14;
    private static final int UNKNOWN = -1;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    static void ensure(Context context, Consumer<String> onStatus) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P || !RUNNING.compareAndSet(false, true)) return;
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            String status;
            try {
                status = ensureSavedHotspot(app);
            } catch (ReflectiveOperationException | RuntimeException error) {
                Log.w(TAG, "Hotspot check failed", error);
                status = "Hotspot · el firmware no permitió la operación";
            } finally {
                RUNNING.set(false);
            }
            String result = status;
            new Handler(Looper.getMainLooper()).post(() -> onStatus.accept(result));
        });
    }

    @SuppressWarnings("deprecation")
    private static String ensureSavedHotspot(Context app) throws ReflectiveOperationException {
        WifiManager wifi = app.getSystemService(WifiManager.class);
        ConnectivityManager connectivity = app.getSystemService(ConnectivityManager.class);
        if (wifi == null || connectivity == null) return "Hotspot · servicio no disponible";

        int state = apState(app, wifi);
        if (state == AP_ENABLED) return activeStatus(connectivity);
        if (state == AP_ENABLING || state == AP_DISABLING) {
            for (int i = 0; i < 40 && (state == AP_ENABLING || state == AP_DISABLING); i++) {
                SystemClock.sleep(250);
                state = apState(app, wifi);
            }
            if (state == AP_ENABLED) return activeStatus(connectivity);
        }
        if (state != AP_DISABLED && state != AP_FAILED) return "Hotspot · estado sin confirmar";
        if (tetheredOnWlan0(connectivity)) return "Hotspot · wlan0 ocupado, sin cambios";
        if (!Settings.System.canWrite(app)) return "Hotspot · concede permiso en Ajustes";

        // Resolve the platform call before touching the Wi-Fi client.
        Field field = ConnectivityManager.class.getDeclaredField("mService");
        field.setAccessible(true);
        Object service = field.get(connectivity);
        if (service == null) return "Hotspot · servicio no disponible";
        Method start = Class.forName("android.net.IConnectivityManager").getMethod(
                "startTethering", int.class, ResultReceiver.class, boolean.class, String.class);

        boolean restoreClient = false;
        try {
            int current = apState(app, wifi);
            if (current == AP_ENABLED) return activeStatus(connectivity);
            if ((current != AP_DISABLED && current != AP_FAILED) || tetheredOnWlan0(connectivity)) {
                return "Hotspot · estado cambió, sin cambios";
            }
            int client = wifi.getWifiState();
            if (client == WifiManager.WIFI_STATE_ENABLED || client == WifiManager.WIFI_STATE_ENABLING) {
                if (!wifi.setWifiEnabled(false)) return "Hotspot · no se pudo apagar Wi-Fi";
                restoreClient = true;
            }
            for (int i = 0; i < 24 && wifi.getWifiState() != WifiManager.WIFI_STATE_DISABLED; i++) {
                SystemClock.sleep(250);
            }
            if (wifi.getWifiState() != WifiManager.WIFI_STATE_DISABLED) return "Hotspot · Wi-Fi no se apagó";
            current = apState(app, wifi);
            if (current == AP_ENABLED) return activeStatus(connectivity);
            if (current != AP_DISABLED && current != AP_FAILED) return "Hotspot · estado cambió, sin cambios";

            AtomicInteger response = new AtomicInteger(Integer.MIN_VALUE);
            ResultReceiver receiver = new ResultReceiver(null) {
                @Override protected void onReceiveResult(int code, Bundle data) {
                    response.set(code);
                }
            };
            // TETHERING_WIFI uses the system's saved configuration (SSID, password and band).
            start.invoke(service, 0, receiver, false, app.getPackageName());
            for (int i = 0; i < 60; i++) {
                int result = response.get();
                if (result != Integer.MIN_VALUE && result != 0) {
                    return "Hotspot · Android rechazó el arranque (" + result + ")";
                }
                if (apState(app, wifi) == AP_ENABLED && tetheredOnWlan0(connectivity)) {
                    return "Hotspot activo · wlan0";
                }
                SystemClock.sleep(250);
            }
            return apState(app, wifi) == AP_ENABLED ? activeStatus(connectivity) : "Hotspot · no se confirmó el arranque";
        } finally {
            int finalState = apState(app, wifi);
            if (restoreClient && (finalState == AP_DISABLED || finalState == AP_FAILED)
                    && !tetheredOnWlan0(connectivity) && !wifi.setWifiEnabled(true)) {
                Log.w(TAG, "Android rejected restoring Wi-Fi client");
            }
        }
    }

    private static String activeStatus(ConnectivityManager connectivity) {
        return tetheredOnWlan0(connectivity)
                ? "Hotspot activo · wlan0" : "Punto de acceso activo · wlan0 sin confirmar";
    }

    private static int apState(Context app, WifiManager wifi) {
        try {
            Object state = WifiManager.class.getMethod("getWifiApState").invoke(wifi);
            if (state instanceof Integer) return (Integer) state;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            // Some firmware exposes only the protected state broadcast.
        }
        Intent last = app.registerReceiver(null, new IntentFilter(AP_ACTION));
        return last == null ? UNKNOWN : last.getIntExtra("wifi_state", UNKNOWN);
    }

    private static boolean tetheredOnWlan0(ConnectivityManager connectivity) {
        try {
            Object value = ConnectivityManager.class.getMethod("getTetheredIfaces").invoke(connectivity);
            return value instanceof String[] && Arrays.asList((String[]) value).contains(IFACE);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return false;
        }
    }

    private G3iHotspot() {}
}
