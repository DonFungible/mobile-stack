@testable import AppCore
import Foundation
import Testing

@MainActor
struct QueryTests {
    private actor Counter {
        private(set) var count = 0

        func next() async throws -> Int {
            try await Task.sleep(for: .milliseconds(5))
            count += 1
            return count
        }
    }

    @Test func trailingNilsMakeAKeyAPrefix() {
        #expect(QueryKey("items", nil) == QueryKey("items"))
        #expect(QueryKey.item(id: 4).hasPrefix(.item(id: nil)))
        #expect(!QueryKey("users", "4").hasPrefix(QueryKey("items")))
    }

    @Test func fetchServesTheCacheUntilForced() async throws {
        let counter = Counter()
        let client = QueryClient()

        let first: Int = try await client.fetch(.items()) { try await counter.next() }
        let second: Int = try await client.fetch(.items()) { try await counter.next() }
        let forced: Int = try await client.fetch(.items(), force: true) { try await counter.next() }

        #expect(first == 1)
        #expect(second == 1)
        #expect(forced == 2)
    }

    @Test func concurrentCallersShareOneRequest() async throws {
        let counter = Counter()
        let client = QueryClient()

        async let first: Int = client.fetch(.items(), force: true) { try await counter.next() }
        async let second: Int = client.fetch(.items(), force: true) { try await counter.next() }

        let results = try await (first, second)
        #expect(results.0 == 1)
        #expect(results.1 == 1)
        #expect(await counter.count == 1)
    }

    @Test func invalidatingAPrefixDropsOnlyMatchingEntries() {
        let client = QueryClient()
        client.set(.item(id: 4), value: 1)
        client.set(QueryKey("users"), value: 2)

        client.invalidate(.item(id: nil))

        let item: Int? = client.cached(.item(id: 4))
        let users: Int? = client.cached(QueryKey("users"))
        #expect(item == nil)
        #expect(users == 2)
    }

    @Test func aFailedFetchCachesNothing() async {
        let client = QueryClient()
        await #expect(throws: APIError.network) {
            let _: Int = try await client.fetch(.items()) { throw APIError.network }
        }
        let cached: Int? = client.cached(.items())
        #expect(cached == nil)
    }
}
