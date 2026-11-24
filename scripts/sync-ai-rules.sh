#!/usr/bin/env bash
# Generates AGENTS.md from .cursor/rules/*.mdc, inlining each rule's body (frontmatter stripped).
# Path-only indexes don't work: agents rarely follow "read this file" instructions.
# CLAUDE.md is a symlink to AGENTS.md so every tool reads the same rules.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RULES="$REPO_ROOT/.cursor/rules"
OUTPUT="$REPO_ROOT/AGENTS.md"

strip_frontmatter() {
  awk '
    NR == 1 && /^---$/ { fm = 1; next }
    fm { if (/^---$/) fm = 0; next }
    !started && /^$/ { next }
    { started = 1; print }
  ' "$1"
}

# project-overview leads; the rest follow alphabetically.
files=("$RULES/project-overview.mdc")
for file in "$RULES"/*.mdc; do
  [ "$file" = "$RULES/project-overview.mdc" ] || files+=("$file")
done

{
  echo "<!-- Auto-generated. Edit .cursor/rules/*.mdc, then run: bun run sync-ai-rules -->"
  echo ""
  echo "# Mobile Stack"
  echo ""
  echo "Rules from \`.cursor/rules/\` are inlined below. Edit the \`.mdc\` sources, never this file."

  for file in "${files[@]}"; do
    globs=$(sed -n 's/^globs: *//p' "$file" | head -1)
    echo ""
    echo "---"
    echo ""
    if [ -n "$globs" ]; then
      echo "<!-- Source: .cursor/rules/$(basename "$file") · applies to: $globs -->"
    else
      echo "<!-- Source: .cursor/rules/$(basename "$file") -->"
    fi
    echo ""
    strip_frontmatter "$file"
  done
} > "$OUTPUT"

# Keep the generated file consistent with `biome check` in CI.
if [ -x "$REPO_ROOT/node_modules/.bin/biome" ]; then
  "$REPO_ROOT/node_modules/.bin/biome" format --write "$OUTPUT" > /dev/null
fi

ln -sf AGENTS.md "$REPO_ROOT/CLAUDE.md"
echo "Generated AGENTS.md (+ CLAUDE.md symlink) from ${#files[@]} rules."
