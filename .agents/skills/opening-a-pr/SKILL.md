---
name: opening-a-pr
description: Walk the three PR gates and open a pull request with the right title, description and evidence. Use when asked to open, create, raise or prepare a PR.
---

# Opening a PR

Follow `PR_GUIDELINES.md`. Do the gates in order; stop and report if one fails.

## Gate 1 — scope

1. `git diff main...HEAD --stat`. Every file must belong to the one change. Move anything else to another branch.
2. If the change is a feature, confirm both `apps/ios` and `apps/android` are in the diff, or that the leading platform is behind a feature flag.
3. Branch is `<author>/<slug>`.

## Gate 2 — production-ready

1. `bun run check` — must be green, including the behavior suite on both platforms. Paste failures, do not summarise them away.
2. Confirm a journey covers the change (`write-behavior-test`). Run the device half of `verify-app` for anything visible. Keep the screenshots.
3. Re-read the diff for: logic in a view, a conditional in an introspection node, state changed outside `update`, a platform import in core, raw controls or values that slipped past lint via suppression, comments that narrate, ticket ids in source, dead code.
4. Note every native config change (`project.yml`, xcconfig, Gradle, manifest, signing).

## Open it

- Title: `type(scope): summary`, conventional commit.
- Body: fill `.github/pull_request_template.md` completely. Under **Not verified / needs a device**, list what you could not run. Be exact.
- Draft if the work is large or anything in gate 2 is outstanding; otherwise ready.

```bash
gh pr create --draft --title "feat(items): add item sharing" --body-file <file>
```

Never push to `main`, never force-push a branch under review, and never mark a PR ready with a failing gate.
