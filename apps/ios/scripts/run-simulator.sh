#!/usr/bin/env bash
# Builds the Debug app, then installs and launches it on a simulator.
# SIM_DEVICE picks the device by name; default is the first available iPhone.
set -euo pipefail
cd "$(dirname "$0")/.."

DEVICE="${SIM_DEVICE:-}"
if [ -z "$DEVICE" ]; then
  UDID=$(xcrun simctl list devices available | grep -E 'iPhone' | head -1 | grep -oE '[0-9A-F-]{36}')
else
  UDID=$(xcrun simctl list devices available | grep -F "$DEVICE (" | head -1 | grep -oE '[0-9A-F-]{36}')
fi
[ -n "$UDID" ] || { echo "No matching simulator found" >&2; exit 1; }

APP=$(bash scripts/xcodebuild.sh install Debug | tail -1)
xcrun simctl bootstatus "$UDID" -b > /dev/null
xcrun simctl install "$UDID" "$APP"
xcrun simctl launch "$UDID" com.example.mobilestack.dev
echo "Launched on $UDID"
