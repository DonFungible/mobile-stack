# Context

The vocabulary used in this repo's code, rules and reviews. Use these words; do not coin synonyms.

## Structure

- **Twin** — the same-named counterpart of a type or file on the other platform (`APIClient` / `ApiClient`). Changing one means changing its twin.
- **Contract** — a file in `packages/contracts` that both platforms test against. A deep link case or an API fixture.
- **Token** — a named design value in `packages/design-tokens/tokens.json`. Colors, spacing, radii, type styles, motion durations.
- **Primitive** — a design-system component prefixed `App` (`AppText`, `AppButton`). The only code allowed to use raw platform controls.
- **Seam** — an interface that hides a vendor or a platform API (`HTTPTransport`, `Analytics`, `ErrorReporter`, `TokenStore`).
- **Core** — everything that is not UI: view models, navigation, session, networking. `AppCore` (iOS), `:core` (Android). Imports no UI framework.
- **Mobile** — the app target: views and platform services. Depends on core, never the reverse.
- **Host** — a program that constructs the core: the mobile app (`AppHost`, real services) or the CLI (fixtures).
- **Platform** — the bundle of services a host passes into the core's constructor.

## Reflex

- **View model** — one per screen, in core. `State`, `Action`, `Message`, and one `update`.
- **Action** — something a user can do on a screen. The view model's public surface.
- **Message** — what the reducer handles: an action wrapped as `viewAction`, or a result from a service. Internal to core.
- **Update** — the reducer. The only place a view model's state changes.
- **Node** — a view model, navigator or the app root as seen from outside: `name`, `state` as JSON, `actions`, `child`.
- **Focus** — the deepest node in the tree; the only one the CLI and tests can act on.
- **Lend** — a container offering an action to whatever is focused beneath it (`goBack`, `openLink`).
- **Scenario** — one fixture file: a slice of the world outside the app (responses, a stored session, flags, preferences).
- **Violation** — a request no scenario answered, or an attempt to reach the network. Fails the test.
- **Journey** — a behavior test: a transcript of what a user did and what they saw, run on both platforms.
- **Behavior session** — a fresh core booted on a list of scenarios, driven through `ls` and `do`.
- **Attach** — driving a running debug build through the same protocol over a local port.

## Data

- **Query cache** — the keyed cache of server data in core that view models fetch through.
- **Query key** — the cache identity of a read. With trailing parts omitted it is a **prefix** matching every key that starts with it.
- **Invalidate** — drop cached entries under a prefix; the next read refetches.
- **Repository** — one type per backend entity; one method per endpoint; no state.
- **Store** — a focused holder of cross-screen client state (`SessionStore`, `Preferences`).
- **Fixture build** — an app build whose transport answers from bundled scenarios instead of the network.

## Session and navigation

- **Session status** — `signedOut`, `signedIn` or `expired`. _Expired_ means the server rejected the token; the root gate turns it into a sign-out.
- **Guest** — a signed-in session created without credentials. The template's only kind of session until you add real auth.
- **Root gate** — `AppViewModel`: the single place that decides between forced update, signed out, and the app.
- **Route** — a destination, as a case of the closed `Route` type. Its **stack** is the list of screens a deep link must push to reach it with a working back button.
- **Pending route** — a deep link that arrived before there was a session, held in the root's state for replay.
- **Forced update** — the state where the running version is below the server's minimum for the platform.

## Environments and release

- **Environment** — development, staging or production. A build configuration, not a runtime switch.
- **Marketing version** — the user-visible version (`0.1.0`), bumped by hand in its own PR.
- **Build number** — the per-build integer (`CURRENT_PROJECT_VERSION`, `versionCode`), assigned by CI.
- **Promote** — a person moving a CI-built artifact from TestFlight / the internal track to users. CI never does this.

## Process

- **Gate** — one of the three PR stages: Opening, Production-ready, Merge-ready.
- **Needs a device** — the list in a PR of what could not be verified without a simulator or emulator.
- **Parity** — both platforms passing the same behavior suite.
