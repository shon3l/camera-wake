#!/bin/sh

# Legacy 2.1 helper: use the tagged release, not the current beta APK.
echo "Legacy activation script. Download Camera Wake 2.1 from https://github.com/shon3l/camera-wake/releases/tag/v2.1.0 . Beta 5 needs no script." >&2
exit 1
set -eu
PHONE_MODEL=2512BPNDAG
export PHONE_MODEL
exec "$(dirname -- "$0")/start.sh" "$@"
