#!/usr/bin/env bash
# Drives the app from a terminal, with no simulator or emulator.
#
#   bun run cli ios --fixtures Items.three,Session.signedIn     headless core on fixtures
#   bun run cli android --fixtures Items.three
#   bun run cli ios --attach                                    the debug build running on a simulator
#   bun run cli android --attach                                the debug build running on an emulator or device
#
# Then type `ls` to see the focused screen and `do Action [json]` to act on it.
set -euo pipefail
cd "$(dirname "$0")/.."

platform="${1:-}"
shift || true

case "$platform" in
  ios) port=4723 ;;
  android) port=4724 ;;
  *) echo "usage: bun run cli <ios|android> [--fixtures A,B | --attach]" >&2; exit 2 ;;
esac

if [ "${1:-}" = "--attach" ]; then
  if [ "$platform" = "android" ]; then
    "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb" forward "tcp:$port" "tcp:$port" > /dev/null
  fi
  exec nc 127.0.0.1 "$port"
fi

(cd "apps/$platform" && bun run --silent build:cli) >&2
if [ "$platform" = "ios" ]; then
  host=apps/ios/Packages/AppCore/.build/debug/mobilestack-cli
else
  host=apps/android/cli/build/install/cli/bin/cli
fi

[ "$#" -gt 0 ] || set -- --fixtures App.default,Items.three
exec "$host" --fixtures-dir packages/contracts/fixtures "$@"
