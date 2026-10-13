package org.telesin.buttonlab;
import android.app.Instrumentation;
import android.bluetooth.*;
import android.content.Context;
import android.os.*;
import android.widget.TextView;
import java.util.*;
import android.view.*;
import android.util.DisplayMetrics;
import android.hardware.display.DisplayManager;
import java.util.concurrent.*;
// Bluetooth permissions are supplied by ADB-started UiAutomation for this session only.
@android.annotation.SuppressLint("MissingPermission")
public final class CompatibilityInstrumentation extends Instrumentation {
 private final Handler wakeHandler=new Handler(Looper.getMainLooper());
 private volatile long lastScreenOn,lastWakeSignal,lastLaunch;
 private android.content.BroadcastReceiver screenReceiver;
 void remoteWakeSignal(){lastWakeSignal=SystemClock.uptimeMillis();android.os.PowerManager p=(android.os.PowerManager)getTargetContext().getSystemService(Context.POWER_SERVICE);if(!p.isInteractive()||lastWakeSignal-lastScreenOn<1500)scheduleWake();}
 void scheduleWake(){wakeHandler.postDelayed(()->{android.os.PowerManager p=(android.os.PowerManager)getTargetContext().getSystemService(Context.POWER_SERVICE);if(p.isInteractive()&&SystemClock.uptimeMillis()-lastWakeSignal<1800)actions.execute(this::openCamera);},250);}
 void openCamera(){
  Context c=getTargetContext();long now=SystemClock.uptimeMillis();if(!ready||!Controls.batteryReady(c)||!Controls.pref(c).getBoolean("m1_camera",true)||now-lastLaunch<1800)return;
  lastLaunch=now;boolean locked=((android.app.KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardLocked();
  String action=locked?"android.media.action.STILL_IMAGE_CAMERA_SECURE":"android.media.action.STILL_IMAGE_CAMERA";
  try(java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand("am start -a "+action+" -n com.android.camera/.Camera"))){log("CAMERA "+(locked?"SECURE":"NORMAL")+" "+new String(in.readAllBytes()).trim());}catch(Exception e){log("CAMERA ERROR "+e);}
 }
 public static volatile CompatibilityInstrumentation active; public volatile boolean ready;
 BluetoothGatt gatt; final ExecutorService actions=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new SynchronousQueue<Runnable>(),new ThreadPoolExecutor.DiscardPolicy());
 final Map<Integer,Integer> reportIds=new ConcurrentHashMap<>(); int zoomFirst,zoomLast,zoomCount,modeFirst,modeLast,modeCount; long lastZoom,lastMode; StringBuilder out=new StringBuilder();
 ArrayDeque<Object> queue=new ArrayDeque<>();
 void log(Object line){synchronized(out){out.append(android.os.SystemClock.uptimeMillis()).append(" ").append(line).append("\n");if(out.length()>64000)out.delete(0,20000);try(java.io.FileWriter f=new java.io.FileWriter(new java.io.File(getTargetContext().getFilesDir(),"instrument.log"))){f.write(out.toString());}catch(Exception ignored){}}}
 void next(){
  if(queue.isEmpty()){ready=true;log("READY: camera compatibility active");return;}
  Object o=queue.remove();try{
   boolean started;
   if(o instanceof BluetoothGattCharacteristic){BluetoothGattCharacteristic c=(BluetoothGattCharacteristic)o;started=gatt.readCharacteristic(c);}
   else{started=gatt.readDescriptor((BluetoothGattDescriptor)o);}
   if(!started){log("READ did not start");next();}
  }catch(Exception e){log(e);next();}
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){
  active=this;getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).adoptShellPermissionIdentity("android.permission.BLUETOOTH_PRIVILEGED", "android.permission.BLUETOOTH_CONNECT");
  Context ctx=getTargetContext();
  screenReceiver=new android.content.BroadcastReceiver(){@Override public void onReceive(Context c,android.content.Intent i){if(android.content.Intent.ACTION_SCREEN_ON.equals(i.getAction())){lastScreenOn=SystemClock.uptimeMillis();if(lastScreenOn-lastWakeSignal<1500)scheduleWake();}else lastScreenOn=0;}};
  android.content.IntentFilter screens=new android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_ON);screens.addAction(android.content.Intent.ACTION_SCREEN_OFF);ctx.registerReceiver(screenReceiver,screens,Context.RECEIVER_NOT_EXPORTED);

  BluetoothAdapter adapter=((BluetoothManager)ctx.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
  log("STEP find paired remote");
  BluetoothDevice remote=null;
  for(BluetoothDevice d:adapter.getBondedDevices())if("TELESIN_SDC".equals(d.getName()))remote=d;
  if(remote==null){log("TELESIN not paired");return;}
  log("STEP connect");
  gatt=remote.connectGatt(ctx,true,new BluetoothGattCallback(){
   @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){log("CONNECTION status="+status+" state="+state);ready=false;if(state==2){queue.clear();reportIds.clear();g.discoverServices();}}
   @Override public void onServicesDiscovered(BluetoothGatt g,int status){
    log("SERVICES status="+status);
    for(BluetoothGattService s:g.getServices()){
     log("SERVICE "+s.getInstanceId()+" "+s.getUuid());
     for(BluetoothGattCharacteristic c:s.getCharacteristics()){
      log(" CHAR "+c.getInstanceId()+" "+c.getUuid()+" props="+c.getProperties());
      if(s.getUuid().toString().startsWith("00001812") && (c.getProperties() & 16)!=0){try{log("LISTEN "+c.getInstanceId()+" "+g.setCharacteristicNotification(c,true));}catch(Exception e){log(e);}}
      for(BluetoothGattDescriptor d:c.getDescriptors())queue.add(d);
      if(c.getUuid().toString().startsWith("00002a4b")||c.getUuid().toString().startsWith("00002a50"))queue.add(c);
     }
    }
    next();
   }
   @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){int id=reportIds.getOrDefault(c.getInstanceId(),-1);
    if(id==2&&value.length>=2&&(value[0]&255)==0x30&&value[1]==0)remoteWakeSignal();
    if(id==4&&value.length==4&&(value[0]&1)!=0&&(((value[1]&255)|((value[2]&15)<<8))<350))remoteWakeSignal();
    decodeZoom(c,value);decodeMode(c,value);}
   @Override public void onDescriptorRead(BluetoothGatt g,BluetoothGattDescriptor d,int status,byte[] value){if(status==0 && d.getUuid().toString().startsWith("00002908") && value.length==2 && value[1]==1)reportIds.put(d.getCharacteristic().getInstanceId(),value[0]&255);next();}
   @Override public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value,int status){
    StringBuilder b=new StringBuilder();for(byte x:value)b.append(String.format("%02x",x));
    log("VALUE service="+c.getService().getInstanceId()+" uuid="+c.getUuid()+" status="+status+" hex="+b);next();
   }
  },BluetoothDevice.TRANSPORT_LE);
 }
 void decodeZoom(BluetoothGattCharacteristic c,byte[] b){
  if(reportIds.getOrDefault(c.getInstanceId(),-1)!=3||b.length!=13)return;
  if((b[0]&1)!=0){int x=(b[1]&255)|((b[2]&15)<<8);if(zoomCount++==0)zoomFirst=x;zoomLast=x;}
  else if(zoomCount>0){int delta=zoomLast-zoomFirst;zoomCount=0;long now=SystemClock.uptimeMillis();if(delta!=0&&now-lastZoom>=180){lastZoom=now;actions.execute(()->pinch(delta<0));}}
 }
 void decodeMode(BluetoothGattCharacteristic c,byte[] b){
  if(reportIds.getOrDefault(c.getInstanceId(),-1)!=4||b.length!=4)return;
  if((b[0]&1)!=0){int x=(b[1]&255)|((b[2]&15)<<8);if(modeCount++==0)modeFirst=x;modeLast=x;}
  else if(modeCount>0){int delta=modeLast-modeFirst;modeCount=0;long now=SystemClock.uptimeMillis();if(Math.abs(delta)>=250&&now-lastMode>=450){lastMode=now;actions.execute(()->mode(delta>0));}}
 }
 void mode(boolean right){
  if(!ready||!Controls.batteryReady(getTargetContext()))return;
  if(!cameraFocused()){if(right)openCamera();return;}
  if(SystemClock.uptimeMillis()-lastLaunch<1800)return;
  DisplayMetrics m=new DisplayMetrics();((DisplayManager)getTargetContext().getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0).getRealMetrics(m);
  int x1=(int)(m.widthPixels*(right?.30:.70)),x2=m.widthPixels-x1,y=(int)(m.heightPixels*.782);
  try(java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand("input touchscreen swipe "+x1+" "+y+" "+x2+" "+y+" 220"))){String result=new String(in.readAllBytes());log("MODE "+(right?"PREVIOUS":"NEXT")+" "+result.trim());}catch(Exception e){log("MODE ERROR "+e);}
 }
 public void stopCompatibility(){ready=false;actions.shutdownNow();if(gatt!=null)gatt.close();active=null;finish(0,new Bundle());}
 boolean cameraFocused(){
  try(java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand("dumpsys window"))){
   return java.util.regex.Pattern.compile("mCurrentFocus=.*com\\.android\\.camera/").matcher(new String(in.readAllBytes())).find();
  }catch(Exception e){log("FOCUS ERROR "+e);return false;}
 }
 void event(long down,int action,int count,float cx,float cy,float separation){
  MotionEvent.PointerProperties[] pp=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] pc=new MotionEvent.PointerCoords[count];
  for(int i=0;i<count;i++){pp[i]=new MotionEvent.PointerProperties();pp[i].id=i;pp[i].toolType=MotionEvent.TOOL_TYPE_FINGER;pc[i]=new MotionEvent.PointerCoords();pc[i].x=cx+(i==0?-separation:separation);pc[i].y=cy;pc[i].pressure=1;pc[i].size=0.05f;}
  MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,count,pp,pc,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
  try{if(!getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(e,true))throw new IllegalStateException("injection rejected");}finally{e.recycle();}
 }
 void pinch(boolean in){
  if(!ready||!Controls.batteryReady(getTargetContext())||!cameraFocused()){log("ZOOM ignored: Camera not focused");return;}
  DisplayMetrics m=new DisplayMetrics();((DisplayManager)getTargetContext().getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0).getRealMetrics(m);
  float cx=m.widthPixels*.5f,cy=m.heightPixels*.43f,start=m.widthPixels*(in?.12f:.15f),end=m.widthPixels*(in?.15f:.12f);long down=SystemClock.uptimeMillis();
  try{event(down,MotionEvent.ACTION_DOWN,1,cx,cy,start);event(down,MotionEvent.ACTION_POINTER_DOWN|(1<<8),2,cx,cy,start);
   for(int i=1;i<=8;i++){SystemClock.sleep(15);event(down,MotionEvent.ACTION_MOVE,2,cx,cy,start+(end-start)*i/8);}
   event(down,MotionEvent.ACTION_POINTER_UP|(1<<8),2,cx,cy,end);event(down,MotionEvent.ACTION_UP,1,cx,cy,end);log("ZOOM "+(in?"IN":"OUT"));
  }catch(Exception e){log("ZOOM ERROR "+e);try{event(down,MotionEvent.ACTION_CANCEL,1,cx,cy,end);}catch(Exception ignored){}}
 }
 static String hex(byte[] value){StringBuilder b=new StringBuilder();for(byte x:value)b.append(String.format("%02x",x));return b.toString();}
 @Override public void onDestroy(){wakeHandler.removeCallbacksAndMessages(null);if(screenReceiver!=null)getTargetContext().unregisterReceiver(screenReceiver);ready=false;active=null;actions.shutdownNow();if(gatt!=null)gatt.close();getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).dropShellPermissionIdentity();super.onDestroy();}
}