package org.telesin.buttonlab;
import android.app.Activity;
import android.os.*;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
public final class MainActivity extends Activity {
 final Handler h=new Handler(Looper.getMainLooper());TextView status;
 final Runnable refresh=new Runnable(){public void run(){CompatibilityInstrumentation a=CompatibilityInstrumentation.active;status.setText(!Controls.batteryReady(MainActivity.this)?"Setup needed • allow unrestricted battery use":a==null?"Paused • activate with start.sh":a.ready?"Ready • TELESIN connected":"Waiting for TELESIN…");h.postDelayed(this,1000);}};
 TextView text(LinearLayout l,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(27,43,54));t.setPadding(0,16,0,16);l.addView(t);return t;}
 void button(LinearLayout l,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setOnClickListener(v->action.run());l.addView(b);}
 @Override public void onCreate(Bundle b){super.onCreate(b);ScrollView scroll=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(38,90,38,50);l.setBackgroundColor(Color.rgb(244,247,249));scroll.addView(l);text(l,"Camera Wake",30);status=text(l,"Checking…",18);
 ImageView image=new ImageView(this);image.setImageResource(org.telesin.buttonlab.R.drawable.controller_reference);image.setAdjustViewBounds(true);image.setContentDescription("TELESIN case with M1, M2, zoom and capture buttons");l.addView(image,new LinearLayout.LayoutParams(-1,420));
 text(l,"M1   Open Camera • previous mode\nM2   Next mode\nW / T   Zoom out / in\nCapture   Take a photo / start or stop video",18);
 Switch wake=new Switch(this);wake.setText("M1 opens Camera, including from screen off");wake.setChecked(Controls.pref(this).getBoolean("m1_camera",true));wake.setOnCheckedChangeListener((v,on)->Controls.pref(this).edit().putBoolean("m1_camera",on).apply());l.addView(wake);
 button(l,"Open Camera",()->startActivity(new Intent().setClassName("com.android.camera","com.android.camera.Camera")));
 button(l,"Allow unrestricted battery use",()->{if(Controls.batteryReady(this))Toast.makeText(this,"Battery optimization is already off",Toast.LENGTH_SHORT).show();else startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())));});
 button(l,"Reset to defaults",()->{Controls.pref(this).edit().putBoolean("m1_camera",true).apply();wake.setChecked(true);Toast.makeText(this,"Default camera and button controls restored",Toast.LENGTH_SHORT).show();});
 button(l,"Stop",()->{CompatibilityInstrumentation a=CompatibilityInstrumentation.active;if(a!=null)a.stopCompatibility();});
 text(l,"Activate once with start.sh after restarting the phone. Then unplug USB.\n\nXiaomi 17 Ultra • Android 17\nNo root required. Your phone stays locked when using secure Camera.",15);setContentView(scroll);}
 @Override protected void onResume(){super.onResume();h.post(refresh);}
 @Override protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
}
