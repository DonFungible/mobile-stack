#!/usr/bin/env bash
# Installs and launches the dev debug build on the connected device or running emulator.
set -euo pipefail
cd "$(dirname "$0")/.."

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
"$ADB" get-state > /dev/null 2>&1 || { echo "No device. Start an emulator first: emulator -avd <name>" >&2; exit 1; }

./gradlew :app:installDevDebug --console=plain -q
"$ADB" shell am start -n com.example.mobilestack.dev/com.example.mobilestack.MainActivity > /dev/null
echo "Launched com.example.mobilestack.dev"
