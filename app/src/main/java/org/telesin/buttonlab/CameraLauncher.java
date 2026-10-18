package org.telesin.buttonlab;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;

final class CameraLauncher {
    private CameraLauncher() { }
    static boolean open(Context context) {
        KeyguardManager keyguard = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
        String action = keyguard != null && keyguard.isKeyguardLocked()
            ? MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE : MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA;
        Intent camera = new Intent(action);
        // The tested Xiaomi has multiple secure handlers; select its main Camera.
        if (android.os.Build.MANUFACTURER.equalsIgnoreCase("xiaomi"))
            camera.setClassName("com.android.camera", "com.android.camera.Camera");
        camera.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try { context.startActivity(camera); return true; }
        catch (RuntimeException error) { return false; }
    }
}
