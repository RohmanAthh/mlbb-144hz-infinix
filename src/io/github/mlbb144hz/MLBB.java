package io.github.mlbb144hz;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MLBB implements IXposedHookLoadPackage {
    private static final String PKG = "com.mobile.legends";
    // ponytail: XOS-only binder code (IDisplayManager.updateRefreshRateForScene on X6855 Android 16); other ROMs ignore/fail silently.
    private static final int TX_UPDATE_REFRESH_RATE_FOR_SCENE = 58;
    private static final long RESEND_MS = 5000;

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static boolean resumed;
    private static android.net.wifi.WifiManager.WifiLock wifiLock;

    // Disables WiFi power-save while MLBB is foreground; cuts ping spikes from radio sleep.
    private static void lowLatency(Activity a, boolean on) {
        try {
            if (wifiLock == null) {
                android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                        a.getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
                wifiLock = wm.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "MLBB:lowlat");
                wifiLock.setReferenceCounted(false);
            }
            if (on) wifiLock.acquire(); else wifiLock.release();
        } catch (Throwable t) {
            XposedBridge.log("MLBB wifi lock failed: " + t);
        }
    }
    private static final Runnable RESEND = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            sendGameScene(1, 144);
            H.postDelayed(this, RESEND_MS);
        }
    };

    private static void set(String key, String value) {
        try {
            java.lang.reflect.Field f = Build.class.getDeclaredField(key);
            f.setAccessible(true);
            f.set(null, value);
        } catch (Throwable t) {
            XposedBridge.log("Failed to set prop: " + key + " " + t);
        }
    }

    // XOS caps unlisted games at 120Hz; a game_scene event with game_refresh_rate=144 lifts it.
    static void sendGameScene(int state, int hz) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            IBinder b = (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class).invoke(null, "display");
            if (b == null) return;
            Bundle s = new Bundle();
            s.putString("scene_name", "game_scene");
            s.putInt("game_state", state);
            s.putInt("game_refresh_rate", hz);
            s.putString("pkg_name", PKG);
            data.writeInterfaceToken("android.hardware.display.IDisplayManager");
            data.writeInt(1);
            s.writeToParcel(data, 0);
            boolean ok = b.transact(TX_UPDATE_REFRESH_RATE_FOR_SCENE, data, reply, 0);
            reply.readException();
            XposedBridge.log("MLBB 144Hz scene sent ok=" + ok + " state=" + state + " hz=" + hz);
        } catch (Throwable t) {
            XposedBridge.log("MLBB 144Hz scene failed: " + t);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!PKG.equals(p.packageName)) return;
        set("BRAND", "iQOO");
        set("MANUFACTURER", "vivo");
        set("DEVICE", "PD2536F");
        set("PRODUCT", "PD2536F");
        set("MODEL", "I2508");
        XposedBridge.log("Spoofed " + p.packageName + " as iQOO 15R");

        XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam m) {
                resumed = true;
                H.removeCallbacks(RESEND);
                H.post(RESEND);
                lowLatency((Activity) m.thisObject, true);
            }
        });
        XposedBridge.hookAllMethods(Activity.class, "onPause", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam m) {
                resumed = false;
                H.removeCallbacks(RESEND);
                sendGameScene(0, 120);
                lowLatency((Activity) m.thisObject, false);
            }
        });
    }
}
