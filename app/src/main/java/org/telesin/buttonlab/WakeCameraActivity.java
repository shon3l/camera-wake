package org.telesin.buttonlab;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Experimental trampoline. It does not unlock the phone or emulate the Power key.
 * It can run ONLY if HyperOS permits our AccessibilityService to launch an activity.
 * Secure-camera intent is sent while the keyguard is locked.
 */
public final class WakeCameraActivity extends Activity {
    private boolean requested;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        BetaLog.add(this, "Wake activity created");
        // Android 9+: request the user-visible camera shortcut to wake/show over lockscreen.
        setShowWhenLocked(true);
        setTurnScreenOn(!getIntent().getBooleanExtra("screen_wake_only", false));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TextView message = new TextView(this);
        message.setText("Opening camera…");
        message.setTextSize(18);
        message.setPadding(36, 40, 36, 40);
        setContentView(message);
    }

    @Override protected void onResume() {
        super.onResume();
        if (requested) return;
        requested = true;
        // Gives the display a chance to begin waking before the secure-camera launch.
        new Handler(Looper.getMainLooper()).postDelayed(this::openSecureCamera, 180);
    }

    private void openSecureCamera() {
        android.os.PowerManager power = (android.os.PowerManager)getSystemService(POWER_SERVICE);
        if (getIntent().getBooleanExtra("screen_wake_only", false) && power != null && !power.isInteractive()) {
            BetaLog.add(this, "Camera wake cancelled: screen turned off");
            finish();
            return;
        }
        SharedPreferences pref = Controls.pref(this);
        KeyguardManager keyguard = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        String action = locked ? MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
                : MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA;
        String target = pref.getString("camera_package", "com.android.camera").trim();
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (!target.isEmpty()) intent.setPackage(target);
        // This Xiaomi exposes two secure-camera handlers; choose its main Camera
        // explicitly to avoid opening a resolver over the locked screen.
        if ("com.android.camera".equals(target)) intent.setClassName(target, "com.android.camera.Camera");
        try {
            startActivity(intent);
            BetaLog.add(this, "Camera intent sent locked=" + locked);
            pref.edit().putString("wake_status", "Camera launch intent sent (locked=" + locked
                    + "). Confirm on device whether Camera actually opened.").apply();
        } catch (ActivityNotFoundException missing) {
            // OEM cameras may not expose the requested action under this package.
            try {
                Intent fallback = new Intent(action);
                startActivity(fallback);
                pref.edit().putString("wake_status", "Camera intent sent via default resolver.").apply();
            } catch (RuntimeException second) {
                pref.edit().putString("wake_status", "Camera launch failed: "
                        + second.getClass().getSimpleName()).apply();
            }
        } catch (RuntimeException problem) {
            pref.edit().putString("wake_status", "Camera launch failed: "
                    + problem.getClass().getSimpleName()).apply();
        } finally {
            finish();
        }
    }
}
