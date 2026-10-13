#!/bin/sh
set -eu
ADB=${ADB:-adb}
SERIAL=${1:-}
if [ -z "$SERIAL" ]; then
  for candidate in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    model=$("$ADB" -s "$candidate" shell getprop ro.product.model | tr -d '\r')
    if [ "$model" = "2512BPNDAG" ]; then
      if [ -n "$SERIAL" ]; then echo "Multiple Xiaomi phones; pass the serial as the first argument." >&2; exit 1; fi
      SERIAL=$candidate
    fi
  done
fi
if [ -z "$SERIAL" ]; then echo "Connect and authorize the Xiaomi 17 Ultra, or pass its ADB serial." >&2; exit 1; fi
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if ! "$ADB" -s "$SERIAL" shell dumpsys package org.telesin.buttonlab | grep -q 'versionCode=20 '; then
  "$ADB" -s "$SERIAL" install -r "$HERE/Camera-Wake-2.0.apk"
fi
"$ADB" -s "$SERIAL" shell am force-stop org.telesin.btprobe
"$ADB" -s "$SERIAL" shell am force-stop org.telesin.buttonlab
"$ADB" -s "$SERIAL" shell run-as org.telesin.buttonlab rm -f files/instrument.log
"$ADB" -s "$SERIAL" shell 'nohup am instrument -w org.telesin.buttonlab/.CompatibilityInstrumentation > /data/local/tmp/camera-wake-combined.log 2>&1 < /dev/null &'
attempt=0
while [ "$attempt" -lt 15 ]; do
  if "$ADB" -s "$SERIAL" shell run-as org.telesin.buttonlab cat files/instrument.log 2>/dev/null | grep -q 'READY: camera compatibility active'; then
    echo 'Compatibility active. Open Xiaomi Camera. USB can be unplugged.'
    exit 0
  fi
  attempt=$((attempt+1))
  sleep 1
done
echo 'Started, but Bluetooth is not ready yet. Check TELESIN pairing and open Camera Wake for status.'
