# PR Guidelines

How we open, validate and merge pull requests. A PR passes through **three gates**; do not move to the next until the current one is clean.

1. **[Opening](#1-opening-a-pr)** — scoped to one change, with a title, branch and description a reviewer can act on.
2. **[Production-ready](#2-production-ready)** — correct on both platforms, verified on devices, safe to ship. Clear this before requesting review.
3. **[Merge-ready](#3-merge-ready)** — approved, CI green, every thread resolved.

> The condensed, always-applied version is `.cursor/rules/pr-guidelines.mdc` (inlined into `AGENTS.md`). This file is the full reference. Agents can run the `opening-a-pr` skill to walk these gates.

---

## 1. Opening a PR

### Keep it scoped

One change, one PR. Touch only the files the change needs. The usual scope mistakes:

- **Drive-by refactors** — a rename or a tidy-up "while I'm here". Split it out or drop it.
- **Reformatting untouched files.** The formatters are deterministic; if a file you did not change shows a diff, something is wrong with your setup, not the file.
- **Speculative fixes to a shared primitive** with no reproduction. A primitive is used on every screen of two apps.
- **Deleting a comment whose code still exists.** It encodes context you may not have.

Decide **ownership before building**. Work that belongs on the backend is closed, not merged.

### Both platforms

A feature PR changes iOS and Android together, plus the contract or token they share. If one platform has to lag, the leading one ships behind a feature flag and the PR says which flag.

A PR that touches only one platform is fine for a platform-specific bug, tooling, or a native config change. Say so in the description.

### Branch and title

- **Branch:** `<author>/<slug>`, or your tracker's generated branch name.
- **Title:** a conventional commit — `type(scope): summary`.
  - `type`: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`, `build`, `ci`, `revert`.
  - `scope`: `ios`, `android`, or a feature (`items`, `settings`). Omit it for a cross-cutting change.
  - Write the summary for someone reading the changelog: "add item sharing", not "sharing stuff".
- The title becomes the squash commit. CI checks it; the `commit-msg` hook checks local commits.

### Description

Use the template. What matters:

- **What and why.** For a bug: Problem / Root cause / Fix. A reviewer should not have to reconstruct the cause from the diff.
- **The ticket**, here and nowhere in the code.
- **Screenshots from both platforms** for anything visible. A GIF if it moves; before/after only for motion or jank.
- **Test plan**, including what you could **not** verify. "Not run on a physical device" is useful; silence is not.
- **Rollout**: backend dependency and deploy order, the feature flag, and any native config change.

Open larger work as a draft (CI skips drafts). Open small fixes ready.

---

## 2. Production-ready

Clear all of this before asking for review.

### Reuse before building

Search before you write: the feature folder, `DesignSystem`, `Common`, and core. Extend what exists. The common misses are a second loading/error treatment (`LoadStatusView` already has one), a second press style (`AppPressable`), and a hand-rolled message screen (`MessageState`).

### Use the design system

Feature code is built from `App*` primitives and tokens. The linters reject raw controls, color literals and font calls; do not work around them with a suppression. If a primitive cannot do what the design needs, extend the primitive in the same PR, on both platforms.

### Cover it with a journey

New or changed behavior has a behavior test in `packages/behavior/tests/` that passes on **both** platforms:

- loading, empty and failure are fixtures; the inverse action (add then remove, sign in then sign out) is a step;
- for a bug fix, the journey fails first, then passes;
- a test that passes on one platform only has found a real bug. Fix the core, not the test.

### See it on devices

A user-visible change has also run on a simulator **and** an emulator, for what the suite cannot see: layout at a large system font size, VoiceOver / TalkBack order, the system back gesture. `bun run cli <platform> --attach` gets you to a state without tapping.

### Keep the architecture's shape

- Decisions live in a view model's `update`. A view reads `state` and sends actions; it holds no logic.
- If a user can see it, it is in state. If a test would assert on it, it is in state.
- An introspection node lists every action with no conditionals, and its names match the other platform's exactly.
- Core imports no UI framework. A new platform capability is a protocol in `Platform`, implemented in the host.
- Fix bad data at its source — the mutation or the cache — not with a filter in the view.
- One bulk endpoint over N requests in a loop. Unbounded lists are lazy, keyed and paginated.

### Ship safely

- Unfinished work is behind a feature flag, and flags fail closed.
- Stay compatible with the backend that is live today. The backend deploys first.
- A change to `project.yml`, `Config/*.xcconfig`, Gradle files, the manifest, entitlements or signing is called out at the top of the description: it cannot be verified by a JS-style reload and it affects release.
- New dependencies are named in the description with the reason. Each one is a liability on two platforms' release trains.

### Conventions

- Comments: default to none; one line on the _why_ when the code cannot say it. No ticket ids.
- Parameter order, naming and file layout per `code-style.mdc`.
- No dead code, no commented-out code.

### Green locally

```bash
bun run check
```

This is what CI runs: format check, lint, compile, unit tests, and the behavior suite against both cores.

---

## 3. Merge-ready

- **Approved**, and every thread answered. Reply with the commit that fixes it, or disagree explicitly with a reason. Do not resolve a thread silently.
- **Re-request review** after pushing fixes.
- **CI green** on the latest commit, with the latest `main` merged in. Do not force-push a branch under review: it destroys the reviewer's "changes since last review".
- **Squash-merge.** The author merges, after confirming the backend deploy order is safe.

---

## For reviewers

- Review the description first. If you cannot tell what the PR is for, that is the first comment.
- Check the twin: is the Android change the same change as the iOS one? Read the journey first; it says what both are supposed to do.
- Prefer a question to an instruction when you are not sure; prefer a concrete suggestion to "this feels off".
- Mark what blocks and what does not. An unmarked comment blocks.
