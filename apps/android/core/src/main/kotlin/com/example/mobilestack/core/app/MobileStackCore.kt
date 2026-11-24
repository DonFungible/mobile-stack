package com.example.mobilestack.core.app

import com.example.mobilestack.core.auth.SessionStore
import com.example.mobilestack.core.config.FeatureFlags
import com.example.mobilestack.core.navigation.DeepLinkParser
import com.example.mobilestack.core.network.ApiClient
import com.example.mobilestack.core.platform.Platform
import com.example.mobilestack.core.query.QueryClient
import com.example.mobilestack.data.ItemsRepository
import com.example.mobilestack.data.SystemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * The whole app without its UI. The mobile app and the CLI are both just hosts that construct this.
 * Construct it, and call everything on it, from the thread behind [Platform.coreDispatcher].
 */
class MobileStackCore(platform: Platform) {
    val app: AppViewModel

    init {
        val session = SessionStore(platform.tokens)
        val queries = QueryClient()
        val api = ApiClient(
            baseUrl = platform.config.apiBaseUrl,
            transport = platform.transport,
            tokens = platform.tokens,
            reporter = platform.reporter,
            onUnauthorized = session::markExpired,
            ioDispatcher = platform.ioDispatcher,
        )
        session.registerSignOutCleanup {
            queries.clear()
            platform.analytics.reset()
        }
        app = AppViewModel(
            Dependencies(
                scope = CoroutineScope(SupervisorJob() + platform.coreDispatcher),
                config = platform.config,
                storeUrl = platform.storeUrl,
                session = session,
                queries = queries,
                items = ItemsRepository(api),
                system = SystemRepository(api),
                flags = FeatureFlags(platform.flagProvider, platform.config.environment, platform.flagOverrides),
                deepLinks = DeepLinkParser(platform.config.urlScheme, platform.config.universalLinkHosts),
                preferences = platform.preferences,
                haptics = Haptics(platform.haptics, platform.preferences),
                external = platform.external,
                analytics = platform.analytics,
            ),
        )
        app.start()
    }
}
