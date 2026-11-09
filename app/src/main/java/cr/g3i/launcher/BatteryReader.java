package cr.g3i.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Reads the XPeng vehicle battery through the Android Car API (android.car).
 *
 * The G3i runs the standard car_service ({@code android.car.ICar}) and exposes its state as vendor
 * VHAL properties. The android.car classes exist on the head unit but are not in the public SDK, so
 * every call here goes through reflection — that keeps the app compiling against the plain android.jar
 * while still talking to the real Car service at runtime.
 *
 * Property IDs were read from {@code dumpsys car_service} on the car (d22 / xmartq6eu, XOS 9):
 *   VCU_BMS_SOCDISP        0x21401202  INT32  battery charge, 0..100 %   (seen = 84)
 *   VCU_DSTBAT_DISP        0x21401211  INT32  remaining range, km
 *   VCU_CHARGE_STATUS      0x21401229  INT32  charging status
 *   VCU_CHARGE_GUN_STATUS  0x21401228  INT32  charge plug connected
 *
 * These are vendor properties, so a plain third-party app may get a SecurityException until it is
 * granted the Car vendor permission (e.g. by root: appops / pm grant, or installed as a privileged
 * app). Every read is defensive: a failure leaves the field unknown instead of crashing the home.
 */
final class BatteryReader {
    private static final String TAG = "BatteryReader";

    private static final int VCU_BMS_SOCDISP = 0x21401202;
    private static final int VCU_DSTBAT_DISP = 0x21401211;
    private static final int VCU_CHARGE_STATUS = 0x21401229;
    private static final int VCU_CHARGE_GUN_STATUS = 0x21401228;
    private static final int GLOBAL_AREA = 0;
    private static final String PROPERTY_SERVICE = "property";

    /** A snapshot of the vehicle battery. Unknown numbers are -1. */
    static final class Battery {
        final int socPercent;
        final int rangeKm;
        final boolean charging;
        final boolean available;

        Battery(int socPercent, int rangeKm, boolean charging, boolean available) {
            this.socPercent = socPercent;
            this.rangeKm = rangeKm;
            this.charging = charging;
            this.available = available;
        }
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    // Cached Car connection, reused across polls. Guarded by the class monitor.
    private static Object car;
    private static Object propertyManager;
    private static Method getIntProperty;

    static void read(Context context, Consumer<Battery> onResult) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            Battery result;
            try {
                result = query(app);
            } catch (Throwable error) {
                Log.w(TAG, "battery read failed", error);
                result = new Battery(-1, -1, false, false);
            }
            Battery delivered = result;
            new Handler(Looper.getMainLooper()).post(() -> onResult.accept(delivered));
        });
    }

    private static synchronized Battery query(Context app) throws Exception {
        connect(app);
        if (propertyManager == null || getIntProperty == null) {
            return new Battery(-1, -1, false, false);
        }
        int soc = readInt(VCU_BMS_SOCDISP, -1);
        int range = readInt(VCU_DSTBAT_DISP, -1);
        int gun = readInt(VCU_CHARGE_GUN_STATUS, 0);
        int status = readInt(VCU_CHARGE_STATUS, 0);
        boolean charging = gun != 0 || status != 0;
        return new Battery(clampPercent(soc), range < 0 ? -1 : range, charging, soc >= 0);
    }

    private static void connect(Context app) throws Exception {
        if (propertyManager != null && getIntProperty != null) return;

        Class<?> carClass = Class.forName("android.car.Car");
        if (car == null) {
            car = carClass.getMethod("createCar", Context.class).invoke(null, app);
            if (car == null) return;
        }
        Method isConnected = carClass.getMethod("isConnected");
        if (!Boolean.TRUE.equals(isConnected.invoke(car))) {
            try {
                carClass.getMethod("connect").invoke(car);
            } catch (Throwable ignored) {
                // Some builds auto-connect from createCar(); isConnected() below is the real check.
            }
            for (int i = 0; i < 50 && !Boolean.TRUE.equals(isConnected.invoke(car)); i++) {
                Thread.sleep(100);
            }
        }
        if (!Boolean.TRUE.equals(isConnected.invoke(car))) return;

        Object manager = carClass.getMethod("getCarManager", String.class).invoke(car, PROPERTY_SERVICE);
        if (manager == null) return;
        getIntProperty = manager.getClass().getMethod("getIntProperty", int.class, int.class);
        propertyManager = manager;
    }

    private static int readInt(int property, int fallback) {
        try {
            Object value = getIntProperty.invoke(propertyManager, property, GLOBAL_AREA);
            return value instanceof Integer ? (Integer) value : fallback;
        } catch (Throwable error) {
            // No permission (SecurityException) or the property is momentarily unavailable.
            return fallback;
        }
    }

    private static int clampPercent(int soc) {
        if (soc < 0) return -1;
        return Math.min(soc, 100);
    }

    private BatteryReader() {}
}
