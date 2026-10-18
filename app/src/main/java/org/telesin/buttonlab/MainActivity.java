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
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(253,254,255)); bg.setCornerRadius(dp(24)); bg.setStroke(dp(1),Color.rgb(218,232,235)); view.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(8); parent.addView(view, params); return view;
    }
    private RippleDrawable buttonSurface(boolean primary) {
        GradientDrawable shape = primary
            ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(18,115,125),Color.rgb(20,80,98)})
            : new GradientDrawable();
        if (!primary) { shape.setColor(Color.rgb(232,243,245)); shape.setStroke(dp(1),Color.rgb(209,230,234)); }
        shape.setCornerRadius(dp(24));
        GradientDrawable mask = new GradientDrawable(); mask.setColor(Color.WHITE); mask.setCornerRadius(dp(24));
        return new RippleDrawable(ColorStateList.valueOf(primary ? 0x33ffffff : 0x2212737d),shape,mask);
    }
    private void button(LinearLayout parent, String label, Runnable action, boolean primary) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setTextSize(primary ? 17 : 15); view.setMinHeight(dp(48));
        view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); view.setLetterSpacing(.01f);
        view.setTextColor(primary ? Color.WHITE : Color.rgb(27,79,89));
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
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.rgb(225,241,245),Color.rgb(245,248,247)}));
        LinearLayout page = column(); page.setPadding(dp(16),dp(8),dp(16),dp(12)); scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        setContentView(scroll); scroll.requestApplyInsets();
        scroll.post(() -> {
            android.view.WindowInsetsController controller = scroll.getWindowInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        });
        LinearLayout header = new LinearLayout(this); header.setGravity(android.view.Gravity.CENTER_VERTICAL); page.addView(header);
        TextView title = new TextView(this); title.setText("Camera Wake"); title.setTextSize(26); title.setTextColor(Color.rgb(27,43,54)); title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); title.setLetterSpacing(-.02f);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView version = new TextView(this); version.setText("FREE & OPEN"); version.setTextSize(10); version.setLetterSpacing(.06f); version.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); version.setTextColor(Color.rgb(25,90,102)); version.setPadding(dp(10),dp(6),dp(10),dp(6)); GradientDrawable badge = new GradientDrawable(); badge.setColor(Color.rgb(207,229,234)); badge.setCornerRadius(dp(20)); version.setBackground(badge); header.addView(version);
        LinearLayout hero = card(page);
        status = text(hero,"Checking setup…",14,Color.rgb(25,100,113)); status.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        status.setPadding(dp(10),dp(6),dp(10),dp(6));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2,-2); statusParams.bottomMargin=dp(2); status.setLayoutParams(statusParams);
        detail = text(hero,"",12,Color.rgb(92,109,119));
        ImageView illustration = new ImageView(this); illustration.setImageResource(R.drawable.controller_reference);
        illustration.setContentDescription("Diving phone case with M1, M2, Capture, W and T buttons");
        illustration.setScaleType(ImageView.ScaleType.FIT_CENTER); illustration.setAdjustViewBounds(true); hero.addView(illustration,new LinearLayout.LayoutParams(-1,-2));
        wake = new Switch(this); wake.setText("Wake with M1"); wake.setTextSize(17); wake.setTextColor(Color.rgb(27,43,54)); wake.setMinHeight(dp(48));
        wake.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Color.WHITE,Color.rgb(119,145,153)}));
        wake.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Color.rgb(25,110,122),Color.rgb(218,230,234)}));
        android.graphics.drawable.StateListDrawable tracks = new android.graphics.drawable.StateListDrawable();
        GradientDrawable trackOn = new GradientDrawable(); trackOn.setColor(Color.rgb(25,110,122)); trackOn.setCornerRadius(dp(14)); trackOn.setSize(dp(46),dp(26));
        GradientDrawable trackOff = new GradientDrawable(); trackOff.setColor(Color.rgb(216,228,233)); trackOff.setCornerRadius(dp(14)); trackOff.setSize(dp(46),dp(26));
        tracks.addState(new int[]{android.R.attr.state_checked},trackOn); tracks.addState(new int[]{},trackOff); wake.setTrackTintList(null); wake.setTrackDrawable(tracks);
        wake.setSwitchPadding(dp(12)); wake.setThumbTextPadding(dp(8));
        wake.setChecked(Controls.enabled(this)); hero.addView(wake);
        wake.setOnCheckedChangeListener((view, enabled) -> {
            Controls.pref(this).edit().putBoolean("camera_on_wake",enabled).remove("status_message").apply();
            if (WakeService.instance != null) WakeService.instance.cancelTransitions();
            updateStatus();
        });
        text(hero,"Hold M1 to wake or sleep. Allow two seconds between actions.\nAlso opens Camera on phone Power; switch off after case use.",12,Color.rgb(92,109,119));
        button(hero,"Open Camera",() -> { if (!CameraLauncher.open(this)) toast("No compatible Camera app found."); },true);
        LinearLayout setup = card(page);
        Button setupToggle = new Button(this); setupToggle.setAllCaps(false); setupToggle.setTextSize(15); setupToggle.setText("Setup & firmware  ▾"); setupToggle.setMinHeight(dp(48)); setupToggle.setTextColor(Color.rgb(25,100,113)); setupToggle.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START); setupToggle.setPadding(dp(4),0,dp(4),0); setupToggle.setBackground(buttonSurface(false)); setupToggle.setStateListAnimator(null); setupToggle.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); setupToggle.setPadding(dp(14),0,dp(14),0);
        setup.removeAllViews(); setup.addView(setupToggle);
        LinearLayout setupBody = column(); setup.addView(setupBody);
        setupBody.setVisibility(WakeService.instance == null ? View.VISIBLE : View.GONE);
        setupToggle.setText(WakeService.instance == null ? "Setup & firmware  ▴" : "Setup & firmware  ▾");
        setupToggle.setOnClickListener(v -> { boolean opening = setupBody.getVisibility() != View.VISIBLE; setupBody.setVisibility(opening ? View.VISIBLE : View.GONE); setupToggle.setText(opening ? "Setup & firmware  ▴" : "Setup & firmware  ▾"); });
        text(setupBody,"1  Update the case in ReefColor: tested V5.7 → V5.9.\n2  Forget TELESIN_SDC in Bluetooth settings, then pair again.\n3  Allow Accessibility and unrestricted battery use below.",16,Color.rgb(27,43,54));
        button(setupBody,"Firmware & pairing guide",() -> openUrl("https://github.com/shon3l/camera-wake/blob/main/docs/FIRMWARE.md"),false);
        button(setupBody,"Enable Accessibility",() -> new AlertDialog.Builder(this).setTitle("Allow Camera Wake")
            .setMessage("Camera Wake uses Accessibility to open secure Camera, cover the remote’s trailing touch gestures and turn the screen off. It does not read screen content. Normal remote controls work without this permission.\n\nEnable Camera Wake in the next screen. If Android blocks it, allow restricted settings in the app’s system settings first.")
            .setPositiveButton("Open settings",(dialog,which) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
            .setNegativeButton("Cancel",null).show(),false);
        button(setupBody,"Allow unrestricted battery use",() -> {
            if (Controls.batteryReady(this)) toast("Unrestricted battery use is already enabled.");
            else try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName()))); }
            catch (RuntimeException missing) { appSettings(); }
        },false);
        button(setupBody,"App settings / Xiaomi autostart",this::appSettings,false);
        text(setupBody,"After a reboot, unlock once with your PIN and let the remote reconnect. No computer or startup script is needed. Tested with Xiaomi 17 Ultra; other phones are unverified.",14,Color.rgb(92,109,119));
        TextView footer = text(page,"Free · No ads or tracking     Privacy & source ↗",12,Color.rgb(92,109,119));
        footer.setMinHeight(dp(40)); footer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        footer.setOnClickListener(v -> openUrl("https://github.com/shon3l/camera-wake"));
    }
    private void updateStatus() {
        if (status == null) return;
        boolean service = WakeService.instance != null;
        status.setText(!service ? "Setup needed · Accessibility" : !Controls.enabled(this) ? "Camera wake paused" : !Controls.remoteConnected() ? "Waiting for TELESIN" : "Ready · TELESIN connected");
        boolean ready = service && Controls.enabled(this) && Controls.remoteConnected();
        GradientDrawable state = new GradientDrawable(); state.setColor(ready ? Color.rgb(218,241,232) : Color.rgb(232,239,243)); state.setCornerRadius(dp(16)); status.setBackground(state);
        status.setTextColor(ready ? Color.rgb(27,95,74) : Color.rgb(61,88,103));
        String error = Controls.pref(this).getString("status_message", "");
        detail.setText(!error.isEmpty() ? error : !service ? "Enable Camera Wake in Accessibility settings below." : !Controls.batteryReady(this) ? "Allow unrestricted battery use for reliable background operation." : Controls.enabled(this) ? "Your phone stays locked in secure Camera." : "Normal remote camera controls still work.");
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private void appSettings() { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))); }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); } catch (RuntimeException missing) { toast("Open github.com/shon3l/camera-wake in a browser."); } }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(refresh); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
