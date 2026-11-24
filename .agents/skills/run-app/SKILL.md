---
name: run-app
description: Build and launch the iOS or Android app on a simulator or emulator. Use when asked to run, start, launch or screenshot the app, or before verifying a change on a device.
---

# Run the app

To see what the app _does_, you usually do not need this: `bun run cli ios` drives the same logic with no simulator (see `drive-app`). Run the app when you need to see it.

Both commands build the development variant (fixtures on, no backend needed), install it and launch it.

## iOS

```bash
bun run ios                          # first available iPhone simulator
SIM_DEVICE="iPhone 17" bun run ios   # a specific device
```

- Bundle id: `com.example.mobilestack.dev`.
- Screenshot: `xcrun simctl io booted screenshot .sim-shots/ios.png`.
- Open a deep link: `xcrun simctl openurl booted "mobilestack://items/2"`.
- If the build fails after changing `project.yml` or a token, run `bun run generate` first.

## Android

An emulator or device must already be running: `emulator -list-avds`, then `emulator -avd <name> &`.

```bash
bun run android
```

- Application id: `com.example.mobilestack.dev`.
- Screenshot: `adb exec-out screencap -p > .sim-shots/android.png`.
- Open a deep link: `adb shell am start -a android.intent.action.VIEW -d "mobilestack://items/2"`.
- Needs JDK 17+ and `ANDROID_HOME`. `bun run doctor` checks both.

## Reset state

- iOS: `xcrun simctl uninstall booted com.example.mobilestack.dev`
- Android: `adb shell pm clear com.example.mobilestack.dev`

`.sim-shots/` is gitignored; put screenshots there.
