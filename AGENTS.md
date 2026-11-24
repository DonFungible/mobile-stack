<!-- Auto-generated. Edit .cursor/rules/*.mdc, then run: bun run sync-ai-rules -->

# Mobile Stack

Rules from `.cursor/rules/` are inlined below. Edit the `.mdc` sources, never this file.

---

<!-- Source: .cursor/rules/project-overview.mdc -->

# Project overview

Two native apps, one repo: `apps/ios` (Swift 6, SwiftUI) and `apps/android` (Kotlin, Jetpack Compose). They share no runtime code. They share **one behavior test suite** (`packages/behavior`), **fixtures and contracts** (`packages/contracts`), **design tokens** (`packages/design-tokens`), and one set of rules (this file). Turborepo runs every task; Bun is the package manager, the script runtime and the test runner.

The architecture is Reflex (see the Reflex rule below): on each platform a **core** with no UI holds every view model, the navigation tree and all logic; the **mobile** app is a thin host that draws it; a **CLI** host runs the same core headlessly so behavior can be tested, and the app driven, from a terminal.

## Layout

```
apps/ios/
  App/                         Mobile: SwiftUI views and platform services. Thin.
    DesignSystem/              primitives and tokens
    Common/                    shared feature-level views (LoadStatusView)
    Features/<Feature>/        <Name>Screen.swift, Components/
    Navigation/                RootView: draws the tree the core owns
    Platform/                  Keychain, UserDefaults, haptics, share sheet
  Packages/AppCore/
    Sources/AppCore/           Core: no SwiftUI, no UIKit. Builds on macOS.
      Base/                    ViewModel
      App/                     MobileStackCore, AppViewModel, StackNavigator
      Features/                one <Name>ViewModel per screen
      Introspection/           one node per view model
      Platform/ Fixtures/      what the core needs from outside, and its fixture forms
      Networking/ Query/ Auth/ Config/ Services/ Navigation/ Models/ Repositories/ System/
    Sources/IntrospectionKit/  node protocol, ls/do session, socket server
    Sources/mobilestack-cli/   CLI host
    Tests/
apps/android/
  app/                         Mobile: Compose screens and platform services. Thin.
  core/                        Core: a plain JVM module. Cannot import android.* or Compose.
  introspection/               node interface, ls/do session, socket server
  cli/                         CLI host
packages/behavior/             the behavior SDK and journeys; one file runs on both platforms
packages/contracts/            fixtures/*.json scenarios and deeplinks.json
packages/design-tokens/        tokens.json -> Swift + Kotlin
e2e/                           two thin Maestro flows for what only pixels can show
```

The two trees mirror each other on purpose. A type in one has a same-named twin in the other (`ItemListViewModel`, `StackNavigator`, `APIClient` / `ApiClient`). When you change one, change its twin in the same PR.

## Commands

Run from the repo root. All of them go through Turborepo and are cached.

| Command                                   | What it does                                                               |
| ----------------------------------------- | -------------------------------------------------------------------------- |
| `bun run generate`                        | Tokens -> both apps, Xcode project from `project.yml`, local secrets files |
| `bun run format`                          | SwiftFormat, ktlint (Spotless), Biome                                      |
| `bun run lint`                            | SwiftLint, convention checks, Android Lint                                 |
| `bun run typecheck`                       | Compile both apps                                                          |
| `bun run test`                            | Unit tests for primitives: `swift test`, `:core:test`, package tests       |
| `bun run behavior`                        | The behavior suite against the headless core, on both platforms            |
| `bun run check`                           | Everything CI runs: format check, lint, typecheck, test, behavior          |
| `bun run cli ios` / `bun run cli android` | Drive the app from the terminal: `ls`, `do Action [json]`                  |
| `bun run ios` / `bun run android`         | Build, install and launch on a simulator / emulator                        |

One platform only: `bun run --filter @mobile-stack/ios <task>`, or `bun run --filter @mobile-stack/behavior behavior:android`.

## Naming

- View models: `<Name>ViewModel` in the core. Screens: `<Name>Screen` in the mobile app, taking their view model.
- Introspection node names are the screen name without a suffix (`ItemList`, `Settings`) and are identical on both platforms: the behavior tests depend on it.
- Action titles are PascalCase (`SelectItem`); actions a container lends are camelCase (`goBack`, `openLink`).
- Design-system primitives are prefixed `App` (`AppText`, `AppButton`). Feature code never defines an `App*` type.
- Repositories: `<Entity>Repository`, one method per endpoint, no caching inside.
- Accessibility ids are kebab-case and identical on both platforms (`settings-sign-out`).

## Generated files: never edit

- `App/DesignSystem/Tokens.generated.swift`, `designsystem/generated/Tokens.kt` — edit `packages/design-tokens/tokens.json`, run `bun run generate`.
- `apps/ios/MobileStack.xcodeproj` — edit `apps/ios/project.yml`. The project is not committed.
- `AGENTS.md` — edit `.cursor/rules/*.mdc`, run `bun run sync-ai-rules`.

## Turborepo notes

- `apps/android/turbo.json` and `apps/ios/turbo.json` chain each app's tasks. Two Gradle, `xcodebuild` or SwiftPM invocations in one project contend for the same build directory, so they run in sequence.
- `JAVA_HOME`, `ANDROID_HOME` and friends reach tasks through `globalPassThroughEnv` in the root `turbo.json`. A new environment variable a task needs must be added there.

---

<!-- Source: .cursor/rules/code-style.mdc · applies to: apps/**/*.swift,apps/**/*.kt -->

# Code style

Formatting is not a review topic: SwiftFormat and ktlint decide. Run `bun run format`.

## Comments

Default to **no comment**. Clear names and small functions carry the meaning.

- Comment the **why**, never the what. One short line.
- A comment is justified for a non-obvious workaround, a platform gotcha, or intent the code cannot express. The bar: the reader would be misled without it.
- A doc comment (`///`, `/** */`) on a seam or a primitive states its contract. Not on every function.
- No step narration inside a function body.
- **No ticket ids in source** (lint-enforced). They belong in the commit and the PR.

```swift
// Bad: restates the code
// Sign the user out
session.signOut()

// Good: a why the code cannot show
// "$()" keeps xcconfig from reading "//" as a comment.
```

## Parameter order

For a view's or composable's parameters, and at its call sites:

1. Required values
2. Optional values (defaulted) — on Android, `modifier` comes first among these
3. Required closures
4. Optional closures, with the content/trailing closure last

```swift
init(_ title: String, variant: AppButtonVariant = .primary, isLoading: Bool = false, action: @escaping () -> Void)
```

```kotlin
fun AppButton(title: String, modifier: Modifier = Modifier, variant: AppButtonVariant = AppButtonVariant.Primary, onClick: () -> Unit)
```

## Conventions

- One screen per file, named for the screen. Private helpers below the main type.
- Views and composables hold no logic: state in, actions out.
- Dependencies come through constructors, from the host (`AppHost`) down. No singletons, no `static let shared`.
- Swift: strict concurrency is on and warnings are errors. No `@unchecked Sendable` outside `AppCore`, no force unwraps.
- Kotlin: warnings are errors. No `!!`. Catch the specific exception, never bare `Exception`.
- Remove dead code. Do not leave commented-out code.

---

<!-- Source: .cursor/rules/data-and-state.mdc · applies to: apps/ios/**/*.swift,apps/android/**/*.kt -->

# Data and state

## Where state lives

| Kind                             | Lives in                                       | Example                                  |
| -------------------------------- | ---------------------------------------------- | ---------------------------------------- |
| What a screen shows              | its view model's `State`                       | the items list, a loading flag, a toggle |
| Shared across screens            | a store in core, passed through `Dependencies` | `SessionStore`, the query cache          |
| Purely visual, never asserted on | the view (`@State` / `remember`)               | scroll offset, a press highlight         |

If a behavior test would want to assert on it, it is view model state, not view state.

## Loading server data

A view model loads through a repository and the query cache, and reflects the result in state:

```swift
run({ try await queries.fetch(.items(), force: force, repository.items) }, then: Message.loaded)
```

- **State carries the whole picture**: `status` (`loading` / `loaded` / `failed`), the data, `isRefreshing`, `errorMessage`. The view renders it with `LoadStatusView` / `LoadStatusContent`; it decides nothing.
- **A refresh keeps the last good value on screen.** Only a first load, or a retry after failure, shows loading.
- **A failed refresh is not an error state.** The user keeps what they had.
- **Query keys are functions** next to the repository (`QueryKey.item(id:)`). Trailing parts omitted make the key a prefix: `queries.invalidate(.item(id: nil))` drops every cached item after a mutation.

### Adding an endpoint

1. A scenario in `packages/contracts/fixtures/` with the response.
2. Model, repository method and key function, on both platforms.
3. A view model message for the result, and state for what the user sees.
4. A journey that exercises it.

## Networking

`APIClient` / `ApiClient` is the only way out to the network. It adds the API version, an `x-request-id`, the bearer token and a 30s timeout, and turns every failure into one `APIError` / `ApiError`:

- `network`, `unauthorized`, `http(status)`, `decoding`.
- A 401 marks the session expired; the app root signs the user out. Screens do not handle 401.
- Only 5xx and decoding failures are reported, tagged with a normalized route (`items/:id`) so one broken endpoint is one issue.
- State shows `error.userMessage`. Never a raw error.

The transport is a `Platform` service: `URLSession` / OkHttp in the app, fixtures in the CLI and in fixture builds.

## Stores

- One store per concern, with every mutation a method.
- Anything that holds user data registers a cleanup with `SessionStore.registerSignOutCleanup`. Sign-out then cannot miss a cache.
- Persisted keys are declared in `StorageKey`. Keys holding per-user data take the user id.
- Tokens go in the `TokenStore` (Keychain / Keystore in the app). Never in preferences.

## Seams

Third-party SDKs sit behind a protocol in core's `Services/` or `Platform`: `Analytics`, `ErrorReporter`, `FeatureFlagProvider`, `HapticsPlayer`, `ExternalActions`. Swapping a console implementation for a real one is a change in the host (`AppHost`), nowhere else.

- **Analytics events** are cases in `AnalyticsEvent`: snake_case name, typed properties.
- **Feature flags fail closed.** `flags.isEnabled(.x)` is false until the provider says true. Read a flag in the view model and put the result in state (`canShare`), so a journey can assert on it.
- **Haptics are semantic** and fired from `update` (`haptics.play(.selection)`); the user's preference is checked in one place.
- **Logging** goes through `Log`. `print`, `NSLog`, `println` and `android.util.Log` are lint errors outside the host.

---

<!-- Source: .cursor/rules/design-system.mdc · applies to: apps/ios/App/**/*.swift,apps/android/app/src/main/**/*.kt -->

# Design system

Screens are built from design-system primitives and draw exactly what their view model's state says. The design system is the only place allowed to touch raw platform controls, colors and fonts, so a fix to press feedback, hit targets or accessibility lands everywhere at once.

## Banned outside the design system

Enforced by SwiftLint custom rules (`apps/ios/.swiftlint.yml`) and `checkConventions` (`apps/android/app/conventions.gradle.kts`). Both fail CI.

| Don't use                                                   | Use instead                                                        |
| ----------------------------------------------------------- | ------------------------------------------------------------------ |
| `Text(...)` / material3 `Text`                              | `AppText(text, style:, color:)`                                    |
| `Button(...)` / material3 buttons                           | `AppButton`, `AppIconButton`, or `AppPressable` for a tappable row |
| `Toggle(...)` / material3 `Switch`                          | `AppToggle`                                                        |
| `Modifier.clickable` (Android)                              | `AppPressable`                                                     |
| Color literals, `Color.red`, `Color(0x...)`, `UIColor(...)` | `AppColor.<token>`                                                 |
| `.font(...)`, `TextStyle(...)`, `fontSize =`                | an `AppText` style                                                 |
| `UIScreen.main`                                             | `GeometryReader` / `containerRelativeFrame`                        |

If a primitive cannot do what a design needs, extend the primitive. Do not reach around it.

## Rules

- **Every screen's root is `ScreenContainer`.** It owns the background, the title bar and system-bar insets.
- **Spacing and radii come from `Spacing` and `Radius`.** No bare numbers for padding or corner radius.
- **Hit targets are at least 44pt / 48dp.** `AppButton`, `AppIconButton` and `AppPressable` guarantee it; a custom gesture must too.
- **Icon-only controls need a label.** `AppIconButton` makes it a required parameter.
- **Text scales with the system font size.** `AppText` handles it. Never fix a height around text.
- **Respect Reduce Motion.** Animation beyond the standard press fade checks `accessibilityReduceMotion` (iOS) or the animator duration scale (Android).
- **Haptics are fired by view models**, semantically (`haptics.play(.success)`). A view never fires one.
- **Loading, empty and error are states you design.** `LoadStatusView` / `LoadStatusContent` render loading and failure from view model state; the screen handles empty with `MessageState`.
- **Lists are lazy and keyed.** `LazyVStack` + `ForEach` over `Identifiable` on iOS; `LazyColumn` with `key` and `contentType` on Android. Paginate anything unbounded.

## Adding a token

1. Add it to `packages/design-tokens/tokens.json`.
2. `bun run generate`.
3. For a color, expose it in `AppColor` on both platforms.

---

<!-- Source: .cursor/rules/environments-and-release.mdc · applies to: apps/ios/Config/**,apps/ios/project.yml,apps/android/**/*.gradle.kts,apps/android/gradle.properties,.github/workflows/** -->

# Environments and release

## Environments

|             | iOS configuration | Android flavor | App id suffix | Data                              |
| ----------- | ----------------- | -------------- | ------------- | --------------------------------- |
| Development | `Debug`           | `dev`          | `.dev`        | fixtures                          |
| Staging     | `Staging`         | `staging`      | `.staging`    | fixtures until you set a real URL |
| Production  | `Release`         | `production`   | none          | live API                          |

- Each environment has its own app id, so all three install side by side.
- Environment values live in `apps/ios/Config/<Env>.xcconfig` and the flavor block of `apps/android/app/build.gradle.kts`. Keep the two in step.
- `AppConfig` validates them **at launch** and refuses to start on a bad value. Production can never run on fixtures.
- Never branch on debug-vs-release in feature code. Read `config.environment` or a feature flag.
- The introspection socket server exists only in debug builds (`#if DEBUG` on iOS, the `debug` source set on Android). Never ship it.

## Secrets

- Local keys go in `apps/ios/Config/Secrets.xcconfig` and `apps/android/secrets.properties`. Both are gitignored and created from their `.example` by `bun run generate`.
- Anything compiled into an app is public. Only client-safe keys belong in those files; real secrets stay on the server.
- Signing keys, store credentials and provisioning profiles live in CI secrets only. Never in the repo, never in an agent's environment.
- The committed `apps/android/keystore/debug.keystore` is deliberate: one debug key for the whole team, so SHA-bound services work on every machine. It signs nothing that ships.

## Versions

- The marketing version is set by hand in exactly two places that must match: `MARKETING_VERSION` in `apps/ios/Config/Base.xcconfig` and `appVersionName` in `apps/android/gradle.properties`. Bump it in its own PR.
- Build numbers are assigned by CI. Never bump `CURRENT_PROJECT_VERSION` or `versionCode` by hand.

## Release

- Store builds are made by CI (`.github/workflows/release.yml`), never on a laptop.
- Nothing auto-releases to users. CI uploads to TestFlight / the Play internal track; a person promotes it.
- The backend a release needs must be live before the release is.
- Raise the server's minimum version only after the replacement build is available in both stores.

---

<!-- Source: .cursor/rules/navigation.mdc · applies to: apps/ios/**/Navigation/**,apps/ios/**/App/*.swift,apps/android/**/navigation/**,apps/android/core/**/app/**,packages/contracts/deeplinks.json -->

# Navigation

Navigation lives in core. The views draw a stack they do not own.

- **The tree is `AppViewModel` -> `StackNavigator` -> screen view models.** `AppViewModel` is the only gate: forced update, signed out, or the app. Screens never check the session.
- **Pushing is a state change.** A view model calls `navigation.push(.settings)`; the navigator builds the destination's view model and adds an entry. No view is involved, which is why the CLI can walk the whole app.
- **The platform's back button and swipe are actions.** iOS reports the new depth (`popTo`); Android's system back sends `goBack`. Never pop a platform stack directly.
- **Screens exist only while signed in.** The stack is built on sign-in and cleared on sign-out, so nothing fetches without a session and nothing leaks between accounts.
- **A deep link is the `openLink` action on the app root.** The host hands the URL over and does nothing else.
- **A route knows its back stack** (`route.stack`), so a link to `settings/about` lands with Settings underneath it.
- **A link that arrives before sign-in is held** in the root's state and replayed once there is a session. It cannot be used to walk around the update gate.
- **Unknown links are ignored.** The parser returns nothing rather than guessing; ids must be positive integers in canonical form.
- **Forced update never locks users out by accident.** Only a readable, higher minimum version blocks the app; a failed check lets them in.

## Adding a destination

1. Add the case to `Route` on both platforms, with its `name` and `stack`.
2. Add cases to `packages/contracts/deeplinks.json`, including a malformed one. `packages/behavior/tests/deeplinks.test.ts` turns each into a test on both platforms; map the route to its node name there.
3. Teach both `DeepLinkParser`s the path, and build the screen in both `StackNavigator`s.
4. `bun run behavior`.

---

<!-- Source: .cursor/rules/platform-parity.mdc -->

# Platform parity

**Share the test, not the code.** Writing the same view model twice is cheaper than maintaining a bridge between two platforms, and it keeps each app able to do what its platform does best. What holds them together is one behavior suite that both must pass.

- **One suite, two platforms.** `bun run behavior` runs every journey against the Swift core and the Kotlin core. A PR cannot merge if it passes on only one.
- **Same surface.** Node names, state keys and action titles are identical. A difference is a failing test, not a review comment.
- **Same fixtures.** Both cores read the scenario files in `packages/contracts/fixtures`; so do the fixture builds of both apps.
- **Tokens, not values.** A new color, spacing step or type style goes into `packages/design-tokens/tokens.json`. Never add one to a single platform.
- **Same names.** Keep type and file names aligned so a reviewer can diff the twins side by side.
- **One PR.** Ship both platforms together. If one must lag, hide the leading one behind a feature flag and say so in the PR.

Platform idiom wins in the mobile layer: iOS uses a `NavigationStack` and the system back button, Android draws its own top bar and handles the system back gesture. The core's behavior matches exactly; the widgets need not.

---

<!-- Source: .cursor/rules/pr-guidelines.mdc -->

# PR guidelines

A PR passes three gates in order. Full reference: `/PR_GUIDELINES.md`.

## Gate 1 — Opening

- **One change, one PR. Surgical diff.** Touch only what the change needs. No drive-by refactors, no renames "while I'm here", no reformatting untouched files, no speculative fixes to shared primitives without a repro.
- **Both platforms in the one PR** (see platform parity), unless the lagging one is behind a flag.
- **Branch** `<author>/<slug>`, or your tracker's branch name. **Title** is a conventional commit: `type(scope): summary` with `feat` / `fix` / `chore` / `docs` / `refactor` / `test`, and scope `ios`, `android`, or a feature name. A hook and CI both check it.
- **Description** follows the template: what and why (Problem / Root cause / Fix for a bug), screenshots from both platforms for anything visible, a test plan that names what you could **not** verify, and rollout or backend dependencies.
- Draft for larger work; ready for small fixes. CI skips drafts.

## Gate 2 — Production-ready (before requesting review)

- **Reuse before building.** Search `DesignSystem`, `Common` and the feature folder for a primitive or helper that already does the job, and extend it.
- **Design-system primitives only** in feature code; tokens for every color, space and radius.
- **A journey covers it.** New behavior has a behavior test that passes on both platforms; loading, empty, failure and the inverse action are fixtures and steps, not manual checks.
- **Seen on a simulator and an emulator** for anything visible.
- **Logic in the core.** Decisions are in `update`; views read state and send actions; nodes have no conditionals.
- **Ship safely**: unfinished work behind a feature flag; stay compatible with the backend that is live today.
- **No native config drift**: a change to `project.yml`, `Config/*.xcconfig`, `build.gradle.kts`, the manifest or signing is called out at the top of the description.
- **Green locally**: `bun run check`, which includes the behavior suite on both platforms.

## Gate 3 — Merge-ready

- Approved, with every thread answered: cite the fixing commit, or disagree explicitly with a reason.
- CI green and the latest `main` merged in. Do not force-push a branch under review.
- Squash-merge. The author merges, after confirming any backend deploy order.

---

<!-- Source: .cursor/rules/reflex.mdc -->

# Reflex: the architecture

The app's logic runs with no UI, so its behavior can be tested in milliseconds without a simulator, and one test suite keeps both platforms in step. Four pieces make that work. Each has rules that are not negotiable, because the whole value is that every screen has the same shape.

## 1. Core has no UI

Core owns everything that is not drawing: view models, navigation state, the session, networking, caching, deep links, feature flags. It is a Swift package that builds on macOS and a plain JVM Gradle module.

- **No UI imports in core.** No `SwiftUI`, `UIKit`, `android.*` or Compose. On Android the module type makes it impossible; on iOS a lint rule does.
- **Core declares what it needs; hosts provide it.** Anything from outside the process — HTTP, the token store, preferences, haptics, the share sheet, flags — is a protocol in core's `Platform`, passed into `MobileStackCore`'s constructor. No DI framework, no singletons.
- **Two hosts construct the same core.** The mobile app passes real services (`AppHost`). The CLI passes fixtures. If something cannot be reached from the CLI, it is in the wrong module.
- **A new platform capability** is: a protocol in `Platform`, a real implementation in the app's `Platform/` folder, a no-op or fixture default, on both platforms.

## 2. Every view model looks the same

```swift
public final class SettingsViewModel: ViewModel<SettingsViewModel.State, SettingsViewModel.Action, SettingsViewModel.Message> {
    public struct State: Equatable { public var hapticsEnabled: Bool }

    public enum Action {                    // what a view (or the CLI) can do
        case setHaptics(enabled: Bool)
        case signOut
    }

    public enum Message: ViewMessage {      // everything the reducer handles
        case viewAction(Action)
    }

    override func update(state: inout State, message: Message) { ... }   // the only writer
}
```

- **State** is everything the screen shows. If a user can see it, it is in state.
- **Action** is everything a user can do. It is the public surface.
- **Message** is internal: every action wrapped as `viewAction`, plus results coming back from services.
- **`update` is the only place state changes.** Decisions live here: whether `Decrement` does anything at zero is `update`'s call, made once, for a tap and for the CLI alike.
- **Async work goes through `run`**, which feeds its result back as a message. Never mutate state from a callback.
- **Services arrive through the constructor** (`Dependencies`). A view model never constructs a client or reads a global.
- **Navigation is a call on `Navigating`**, not a view. Pushing a screen is a state change on the navigator.

A view's entire contract is: read `viewModel.state`, call `viewModel.send(action:)`. It cannot do more: `update`, messages and the state setter are internal to core, and a lint rule rejects any other member access on a view model from a screen.

## 3. Every view model has an introspection node

A node is the view model seen from outside: a `name`, its `state` as JSON, its `actions`, and its `child`. Swift nodes are extensions in `Introspection/Nodes.swift`; Kotlin nodes are `<Name>ViewModelIntrospectionNode` classes in `introspection/Nodes.kt`. The app never calls them.

Three rules:

- **If a user can see it, it's in the node's state. If a user can do it, it's in its actions.** State keys and action titles are identical on both platforms.
- **A node never repeats logic.** It lists every action, every time, with no conditionals. If you are writing an `if` in a node, the rule belongs in `update` and the result belongs in state.
- **Actions go through `send(action:)`, nothing else.** A node never calls another method or touches state. The CLI can do nothing a view could not.

A parameterized action declares an input type and a schema. The payload is decoded before the closure runs, so a malformed one never reaches the view model.

## 4. The tree, and focus

Nodes mirror the object tree: `App` -> `Stack` -> the top screen. The CLI always focuses the **deepest** node, the way a user only touches what is on top. Containers **lend** a few actions to whatever is focused: the stack lends `goBack`, the app root lends `openLink`. Anything else is refused, with the list of what is available.

## Adding a screen

Do every step on both platforms, in one PR.

1. **Fixture** — a scenario in `packages/contracts/fixtures/` for each state the screen can be in (see the `add-fixture` skill).
2. **View model** in core `Features/`: State, Action, Message, `update`.
3. **Route** — a `Route` case, and a `Screen` case built in `StackNavigator`.
4. **Node** — name, state as JSON, one entry per action.
5. **Journey** — a behavior test in `packages/behavior/tests/` that walks to the screen and uses it. Run `bun run behavior`; it must pass on both platforms before any view exists.
6. **Screen** in the mobile app: design-system primitives, `viewModel.state` in, `send` out.
7. **Verify on a device** for what only pixels show.

## The CLI

```
$ bun run cli ios --fixtures App.default,Items.three,Session.signedIn
> ls
ItemList
  state   {"status":"loaded","items":[...],"isRefreshing":false,"errorMessage":null}
  actions Refresh, SelectItem {id}, OpenSettings, goBack, openLink {url}
> do SelectItem {"id": 2}
> ls
ItemDetail
  ...
```

- `bun run cli <platform> --attach` talks to the debug build running on a simulator or emulator through the same protocol; the screen follows along. The server exists only in debug builds.
- The fastest way to find out what the app does is to open a terminal and type `ls`. Reach for it before reading code, and before a simulator.
- The CLI is a tool for agents: no colors, no prompts, no wizard. Keep it that way.

---

<!-- Source: .cursor/rules/testing-and-verification.mdc -->

# Testing and verification

Three layers, with a very wide middle.

## Behavior tests: almost everything

`packages/behavior/tests/*.test.ts`, run by Bun against the headless core. Business rules, validation, navigation, error handling, session and deep links, whole user journeys. **One file runs unchanged on iOS and Android**; that suite is the source of truth for what the app does.

```ts
test('a guest can change a setting and sign out', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.three')],
  })
  await enterAsGuest(session)

  await session.do({
    action: 'OpenSettings',
    expectation: ({ name, state }) => {
      expect(name).toBe('Settings')
      expect(state.hapticsEnabled).toBe(true)
    },
  })
})
```

- **A test is a transcript of what a user did and what they saw.** `do` runs an action, then polls the focused node until the expectation passes or two seconds elapse, and fails with the last state seen. No sleeps, no waits.
- **Fold a journey into one test.** A failure already names the step, the action and the state. Split only when the _fixtures_ differ (a save that fails needs its own setup).
- **Fixtures are constructor arguments.** An edge case is a different scenario file, never a mock patched mid-test. There are no mocking frameworks in the suite.
- **Zero network, enforced.** A request with no fixture is a violation and fails the test even if the app recovered. So is any real connection attempt.
- **Both platforms or neither.** A change is not done until `bun run behavior` passes on iOS and Android. If they disagree, one of them has a bug; the test decides which.
- Tests in a file run one after another against one warm host process. Do not use `test.concurrent`.

## Unit tests: primitives only

`swift test` and `:core:test`. The base view model, the query cache, the API client, parsers, formatters, the introspection session. Small and few. If a rule is reachable through a screen, it belongs in a behavior test, not here.

## Device: what only pixels can show

The behavior suite does not test the UI. Checking that a button is wired to `send` is rarely where the bugs are, but layout, tokens, accessibility and platform chrome are real.

```bash
bun run ios && bun run android
maestro test -e APP_ID=com.example.mobilestack.dev e2e/
bun run cli ios --attach        # drive the running build from the terminal
```

- A change a user can see is run on a simulator and an emulator: large font size, VoiceOver / TalkBack order, the system back gesture.
- `e2e/` holds two thin flows that prove the views are wired to the core. Do not grow it into a second behavior suite.
- If you could not run something, say so. "Unverified" is acceptable in a PR; "verified" when it was not is not.

An agent without a simulator verifies behavior fully with `bun run check`, and lists what still needs eyes under **Needs a device** in the PR.

## Order of work

Write the journey first. It fails on both platforms, then passes on both, before a single view exists. `bun run check` is exactly what CI runs.
