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
public final class MappingService extends AccessibilityService {
    static volatile MappingService instance;
    private String foregroundPackage = "";
    private boolean screenReceiverRegistered;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long screenOffAt;
    private View transitionGuard;
    private final Runnable clearTransitionGuard = this::removeTransitionGuard;

    private void showTransitionGuard(long timeoutMillis) {
        handler.removeCallbacks(clearTransitionGuard);
        if (transitionGuard == null) {
            if (windowManager == null) windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
            View shield = new View(this);
            shield.setBackgroundColor(0xff000000);
            shield.setOnTouchListener((view, event) -> true);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            try { windowManager.addView(shield, params); transitionGuard = shield; }
            catch (RuntimeException error) { BetaLog.add(this, "Transition guard unavailable: " + error.getClass().getSimpleName()); }
        }
        // Always remove the temporary input shield, even if Camera cannot launch.
        handler.postDelayed(clearTransitionGuard, timeoutMillis);
    }

    private void removeTransitionGuard() {
        handler.removeCallbacks(clearTransitionGuard);
        if (transitionGuard != null) {
            try { windowManager.removeViewImmediate(transitionGuard); } catch (RuntimeException ignored) { }
            transitionGuard = null;
        }
    }
    private final Runnable wakeAfterScreenSettles = () -> {
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        if (pm != null && pm.isInteractive() && Controls.pref(this).getBoolean("camera_on_wake", false))
            requestWakeCamera("stable screen wake");
        handler.removeCallbacks(clearTransitionGuard);
        handler.postDelayed(clearTransitionGuard, 250);
    };
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) {
                screenOffAt = android.os.SystemClock.elapsedRealtime();
                if (Controls.pref(c).getBoolean("camera_on_wake", false)) showTransitionGuard(1600);
                handler.removeCallbacks(wakeAfterScreenSettles);
                BetaLog.add(c, "Screen off: pending camera wake cancelled");
            } else if (Intent.ACTION_SCREEN_ON.equals(i.getAction()) && Controls.pref(c).getBoolean("camera_on_wake", false)) {
                handler.removeCallbacks(wakeAfterScreenSettles);
                long offMillis = android.os.SystemClock.elapsedRealtime() - screenOffAt;
                if (screenOffAt != 0 && offMillis < 800) {
                    BetaLog.add(c, "Rapid screen bounce after " + offMillis + "ms; restoring screen off");
                    // Lock immediately: waiting half a second exposes the resumed Camera.
                    BetaLog.add(c, "Restore screen off immediately: " + performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN));
                } else {
                    // M1 trails Power with a swipe at ~650 ms and another key at ~1400 ms.
                    showTransitionGuard(2200);
                    handler.postDelayed(wakeAfterScreenSettles, 1600);
                }
            }
            if (System.currentTimeMillis() < Controls.pref(c).getLong("diagnostic_until", 0))
                BetaLog.add(c, "Screen transition: " + i.getAction());
        }
    };
    private WindowManager windowManager;
    private View pickerOverlay;
    private long pointerDiagnosticUntil;
    private final Runnable endPointerDiagnostic = () -> {
        pointerDiagnosticUntil = 0;
        if (Build.VERSION.SDK_INT >= 34) {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info != null) { info.setMotionEventSources(0); setServiceInfo(info); }
        }
        BetaLog.add(this, "Pointer diagnostic ended; native input restored");
    };

    void startPointerDiagnostic() {
        if (Build.VERSION.SDK_INT < 34) { Toast.makeText(this, "Requires Android 14 or newer", Toast.LENGTH_LONG).show(); return; }
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;
        pointerDiagnosticUntil = android.os.SystemClock.elapsedRealtime() + 60000;
        info.setMotionEventSources(InputDevice.SOURCE_MOUSE);
        setServiceInfo(info);
        handler.removeCallbacks(endPointerDiagnostic);
        handler.postDelayed(endPointerDiagnostic, 60000);
        BetaLog.add(this, "Pointer diagnostic started for 60 seconds (native pointer input paused)");
    }

    @Override public void onMotionEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT < 34 || pointerDiagnosticUntil == 0) return;
        if (android.os.SystemClock.elapsedRealtime() >= pointerDiagnosticUntil) { endPointerDiagnostic.run(); return; }
        InputDevice device = event.getDevice();
        if (device == null || !device.getName().startsWith("TELESIN")) return;
        StringBuilder line = new StringBuilder("TELESIN pointer ").append(MotionEvent.actionToString(event.getAction())).append(" source=").append(Integer.toHexString(event.getSource()));
        for (int i=0;i<event.getPointerCount();i++) line.append(" p").append(i).append("=").append(Math.round(event.getX(i))).append(",").append(Math.round(event.getY(i)));
        BetaLog.add(this, line.toString());
    }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        BetaLog.add(this, "Accessibility connected");
        if (!screenReceiverRegistered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            screenReceiverRegistered = true;
        }
        AccessibilityServiceInfo config = getServiceInfo();
        if (config != null) {
            config.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            if (Build.VERSION.SDK_INT >= 34) config.setMotionEventSources(0);
            setServiceInfo(config);
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getPackageName() != null) {
            String name = event.getPackageName().toString();
            if (!name.equals(getPackageName())) foregroundPackage = name;
            if (name.equals(cameraPackage()) && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                BetaLog.add(this, "Camera window observed by Accessibility");
        }
    }

    void refreshNativeControls() {
        AccessibilityServiceInfo config = getServiceInfo();
        if (config == null) return;
        config.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        if (Build.VERSION.SDK_INT >= 34) config.setMotionEventSources(0);
        setServiceInfo(config);
        cancelPendingActions();
    }

    @Override public void onInterrupt() { clearPresses(); if (pointerDiagnosticUntil != 0) endPointerDiagnostic.run(); }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeTransitionGuard();
        clearPresses();
        removePicker();
        if (screenReceiverRegistered) { unregisterReceiver(screenReceiver); screenReceiverRegistered = false; }
        BetaLog.add(this, "Accessibility disconnected");
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
        if (transitionGuard != null && device != null && device.getName().startsWith("TELESIN"))
            return true;
        // Log only while this app is foreground or the user explicitly requested learning.
        int learning = p.getInt("learning_slot", -1);
        boolean labForeground = isLabForeground();
        if (learning >= 0 && (!labForeground || System.currentTimeMillis() >= p.getLong("learning_deadline", 0))) {
            p.edit().remove("learning_slot").remove("learning_deadline").apply();
            learning = -1;
        }
        boolean diagnostic = labForeground;
        if (device != null && device.isExternal() && (diagnostic ||
                System.currentTimeMillis() < p.getLong("diagnostic_until", 0))) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            BetaLog.add(this, Controls.details(event) + " screen=" + pm.isInteractive());
        }
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
        if (pointerDiagnosticUntil != 0) endPointerDiagnostic.run();
        clearPresses();
        handler.removeCallbacksAndMessages(null);
        removeTransitionGuard();
        removePicker();
    }

    private void clearPresses() {
        for (Press press : presses.values()) handler.removeCallbacks(press.timer);
        presses.clear();
    }

    private void runAction(int action, int slot) {
        if (action == Controls.PASS) return;
        BetaLog.add(this, "Mapped " + Controls.SLOTS[slot] + " to " + Controls.ACTIONS[action]);
        if (action == Controls.WAKE_CAMERA) {
            if (isCameraForeground() && ((PowerManager)getSystemService(POWER_SERVICE)).isInteractive()) {
                BetaLog.add(this, "Lock screen: " + performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN));
            } else attemptWakeCamera(slot);
        }
        else if (isCameraForeground()) perform(action, slot);
    }

    private void attemptWakeCamera(int slot) {
        requestWakeCamera(Controls.SLOTS[slot]);
    }

    void scheduleCameraTest() { handler.postDelayed(() -> requestWakeCamera("10-second screen-off test"), 10000); }

    void requestWakeCamera(String source) {
        BetaLog.add(this, "Camera requested by " + source);
        if (source.equals("stable screen wake")) {
            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
            if (pm == null || !pm.isInteractive()) return;
            // Screen is already awake: launch Camera directly, without a visible trampoline.
            android.app.KeyguardManager keyguard = (android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            String action = keyguard != null && keyguard.isKeyguardLocked()
                ? android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
                : android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA;
            Intent camera = new Intent(action);
            String target = cameraPackage();
            if (!target.isEmpty()) camera.setPackage(target);
            if ("com.android.camera".equals(target)) camera.setClassName(target, "com.android.camera.Camera");
            camera.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try { startActivity(camera); BetaLog.add(this, "Camera opened directly on stable wake"); }
            catch (RuntimeException error) { BetaLog.add(this, "Direct camera launch failed: " + error.getClass().getSimpleName()); }
            return;
        }
        PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean interactive = power != null && power.isInteractive();
        SharedPreferences pref = Controls.pref(this);
        pref.edit().putString("wake_status", "Received " + source
            + " at " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())
            + ", screen interactive=" + interactive
            + ". Requesting secure camera; launch may be restricted by HyperOS.").apply();
        Intent intent = new Intent(this, WakeCameraActivity.class);
        intent.putExtra("screen_wake_only", source.equals("stable screen wake"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            startActivity(intent);
        } catch (RuntimeException problem) {
            pref.edit().putString("wake_status", "Wake request failed: "
                    + problem.getClass().getSimpleName()).apply();
        }
    }

    private final GestureResultCallback gestureResult = new GestureResultCallback() {
        @Override public void onCompleted(GestureDescription gesture) { BetaLog.add(MappingService.this, "Gesture completed"); }
        @Override public void onCancelled(GestureDescription gesture) { BetaLog.add(MappingService.this, "Gesture cancelled"); }
    };

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
        if (action == Controls.ZOOM_OUT || action == Controls.ZOOM_IN) {
            boolean inward = action == Controls.ZOOM_OUT;
            float from = display.x * (inward ? .28f : .10f);
            float to = display.x * (inward ? .10f : .28f);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            for (int sign : new int[]{-1, 1}) {
                Path pinch = new Path();
                pinch.moveTo(display.x*.5f + sign*from, display.y*.45f);
                pinch.lineTo(display.x*.5f + sign*to, display.y*.45f);
                builder.addStroke(new GestureDescription.StrokeDescription(pinch, 0, 300));
            }
            BetaLog.add(this, "Zoom gesture accepted=" + dispatchGesture(builder.build(), gestureResult, handler));
            return;
        }
        float x1, x2, y1, y2;
        long duration;
        if (action == Controls.TAP) {
            SharedPreferences p = Controls.pref(this);
            String pre = Controls.prefix(slot);
            if (!p.contains(pre + "x") || !p.contains(pre + "y")) { BetaLog.add(this, "Tap needs calibration: " + Controls.SLOTS[slot]); return; }
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
        BetaLog.add(this, "Camera gesture accepted=" + dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), gestureResult, handler));
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
                    Toast.makeText(MappingService.this, "Open Xiaomi Camera first, then try Pick again.", Toast.LENGTH_LONG).show();
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
