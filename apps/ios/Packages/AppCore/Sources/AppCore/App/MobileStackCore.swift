import Foundation

/// The whole app without its UI. The mobile app and the CLI are both just hosts that construct this.
@MainActor
public final class MobileStackCore {
    public let app: AppViewModel

    public init(platform: Platform) {
        let session = SessionStore(tokens: platform.tokens)
        let queries = QueryClient()
        let api = APIClient(
            baseURL: platform.config.apiBaseURL,
            transport: platform.transport,
            tokens: platform.tokens,
            reporter: platform.reporter,
            onUnauthorized: { [weak session] in await session?.markExpired() }
        )
        let dependencies = Dependencies(
            config: platform.config,
            storeURL: platform.storeURL,
            session: session,
            queries: queries,
            items: ItemsRepository(api: api),
            system: SystemRepository(api: api),
            flags: FeatureFlags(
                provider: platform.flagProvider,
                environment: platform.config.environment,
                stagingOverrides: platform.flagOverrides
            ),
            deepLinks: DeepLinkParser(
                scheme: platform.urlScheme,
                universalLinkHosts: platform.config.universalLinkHosts
            ),
            preferences: platform.preferences,
            haptics: Haptics(player: platform.haptics, preferences: platform.preferences),
            external: platform.external,
            analytics: platform.analytics
        )
        session.registerSignOutCleanup {
            queries.clear()
            platform.analytics.reset()
        }
        app = AppViewModel(dependencies: dependencies)
        app.start()
    }
}
