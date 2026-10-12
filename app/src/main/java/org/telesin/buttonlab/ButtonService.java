package org.telesin.buttonlab;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Intercepts ordinary Android key events using user-enabled Accessibility.
 * No Bluetooth protocol writing and no device firmware reprogramming.
 */
public final class ButtonService extends AccessibilityService {
    static volatile ButtonService instance;
    private String foregroundPackage = "";
    private WindowManager windowManager;
    private View pickerOverlay;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean screenReceiverRegistered;
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())
                    && Controls.pref(ButtonService.this).getBoolean("camera_on_wake", false)) {
                if (!Controls.batteryReady(ButtonService.this)) {
                    Controls.pref(ButtonService.this).edit().putString("wake_status",
                        "Paused: disable battery optimization in required setup.").apply();
                    return;
                }
                requestWakeCamera("Screen wake");
            }
        }
    };

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        if (!screenReceiverRegistered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(screenReceiver, filter);
            screenReceiverRegistered = true;
        }
        AccessibilityServiceInfo config = getServiceInfo();
        if (config != null) {
            config.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(config);
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getPackageName() != null) {
            String name = event.getPackageName().toString();
            if (!name.equals(getPackageName())) foregroundPackage = name;
        }
    }

    @Override public void onInterrupt() { clearPresses(); }

    @Override public void onDestroy() {
        clearPresses();
        removePicker();
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver);
            screenReceiverRegistered = false;
        }
        instance = null;
        super.onDestroy();
    }

    private String cameraPackage() {
        return Controls.pref(this).getString("camera_package", "com.android.camera").trim();
    }

    private boolean isCameraForeground() {
        String expected = cameraPackage();
        if (expected.isEmpty()) return false;
        try {
            if (getRootInActiveWindow() != null && getRootInActiveWindow().getPackageName() != null) {
                String active = getRootInActiveWindow().getPackageName().toString();
                return expected.equals(active);
            }
        } catch (Exception ignored) { }
        return expected.equals(foregroundPackage);
    }

    private boolean isLabForeground() {
        try {
            if (getRootInActiveWindow() != null && getRootInActiveWindow().getPackageName() != null) {
                return getPackageName().equals(getRootInActiveWindow().getPackageName().toString());
            }
        } catch (Exception ignored) { }
        return false;
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        SharedPreferences p = Controls.pref(this);
        InputDevice device = event.getDevice();
        // Log only while this app is foreground or the user explicitly requested learning.
        int learning = p.getInt("learning_slot", -1);
        boolean diagnostic = learning >= 0 || isLabForeground();
        if (diagnostic && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0
                && device != null && device.isExternal()) {
            p.edit().putString("last_event", Controls.details(event)).apply();
            if (learning >= 0 && learning < Controls.SLOTS.length) {
                Controls.saveLearn(this, learning, event);
                return true;
            }
        }
        int slot = Controls.match(p, event);
        if (slot < 0) return false;
        String identity = event.getDeviceId() + ":" + event.getKeyCode() + ":" + event.getScanCode();
        Press pending = presses.get(identity);
        if (pending != null) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(pending.timer);
                presses.remove(identity);
                if (!event.isCanceled() && !pending.longFired) {
                    if (event.getEventTime() - pending.downTime >= pending.holdMillis) {
                        runAction(pending.longAction, pending.slot);
                    } else {
                        runAction(pending.shortAction, pending.slot);
                    }
                }
            }
            return pending.consume;
        }
        int action = p.getInt(Controls.prefix(slot) + "action", Controls.PASS);
        int longAction = p.getInt(Controls.prefix(slot) + "long_action", Controls.PASS);
        if (longAction != Controls.PASS && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0
                && (longAction == Controls.WAKE_CAMERA || action == Controls.WAKE_CAMERA || isCameraForeground())) {
            Press press = new Press();
            press.slot = slot;
            press.shortAction = action;
            press.longAction = longAction;
            press.downTime = event.getEventTime();
            press.holdMillis = p.getInt("hold_millis", 700);
            // Passing through keeps native short behavior; it can also react during a hold.
            press.consume = action != Controls.PASS;
            press.timer = () -> {
                if (presses.get(identity) == press) {
                    press.longFired = true;
                    runAction(press.longAction, press.slot);
                }
            };
            presses.put(identity, press);
            handler.postDelayed(press.timer, press.holdMillis);
            return press.consume;
        }
        if (action == Controls.PASS) return false;
        if (action != Controls.WAKE_CAMERA && !isCameraForeground()) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            runAction(action, slot);
        }
        return true;
    }

    private static final class Press {
        int slot, shortAction, longAction, holdMillis;
        long downTime;
        boolean longFired, consume;
        Runnable timer;
    }
    private final java.util.Map<String, Press> presses = new java.util.HashMap<>();

    void cancelPendingActions() {
        clearPresses();
        handler.removeCallbacksAndMessages(null);
        removePicker();
    }

    private void clearPresses() {
        for (Press press : presses.values()) handler.removeCallbacks(press.timer);
        presses.clear();
    }

    private void runAction(int action, int slot) {
        if (action == Controls.PASS) return;
        if (action == Controls.WAKE_CAMERA) attemptWakeCamera(slot);
        else if (isCameraForeground()) perform(action, slot);
    }

    private void attemptWakeCamera(int slot) {
        requestWakeCamera(Controls.SLOTS[slot]);
    }

    private void requestWakeCamera(String source) {
        PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean interactive = power != null && power.isInteractive();
        SharedPreferences pref = Controls.pref(this);
        pref.edit().putString("wake_status", "Received " + source
            + " at " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())
            + ", screen interactive=" + interactive
            + ". Requesting secure camera; launch may be restricted by HyperOS.").apply();
        Intent intent = new Intent(this, WakeCameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            startActivity(intent);
        } catch (RuntimeException problem) {
            pref.edit().putString("wake_status", "Wake request failed: "
                    + problem.getClass().getSimpleName()).apply();
        }
    }

    private Point screen() {
        if (windowManager == null) windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point result = new Point();
        // getRealSize reports the full display, including navigation/system bars.
        windowManager.getDefaultDisplay().getRealSize(result);
        return result;
    }

    private void perform(int action, int slot) {
        if (action == Controls.IGNORE) return;
        Point display = screen();
        if (display.x <= 0 || display.y <= 0) return;
        float x1, x2, y1, y2;
        long duration;
        if (action == Controls.TAP) {
            SharedPreferences p = Controls.pref(this);
            String pre = Controls.prefix(slot);
            if (!p.contains(pre + "x") || !p.contains(pre + "y")) return;
            // Camera UI targets are orientation-specific. Avoid mis-taps after rotation.
            int orientation = getResources().getConfiguration().orientation;
            if (p.getInt(pre + "orientation", orientation) != orientation) return;
            x1 = x2 = display.x * p.getFloat(pre + "x", 0.5f);
            y1 = y2 = display.y * p.getFloat(pre + "y", 0.5f);
            duration = 65;
        } else {
            x1 = x2 = display.x * 0.5f;
            y1 = y2 = display.y * 0.5f;
            switch (action) {
                case Controls.LEFT: x1 = display.x * 0.68f; x2 = display.x * 0.32f; break;
                case Controls.RIGHT: x1 = display.x * 0.32f; x2 = display.x * 0.68f; break;
                case Controls.UP: y1 = display.y * 0.68f; y2 = display.y * 0.32f; break;
                case Controls.DOWN: y1 = display.y * 0.32f; y2 = display.y * 0.68f; break;
                default: return;
            }
            duration = 260;
        }
        Path path = new Path();
        path.moveTo(x1, y1);
        if (action != Controls.TAP) path.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    /** Overlay allows calibrating actual Xiaomi Camera coordinates rather than guessing them. */
    void pickTapPosition(int slot) {
        if (slot < 0 || slot >= Controls.SLOTS.length) return;
        handler.postDelayed(new Runnable() {
            int attempts = 0;
            @Override public void run() {
                if (isCameraForeground()) {
                    showPicker(slot);
                } else if (++attempts <= 14) {
                    handler.postDelayed(this, 400);
                } else {
                    Toast.makeText(ButtonService.this, "Open Xiaomi Camera first, then try Pick again.", Toast.LENGTH_LONG).show();
                }
            }
        }, 350);
    }

    private void showPicker(int slot) {
        removePicker();
        if (!isCameraForeground()) {
            Toast.makeText(this, "Open Xiaomi Camera first, then try Pick again.", Toast.LENGTH_LONG).show();
            return;
        }
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        FrameLayout panel = new FrameLayout(this);
        panel.setBackgroundColor(0x22000000);
        TextView instructions = new TextView(this);
        instructions.setText("TELESIN Button Lab: Tap the CAMERA control you want this button to activate.");
        instructions.setTextColor(0xffffffff);
        instructions.setTextSize(17);
        instructions.setPadding(18, 25, 130, 25);
        instructions.setBackgroundColor(0xcc111111);
        panel.addView(instructions, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        Button cancel = new Button(this);
        cancel.setText("Cancel");
        FrameLayout.LayoutParams cancelLayout = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        panel.addView(cancel, cancelLayout);
        cancel.setOnClickListener(v -> removePicker());
        panel.setOnTouchListener((v, touch) -> {
            if (touch.getAction() != MotionEvent.ACTION_DOWN) return true;
            Point size = screen();
            if (size.x == 0 || size.y == 0) return true;
            float x = Math.max(0f, Math.min(1f, touch.getRawX() / size.x));
            float y = Math.max(0f, Math.min(1f, touch.getRawY() / size.y));
            String pre = Controls.prefix(slot);
            Controls.pref(this).edit().putFloat(pre + "x", x).putFloat(pre + "y", y)
                .putInt(pre + "orientation", getResources().getConfiguration().orientation)
                .putString("last_event", "Calibrated " + Controls.SLOTS[slot] + " tap at " + Math.round(x * 100) + "%, " + Math.round(y * 100) + "%")
                .apply();
            removePicker();
            Toast.makeText(this, "Tap saved for " + Controls.SLOTS[slot], Toast.LENGTH_SHORT).show();
            Intent app = new Intent(this, MainActivity.class);
            app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(app);
            return true;
        });
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        try {
            windowManager.addView(panel, params);
            pickerOverlay = panel;
        } catch (Exception error) {
            Toast.makeText(this, "Overlay unavailable: " + error.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void removePicker() {
        if (pickerOverlay != null && windowManager != null) {
            try { windowManager.removeView(pickerOverlay); } catch (Exception ignored) { }
            pickerOverlay = null;
        }
    }
}
