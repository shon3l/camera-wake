#!/bin/sh

# Legacy 2.1 helper: use the tagged release, not the current beta APK.
echo "Legacy activation script. Download Camera Wake 2.1 from https://github.com/shon3l/camera-wake/releases/tag/v2.1.0 . Beta 5 needs no script." >&2
exit 1
set -eu
ADB=${ADB:-adb}
SERIAL=${1:-}
if [ -z "$SERIAL" ]; then
  for candidate in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    model=$("$ADB" -s "$candidate" shell getprop ro.product.model | tr -d '\r')
    if { [ "$model" = "2512BPNDAG" ] || [ "$model" = "SM-S906B" ]; } && { [ -z "${PHONE_MODEL:-}" ] || [ "$model" = "$PHONE_MODEL" ]; }; then
      if [ -n "$SERIAL" ]; then echo "Multiple supported phones: use start-s22.sh or start-xiaomi.sh, or pass a serial." >&2; exit 1; fi
      SERIAL=$candidate
    fi
  done
fi
if [ -z "$SERIAL" ]; then echo "Connect and authorize the target phone, or pass its ADB serial." >&2; exit 1; fi
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if ! "$ADB" -s "$SERIAL" shell dumpsys package org.telesin.buttonlab | grep -q 'versionCode=21 '; then
  "$ADB" -s "$SERIAL" install -r "$HERE/Camera-Wake-2.1.apk"
fi
"$ADB" -s "$SERIAL" shell am force-stop org.telesin.btprobe
"$ADB" -s "$SERIAL" shell am force-stop org.telesin.buttonlab
"$ADB" -s "$SERIAL" shell run-as org.telesin.buttonlab rm -f files/instrument.log
"$ADB" -s "$SERIAL" shell 'nohup am instrument -w org.telesin.buttonlab/.CompatibilityInstrumentation > /data/local/tmp/camera-wake-combined.log 2>&1 < /dev/null &'
attempt=0
while [ "$attempt" -lt 15 ]; do
  if "$ADB" -s "$SERIAL" shell run-as org.telesin.buttonlab cat files/instrument.log 2>/dev/null | grep -q 'READY: camera compatibility active'; then
    echo 'Compatibility active. Hold M1 to open Camera or turn the screen off. USB can be unplugged.'
    exit 0
  fi
  attempt=$((attempt+1))
  sleep 1
done
echo 'Started, but Bluetooth is not ready yet. Check TELESIN pairing and open Camera Wake for status.'
