import Foundation

public struct AppVersion: Comparable, Equatable, Sendable {
    public let components: [Int]

    public init?(_ string: String) {
        let fields = string.split(separator: ".", omittingEmptySubsequences: false)
        let parts = fields.compactMap { Int($0) }.filter { $0 >= 0 }
        guard !fields.isEmpty, fields.count <= 3, parts.count == fields.count else { return nil }
        components = parts + Array(repeating: 0, count: 3 - parts.count)
    }

    public static func < (lhs: AppVersion, rhs: AppVersion) -> Bool {
        lhs.components.lexicographicallyPrecedes(rhs.components)
    }
}

public struct MinimumVersion: Codable, Equatable, Sendable {
    public let ios: String
    public let android: String
}

public enum UpdatePolicy {
    /// An unreadable version never locks users out: only a well-formed, higher minimum forces an update.
    public static func isUpdateRequired(current: String, minimum: String) -> Bool {
        guard let current = AppVersion(current), let minimum = AppVersion(minimum) else { return false }
        return current < minimum
    }
}

public struct SystemRepository: Sendable {
    private let api: APIClient

    public init(api: APIClient) {
        self.api = api
    }

    public func minimumVersion() async throws -> MinimumVersion {
        try await api.get("min-version")
    }
}
