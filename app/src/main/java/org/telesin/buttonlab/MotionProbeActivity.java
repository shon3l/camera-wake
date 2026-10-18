package org.telesin.buttonlab;

import android.app.Activity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

/** Local diagnostic only. Does not change the controller or system input settings. */
public final class MotionProbeActivity extends Activity {
    private final StringBuilder events = new StringBuilder();
    private TextView output;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 100, 32, 40);
        TextView title = new TextView(this);
        title.setText("Remote input diagnostic\n\nBriefly press M1, M2, W and T, with 3 seconds between each. Remote clicks cannot change settings here.");
        title.setTextSize(19);
        layout.addView(title);
        output = new TextView(this);
        output.setText("Waiting for TELESIN input…");
        output.setTextSize(14);
        layout.addView(output, new LinearLayout.LayoutParams(-1, 0, 1));
        Button back = new Button(this);
        back.setText("Return to Camera Wake");
        back.setOnClickListener(v -> finish());
        layout.addView(back);
        setContentView(layout);
        Controls.pref(this).edit().remove("motion_probe").apply();
    }
    private boolean record(MotionEvent event) {
        if (output == null) return false;
        InputDevice device = event.getDevice();
        if (device == null || !device.getName().startsWith("TELESIN")) return false;
        String line = event.getEventTime() + " " + MotionEvent.actionToString(event.getAction())
            + " x=" + Math.round(event.getX()) + " y=" + Math.round(event.getY())
            + " pointers=" + event.getPointerCount() + " buttons=" + event.getButtonState()
            + " source=" + Integer.toHexString(event.getSource())
            + " relative=" + event.getAxisValue(MotionEvent.AXIS_RELATIVE_X) + "," + event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
            + " scroll=" + event.getAxisValue(MotionEvent.AXIS_HSCROLL) + "," + event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            + " generic1=" + event.getAxisValue(MotionEvent.AXIS_GENERIC_1)
            + " raw=" + event.getRawX() + "," + event.getRawY() + "\n";
        events.append(line);
        if (events.length() > 20000) events.delete(0, events.length() - 16000);
        Controls.pref(this).edit().putString("motion_probe", events.toString()).apply();
        output.setText(events.substring(Math.max(0, events.length() - 2200)));
        return true;
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        return record(event) || super.dispatchTouchEvent(event);
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return record(event) || super.dispatchGenericMotionEvent(event);
    }
}
