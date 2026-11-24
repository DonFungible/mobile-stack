import Foundation

public struct AppConfig: Equatable, Sendable {
    public enum Environment: String, Sendable, CaseIterable {
        case development
        case staging
        case production
    }

    public enum ConfigError: Error, Equatable {
        case missing(String)
        case invalid(key: String, value: String)
    }

    public let environment: Environment
    public let apiBaseURL: URL
    public let useFixtures: Bool
    public let appVersion: String
    public let universalLinkHosts: Set<String>

    public var isProduction: Bool {
        environment == .production
    }

    public init(
        environment: Environment,
        apiBaseURL: URL,
        useFixtures: Bool,
        appVersion: String,
        universalLinkHosts: Set<String>
    ) {
        self.environment = environment
        self.apiBaseURL = apiBaseURL
        self.useFixtures = useFixtures
        self.appVersion = appVersion
        self.universalLinkHosts = universalLinkHosts
    }

    /// Fails at launch on a bad build setting instead of at the first request.
    public init(infoDictionary: [String: Any]) throws {
        let environmentValue = try Self.string("APP_ENVIRONMENT", in: infoDictionary)
        guard let environment = Environment(rawValue: environmentValue) else {
            throw ConfigError.invalid(key: "APP_ENVIRONMENT", value: environmentValue)
        }
        let urlValue = try Self.string("API_BASE_URL", in: infoDictionary)
        guard let url = URL(string: urlValue), url.scheme == "https", url.host() != nil else {
            throw ConfigError.invalid(key: "API_BASE_URL", value: urlValue)
        }
        let useFixtures = (infoDictionary["USE_FIXTURES"] as? String) == "YES"
        if useFixtures, environment == .production {
            throw ConfigError.invalid(key: "USE_FIXTURES", value: "YES")
        }
        let hosts = (infoDictionary["UNIVERSAL_LINK_HOSTS"] as? String ?? "")
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }

        self.environment = environment
        apiBaseURL = url
        self.useFixtures = useFixtures
        appVersion = try Self.string("CFBundleShortVersionString", in: infoDictionary)
        universalLinkHosts = Set(hosts)
    }

    private static func string(_ key: String, in dictionary: [String: Any]) throws -> String {
        guard let value = dictionary[key] as? String, !value.isEmpty else {
            throw ConfigError.missing(key)
        }
        return value
    }
}
