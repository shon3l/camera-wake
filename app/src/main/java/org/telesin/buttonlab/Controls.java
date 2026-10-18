package org.telesin.buttonlab;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.view.InputDevice;

final class Controls {
    private Controls() { }
    static SharedPreferences pref(Context context) { return context.getSharedPreferences("camera_wake", Context.MODE_PRIVATE); }
    static boolean enabled(Context context) { return pref(context).getBoolean("camera_on_wake", false); }
    static boolean batteryReady(Context context) {
        PowerManager power = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isIgnoringBatteryOptimizations(context.getPackageName());
    }
    static boolean remoteConnected() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && device.isExternal() && device.getName().startsWith("TELESIN")) return true;
        }
        return false;
    }
}
