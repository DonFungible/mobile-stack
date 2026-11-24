#!/usr/bin/env bash
# Usage: xcodebuild.sh <build|install> <Debug|Staging|Release>
set -euo pipefail
cd "$(dirname "$0")/.."

ACTION="${1:-build}"
CONFIGURATION="${2:-Debug}"
DERIVED_DATA="${DERIVED_DATA:-build/DerivedData}"

run() {
  if command -v xcbeautify > /dev/null; then
    set -o pipefail && "$@" | xcbeautify --quiet
  else
    "$@" -quiet
  fi
}

run xcodebuild build \
  -project MobileStack.xcodeproj \
  -scheme MobileStack \
  -configuration "$CONFIGURATION" \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath "$DERIVED_DATA" \
  CODE_SIGNING_ALLOWED=NO

if [ "$ACTION" = "install" ]; then
  echo "$DERIVED_DATA/Build/Products/$CONFIGURATION-iphonesimulator/MobileStack.app"
fi
