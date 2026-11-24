import Foundation

public enum FeatureFlag: String, CaseIterable, Sendable {
    case itemSharing = "item_sharing"
}

public protocol FeatureFlagProvider: Sendable {
    /// `nil` means the provider has no answer yet.
    func value(for flag: FeatureFlag) -> Bool?
}

public struct FeatureFlags: Sendable {
    private let provider: FeatureFlagProvider
    private let stagingOverrides: Set<FeatureFlag>
    private let environment: AppConfig.Environment

    public init(
        provider: FeatureFlagProvider,
        environment: AppConfig.Environment,
        stagingOverrides: Set<FeatureFlag> = []
    ) {
        self.provider = provider
        self.environment = environment
        self.stagingOverrides = stagingOverrides
    }

    /// Fails closed: unfinished work stays hidden when the flag service is unreachable.
    public func isEnabled(_ flag: FeatureFlag) -> Bool {
        if environment != .production, stagingOverrides.contains(flag) {
            return true
        }
        return provider.value(for: flag) == true
    }
}

public struct StaticFeatureFlagProvider: FeatureFlagProvider {
    private let values: [FeatureFlag: Bool]

    public init(_ values: [FeatureFlag: Bool] = [:]) {
        self.values = values
    }

    public func value(for flag: FeatureFlag) -> Bool? {
        values[flag]
    }
}
