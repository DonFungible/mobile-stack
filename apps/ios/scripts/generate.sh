#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

cp ../../packages/design-tokens/dist/Tokens.generated.swift App/DesignSystem/Tokens.generated.swift
[ -f Config/Secrets.xcconfig ] || cp Config/Secrets.example.xcconfig Config/Secrets.xcconfig
xcodegen generate --quiet

# Lets sourcekit-lsp resolve the app target (not just the Swift package) for editors and agents.
if command -v xcode-build-server > /dev/null; then
  xcode-build-server config -project MobileStack.xcodeproj -scheme MobileStack > /dev/null
  # Point it at the DerivedData our scripts build into, not Xcode's default location.
  bun -e '
    const fs = require("fs")
    const config = JSON.parse(fs.readFileSync("buildServer.json", "utf8"))
    config.build_root = process.argv[1]
    fs.writeFileSync("buildServer.json", JSON.stringify(config, null, 2))
  ' "$PWD/${DERIVED_DATA:-build/DerivedData}"
fi
