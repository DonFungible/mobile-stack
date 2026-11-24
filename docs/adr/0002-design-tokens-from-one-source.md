# 0002 — Design tokens are generated from one JSON file

**Status:** accepted

## Context

Colors, spacing, radii, type and motion durations must be identical on both platforms, and feature code must not invent values.

## Decision

`packages/design-tokens/tokens.json` is the only place a token is defined. `generate.mjs` (no dependencies) writes `Tokens.generated.swift` and `Tokens.kt`. The generated files are committed, so the apps build without running Bun first; CI regenerates them and fails on a diff.

Feature code reaches tokens only through the design system (`AppColor`, `AppText` styles, `Spacing`, `Radius`). Raw color and font APIs are lint errors outside it.

## Consequences

- A token cannot exist on one platform only.
- Colors are opaque `#RRGGBB`. Alpha variants are separate named tokens; the generator rejects anything else rather than guessing.
- The app is dark-only today. A light theme means a second value per color token and a theme-aware `AppColor`; the call sites would not change.
