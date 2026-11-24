---
name: write-behavior-test
description: Write or extend a behavior test - a journey that runs against the headless core on both iOS and Android. Use when adding or changing any behavior (rules, validation, navigation, errors, session, deep links), when fixing a bug, or when asked to test a screen or flow.
---

# Write a behavior test

Behavior tests live in `packages/behavior/tests/*.test.ts` and run on Bun. Each one boots a headless core on fixtures and walks it the way a user would. The same file runs against the Swift core and the Kotlin core; both must pass.

## Shape

```ts
import { expect, test } from 'bun:test'
import { createBehaviorSession, fixture } from '../src/session'
import { enterAsGuest } from './journeys'

test('given the list fails to load, the user sees a message and can retry', async () => {
  await using session = await createBehaviorSession({
    fixtures: [fixture('Items.loadFails')],
  })
  await enterAsGuest(session)

  await session.do({
    action: 'Refresh',
    expectation: ({ state }) => {
      expect(state.status).toBe('failed')
      expect(state.errorMessage).toBe('Something went wrong. Please try again.')
    },
  })
})
```

- `createBehaviorSession({ fixtures, appVersion? })` boots a fresh core. Always `await using`, so violations are checked when the test ends.
- `session.precondition('ItemList')` waits until that node is focused. Start a journey with it.
- `session.do({ action, payload?, expectation })` runs the action, then polls the focused node until `expectation` passes (about two seconds), and fails with the last state seen. The expectation receives `{ name, path, state, actions }`.
- `session.attempt(action, payload?)` returns the raw answer, for asserting that something is refused.
- Shared openings go in `tests/journeys.ts` (`enterAsGuest`, `openItem`).

## How to write one

1. **Find the surface first.** `bun run cli ios --fixtures ...`, then `ls` at each step. Use the node names, state keys and action titles you see; do not guess them.
2. **Name it as a sentence about the user**: `a guest can ...`, `given <situation>, <what the user sees>`. Never the function under test.
3. **Fold the journey into one test.** A failure already says which step, which action, and what state was seen. One test per assertion buys nothing and costs run time.
4. **Split when the fixtures differ.** The failing-save case is its own test because its setup is different, not because its assertion is.
5. **Assert on what a user could see**: the focused `name`, and fields of `state`. If what you need is not in state, the fix is in the view model (and its node), not in the test.
6. **Assert the no-op too.** Actions are always listed; whether one does anything is the view model's rule. `goBack` at the root stays at the root: say so.

## Do not

- No sleeps, no retries, no `test.concurrent`. `do` waits; tests in a run share one warm host.
- No mocks. A different situation is a different fixture (see `add-fixture`).
- No platform branches (`if (ios)`). If the platforms differ, one has a bug.
- No UI assertions. Layout, copy placement and tokens are not reachable here, by design.

## Run

```bash
bun run behavior                                                   # both platforms
bun run --filter @mobile-stack/behavior behavior:ios               # one
cd packages/behavior && PLATFORM=android bun test -t "guest"    # by name, once `bun run cli android` has built the host
```

A failure reads like this; act on it directly:

```
error: Timed out waiting for expectation after Refresh.
expect(received).toBe(expected)  Expected: "failed"  Received: "loaded"
Last seen: ItemList {"status":"loaded","items":[...]}
```

A test that passes on one platform and fails on the other has found a real divergence. Fix the core that is wrong; never weaken the test.
