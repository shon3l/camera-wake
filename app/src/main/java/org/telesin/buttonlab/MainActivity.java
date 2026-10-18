package org.telesin.buttonlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(20)); view.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(8); parent.addView(view, params); return view;
    }
    private void button(LinearLayout parent, String label, Runnable action, boolean primary) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setTextSize(16); view.setMinHeight(dp(48));
        view.setTextColor(primary ? Color.WHITE : Color.rgb(27, 63, 75));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(primary ? Color.rgb(25, 100, 113) : Color.rgb(232, 241, 244)); bg.setCornerRadius(dp(14));
        view.setBackground(bg); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(5); parent.addView(view, params);
        view.setOnClickListener(v -> action.run());
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(244,247,249));
        LinearLayout page = column(); page.setPadding(dp(16),dp(8),dp(16),dp(12)); scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        setContentView(scroll); scroll.requestApplyInsets();
        LinearLayout header = new LinearLayout(this); header.setGravity(android.view.Gravity.CENTER_VERTICAL); page.addView(header);
        TextView title = new TextView(this); title.setText("Camera Wake"); title.setTextSize(26); title.setTextColor(Color.rgb(27,43,54)); title.setTypeface(null,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView version = new TextView(this); version.setText("3.0 · FREE"); version.setTextSize(12); version.setTextColor(Color.rgb(25,100,113)); header.addView(version);
        LinearLayout hero = card(page);
        status = text(hero,"Checking setup…",16,Color.rgb(25,100,113)); status.setTypeface(null,Typeface.BOLD);
        detail = text(hero,"",12,Color.rgb(92,109,119));
        ImageView illustration = new ImageView(this); illustration.setImageResource(R.drawable.controller_reference);
        illustration.setContentDescription("Diving phone case with M1, M2, Capture, W and T buttons");
        illustration.setScaleType(ImageView.ScaleType.FIT_CENTER); illustration.setAdjustViewBounds(true); hero.addView(illustration,new LinearLayout.LayoutParams(-1,-2));
        wake = new Switch(this); wake.setText("Wake with M1"); wake.setTextSize(17); wake.setTextColor(Color.rgb(27,43,54)); wake.setMinHeight(dp(48));
        wake.setChecked(Controls.enabled(this)); hero.addView(wake);
        wake.setOnCheckedChangeListener((view, enabled) -> {
            Controls.pref(this).edit().putBoolean("camera_on_wake",enabled).remove("status_message").apply();
            if (WakeService.instance != null) WakeService.instance.cancelTransitions();
            updateStatus();
        });
        text(hero,"Hold M1 to wake or sleep. Allow two seconds between actions.\nAlso opens Camera on phone Power; switch off after case use.",12,Color.rgb(92,109,119));
        button(hero,"Open Camera",() -> { if (!CameraLauncher.open(this)) toast("No compatible Camera app found."); },true);
        LinearLayout setup = card(page);
        Button setupToggle = new Button(this); setupToggle.setAllCaps(false); setupToggle.setTextSize(15); setupToggle.setText("Setup & firmware  ▾"); setupToggle.setMinHeight(dp(48)); setupToggle.setTextColor(Color.rgb(25,100,113)); setupToggle.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START); setupToggle.setPadding(dp(4),0,dp(4),0); setupToggle.setBackgroundColor(Color.TRANSPARENT);
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
        String error = Controls.pref(this).getString("status_message", "");
        detail.setText(!error.isEmpty() ? error : !service ? "Enable Camera Wake in Accessibility settings below." : !Controls.batteryReady(this) ? "Allow unrestricted battery use for reliable background operation." : Controls.enabled(this) ? "No ADB activation · native camera controls" : "Normal remote camera controls still work.");
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private void appSettings() { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))); }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); } catch (RuntimeException missing) { toast("Open github.com/shon3l/camera-wake in a browser."); } }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(refresh); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
