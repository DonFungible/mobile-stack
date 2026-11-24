import Foundation

public extension QueryKey {
    static func items() -> QueryKey {
        QueryKey("items")
    }

    static func item(id: Int?) -> QueryKey {
        QueryKey("items", id.map(String.init))
    }
}

public struct ItemsRepository: Sendable {
    private let api: APIClient

    public init(api: APIClient) {
        self.api = api
    }

    public func items() async throws -> [Item] {
        try await api.get("items")
    }

    public func item(id: Int) async throws -> Item {
        try await api.get("items/\(id)")
    }
}
