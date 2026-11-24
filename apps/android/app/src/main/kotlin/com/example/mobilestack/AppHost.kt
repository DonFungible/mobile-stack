package com.example.mobilestack

import android.content.Context
import com.example.mobilestack.core.app.MobileStackCore
import com.example.mobilestack.core.config.AppConfig
import com.example.mobilestack.core.config.FeatureFlag
import com.example.mobilestack.core.fixtures.FixtureSet
import com.example.mobilestack.core.platform.Platform
import com.example.mobilestack.platform.KeystoreTokenStore
import com.example.mobilestack.platform.OkHttpTransport
import com.example.mobilestack.platform.SharedPreferencesStore
import com.example.mobilestack.platform.SystemExternalActions
import com.example.mobilestack.platform.SystemHaptics
import com.example.mobilestack.platform.routeLogsToLogcat
import kotlinx.coroutines.Dispatchers

/** The mobile host: constructs the core with real platform services. The CLI does the same with fixtures. */
class AppHost(context: Context) {
    val haptics = SystemHaptics()

    val core: MobileStackCore

    init {
        routeLogsToLogcat()
        val config = AppConfig.from(
            environment = BuildConfig.APP_ENVIRONMENT,
            apiBaseUrl = BuildConfig.API_BASE_URL,
            useFixtures = BuildConfig.USE_FIXTURES,
            appVersion = BuildConfig.VERSION_NAME,
            urlScheme = BuildConfig.URL_SCHEME,
            universalLinkHosts = BuildConfig.UNIVERSAL_LINK_HOSTS,
        )
        core = MobileStackCore(
            Platform(
                config = config,
                storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}",
                transport = if (config.useFixtures) bundledFixtures(context).transport() else OkHttpTransport(),
                tokens = KeystoreTokenStore(context),
                preferences = SharedPreferencesStore(context),
                // Not `Main.immediate`: service results must land after the current update, never inside it.
                coreDispatcher = Dispatchers.Main,
                flagOverrides = setOf(FeatureFlag.ItemSharing),
                haptics = haptics,
                external = SystemExternalActions(context),
            ),
        )
    }

    /** Builds with USE_FIXTURES = true answer from the same scenario files the behavior tests use. */
    private fun bundledFixtures(context: Context) = FixtureSet.parse(
        listOf("App.default", "Items.three").map { name ->
            context.assets.open("$name.json").bufferedReader().use { it.readText() }
        },
    )
}
