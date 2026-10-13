#!/bin/sh
set -eu
PHONE_MODEL=SM-S906B
export PHONE_MODEL
exec "$(dirname -- "$0")/start.sh" "$@"
