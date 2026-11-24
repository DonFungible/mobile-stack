#!/usr/bin/env bash
# Checks that this machine can build both apps. Prints what is missing and how to get it.
set -uo pipefail

missing=0
check() {
  local name="$1" hint="$2"
  shift 2
  if "$@" > /dev/null 2>&1; then
    printf '  ok       %s\n' "$name"
  else
    printf '  MISSING  %s  ->  %s\n' "$name" "$hint"
    missing=1
  fi
}

java_ok() {
  local version
  version=$("${JAVA_HOME:+$JAVA_HOME/bin/}java" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
  [ "${version:-0}" -ge 17 ]
}

sdk="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

echo "Shared"
check "bun >= 1.3" "curl -fsSL https://bun.sh/install | bash" command -v bun

echo "iOS"
check "Xcode" "install from the App Store, then xcode-select --install" xcodebuild -version
check "xcodegen" "brew install xcodegen" command -v xcodegen
check "swiftlint" "brew install swiftlint" command -v swiftlint
check "swiftformat" "brew install swiftformat" command -v swiftformat
check "an iPhone simulator" "Xcode > Settings > Components" bash -c 'xcrun simctl list devices available | grep -q iPhone'

echo "Android"
check "JDK >= 17" "brew install openjdk@17 and set JAVA_HOME" java_ok
check "Android SDK" "install Android Studio, or set ANDROID_HOME" test -d "$sdk/platforms"
check "SDK platform 37.2" "sdkmanager 'platforms;android-37.2'" test -d "$sdk/platforms/android-37.2"
check "adb" "sdkmanager platform-tools" test -x "$sdk/platform-tools/adb"

echo "Optional"
check "maestro (UI flows)" "curl -fsSL https://get.maestro.mobile.dev | bash" command -v maestro
check "xcbeautify (readable xcodebuild output)" "brew install xcbeautify" command -v xcbeautify

echo "Code intelligence (swift-lsp and kotlin-lsp plugins, enabled in .claude/settings.json)"
check "sourcekit-lsp" "ships with Xcode" command -v sourcekit-lsp
check "xcode-build-server (app target in sourcekit-lsp)" "brew install xcode-build-server" command -v xcode-build-server
check "kotlin-lsp" "brew install JetBrains/utils/kotlin-lsp" command -v kotlin-lsp

exit "$missing"
