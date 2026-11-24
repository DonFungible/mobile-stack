package com.example.mobilestack.core.app

import com.example.mobilestack.core.auth.SessionStore
import com.example.mobilestack.core.config.AppConfig
import com.example.mobilestack.core.config.FeatureFlags
import com.example.mobilestack.core.navigation.DeepLinkParser
import com.example.mobilestack.core.navigation.Route
import com.example.mobilestack.core.platform.ExternalActions
import com.example.mobilestack.core.platform.HapticEvent
import com.example.mobilestack.core.platform.HapticsPlayer
import com.example.mobilestack.core.platform.PreferencesStore
import com.example.mobilestack.core.platform.StorageKey
import com.example.mobilestack.core.query.QueryClient
import com.example.mobilestack.core.services.Analytics
import com.example.mobilestack.data.ItemsRepository
import com.example.mobilestack.data.SystemRepository
import kotlinx.coroutines.CoroutineScope

/** What view models are constructed with. Built once by the core from the platform it was given. */
internal class Dependencies(
    val scope: CoroutineScope,
    val config: AppConfig,
    val storeUrl: String,
    val session: SessionStore,
    val queries: QueryClient,
    val items: ItemsRepository,
    val system: SystemRepository,
    val flags: FeatureFlags,
    val deepLinks: DeepLinkParser,
    val preferences: PreferencesStore,
    val haptics: Haptics,
    val external: ExternalActions,
    val analytics: Analytics,
)

/** Plays feedback only when the user has not turned it off, so the preference is checked in one place. */
internal class Haptics(private val player: HapticsPlayer, private val preferences: PreferencesStore) {
    val isEnabled: Boolean get() = preferences.boolean(StorageKey.HAPTICS_ENABLED) ?: true

    fun play(event: HapticEvent) {
        if (isEnabled) player.play(event)
    }
}

/** How a screen asks to move. The stack navigator implements it; pushing is a state change, not a view. */
internal fun interface Navigating {
    fun push(route: Route)
}
