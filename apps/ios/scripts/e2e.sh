#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

command -v maestro > /dev/null || { echo "maestro not installed: https://maestro.mobile.dev" >&2; exit 1; }
bash scripts/run-simulator.sh
maestro test -e APP_ID=com.example.mobilestack.dev ../../e2e/smoke.yaml
