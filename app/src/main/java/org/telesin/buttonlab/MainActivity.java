package org.telesin.buttonlab;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;

public final class MainActivity extends Activity {
 private LinearLayout layout; private TextView status,log; private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable refresh=new Runnable(){public void run(){
  status.setText(MappingService.instance==null?"Setup needed: enable Camera Wake Beta in Accessibility":"Accessibility connected • no ADB activation");
  log.setText(Controls.pref(MainActivity.this).getString("wake_status", "")+"\n"+Controls.pref(MainActivity.this).getString("last_event", "")+"\n\n"+Controls.pref(MainActivity.this).getString("beta_log", "No remote events recorded yet."));handler.postDelayed(this,1000);
 }};
 @Override public void onCreate(Bundle state){super.onCreate(state);
  if(!Controls.pref(this).contains("beta_initialized")) Controls.pref(this).edit().putBoolean("beta_initialized",true).putBoolean("native_controls",false).putString("camera_package",Build.MANUFACTURER.equalsIgnoreCase("samsung")?"com.sec.android.app.camera":"com.android.camera").apply();
  getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  ScrollView scroll=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,24,24,24);scroll.addView(layout);
  scroll.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
  setContentView(scroll);scroll.requestApplyInsets();
  text("Camera Wake 3.0 Beta 5",24);status=text("",17);
  text("Goal: all remote controls without ADB. This beta tests which TELESIN events Android exposes. Pair the remote in Bluetooth settings. Stop the old Camera Wake helper before testing.",16);
  button("Test vendor Bluetooth channel",()->startActivity(new Intent(this,VendorProbeActivity.class)));
  button("1. Enable Accessibility",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  text("If activation is blocked: Settings → Apps → Camera Wake Beta → menu → Allow restricted settings, then enable Accessibility. On Xiaomi also allow autostart and background pop-ups if offered.",14);
  button("2. Allow unrestricted battery",()->{if(!Controls.batteryReady(this))startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())));else toast("Battery exemption already enabled");});
  button("App settings / autostart",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
  Switch wake = new Switch(this);wake.setText("Enable camera on screen wake");wake.setChecked(Controls.pref(this).getBoolean("camera_on_wake",false));wake.setOnCheckedChangeListener((view,enabled)->Controls.pref(this).edit().putBoolean("camera_on_wake",enabled).apply());layout.addView(wake);
  text("Previously tested M1 wake mode: hold M1 from screen off to open Camera. This opens Camera after ANY screen wake, including the phone Power button; disable it after case use. No key learning required.",14);
  button("Open Camera now",()->{if(service())MappingService.instance.requestWakeCamera("on-device test");});
  button("Test Camera after 10 seconds",()->{if(service()){Controls.pref(this).edit().putLong("diagnostic_until",System.currentTimeMillis()+120000).apply();MappingService.instance.scheduleCameraTest();toast("Lock phone now. Camera request in 10 seconds.");}});
  button("Lock screen now",()->{if(service())BetaLog.add(this,"Manual lock accepted="+MappingService.instance.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN));});
  text("Learn buttons with this app visible. Learning captures the next external key for 15 seconds. Buttons that send only touch or system Power reports may not be learnable. Unmapped keys pass through.",16);
  for(int slot:Controls.DISPLAY_ORDER){final int index=slot;text(Controls.SLOTS[slot],20);
   button("Learn "+Controls.SLOTS[slot],()->{if(service()){Controls.pref(this).edit().putInt("learning_slot",index).putLong("learning_deadline",System.currentTimeMillis()+15000).apply();toast("Press "+Controls.SLOTS[index]+" now");}});
   selector(index,"action","Short press",Controls.PASS);selector(index,"long_action","Hold",index==3?Controls.WAKE_CAMERA:Controls.PASS);
   button("Pick camera tap target",()->{if(service()){MappingService.instance.requestWakeCamera("calibration");MappingService.instance.pickTapPosition(index);}});
  }
  button("Record pointer reports for 60 seconds",()->{if(service()){MappingService.instance.startPointerDiagnostic();toast("Pointer input paused for 60 seconds. Test remote buttons now; normal input returns automatically.");}});
  button("Test remote touch reports",()->startActivity(new Intent(this,MotionProbeActivity.class)));
  button("Record remote events for 2 minutes",()->{Controls.pref(this).edit().putLong("diagnostic_until",System.currentTimeMillis()+120000).apply();BetaLog.add(this,"Remote diagnostic started for 2 minutes");toast("Test buttons unlocked, locked and screen off; return here to share results.");});
  button("Share diagnostic report",()->{String report="Camera Wake 3.0-beta2\n"+Build.MANUFACTURER+" "+Build.MODEL+" Android "+Build.VERSION.RELEASE+"\nAccessibility connected="+(MappingService.instance!=null)+"\nBattery exempt="+Controls.batteryReady(this)+"\n"+mappingReport()+"Vendor Bluetooth diagnostic:\n"+Controls.pref(this).getString("vendor_probe", "Not run")+"\nTouch diagnostic:\n"+Controls.pref(this).getString("motion_probe", "Not run")+"\n"+Controls.pref(this).getString("wake_status","")+"\n"+Controls.pref(this).getString("last_event","")+"\n"+Controls.pref(this).getString("beta_log","");Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,report);startActivity(Intent.createChooser(send,"Share beta results"));});
  button("Clear diagnostic log",()->Controls.pref(this).edit().remove("beta_log").remove("last_event").remove("wake_status").apply());
  button("Stop beta",()->{if(service())MappingService.instance.disableSelf();});log=text("",13);
 }
 private void selector(int slot,String key,String label,int fallback){text(label,14);Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,Controls.ACTIONS));String pref=Controls.prefix(slot)+key;int value=Controls.pref(this).getInt(pref,fallback);Controls.pref(this).edit().putInt(pref,value).apply();spinner.setSelection(value);spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int position,long id){Controls.pref(MainActivity.this).edit().putInt(pref,position).apply();}});layout.addView(spinner);}
 private String mappingReport(){
  StringBuilder result=new StringBuilder("Camera package="+Controls.pref(this).getString("camera_package","")+"\nUptime milliseconds="+SystemClock.elapsedRealtime()+"\n");
  for(int slot:Controls.DISPLAY_ORDER){String pre=Controls.prefix(slot);result.append(Controls.SLOTS[slot]).append(": learned=").append(Controls.isLearned(Controls.pref(this),slot)).append(" code=").append(Controls.pref(this).getInt(pre+"code",-1)).append(" scan=").append(Controls.pref(this).getInt(pre+"scan",-1)).append(" device=").append(Controls.pref(this).getString(pre+"device","")).append(" short=").append(Controls.pref(this).getInt(pre+"action",0)).append(" hold=").append(Controls.pref(this).getInt(pre+"long_action",0)).append("\n");}
  return result.toString();
 }
 private boolean service(){if(MappingService.instance!=null)return true;toast("Enable Camera Wake Beta in Accessibility first");return false;}
 private TextView text(String value,int size){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setPadding(0,12,0,12);layout.addView(view);return view;}
 private void button(String label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());layout.addView(button);}
 private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
 @Override protected void onResume(){super.onResume();handler.post(refresh);}
 @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
