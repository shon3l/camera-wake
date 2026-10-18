package org.telesin.buttonlab;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.WindowManager;
import android.widget.*;
import java.util.UUID;

/** Ordinary app connection to the vendor service. Does not bypass HID permissions or send vendor commands. */
@SuppressLint("MissingPermission")
public final class VendorProbeActivity extends Activity {
 private static final UUID SERVICE=UUID.fromString("0000fab0-0000-1000-8000-00805f9b34fb");
 private static final UUID NOTIFY=UUID.fromString("0000fab3-0000-1000-8000-00805f9b34fb");
 private static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
 private final Handler handler=new Handler(Looper.getMainLooper());
 private BluetoothGatt gatt;
 private TextView output;
 private boolean stopped;
 private int notifications;
 private String log="";
 private void record(String message){ handler.post(()->{
  log+=new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.US).format(new java.util.Date())+" "+message+"\n";
  if(log.length()>20000) log=log.substring(log.indexOf('\n',log.length()-18000)+1);
  Controls.pref(this).edit().putString("vendor_probe",log).apply();
  output.setText(log);
 }); }
 @Override public void onCreate(Bundle state){super.onCreate(state);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,120,24,70);
  TextView title=new TextView(this);title.setText("Vendor Bluetooth test\nOnly subscribes to FAB3 notifications. No vendor commands or firmware changes. Optional public HID read checks permission enforcement. Press remote buttons after SUBSCRIBED appears. Stops after five minutes or when closed.");title.setTextSize(17);layout.addView(title);
  Button hid=new Button(this);hid.setText("Check public HID permission");hid.setOnClickListener(v->checkHidPermission());layout.addView(hid);
  Button stop=new Button(this);stop.setText("Stop and return");stop.setOnClickListener(v->finish());layout.addView(stop);
  ScrollView scroll=new ScrollView(this);output=new TextView(this);output.setTextSize(14);scroll.addView(output);layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(layout);
  Controls.pref(this).edit().remove("vendor_probe").apply();
  if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},42);else connect();
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==42&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)connect();else record("Bluetooth permission denied; no connection made");}
 private void connect(){
  record("Ordinary app UID="+android.os.Process.myUid()+"; protected HID not accessed");
  try{
   BluetoothManager manager=getSystemService(BluetoothManager.class);
   BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
   if(adapter==null||!adapter.isEnabled()){record("Turn Bluetooth on and retry");return;}
   BluetoothDevice remote=null;
   for(BluetoothDevice device:adapter.getBondedDevices())if("TELESIN_SDC".equals(device.getName()))remote=device;
   if(remote==null){record("Pair TELESIN_SDC first");return;}
   gatt=remote.connectGatt(this,false,callback,BluetoothDevice.TRANSPORT_LE);
   handler.postDelayed(()->{record("Five-minute timeout; notifications="+notifications);close();},300000);
  }catch(Exception error){record("Connection error: "+error);close();}
 }
 private void checkHidPermission(){
  if(gatt==null||stopped){record("Connect first");return;}
  BluetoothGattService service=gatt.getService(UUID.fromString("00001812-0000-1000-8000-00805f9b34fb"));
  BluetoothGattCharacteristic report=service==null?null:service.getCharacteristic(UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb"));
  if(report==null){record("HID report map not discovered");return;}
  try{record("Public HID read accepted="+gatt.readCharacteristic(report));}catch(SecurityException denied){record("Public HID read DENIED: "+denied.getMessage());}catch(Exception error){record("Public HID read error: "+error);}
 }
 private final BluetoothGattCallback callback=new BluetoothGattCallback(){
  @Override public void onConnectionStateChange(BluetoothGatt connection,int status,int state){
   record("Connection status="+status+" state="+state);
   if(stopped)return;
   if(state==BluetoothProfile.STATE_CONNECTED)try{record("Discover services="+connection.discoverServices());}catch(Exception error){record(error.toString());}
  }
  @Override public void onServicesDiscovered(BluetoothGatt connection,int status){
   if(stopped)return;
   record("Services discovered status="+status);
   if(status!=BluetoothGatt.GATT_SUCCESS)return;
   BluetoothGattService service=connection.getService(SERVICE);
   if(service==null){record("Vendor service FAB0 absent");return;}
   for(BluetoothGattCharacteristic characteristic:service.getCharacteristics()) record("Vendor characteristic "+characteristic.getUuid()+" properties="+characteristic.getProperties());
   BluetoothGattCharacteristic channel=service.getCharacteristic(NOTIFY);
   if(channel==null){record("FAB3 absent");return;}
   try{
    boolean accepted=connection.setCharacteristicNotification(channel,true);record("Local FAB3 registration="+accepted);
    if(!accepted)return;
    BluetoothGattDescriptor descriptor=channel.getDescriptor(CCCD);
    if(descriptor==null){record("No CCCD; listening locally only");return;}
    int result=connection.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    record("Standard notification subscription request="+result);
   }catch(Exception error){record("Subscription error: "+error);}
  }
  @Override public void onCharacteristicRead(BluetoothGatt connection,BluetoothGattCharacteristic characteristic,byte[] value,int status){StringBuilder hex=new StringBuilder();for(byte part:value)hex.append(String.format(java.util.Locale.US,"%02x",part&255));record("Read "+characteristic.getUuid()+" callback status="+status+" bytes="+value.length+" hex="+hex);}
  @Override public void onDescriptorWrite(BluetoothGatt connection,BluetoothGattDescriptor descriptor,int status){
   record(status==BluetoothGatt.GATT_SUCCESS?"SUBSCRIBED to FAB3; test M1/M2/W/T/Capture":"Subscription failed status="+status);
   if(status==BluetoothGatt.GATT_SUCCESS){
    BluetoothGattService hid=connection.getService(UUID.fromString("00001812-0000-1000-8000-00805f9b34fb"));
    BluetoothGattCharacteristic mode=hid==null?null:hid.getCharacteristic(UUID.fromString("00002a4e-0000-1000-8000-00805f9b34fb"));
    if(mode!=null)try{record("Standard Protocol Mode read accepted="+connection.readCharacteristic(mode));}catch(Exception error){record("Protocol Mode read error: "+error);}
   }
  }
  @Override public void onCharacteristicChanged(BluetoothGatt connection,BluetoothGattCharacteristic characteristic,byte[] value){
   if(stopped||!NOTIFY.equals(characteristic.getUuid()))return;
   notifications++;
   StringBuilder hex=new StringBuilder();for(byte part:value)hex.append(String.format(java.util.Locale.US,"%02x",part&255));
   record("FAB3 packet #"+notifications+" length="+value.length+" hex="+hex);
  }
 };
 private void close(){stopped=true;if(gatt!=null){try{gatt.close();}catch(Exception ignored){}gatt=null;}}
 @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);close();super.onDestroy();}
}
