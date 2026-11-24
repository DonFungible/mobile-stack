---
name: verify-app
description: Prove a change works - behavior through the headless suite on both platforms, then the real apps on a simulator and an emulator for what only pixels show. Use after any change, before opening a PR, or when asked to verify, QA or smoke-test the app.
---

# Verify the app

Verification has two parts. The first covers behavior completely and needs no device. The second covers what a screen looks like.

## 1. Behavior: no device

```bash
bun run check
```

Format, lint, compile, unit tests, and the behavior suite against both cores. If you changed behavior and no journey changed, you have not verified it: add or extend one (`write-behavior-test`).

To look at a specific state by hand, drive the core: `bun run cli ios --fixtures ...` (`drive-app`).

## 2. Pixels: a simulator and an emulator

Needed for anything a user can see.

```bash
bun run ios && bun run android
maestro --udid <device> test -e APP_ID=com.example.mobilestack.dev e2e/
```

- The two flows in `e2e/` prove the views are wired to the core on both platforms. They are deliberately thin; do not add behavior coverage there.
- To reach a state without tapping, attach the CLI to the running build: `bun run cli ios --attach`, then `do ...`. Take a screenshot (`xcrun simctl io booted screenshot`, `adb exec-out screencap -p`).
- Look at: layout at a large font size, VoiceOver / TalkBack order, the system back gesture, dark bars, safe areas.

## Reporting

State exactly what ran: "`bun run check` green (26 journeys on both platforms); e2e flows pass on iPhone 17 (iOS 26.4) and Pixel 7 Pro; not run on a physical device". If a platform's device run was skipped, list it under **Needs a device** in the PR. Never report something as passing unless you saw it pass.

## Troubleshooting

- `iOS driver not ready in time`: the first Maestro run on a simulator builds its test runner. Open Simulator.app and retry with `MAESTRO_DRIVER_STARTUP_TIMEOUT=300000`.
- `--attach` prints nothing when piped: `nc` exits at end of input. Keep the pipe open (`(echo ls; sleep 1) | bun run --silent cli ios --attach`) or use it interactively.
- An id is not found on Android: the root sets `testTagsAsResourceId`; check the control uses `Modifier.testTag`.
