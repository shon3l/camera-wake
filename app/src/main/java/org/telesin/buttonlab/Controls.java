package org.telesin.buttonlab;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.InputDevice;
import android.view.KeyEvent;
import java.util.Locale;

/** Preferences and key identity. Buttons are distinguishable only if their key-event identities differ. */
final class Controls {
    static final String[] SLOTS = {"Capture", "Wide W", "Tele T", "Mode M1", "Mode M2"};
    static final String[] ACTIONS = {"Pass through", "Ignore", "Tap camera control", "Swipe left", "Swipe right", "Swipe up", "Swipe down", "Launch camera"};
    static final int PASS = 0, IGNORE = 1, TAP = 2, LEFT = 3, RIGHT = 4, UP = 5, DOWN = 6, WAKE_CAMERA = 7;
    private Controls() { }

    static SharedPreferences pref(Context context) {
        return context.getSharedPreferences("button_map", Context.MODE_PRIVATE);
    }

    static final int[] DISPLAY_ORDER = {3, 4, 0, 1, 2};

    static void restoreDefaults(Context context, int slot) {
        SharedPreferences p = pref(context);
        SharedPreferences.Editor edit = p.edit();
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot >= 0 && i != slot) continue;
            String pre = prefix(i);
            for (String key : new String[]{"action", "long_action", "x", "y", "orientation"}) edit.remove(pre + key);
        }
        edit.remove("learning_slot").remove("learning_deadline");
        if (slot < 0) edit.putBoolean("native_controls", true).putInt("hold_millis", 700).putString("camera_package", "com.android.camera");
        edit.apply();
    }

    static boolean batteryReady(Context context) {
        android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    static String prefix(int slot) { return "button_" + slot + "_"; }

    static boolean isLearned(SharedPreferences p, int slot) {
        return p.contains(prefix(slot) + "code");
    }

    static String desc(KeyEvent event) {
        InputDevice d = event.getDevice();
        return d == null ? "(unknown device)" : d.getName();
    }

    static String descriptor(KeyEvent event) {
        InputDevice d = event.getDevice();
        return d == null ? "" : d.getDescriptor();
    }

    static String details(KeyEvent event) {
        return String.format(Locale.US, "%s | %s (%d), scan %d, device %d, %s",
            desc(event), KeyEvent.keyCodeToString(event.getKeyCode()), event.getKeyCode(),
            event.getScanCode(), event.getDeviceId(), event.getAction() == KeyEvent.ACTION_DOWN ? "DOWN" : "UP");
    }

    static void saveLearn(Context context, int slot, KeyEvent e) {
        SharedPreferences p = pref(context);
        String other = "";
        for (int i = 0; i < SLOTS.length; i++) {
            if (i != slot && isLearned(p, i)
                    && p.getInt(prefix(i) + "code", -1) == e.getKeyCode()
                    && p.getInt(prefix(i) + "scan", -1) == e.getScanCode()
                    && p.getString(prefix(i) + "descriptor", "").equals(descriptor(e))) {
                other = " WARNING: indistinguishable from " + SLOTS[i] + ".";
            }
        }
        p.edit().putInt(prefix(slot) + "code", e.getKeyCode())
            .putInt(prefix(slot) + "scan", e.getScanCode())
            .putString(prefix(slot) + "descriptor", descriptor(e))
            .putString(prefix(slot) + "device", desc(e))
            .remove("learning_slot").remove("learning_deadline")
            .putString("last_event", "Learned " + SLOTS[slot] + ": " + details(e) + other)
            .apply();
    }

    static int match(SharedPreferences p, KeyEvent e) {
        String device = descriptor(e);
        if (device.isEmpty()) return -1; // Never remap unknown-source keys.
        for (int i = 0; i < SLOTS.length; i++) {
            String k = prefix(i);
            if (isLearned(p, i)
                    && p.getInt(k + "code", -1) == e.getKeyCode()
                    && p.getInt(k + "scan", -1) == e.getScanCode()
                    && p.getString(k + "descriptor", "").equals(device)) return i;
        }
        return -1;
    }
}
