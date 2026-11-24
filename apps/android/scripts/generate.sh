#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

GENERATED=app/src/main/kotlin/com/example/mobilestack/designsystem/generated
mkdir -p "$GENERATED"
cp ../../packages/design-tokens/dist/Tokens.kt "$GENERATED/Tokens.kt"
[ -f secrets.properties ] || cp secrets.example.properties secrets.properties

# Gradle needs to know where the SDK is; Android Studio writes this file, the CLI does not.
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
if [ ! -f local.properties ] && [ -d "$SDK" ]; then
  echo "sdk.dir=$SDK" > local.properties
fi
