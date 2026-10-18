package org.telesin.buttonlab;
import android.content.Context;
final class BetaLog {
 static synchronized void add(Context c, String message) {
  String old=Controls.pref(c).getString("beta_log", "");
  String next=old+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",java.util.Locale.US).format(new java.util.Date())+" "+message+"\n";
  if(next.length()>16000) next=next.substring(next.indexOf('\n',next.length()-16000)+1);
  Controls.pref(c).edit().putString("beta_log",next).apply();
 }
}
