# 0001 — Native twins in one Turborepo, sharing contracts instead of code

**Status:** accepted

## Context

We want two fully native apps (SwiftUI, Compose) that behave as one product. The usual failure is drift: a deep link that works on one platform, a color that differs by a shade, a rule that one team follows.

## Decision

Both apps live in one Bun + Turborepo workspace. They share **no runtime code**. They share inputs:

- `packages/contracts` — deep link cases and API fixtures. Both platforms' unit tests read these files.
- `packages/design-tokens` — one JSON file generates the Swift and Kotlin token sources.
- `e2e/` — Maestro flows that run unchanged on both apps, keyed on shared accessibility ids.
- `.cursor/rules` -> `AGENTS.md` — one rulebook.

Each app is a workspace package whose `package.json` scripts wrap its native toolchain (`xcodebuild`, `swift test`, `./gradlew`), so `turbo run test` means the same thing everywhere and results are cached.

## Consequences

- Parity is enforced by failing tests, not by review: add a deep link case and both parsers fail until they handle it.
- Every feature is written twice. That is the price of native; mirrored names and structure keep the second copy cheap.
- Bun is a build-time dependency of two native apps. It is only orchestration: neither app ships JavaScript.
- Turborepo cannot see inside Gradle's or Xcode's own caches; it caches at the task level on declared inputs.
