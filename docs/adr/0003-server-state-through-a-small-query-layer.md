# 0003 — Server state goes through a small in-house query layer

**Status:** superseded in part by [0005](0005-reflex.md). The cache and its prefix keys remain; `Query`, `QueryView` and `QueryContent` are gone, and view models own loading state.

## Context

On the web, TanStack Query routes all server data through one cache, and its most valuable habit is the query-key function whose partial form is a prefix for invalidation. Native has no single equivalent library, and the common alternative — each view model owning its own loading flags and fetch calls — is exactly what that habit exists to prevent.

## Decision

Each platform has a ~100-line `QueryClient` + `Query`:

- a cache keyed by `QueryKey`, where trailing nils are dropped so a key doubles as a prefix;
- `load()` serves the cache, `refetch()` keeps the last good value on screen and shares one in-flight request;
- `invalidate(prefix)` drops matching entries and refetches the queries still alive;
- `QueryView` / `QueryContent` render loading, failure-with-retry and pull-to-refresh once.

Screens never fetch on their own.

## Consequences

- No third-party dependency in the core of the app, and the whole mechanism fits in one file a new engineer can read.
- It is deliberately minimal: no stale time, no retry policy, no pagination helper, no persistence, no optimistic-update helper. Add each when a feature needs it, with a test, on both platforms.
- If requirements outgrow it, the seam is `QueryView` / `QueryContent`: screens would not change.
