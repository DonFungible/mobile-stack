---
name: add-fixture
description: Add or change a fixture scenario for behavior tests and fixture builds. Use when a test needs a new server response, an edge case (empty, failing, unauthorized), a stored session, a feature flag or a preference, or when a test fails with "no fixture for".
---

# Add a fixture

A fixture is a **scenario**: one JSON file in `packages/contracts/fixtures/` describing a slice of the world outside the app. Both platforms' cores read the same files, and so do the apps' fixture builds.

## Shape

```json
{
  "session": "fixture-token",
  "http": [
    { "method": "GET", "path": "items", "status": 200, "body": [] },
    { "method": "GET", "path": "items/1", "status": 404 }
  ],
  "flags": { "item_sharing": true },
  "preferences": { "haptics_enabled": false }
}
```

Every field is optional.

- `http[].path` is the endpoint as the repository writes it: no leading slash, no API version, no query string.
- `session` is a stored token: the app starts signed in.
- `flags` keys are the flag's wire name (`FeatureFlag` raw value / `key`). A flag that is not listed is off.
- `preferences` keys are `StorageKey` values.

## Naming

`<Area>.<situation>.json`, in PascalCase then camelCase: `Items.three`, `Items.empty`, `Items.loadFails`, `Session.signedIn`, `Update.required`, `Flags.sharingOn`. Name the situation, not the test.

## Rules

- **One situation per file.** Tests compose them: `fixtures: [fixture('Items.three'), fixture('Session.signedIn')]`. Later scenarios win for the same request, flag or preference.
- **An edge case is a new scenario**, never a change to an existing one that other tests rely on.
- **Every request needs an answer.** A request no scenario answers is a violation and fails the test, even if the screen handled the error. For "the server fails here", write the failure explicitly (`"status": 503`).
- `App.default` is loaded first in every behavior session. Put only what every run needs there.
- A response body must decode into the model on **both** platforms. If it does not, the screen shows a failure and the journey says so.

## Check

```bash
bun run cli ios --fixtures App.default,Your.scenario,Session.signedIn     # then: ls
bun run behavior
```
