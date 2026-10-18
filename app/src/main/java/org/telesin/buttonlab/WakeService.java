package org.telesin.buttonlab;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/** Native controls pass through. Only wake/sleep transitions are guarded. */
public final class WakeService extends AccessibilityService {
    static volatile WakeService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WakePolicy policy = new WakePolicy();
    private boolean registered;
    private WindowManager windows;
    private View guard;
    private final Runnable clearGuard = this::removeGuard;
    private final Runnable openCamera = () -> {
        PowerManager power = (PowerManager)getSystemService(POWER_SERVICE);
        if (Controls.enabled(this) && power != null && power.isInteractive()) {
            if (!CameraLauncher.open(this)) report("Camera could not open. Check that a camera app is enabled.");
        }
        handler.removeCallbacks(clearGuard);
        handler.postDelayed(clearGuard, 250);
    };
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!Controls.enabled(WakeService.this)) { cancelTransitions(); return; }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                policy.screenOff(SystemClock.elapsedRealtime());
                handler.removeCallbacks(openCamera);
                showGuard(1600);
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                handler.removeCallbacks(openCamera);
                WakePolicy.Action action = policy.screenOn(true, SystemClock.elapsedRealtime());
                if (action == WakePolicy.Action.RESTORE_SLEEP) {
                    if (!performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)) {
                        report("Screen lock was unavailable. Check Accessibility settings.");
                        removeGuard();
                    }
                } else if (action == WakePolicy.Action.OPEN_CAMERA) {
                    showGuard(2200);
                    handler.postDelayed(openCamera, 1600);
                }
            }
        }
    };
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        if (!registered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            registered = true;
        }
    }
    private void report(String message) { Controls.pref(this).edit().putString("status_message", message).apply(); }
    private void showGuard(long timeout) {
        handler.removeCallbacks(clearGuard);
        if (guard == null) {
            if (windows == null) windows = (WindowManager)getSystemService(WINDOW_SERVICE);
            View shield = new View(this);
            shield.setBackgroundColor(0xff000000);
            shield.setOnTouchListener((view, event) -> true);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            try { windows.addView(shield, params); guard = shield; }
            catch (RuntimeException error) { report("Transition cover unavailable. Camera may briefly flash."); }
        }
        // Fail open: the input guard must never stay indefinitely.
        handler.postDelayed(clearGuard, timeout);
    }
    private void removeGuard() {
        handler.removeCallbacks(clearGuard);
        if (guard != null) {
            try { windows.removeViewImmediate(guard); } catch (RuntimeException ignored) { }
            guard = null;
        }
    }
    void cancelTransitions() {
        handler.removeCallbacks(openCamera);
        removeGuard();
        policy.reset();
    }
    @Override protected boolean onKeyEvent(KeyEvent event) {
        InputDevice device = event.getDevice();
        return guard != null && device != null && device.isExternal() && device.getName().startsWith("TELESIN");
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* No screen content is read. */ }
    @Override public void onInterrupt() { cancelTransitions(); }
    @Override public void onDestroy() {
        cancelTransitions();
        handler.removeCallbacksAndMessages(null);
        if (registered) { unregisterReceiver(receiver); registered = false; }
        instance = null;
        super.onDestroy();
    }
}
