import Foundation

/// The server-state cache. View models read and fetch through it, so a revisited screen starts from the
/// last good value and two screens asking for the same key share one request.
@MainActor
public final class QueryClient {
    private var cache: [QueryKey: any Sendable] = [:]
    private var inFlight: [QueryKey: Task<any Sendable, Error>] = [:]

    public init() {}

    public func cached<Value: Sendable>(_ key: QueryKey) -> Value? {
        cache[key] as? Value
    }

    public func set(_ key: QueryKey, value: some Sendable) {
        cache[key] = value
    }

    /// Returns the cached value unless `force` is set; concurrent callers for one key share a request.
    public func fetch<Value: Sendable>(
        _ key: QueryKey,
        force: Bool = false,
        _ fetcher: @escaping @Sendable () async throws -> Value
    ) async throws -> Value {
        if !force, let cached: Value = cached(key) {
            return cached
        }
        if let running = inFlight[key], let value = try await running.value as? Value {
            return value
        }
        let task = Task<any Sendable, Error> { try await fetcher() }
        inFlight[key] = task
        defer { inFlight[key] = nil }
        let value = try await task.value
        cache[key] = value
        guard let typed = value as? Value else { throw APIError.network }
        return typed
    }

    /// Drops every entry under `prefix`; the next read refetches.
    public func invalidate(_ prefix: QueryKey) {
        cache = cache.filter { !$0.key.hasPrefix(prefix) }
    }

    public func clear() {
        cache.removeAll()
    }
}
