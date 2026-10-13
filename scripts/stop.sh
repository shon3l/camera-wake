#!/bin/sh
set -eu
ADB=${ADB:-adb}
SERIAL=${1:-}
if [ -z "$SERIAL" ]; then
  for candidate in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    model=$("$ADB" -s "$candidate" shell getprop ro.product.model | tr -d '\r')
    if [ "$model" = "2512BPNDAG" ] || [ "$model" = "SM-S906B" ]; then
      if [ -n "$SERIAL" ]; then echo "Multiple supported phones; pass the target serial as the first argument." >&2; exit 1; fi
      SERIAL=$candidate
    fi
  done
fi
if [ -z "$SERIAL" ]; then echo "Connect and authorize the target phone, or pass its ADB serial." >&2; exit 1; fi
"$ADB" -s "$SERIAL" shell am force-stop org.telesin.buttonlab
echo 'Compatibility stopped. Original Bluetooth behavior restored.'
