@testable import AppCore
import Foundation
import Testing

struct ConfigTests {
    private let valid: [String: Any] = [
        "APP_ENVIRONMENT": "staging",
        "API_BASE_URL": "https://staging.example.com",
        "USE_FIXTURES": "YES",
        "UNIVERSAL_LINK_HOSTS": "a.example.com, b.example.com",
        "CFBundleShortVersionString": "1.2.0",
    ]

    @Test func readsAValidConfiguration() throws {
        let config = try AppConfig(infoDictionary: valid)
        #expect(config.environment == .staging)
        #expect(config.apiBaseURL.absoluteString == "https://staging.example.com")
        #expect(config.useFixtures)
        #expect(config.universalLinkHosts == ["a.example.com", "b.example.com"])
        #expect(!config.isProduction)
    }

    @Test func aMissingKeyFailsAtLaunch() {
        var info = valid
        info["API_BASE_URL"] = nil
        #expect(throws: AppConfig.ConfigError.missing("API_BASE_URL")) {
            try AppConfig(infoDictionary: info)
        }
    }

    @Test(arguments: ["http://insecure.example.com", "not a url", "https://"])
    func rejectsABaseURLThatIsNotHTTPS(url: String) {
        var info = valid
        info["API_BASE_URL"] = url
        #expect(throws: AppConfig.ConfigError.self) { try AppConfig(infoDictionary: info) }
    }

    @Test func productionCanNeverRunOnFixtures() {
        var info = valid
        info["APP_ENVIRONMENT"] = "production"
        #expect(throws: AppConfig.ConfigError.self) { try AppConfig(infoDictionary: info) }
    }

    @Test func flagsFailClosed() {
        let flags = FeatureFlags(provider: StaticFeatureFlagProvider(), environment: .production)
        #expect(!flags.isEnabled(.itemSharing))
    }

    @Test func stagingOverridesNeverLeakIntoProduction() {
        let provider = StaticFeatureFlagProvider()
        let staging = FeatureFlags(provider: provider, environment: .staging, stagingOverrides: [.itemSharing])
        let production = FeatureFlags(provider: provider, environment: .production, stagingOverrides: [.itemSharing])
        #expect(staging.isEnabled(.itemSharing))
        #expect(!production.isEnabled(.itemSharing))
    }

    @Test(arguments: [
        ("1.2.0", "1.3.0", true),
        ("1.3.0", "1.3.0", false),
        ("2.0", "1.9.9", false),
        ("1.9", "1.10", true),
        ("1.0.0", "garbage", false),
        ("garbage", "1.0.0", false),
    ])
    func updateIsForcedOnlyBelowAWellFormedMinimum(current: String, minimum: String, expected: Bool) {
        #expect(UpdatePolicy.isUpdateRequired(current: current, minimum: minimum) == expected)
    }
}
