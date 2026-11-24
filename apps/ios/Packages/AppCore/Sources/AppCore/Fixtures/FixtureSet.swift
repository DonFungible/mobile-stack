import Foundation
import IntrospectionKit

/// One scenario file from `packages/contracts/fixtures`. Both platforms read the same files.
public struct FixtureScenario: Decodable, Sendable {
    public struct Response: Decodable, Sendable {
        public let method: String
        public let path: String
        public let status: Int
        public let body: JSONValue?
    }

    public var session: String?
    public var http: [Response]?
    public var flags: [String: Bool]?
    public var preferences: [String: Bool]?
}

/// Scenarios merged in order: a later one wins for the same request, flag or preference.
public struct FixtureSet: Sendable {
    public enum LoadError: Error {
        case missing(String)
    }

    private let scenarios: [FixtureScenario]

    public init(_ scenarios: [FixtureScenario]) {
        self.scenarios = scenarios
    }

    public static func load(names: [String], directory: URL) throws -> FixtureSet {
        try FixtureSet(names.map { name in
            let url = directory.appending(path: "\(name).json")
            guard let data = try? Data(contentsOf: url) else { throw LoadError.missing(name) }
            return try JSONDecoder().decode(FixtureScenario.self, from: data)
        })
    }

    public var tokens: TokenStore {
        InMemoryTokenStore(token: scenarios.compactMap(\.session).last)
    }

    public var preferences: PreferencesStore {
        InMemoryPreferencesStore(merged(\.preferences))
    }

    public var flagProvider: FeatureFlagProvider {
        let flags = merged(\.flags)
        return StaticFeatureFlagProvider(Dictionary(
            uniqueKeysWithValues: FeatureFlag.allCases.compactMap { flag in flags[flag.rawValue].map { (flag, $0) } }
        ))
    }

    /// With a `recorder`, a request no scenario answers is a violation: the suite stays deterministic
    /// because nothing can reach, or silently miss, the network.
    public func transport(recorder: FixtureViolations? = nil) -> FixtureTransport {
        let responses = scenarios.flatMap { $0.http ?? [] }
        return FixtureTransport { method, path in
            let segments = path.split(separator: "/").map(String.init)
            let version = segments.firstIndex(of: APIClient.apiVersion).map { $0 + 1 } ?? 0
            let endpoint = segments[version...].joined(separator: "/")
            if let match = responses.last(where: { $0.method == method && $0.path == endpoint }) {
                return HTTPResponse(status: match.status, body: match.body?.data ?? Data())
            }
            recorder?.record("no fixture for \(method) \(endpoint)")
            return nil
        }
    }

    private func merged(_ field: (FixtureScenario) -> [String: Bool]?) -> [String: Bool] {
        scenarios.compactMap(field).reduce(into: [:]) { $0.merge($1) { _, new in new } }
    }
}

public final class FixtureViolations: @unchecked Sendable {
    private let lock = NSLock()
    private var recorded: [String] = []

    public init() {}

    public var all: [String] {
        lock.withLock { recorded }
    }

    public func record(_ violation: String) {
        lock.withLock { recorded.append(violation) }
    }
}
