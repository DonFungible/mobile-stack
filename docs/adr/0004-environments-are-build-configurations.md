# 0004 — Environments are build configurations, validated at launch

**Status:** accepted

## Context

A common setup has one bundle id and picks staging or production at runtime (debug flag plus an admin toggle). That means a production binary can talk to staging, the two cannot be installed together, and a bad value surfaces at the first request.

## Decision

Three environments — development, staging, production — are build configurations: Xcode configurations backed by `Config/*.xcconfig`, and Gradle product flavors. Each has its own app id suffix and display name. `AppConfig` parses the values at launch and refuses to start if one is missing or malformed, or if production is asked to run on fixtures.

Development and staging run on bundled fixtures out of the box so the template works with no backend.

## Consequences

- All three install side by side; QA always knows which one they are holding.
- Third-party services that bind to an app id (push, sign-in, App Links) need an entry per environment.
- There is no runtime environment switch. If you need one for internal builds, add it behind a debug-only screen and keep production pinned.
