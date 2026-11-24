---
name: drive-app
description: Drive the app from the terminal with the Reflex CLI - list the focused screen's state and actions, perform actions, walk journeys - with no simulator or emulator. Use to find out what the app does, to reproduce a bug, to check a change, or before reading view model code.
---

# Drive the app

The CLI runs the real core (view models, navigation, session, networking) on fixtures. It answers in milliseconds and needs no device. Reach for it first.

## Start

```bash
bun run cli ios                                               # App.default + Items.three, signed out
bun run cli android --fixtures App.default,Items.three,Session.signedIn
```

`--fixtures` is a comma-separated list of scenario names from `packages/contracts/fixtures/` (without `.json`). Later names win. Always include `App.default`. `--app-version 0.0.1` sets the version the update check compares.

## Two commands

```
> ls
ItemList
  state   {"status":"loaded","items":[...],"isRefreshing":false,"errorMessage":null}
  actions Refresh, SelectItem {id}, OpenSettings, goBack, openLink {url}
> do SelectItem {"id": 2}
> ls
ItemDetail
  ...
```

- `ls` prints the **focused** node: the deepest screen, its state, and every action available there. Actions in `{}` take a JSON payload with those keys.
- `do Action [json]` performs one. It prints nothing on success; `ls` again to see the result.
- An action that is not on the focused screen is refused, and the refusal lists what is available. That list is the truth about what a user can do right now.
- `goBack` and `openLink` are lent by the stack and the app root, so they are available on every screen under them.

## Scripted

Pipe commands in; results from services land between lines, so pause briefly after an action that loads something.

```bash
(echo 'do ContinueAsGuest'; sleep 0.3; echo ls) | bun run --silent cli ios
```

For anything longer than a few steps, write a behavior test instead (see `write-behavior-test`): it waits correctly and it stays.

## A running build

```bash
bun run ios           # or: bun run android
bun run cli ios --attach
```

`--attach` speaks the same protocol to the debug build on the simulator or emulator, and the screen follows along. Use it to look at a state on a real screen without tapping your way there. Fixtures cannot be changed on a running build; it runs on whatever its configuration loads.

## Reading failures

- `error: X is not available here. Available: ...` — you are on a different screen than you think. `ls`.
- `error: X payload does not match its schema` — check the keys and types shown in `ls`.
- State stuck on `"status":"failed"` with no fixture for the request — the scenario list is missing a response. See `add-fixture`.
