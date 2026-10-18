package org.telesin.buttonlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;

/** The original light Camera Wake layout, with a short guided setup. */
public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status, detail;
    private TextView firmwareState, remoteState, accessibilityState, batteryState, autostartState;
    private Switch wake;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { updateStatus(); handler.postDelayed(this, 1000); }
    };
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private TextView text(LinearLayout parent, String label, int size, int color) {
        TextView view = new TextView(this); view.setText(label); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(3), 0, dp(3)); parent.addView(view); return view;
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout view = column(); view.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(0xda0e3147); bg.setCornerRadius(dp(24)); bg.setStroke(dp(1),0x5579c9db); view.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(8); parent.addView(view, params); return view;
    }
    private RippleDrawable buttonSurface(boolean primary) {
        GradientDrawable shape = primary
            ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(8,116,145),Color.rgb(15,106,153)})
            : new GradientDrawable();
        if (!primary) { shape.setColor(0xcc174963); shape.setStroke(dp(1),0x6686d1e4); }
        shape.setCornerRadius(dp(24));
        GradientDrawable mask = new GradientDrawable(); mask.setColor(Color.WHITE); mask.setCornerRadius(dp(24));
        return new RippleDrawable(ColorStateList.valueOf(primary ? 0x33ffffff : 0x2212737d),shape,mask);
    }
    private void button(LinearLayout parent, String label, Runnable action, boolean primary) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setTextSize(primary ? 17 : 15); view.setMinHeight(dp(48));
        view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); view.setLetterSpacing(.01f);
        view.setTextColor(primary ? Color.WHITE : Color.rgb(221,245,252));
        view.setStateListAnimator(null); view.setElevation(0); view.setBackgroundTintList(null);
        view.setPadding(dp(18),dp(8),dp(18),dp(8)); view.setBackground(buttonSurface(primary));
        if (primary) {
            android.graphics.drawable.Drawable icon = getDrawable(R.drawable.ic_camera_action);
            if (icon != null) { icon.setBounds(0,0,dp(22),dp(22)); view.setCompoundDrawables(icon,null,null,null); view.setCompoundDrawablePadding(dp(10)); }
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(primary ? 8 : 6); parent.addView(view, params);
        view.setOnClickListener(v -> action.run());
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = column(); page.setPadding(dp(16),dp(8),dp(16),dp(8));
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left+dp(16),bars.top+dp(8),bars.right+dp(16),bars.bottom+dp(8)); return insets;
        });
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.rgb(5,38,61));
        View water = new View(this); water.setBackground(new OceanBackground());
        water.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(dp(18),dp(18),android.graphics.Shader.TileMode.CLAMP));
        root.addView(water,new FrameLayout.LayoutParams(-1,-1)); root.addView(page,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root); page.requestApplyInsets();
        page.post(() -> {
            android.view.WindowInsetsController controller = page.getWindowInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                0,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        });
        LinearLayout header = new LinearLayout(this); header.setGravity(android.view.Gravity.CENTER_VERTICAL); page.addView(header);
        TextView title = new TextView(this); title.setText("Camera Wake"); title.setTextSize(26); title.setTextColor(Color.rgb(235,248,255)); title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); title.setLetterSpacing(-.02f);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView version = new TextView(this); version.setText("FREE & OPEN"); version.setTextSize(10); version.setLetterSpacing(.06f); version.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); version.setTextColor(Color.rgb(180,238,246)); version.setPadding(dp(10),dp(6),dp(10),dp(6)); GradientDrawable badge = new GradientDrawable(); badge.setColor(0xaa174c66); badge.setCornerRadius(dp(20)); version.setBackground(badge); header.addView(version);
        LinearLayout hero = card(page);
        LinearLayout.LayoutParams heroSize = new LinearLayout.LayoutParams(-1,0,1); heroSize.topMargin=dp(8); hero.setLayoutParams(heroSize);
        status = text(hero,"Checking setup…",14,Color.rgb(137,231,239)); status.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        status.setPadding(dp(10),dp(6),dp(10),dp(6));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2,-2); statusParams.bottomMargin=dp(2); status.setLayoutParams(statusParams);
        detail = text(hero,"",12,Color.rgb(180,210,223));
        ImageView illustration = new ImageView(this); illustration.setImageResource(R.drawable.controller_reference);
        illustration.setContentDescription("Diving phone case with M1, M2, Capture, W and T buttons");
        illustration.setColorFilter(0xffa9d9e6,android.graphics.PorterDuff.Mode.MULTIPLY);
        illustration.setClipToOutline(true);
        illustration.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) {
                // Outline follows the fitted image, so labels are never center-cropped.
                android.graphics.drawable.Drawable image = illustration.getDrawable();
                if (image == null || view.getWidth() == 0 || view.getHeight() == 0) return;
                float scale = Math.min(view.getWidth()/(float)image.getIntrinsicWidth(),view.getHeight()/(float)image.getIntrinsicHeight());
                int width = Math.round(image.getIntrinsicWidth()*scale), height = Math.round(image.getIntrinsicHeight()*scale);
                int left=(view.getWidth()-width)/2, top=(view.getHeight()-height)/2;
                outline.setRoundRect(left,top,left+width,top+height,dp(18));
            }
        });
        illustration.setScaleType(ImageView.ScaleType.FIT_CENTER); hero.addView(illustration,new LinearLayout.LayoutParams(-1,0,1));
        wake = new Switch(this); wake.setText("Wake with M1"); wake.setTextSize(17); wake.setTextColor(Color.rgb(235,248,255)); wake.setMinHeight(dp(48));
        wake.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Color.WHITE,Color.rgb(119,145,153)}));
        wake.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Color.rgb(24,176,192),Color.rgb(64,95,117)}));
        android.graphics.drawable.StateListDrawable tracks = new android.graphics.drawable.StateListDrawable();
        GradientDrawable trackOn = new GradientDrawable(); trackOn.setColor(Color.rgb(24,176,192)); trackOn.setCornerRadius(dp(14)); trackOn.setSize(dp(46),dp(26));
        GradientDrawable trackOff = new GradientDrawable(); trackOff.setColor(Color.rgb(64,95,117)); trackOff.setCornerRadius(dp(14)); trackOff.setSize(dp(46),dp(26));
        tracks.addState(new int[]{android.R.attr.state_checked},trackOn); tracks.addState(new int[]{},trackOff); wake.setTrackTintList(null); wake.setTrackDrawable(tracks);
        wake.setSwitchPadding(dp(12)); wake.setThumbTextPadding(dp(8));
        wake.setChecked(Controls.enabled(this)); hero.addView(wake);
        wake.setOnCheckedChangeListener((view, enabled) -> {
            Controls.pref(this).edit().putBoolean("camera_on_wake",enabled).remove("status_message").apply();
            if (WakeService.instance != null) WakeService.instance.cancelTransitions();
            updateStatus();
        });
        text(hero,"Hold M1 to wake or sleep. Allow two seconds between actions.\nAlso opens Camera on phone Power; switch off after case use.",12,Color.rgb(180,210,223));
        button(hero,"Open Camera",() -> { if (!CameraLauncher.open(this)) toast("No compatible Camera app found."); },true);
        LinearLayout setup = card(page);
        Button setupToggle = new Button(this); setupToggle.setAllCaps(false); setupToggle.setTextSize(15); setupToggle.setText("Setup & firmware  ▾"); setupToggle.setMinHeight(dp(48)); setupToggle.setTextColor(Color.rgb(137,231,239)); setupToggle.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START); setupToggle.setPadding(dp(4),0,dp(4),0); setupToggle.setBackground(buttonSurface(false)); setupToggle.setStateListAnimator(null); setupToggle.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); setupToggle.setPadding(dp(14),0,dp(14),0);
        setup.removeAllViews(); setup.addView(setupToggle);
        LinearLayout setupBody = column(); setupBody.setPadding(dp(20),dp(8),dp(20),dp(16));
        setupToggle.setText("Setup & firmware  ›");
        setupToggle.setOnClickListener(v -> {
            updateStatus();
            if (setupBody.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup)setupBody.getParent()).removeView(setupBody);
            ScrollView setupScroll = new ScrollView(this); setupScroll.addView(setupBody);
            AlertDialog dialog = new AlertDialog.Builder(this,android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Setup & firmware").setView(setupScroll).setPositiveButton("Done",null).create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(137,231,239));
            if (dialog.getWindow() != null) {
                GradientDrawable sheet = new GradientDrawable(); sheet.setColor(Color.rgb(10,43,63)); sheet.setCornerRadius(dp(24)); sheet.setStroke(dp(1),0x5579c9db);
                dialog.getWindow().setBackgroundDrawable(sheet);
            }
        });
        text(setupBody,"1  Update the case in ReefColor: tested V5.7 → V5.9.\n2  Forget TELESIN_SDC in Bluetooth settings, then pair again.\n3  Allow Accessibility and unrestricted battery use below.",16,Color.rgb(235,248,255));
        firmwareState = text(setupBody,"",14,Color.WHITE);
        button(setupBody,"Firmware & pairing guide",() -> openUrl("https://github.com/shon3l/camera-wake/blob/main/docs/FIRMWARE.md"),false);
        button(setupBody,"Confirm firmware & pairing",() -> new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Have you checked both steps?")
            .setMessage("Verify V5.9 for the Diving Phone Case in ReefColor, then forget and re-pair TELESIN_SDC in Bluetooth settings. Camera Wake cannot read the firmware version. This marks your own confirmation, not an automatic check.")
            .setPositiveButton("I checked both",(dialog,which) -> { Controls.pref(this).edit().putBoolean("firmware_confirmed",true).apply(); updateStatus(); })
            .setNeutralButton("Clear confirmation",(dialog,which) -> { Controls.pref(this).edit().putBoolean("firmware_confirmed",false).apply(); updateStatus(); })
            .setNegativeButton("Cancel",null).show(),false);
        remoteState = text(setupBody,"",14,Color.WHITE);
        accessibilityState = text(setupBody,"",14,Color.WHITE);
        button(setupBody,"Enable Accessibility",() -> new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Allow Camera Wake")
            .setMessage("Camera Wake uses Accessibility to open secure Camera, cover the remote’s trailing touch gestures and turn the screen off. It does not read screen content. Normal remote controls work without this permission.\n\nEnable Camera Wake in the next screen. If Android blocks it, allow restricted settings in the app’s system settings first.")
            .setPositiveButton("Open settings",(dialog,which) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
            .setNegativeButton("Cancel",null).show(),false);
        batteryState = text(setupBody,"",14,Color.WHITE);
        button(setupBody,"Allow unrestricted battery use",() -> {
            if (Controls.batteryReady(this)) toast("Unrestricted battery use is already enabled.");
            else try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName()))); }
            catch (RuntimeException missing) { appSettings(); }
        },false);
        autostartState = text(setupBody,"",14,Color.WHITE);
        button(setupBody,"App settings / Xiaomi autostart",this::appSettings,false);
        text(setupBody,"After a reboot, unlock once with your PIN and let the remote reconnect. No computer or startup script is needed. Tested with Xiaomi 17 Ultra; other phones are unverified.",14,Color.rgb(180,210,223));
        TextView footer = text(page,"Free · No ads or tracking     Privacy & source ↗",12,Color.rgb(180,210,223));
        footer.setMinHeight(dp(40)); footer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        footer.setOnClickListener(v -> openUrl("https://github.com/shon3l/camera-wake"));
    }
    private void settingStatus(TextView view, boolean confirmed, String message) {
        if (view == null) return;
        String label = (confirmed ? "✓  " : "!  ") + message;
        if (label.contentEquals(view.getText())) return;
        view.setText(label);
        view.setTextColor(confirmed ? Color.rgb(141,245,218) : Color.rgb(255,214,142));
        view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        view.setPadding(dp(10),dp(8),dp(10),dp(8));
        GradientDrawable badge = new GradientDrawable(); badge.setColor(confirmed ? 0xff164f58 : 0xff4c4436); badge.setCornerRadius(dp(12)); view.setBackground(badge);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2); params.topMargin=dp(10); view.setLayoutParams(params);
    }
    private void updateStatus() {
        if (status == null) return;
        boolean service = WakeService.instance != null;
        boolean battery = Controls.batteryReady(this);
        boolean remote = Controls.remoteConnected();
        boolean firmware = Controls.pref(this).getBoolean("firmware_confirmed",false);
        settingStatus(firmwareState,firmware,firmware ? "Firmware & pairing · confirmed by you" : "Firmware & pairing · manual check needed");
        settingStatus(remoteState,remote,remote ? "TELESIN · connected" : "TELESIN · not connected");
        settingStatus(accessibilityState,service,service ? "Accessibility · connected" : "Accessibility · not enabled / not connected");
        settingStatus(batteryState,battery,battery ? "Battery · unrestricted" : "Battery · unrestricted use not enabled");
        settingStatus(autostartState,false,"Xiaomi autostart · check in system settings");
        status.setText(!service ? "Setup needed · Accessibility" : !Controls.enabled(this) ? "Camera wake paused" : !Controls.remoteConnected() ? "Waiting for TELESIN" : "Ready · TELESIN connected");
        boolean ready = service && Controls.enabled(this) && Controls.remoteConnected();
        GradientDrawable state = new GradientDrawable(); state.setColor(ready ? 0xff164f58 : 0xff25465e); state.setCornerRadius(dp(16)); status.setBackground(state);
        status.setTextColor(ready ? Color.rgb(141,245,218) : Color.rgb(206,230,245));
        String error = Controls.pref(this).getString("status_message", "");
        detail.setText(!error.isEmpty() ? error : !service ? "Enable Camera Wake in Accessibility settings below." : !Controls.batteryReady(this) ? "Allow unrestricted battery use for reliable background operation." : Controls.enabled(this) ? "Your phone stays locked in secure Camera." : "Normal remote camera controls still work.");
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private void appSettings() { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))); }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); } catch (RuntimeException missing) { toast("Open github.com/shon3l/camera-wake in a browser."); } }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(refresh); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
