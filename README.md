# Mobile Stack

A template for shipping one product as two native apps: **Swift / SwiftUI** on iOS and **Kotlin / Jetpack Compose** on Android, in one **Turborepo**.

The apps share no runtime code. They share **one behavior test suite** that both must pass, the **fixtures** it runs on, **design tokens**, and one **rulebook** for people and coding agents.

The architecture is [Reflex](https://shopify.engineering/reflex): all logic lives in a UI-free core you can drive from a terminal, so behavior is verified in seconds with no simulator.

```
$ bun run cli ios --fixtures App.default,Items.three,Session.signedIn
> ls
ItemList
  state   {"status":"loaded","items":[...],"isRefreshing":false,"errorMessage":null}
  actions Refresh, SelectItem {id}, OpenSettings, goBack, openLink {url}
> do SelectItem {"id": 2}
> ls
ItemDetail
  state   {"itemId":2,"status":"loaded","item":{"title":"Wire the real API",...},"canShare":false,...}
  actions Refresh, Share, goBack, openLink {url}
```

Both apps run out of the box on bundled fixtures. No backend, no accounts, no keys.

## Quickstart

```bash
bun install
bun run doctor  # checks Xcode, JDK, Android SDK, Bun and the linters
bun run generate    # tokens -> both apps, Xcode project, local secrets files
bun run behavior    # every journey, against both headless cores
bun run cli ios     # drive the app from the terminal
bun run ios         # build, install and launch on a simulator
bun run android     # same, on a running emulator
```

Requirements: macOS with Xcode 26, Bun 1.3+, JDK 17+, Android SDK platform 37.2. For iOS tooling: `brew install xcodegen swiftlint swiftformat xcbeautify`.

Open the projects in an IDE with `open apps/ios/MobileStack.xcodeproj` (after `bun run generate`) or by opening `apps/android` in Android Studio.

## Make it yours

```bash
bun run rename --name Acme --bundle com.acme.app --scheme acme
bun run generate && bun run format && bun run check
```

Then set real API URLs in `apps/ios/Config/*.xcconfig` and the flavor blocks of `apps/android/app/build.gradle.kts`, and turn `USE_FIXTURES` off for the environments that have a backend.

## Commands

Everything runs through Turborepo from the repo root and is cached.

| Command                                   | What it does                                                        |
| ----------------------------------------- | ------------------------------------------------------------------- |
| `bun run generate`                        | Regenerate tokens, the Xcode project and local secrets files        |
| `bun run format`                          | SwiftFormat, ktlint, Biome                                          |
| `bun run lint`                            | SwiftLint, convention checks, Android Lint                          |
| `bun run typecheck`                       | Compile both apps                                                   |
| `bun run test`                            | Unit tests for primitives on both platforms and the shared packages |
| `bun run behavior`                        | The behavior suite against the headless core, on both platforms     |
| `bun run check`                           | Exactly what CI runs                                                |
| `bun run cli ios` / `bun run cli android` | Drive the app from the terminal (`--attach` for a running build)    |
| `bun run build`                           | Staging builds of both apps                                         |
| `bun run ios` / `bun run android`         | Run one app                                                         |

One platform: `bun run --filter @mobile-stack/ios test`, `bun run --filter @mobile-stack/behavior behavior:android`.

## How it fits together

```
                 packages/behavior  (Bun)         one journey file
                        |   ls / do  (newline-delimited JSON)
        +---------------+----------------+
   mobilestack-cli (macOS)          cli (JVM)                 hosts: fixtures in
        |                                |
   AppCore (Swift package)          :core (JVM module)        no UI imports
     AppViewModel -> StackNavigator -> <Screen>ViewModel      state, actions, one update
        |                                |
   App/ (SwiftUI)                   :app (Compose)            hosts: real services in
```

- **Core** owns view models, navigation, session, networking, deep links. It cannot import a UI framework.
- **Mobile** is views and platform services. A view reads `viewModel.state` and calls `viewModel.send(action:)`. Nothing else compiles.
- **CLI** constructs the same core with fixtures.
- **Introspection** gives every view model a node: name, state as JSON, actions. The CLI focuses the deepest one.

## Layout

```
apps/
  ios/App/                       SwiftUI views, design system, platform services
  ios/Packages/AppCore/          AppCore, IntrospectionKit, mobilestack-cli
  android/app/                   Compose screens, design system, platform services
  android/core/                  the core, a plain JVM module
  android/introspection/ cli/    the node contract and the CLI host
packages/
  behavior/                      the behavior SDK and journeys
  contracts/                     fixtures/*.json scenarios, deeplinks.json
  design-tokens/                 tokens.json -> Tokens.generated.swift + Tokens.kt
e2e/                             two thin Maestro flows
docs/adr/                        the decisions, including 0005 on Reflex
.cursor/rules/                   the rulebook; generates AGENTS.md (CLAUDE.md is a symlink to it)
.agents/skills/                  drive-app, add-fixture, write-behavior-test, verify-app, run-app, opening-a-pr
.github/workflows/               ci, pull_request, preview-build, release
```

## What you get

- **Behavior tests without a device.** 26 journeys run against each platform's core in a few seconds. Zero network is enforced: a request with no fixture fails the test.
- **Parity by construction.** The same test file runs on both platforms; a PR merges only when both pass.
- **An app you can drive from a terminal**, headless or attached to a running debug build.
- **A design system with teeth.** Raw controls, color literals and font calls are lint errors in screens, on both platforms.
- **One source of design tokens**, generated into Swift and Kotlin.
- **One networking seam**: versioned requests, request ids, bearer auth from Keychain / Keystore, a closed error type.
- **Navigation as state**, with deep links, links held until sign-in, and a forced-update gate, all testable headlessly.
- **Three environments** with their own app ids, validated at launch.
- **Seams, not vendors**: analytics, error reporting, flags, haptics and the share sheet behind protocols.
- **Process**: a three-gate PR guide, conventional commits, git hooks, CI, and a release workflow that keeps signing off laptops.

## Adding a screen

Both platforms, one PR.

1. **Fixture** — a scenario in `packages/contracts/fixtures/` for each state of the world.
2. **View model** in the core: `State`, `Action`, `Message`, `update`.
3. **Route** and its entry in `StackNavigator`.
4. **Node** — name, state as JSON, one entry per action.
5. **Journey** in `packages/behavior/tests/`. `bun run behavior` passes on both platforms before any view exists.
6. **Screen** in the mobile app, from design-system primitives.
7. **Look at it** on a simulator and an emulator.

The rules are in [`AGENTS.md`](AGENTS.md); the reasoning is in [`docs/adr`](docs/adr).

## Environments

|             | iOS configuration | Android flavor | App id                            |
| ----------- | ----------------- | -------------- | --------------------------------- |
| Development | `Debug`           | `dev`          | `com.example.mobilestack.dev`     |
| Staging     | `Staging`         | `staging`      | `com.example.mobilestack.staging` |
| Production  | `Release`         | `production`   | `com.example.mobilestack`         |

Local keys go in `apps/ios/Config/Secrets.xcconfig` and `apps/android/secrets.properties` (gitignored, created by `bun run generate`). Anything compiled into an app is public: only client-safe keys belong there.

## Releasing

Store builds come from `.github/workflows/release.yml` and nowhere else. The marketing version is bumped by hand in `apps/ios/Config/Base.xcconfig` and `apps/android/gradle.properties`; the build number is the CI run number. CI uploads; a person promotes. The workflow lists the secrets it needs. It has not been exercised against real store credentials: expect to adjust it on first use.

## Where the practices come from

- The architecture follows Shopify's Reflex article; [`docs/adr/0005-reflex.md`](docs/adr/0005-reflex.md) records how, and what the article leaves open.

## Contributing

Read [`PR_GUIDELINES.md`](PR_GUIDELINES.md). In short: one change per PR, both platforms together, a journey that covers it, `bun run check` green, conventional-commit title.

## License

[MIT](LICENSE)
