# 0005 — Reflex: a UI-free core, uniform view models, introspection, and a CLI

**Status:** accepted. Supersedes part of [0003](0003-server-state-through-a-small-query-layer.md).

## Context

Screens were thin SwiftUI and Compose views that fetched through a `QueryView`, with navigation and the session gate living in the view layer. Verifying behavior meant a simulator and a tap script, and parity between the platforms rested on duplicated unit tests and review.

Shopify's Reflex ([shopify.engineering/reflex](https://shopify.engineering/reflex)) describes an architecture where app behavior runs headlessly and one test suite keeps two native apps in sync. It is published as a description, not a library: "a capable coding agent can implement it for your app". This ADR records how it was implemented here and the decisions the article leaves open.

## Decision

Adopt Reflex on both platforms.

- **Core / Mobile / CLI.** Core (`AppCore` Swift package; `:core` JVM module) owns view models, navigation, session, networking and deep links, and imports no UI framework. The mobile app and the CLI are hosts that construct `MobileStackCore` with a `Platform`: real services in the app, fixtures in the CLI.
- **One view model shape.** `State`, a public `Action`, an internal `Message`, and one `update`. Views read `state` and call `send(action:)`; everything else is internal to the core module, so the compiler enforces the contract.
- **Introspection.** Each view model, the stack navigator and the app root expose a node: name, state as JSON, actions, child. The CLI focuses the deepest node; containers lend `goBack` and `openLink`.
- **One behavior suite.** `packages/behavior` (Bun) speaks the protocol to either platform's CLI. Every journey must pass on both.

## Decisions the article leaves open

| Question         | Choice                                                                                                                                                                                                                                                                                | Why                                                                                                                          |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Wire format      | Newline-delimited JSON: `{id, command: "ls" \| "do" \| "boot", action?, payload?}` -> `{id, ok, node: {name, path, state, actions}, violations}` or `{id, ok: false, error: {code, message, available?}}`. A line not starting with `{` is the plain form (`ls`, `do Action [json]`). | One protocol serves a person at a terminal and the SDK.                                                                      |
| Warm host        | A `boot` command replaces the running core with a fresh one on new fixtures.                                                                                                                                                                                                          | One process serves a whole run, instead of paying JVM start-up per test.                                                     |
| State mutability | Swift: a value-type `State` mutated through `inout`. Kotlin: an immutable `State` that `update` returns.                                                                                                                                                                              | Idiomatic on each side; both make `update` the only writer. The article's Swift sample uses a class.                         |
| Re-entrancy      | A message sent during an update is queued and applied after it.                                                                                                                                                                                                                       | Session changes and navigation fire from inside reducers.                                                                    |
| Async            | `run(work, then:)` launches the work and dispatches its result as a message. Swift is main-actor confined; Kotlin is confined to one dispatcher (`Main` in the app, a dedicated thread in the CLI).                                                                                   | The article says only that results "come back as messages".                                                                  |
| Lending          | `lentActions` on the node protocol, beside the article's four fields.                                                                                                                                                                                                                 | Needed to express "a stack lends goBack".                                                                                    |
| Node generation  | Hand-written, in one file per platform.                                                                                                                                                                                                                                               | The article calls this "a perfectly fine approach". A macro and a KSP processor would add two code generators to a template. |
| Fixtures         | Scenario files in `packages/contracts/fixtures`, merged in order; later wins. Shared by both platforms and by the apps' fixture builds.                                                                                                                                               | One place to describe a state of the world.                                                                                  |
| Zero network     | A request no scenario answers is recorded as a violation, and so is any connection attempt (a `URLProtocol` on Swift, the default `ProxySelector` on the JVM). The session fails the test on any violation.                                                                           | Deterministic, and loud.                                                                                                     |
| Real builds      | A debug-only socket server speaks the same protocol on a local port.                                                                                                                                                                                                                  | `bun run cli <platform> --attach` drives a running simulator. It is compiled out of release builds.                          |
| Query cache      | Kept in core as a plain keyed cache with prefix invalidation; view models own loading state.                                                                                                                                                                                          | The cache is still useful; the view-layer `QueryView` was the part Reflex replaces.                                          |

## Consequences

- Behavior is verified in a few seconds per platform with no simulator, by people and agents alike.
- Parity is a failing test: the suite caught each divergence between the two cores while this was being built.
- Every screen costs a view model, a node and a journey, twice. The shape is rigid on purpose; that is what makes it repeatable.
- Fixtures grow with the app. A journey may need a dozen responses.
- The suite does not test pixels. Two Maestro flows and a person cover what only a screen can show.
- The Android back stack no longer survives process death on its own. Restoring it means persisting `StackNavigator` state through `Platform`; nothing does yet.
- A live query no longer refetches when its key is invalidated. A view model refetches when it starts or is asked to.
