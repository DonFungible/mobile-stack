import AppCore
import Foundation

/// The mobile host: constructs the core with real platform services. The CLI does the same with fixtures.
enum AppHost {
    static func makeCore(bundle: Bundle = .main) -> MobileStackCore {
        let info = bundle.infoDictionary ?? [:]
        do {
            let config = try AppConfig(infoDictionary: info)
            var platform = Platform(
                config: config,
                urlScheme: info["URL_SCHEME"] as? String ?? "",
                // Replace with your App Store id once the listing exists.
                storeURL: "https://apps.apple.com/app/id0000000000",
                transport: URLSessionTransport(),
                tokens: KeychainTokenStore(service: bundle.bundleIdentifier ?? "app"),
                preferences: UserDefaultsPreferencesStore(),
                flagOverrides: [.itemSharing],
                haptics: SystemHaptics(),
                external: SystemExternalActions()
            )
            if config.useFixtures {
                platform.transport = try bundledFixtures(in: bundle).transport()
            }
            return MobileStackCore(platform: platform)
        } catch {
            fatalError("Invalid build configuration: \(error). Check Config/*.xcconfig.")
        }
    }

    /// Builds with USE_FIXTURES = YES answer from the same scenario files the behavior tests use.
    private static func bundledFixtures(in bundle: Bundle) throws -> FixtureSet {
        guard let directory = bundle.url(forResource: "fixtures", withExtension: nil) else {
            throw FixtureSet.LoadError.missing("fixtures")
        }
        return try FixtureSet.load(names: ["App.default", "Items.three"], directory: directory)
    }
}
