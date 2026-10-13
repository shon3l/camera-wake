package org.telesin.buttonlab;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

/** Wake-only listener: no key filtering, window scraping or gesture injection. */
public final class ButtonService extends AccessibilityService {
    static volatile ButtonService instance;
    private boolean registered;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_SCREEN_ON.equals(intent.getAction())
                    || !Controls.pref(ButtonService.this).getBoolean("camera_on_wake", false)) return;
            if (!Controls.batteryReady(ButtonService.this)) {
                Controls.pref(ButtonService.this).edit().putString("wake_status", "Paused: disable battery optimization").apply();
                return;
            }
            Controls.pref(ButtonService.this).edit().putString("wake_status", "Screen woke; requesting secure camera").apply();
            Intent camera = new Intent(ButtonService.this, WakeCameraActivity.class);
            camera.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            try { startActivity(camera); }
            catch (RuntimeException failure) {
                Controls.pref(ButtonService.this).edit().putString("wake_status", "Camera request failed: " + failure.getClass().getSimpleName()).apply();
            }
        }
    };
    @Override public void onServiceConnected() {
        instance = this;
        if (!registered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(receiver, filter);
            registered = true;
        }
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        if (registered) unregisterReceiver(receiver);
        instance = null;
        super.onDestroy();
    }
    void cancelPendingActions() {
        if (MappingService.instance != null) MappingService.instance.cancelPendingActions();
    }
    void refreshNativeControls() {
        if (MappingService.instance != null) MappingService.instance.refreshNativeControls();
    }
}
