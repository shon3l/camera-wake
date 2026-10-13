#!/bin/sh
set -eu
PHONE_MODEL=2512BPNDAG
export PHONE_MODEL
exec "$(dirname -- "$0")/start.sh" "$@"
