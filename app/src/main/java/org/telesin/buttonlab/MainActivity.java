package org.telesin.buttonlab;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.Toast;

/** Deliberately uses the Android framework only; no dependencies, account, Internet or ads. */
public final class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView batteryStatus;
    private TextView readyStatus;
    private Button listenerButton;
    private Button batteryButton;
    private TextView lastKey;
    private final TextView[] slotStatuses = new TextView[Controls.SLOTS.length];
    private final Spinner[] actionSpinners = new Spinner[Controls.SLOTS.length];
    private final Spinner[] longSpinners = new Spinner[Controls.SLOTS.length];
    private android.widget.Switch wakeSwitch;
    private Spinner holdSpinner;
    private EditText cameraPackageField;
    private SharedPreferences prefs;
    private final Runnable ticker = new Runnable() {
        @Override public void run() { updateLabels(); ui.postDelayed(this, 900); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = Controls.pref(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(dp(18), dp(18), dp(18), dp(28));
        layout.setBackgroundColor(0xfff2f5fa);
        getWindow().setStatusBarColor(0xff17244a);
        getWindow().setNavigationBarColor(0xfff2f5fa);
        scroll.setClipToPadding(true);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        layout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(layout);
        setContentView(scroll);

        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.ic_camera_wake);
        logo.setContentDescription("Camera Wake logo");
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        logoParams.setMargins(0, 0, dp(14), 0);
        hero.addView(logo, logoParams);
        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        TextView title = addText(heroText, "Camera Wake");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(0xff17244a);
        addText(heroText, "TELESIN · Xiaomi 17 Ultra");
        hero.addView(heroText, new LinearLayout.LayoutParams(0, -2, 1));
        layout.addView(hero);
        LinearLayout setup = card(layout);
        heading(setup, "Required setup", 19);
        addText(setup, "Pair TELESIN_SDC in Bluetooth, then complete both checks.");
        status = addText(setup, "1 · Accessibility listener");
        listenerButton = button(setup, "Enable listener", () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        batteryStatus = addText(setup, "2 · Battery optimization");
        batteryButton = button(setup, "Disable battery optimization", this::requestBatteryExemption);

        LinearLayout test = card(layout);
        heading(test, "M1 camera shortcut", 19);
        readyStatus = addText(test, "Checking setup…");
        addText(test, "Hold M1 → open camera from screen off.\nPress M1 again → screen off.");
        addText(test, "Other screen wakes can open Camera while enabled. Turn off after case use.");
        wakeSwitch = new android.widget.Switch(this);
        wakeSwitch.setText("Enable M1 camera wake");
        wakeSwitch.setTextSize(14);
        wakeSwitch.setMinHeight(dp(48));
        wakeSwitch.setChecked(prefs.getBoolean("camera_on_wake", false));
        wakeSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked && (ButtonService.instance == null || !Controls.batteryReady(this))) {
                wakeSwitch.setChecked(false);
                Toast.makeText(this, "Complete both required setup steps first", Toast.LENGTH_LONG).show();
                return;
            }
            prefs.edit().putBoolean("camera_on_wake", checked).apply();
        });
        test.addView(wakeSwitch);
        button(test, "Test camera launch now", this::openCameraIntentTest);

        LinearLayout guide = card(layout);
        heading(guide, "Know your buttons", 19);
        android.widget.ImageView controller = new android.widget.ImageView(this);
        controller.setImageResource(R.drawable.controller_reference);
        controller.setAdjustViewBounds(true);
        controller.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        controller.setContentDescription("TELESIN product photo identifying Capture, M1, M2, W and T buttons");
        guide.addView(controller, new LinearLayout.LayoutParams(-1, -2));
        addText(guide, "M1 = ◀M  ·  M2 = M▶  ·  W = wide  ·  T = tele");
        addText(guide, "Product button guide · TELESIN P4-WTP-06");
        LinearLayout diagnostics = expandable(layout, "Diagnostics & resets");
        heading(diagnostics, "Setup help", 19);
        addText(diagnostics, "If HyperOS blocks the listener, allow restricted settings in App info. Also set Battery saver to No restrictions. After an update, check that the listener is still enabled.");
        heading(diagnostics, "Button diagnostics", 19);
        addText(diagnostics, "Press a remote button to check detection. Use Learn in a button card below to assign it.");
        lastKey = addText(diagnostics, "Last event: none");
        button(diagnostics, "Copy diagnostic report", this::copyReport);
        button(diagnostics, "Clear diagnostics", () -> {
            prefs.edit().remove("last_event").remove("wake_status").apply();
            updateLabels();
        });
        button(diagnostics, "Cancel pending Learn", () -> {
            prefs.edit().remove("learning_slot").apply();
            updateLabels();
        });

        LinearLayout mappings = expandable(layout, "Buttons · M1, M2, Capture, W, T");
        button(mappings, "Restore default button actions", () -> new android.app.AlertDialog.Builder(this)
            .setTitle("Restore defaults?")
            .setMessage("Restores native pass-through actions and clears tap positions. Keeps learned keys, M1 camera wake and system permissions.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore defaults", (dialog, which) -> {
                cancelRuntime();
                Controls.restoreDefaults(this, -1);
                syncActionSelectors();
                holdSpinner.setSelection(1);

                cameraPackageField.setText("com.android.camera");
                updateLabels();
                Toast.makeText(this, "Native button defaults restored", Toast.LENGTH_SHORT).show();
            }).show());
        heading(mappings, "Short & long press actions", 19);
        addText(mappings, "PASS keeps native behavior. IGNORE/TAP/SWIPE operate only in Xiaomi Camera. The final action, LAUNCH CAMERA, tries to wake and open the secure camera from anywhere. It works only if Android delivers the external key while screen-off AND HyperOS permits the activity launch. It cannot emulate double Power, unlock the phone, or force these permissions.");
        addText(mappings, "These mappings are for ordinary key events only. M1 camera wake above works separately. Learn a key before selecting actions; some mode buttons send gestures or Power pulses instead.");
        addText(mappings, "Hold duration · applies to all buttons");
        Spinner hold = new Spinner(this);
        holdSpinner = hold;
        hold.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"0.5 seconds", "0.7 seconds", "1 second", "1.5 seconds", "2 seconds"}));
        final int[] durations = {500, 700, 1000, 1500, 2000};
        int selectedHold = 1;
        for (int i = 0; i < durations.length; i++) if (durations[i] == prefs.getInt("hold_millis", 700)) selectedHold = i;
        hold.setSelection(selectedHold);
        hold.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("hold_millis", durations[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        mappings.addView(hold);
        for (int i : Controls.DISPLAY_ORDER) addSlot(mappings, i);

        LinearLayout advanced = expandable(layout, "Camera & calibration settings");
        heading(advanced, "Camera & calibration", 19);
        addText(advanced, "Pick camera control saves a tap position without changing your actions. Select Tap camera control separately for short or long press. Calibrate in the orientation you’ll use.");
        EditText cameraPackage = new EditText(this);
        cameraPackageField = cameraPackage;
        cameraPackage.setSingleLine(true);
        cameraPackage.setText(prefs.getString("camera_package", "com.android.camera"));
        cameraPackage.setHint("Camera package");
        advanced.addView(cameraPackage);
        button(advanced, "Save camera package", () -> {
            String pkg = cameraPackage.getText().toString().trim();
            if (pkg.isEmpty()) return;
            prefs.edit().putString("camera_package", pkg).apply();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        button(advanced, "Open Xiaomi Camera", this::openCamera);
        addText(layout, "Camera Wake 1.1 · No Internet access");
        updateLabels();
    }

    private void addSlot(LinearLayout parent, int idx) {
        LinearLayout layout = card(parent);
        heading(layout, Controls.SLOTS[idx], 17);
        button(layout, "Reset " + Controls.SLOTS[idx] + "…", () -> new android.app.AlertDialog.Builder(this)
            .setTitle("Reset " + Controls.SLOTS[idx])
            .setItems(new String[]{"Forget learned key", "Reset short & long actions", "Clear tap position", "Restore this button’s defaults"},
                (dialog, which) -> resetButton(idx, which))
            .setNegativeButton("Cancel", null).show());
        slotStatuses[idx] = addText(layout, "Not learned");
        button(layout, "Learn " + Controls.SLOTS[idx], () -> {
            prefs.edit().putInt("learning_slot", idx).putLong("learning_deadline", System.currentTimeMillis() + 15000).putString("last_event", "Waiting for " + Controls.SLOTS[idx] + " button. Press it now (15 seconds).").apply();
            updateLabels();
        });
        addText(layout, "Short press action");
        Spinner select = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Controls.ACTIONS);
        select.setAdapter(adapter);
        select.setSelection(prefs.getInt(Controls.prefix(idx) + "action", Controls.PASS));
        select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(Controls.prefix(idx) + "action", position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        layout.addView(select);
        actionSpinners[idx] = select;
        addText(layout, "Long press action");
        Spinner longSelect = new Spinner(this);
        String[] longActions = Controls.ACTIONS.clone();
        longActions[Controls.PASS] = "No separate long-press action";
        longSelect.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, longActions));
        longSelect.setSelection(prefs.getInt(Controls.prefix(idx) + "long_action", Controls.PASS));
        longSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(Controls.prefix(idx) + "long_action", position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        layout.addView(longSelect);
        longSpinners[idx] = longSelect;
        button(layout, "Pick camera control · " + Controls.SLOTS[idx], () -> {
            prefs.edit().remove("learning_slot").remove("learning_deadline").apply();
            ButtonService service = ButtonService.instance;
            if (service == null) {
                Toast.makeText(this, "Enable accessibility listener first", Toast.LENGTH_LONG).show();
                return;
            }
            service.pickTapPosition(idx);
            openCamera();
        });
    }

    private void requestBatteryExemption() {
        Intent request = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            android.net.Uri.parse("package:" + getPackageName()));
        try { startActivity(request); }
        catch (RuntimeException unavailable) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void cancelRuntime() {
        ButtonService service = ButtonService.instance;
        if (service != null) service.cancelPendingActions();
    }

    private void syncActionSelectors() {
        for (int i = 0; i < Controls.SLOTS.length; i++) {
            if (actionSpinners[i] != null) actionSpinners[i].setSelection(prefs.getInt(Controls.prefix(i) + "action", Controls.PASS));
            if (longSpinners[i] != null) longSpinners[i].setSelection(prefs.getInt(Controls.prefix(i) + "long_action", Controls.PASS));
        }
    }

    private void resetButton(int slot, int choice) {
        cancelRuntime();
        if (choice == 3) {
            Controls.restoreDefaults(this, slot);
            syncActionSelectors();
            updateLabels();
            Toast.makeText(this, "Native actions restored for " + Controls.SLOTS[slot], Toast.LENGTH_SHORT).show();
            return;
        }
        String prefix = Controls.prefix(slot);
        SharedPreferences.Editor edit = prefs.edit();
        if (choice == 0 || choice == 3) {
            for (String key : new String[]{"code", "scan", "descriptor", "device"}) edit.remove(prefix + key);
        }
        if (choice == 1 || choice == 3) {
            edit.remove(prefix + "action").remove(prefix + "long_action");
        }
        if (choice == 2 || choice == 3) {
            edit.remove(prefix + "x").remove(prefix + "y").remove(prefix + "orientation");
            // An action without a calibrated target would silently do nothing.
            if (prefs.getInt(prefix + "action", Controls.PASS) == Controls.TAP) edit.remove(prefix + "action");
            if (prefs.getInt(prefix + "long_action", Controls.PASS) == Controls.TAP) edit.remove(prefix + "long_action");
        }
        edit.remove("learning_slot").remove("last_event").remove("wake_status").apply();
        syncActionSelectors();
        updateLabels();
        Toast.makeText(this, Controls.SLOTS[slot] + " reset", Toast.LENGTH_SHORT).show();
    }

    private void copyReport() {
        StringBuilder report = new StringBuilder("TELESIN P4-WTP-06 / Xiaomi 17 Ultra\n");
        report.append("Listener active: ").append(ButtonService.instance != null).append('\n');
        for (int i = 0; i < Controls.SLOTS.length; i++) {
            String pre = Controls.prefix(i);
            report.append(Controls.SLOTS[i]).append(": ");
            if (Controls.isLearned(prefs, i)) {
                report.append(prefs.getString(pre + "device", "external"))
                    .append(" key=").append(prefs.getInt(pre + "code", -1))
                    .append(" scan=").append(prefs.getInt(pre + "scan", -1));
            } else report.append("not detected");
            report.append(" short=").append(Controls.ACTIONS[prefs.getInt(pre + "action", Controls.PASS)]);
            report.append(" long=").append(Controls.ACTIONS[prefs.getInt(pre + "long_action", Controls.PASS)]);
            report.append('\n');
        }
        report.append("Last event: ").append(prefs.getString("last_event", "none"));
        report.append("\nScreen-off launch status: ").append(prefs.getString("wake_status", "No request received"));
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("TELESIN Button Lab", report.toString()));
        Toast.makeText(this, "Diagnostic report copied", Toast.LENGTH_SHORT).show();
    }

    private void openCameraIntentTest() {
        startActivity(new Intent(this, WakeCameraActivity.class));
    }

    private void openCamera() {
        Intent i = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        i.setPackage(prefs.getString("camera_package", "com.android.camera"));
        try { startActivity(i); }
        catch (ActivityNotFoundException e) {
            try { startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)); }
            catch (ActivityNotFoundException e2) { Toast.makeText(this, "Could not launch Camera. Open it manually.", Toast.LENGTH_LONG).show(); }
        }
    }

    private TextView addText(LinearLayout parent, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setPadding(0, dp(4), 0, dp(10));
        t.setLineSpacing(dp(2), 1f);
        t.setTextColor(0xff333333);
        parent.addView(t);
        return t;
    }

    private void heading(LinearLayout parent, String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xff17244a);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(12), 0, dp(10));
        parent.addView(t);
    }

    private Button button(LinearLayout parent, String title, Runnable action) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(0xff244c97);
        b.setMinHeight(dp(48));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffeaf0ff));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(4), 0, dp(6));
        b.setLayoutParams(params);
        parent.addView(b);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout expandable(LinearLayout parent, String title) {
        LinearLayout section = card(parent);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setVisibility(View.GONE);
        button(section, title + "  ▾", () -> content.setVisibility(content.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        section.addView(content);
        return content;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(6), dp(16), dp(14));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(0xffffffff);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), 0xffe2e8f0);
        card.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(12), 0, dp(4));
        parent.addView(card, params);
        return card;
    }

    private void updateLabels() {
        if (status == null) return;
        boolean active = ButtonService.instance != null;
        boolean battery = Controls.batteryReady(this);
        boolean ready = active && battery;
        status.setText(active ? "✓ Accessibility listener on" : "1 · Accessibility listener required");
        batteryStatus.setText(battery ? "✓ Battery optimization off" : "2 · Disable battery optimization");
        batteryStatus.setTextColor(battery ? 0xff166534 : 0xff92400e);
        listenerButton.setVisibility(active ? View.GONE : View.VISIBLE);
        batteryButton.setVisibility(battery ? View.GONE : View.VISIBLE);
        readyStatus.setText(ready ? "● Ready" : "● Paused · complete required setup");
        readyStatus.setTextColor(ready ? 0xff166534 : 0xff92400e);
        wakeSwitch.setEnabled(ready || wakeSwitch.isChecked());
        status.setTextColor(active ? 0xff166534 : 0xff92400e);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        String last = prefs.getString("last_event", "No key detected yet");
        int pending = prefs.getInt("learning_slot", -1);
        if (pending >= 0 && System.currentTimeMillis() >= prefs.getLong("learning_deadline", 0)) {
            prefs.edit().remove("learning_slot").remove("learning_deadline").apply();
            pending = -1;
        }
        if (pending >= 0 && pending < Controls.SLOTS.length) last += "\nWaiting for: " + Controls.SLOTS[pending];
        lastKey.setText("Last event: " + last + "\nCamera wake: "
                + prefs.getString("wake_status", "No wake request received yet"));
        for (int i = 0; i < Controls.SLOTS.length; i++) {
            TextView txt = slotStatuses[i];
            if (txt == null) continue;
            String pre = Controls.prefix(i);
            if (!Controls.isLearned(prefs, i)) {
                txt.setText("Not learned · tap Learn, then press this remote button");
            } else {
                String desc = prefs.getString(pre + "device", "external device");
                int code = prefs.getInt(pre + "code", -1);
                int scan = prefs.getInt(pre + "scan", -1);
                String coordinate = prefs.contains(pre + "x")
                    ? "; tap " + Math.round(prefs.getFloat(pre + "x", 0f) * 100) + "%, " + Math.round(prefs.getFloat(pre + "y", 0f) * 100) + "%"
                    : "; no tap location selected";
                txt.setText(desc + " / " + KeyEvent.keyCodeToString(code) + " / scan " + scan + coordinate);
            }
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        // Activity-based fallback for users who haven't enabled Accessibility yet.
        // When the Accessibility service owns key filtering, it performs learning instead.
        if (prefs != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (event.getDevice() != null && event.getDevice().isExternal()) {
                prefs.edit().putString("last_event", Controls.details(event)).apply();
                if (ButtonService.instance == null) {
                    int learning = prefs.getInt("learning_slot", -1);
                    if (learning >= 0 && learning < Controls.SLOTS.length) {
                        Controls.saveLearn(this, learning, event);
                        updateLabels();
                        return true;
                    }
                }
                updateLabels();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onResume() {
        super.onResume();
        // On return from tap picker, ensure UI reflects any changed action.
        if (prefs != null) for (int i = 0; i < actionSpinners.length; i++) {
            if (longSpinners[i] != null) longSpinners[i].setSelection(prefs.getInt(Controls.prefix(i) + "long_action", Controls.PASS));
            if (actionSpinners[i] != null) actionSpinners[i].setSelection(prefs.getInt(Controls.prefix(i) + "action", Controls.PASS));
        }
        ui.removeCallbacks(ticker);
        ui.post(ticker);
    }

    @Override protected void onPause() {
        ui.removeCallbacks(ticker);
        prefs.edit().remove("learning_slot").remove("learning_deadline").apply();
        super.onPause();
    }
}
