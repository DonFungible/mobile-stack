package com.example.mobilestack.core.platform

import com.example.mobilestack.core.auth.TokenStore
import com.example.mobilestack.core.config.AppConfig
import com.example.mobilestack.core.config.FeatureFlag
import com.example.mobilestack.core.config.FeatureFlagProvider
import com.example.mobilestack.core.network.HttpTransport
import com.example.mobilestack.core.services.Analytics
import com.example.mobilestack.core.services.ConsoleAnalytics
import com.example.mobilestack.core.services.ConsoleErrorReporter
import com.example.mobilestack.core.services.ErrorReporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Everything the core needs from outside the process. The app passes real implementations;
 * the CLI passes fixtures. The core never reaches for a platform API itself.
 */
class Platform(
    val config: AppConfig,
    val storeUrl: String,
    val transport: HttpTransport,
    val tokens: TokenStore,
    val preferences: PreferencesStore,
    /** The single thread view models live on: the main thread in the app, a dedicated one in the CLI. */
    val coreDispatcher: CoroutineDispatcher,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val flagProvider: FeatureFlagProvider = FeatureFlagProvider { null },
    val flagOverrides: Set<FeatureFlag> = emptySet(),
    val haptics: HapticsPlayer = HapticsPlayer {},
    val external: ExternalActions = NoExternalActions,
    val analytics: Analytics = ConsoleAnalytics(),
    val reporter: ErrorReporter = ConsoleErrorReporter(),
)

/** The registry of every persisted key. User-scoped keys take the user id so accounts never share state. */
object StorageKey {
    const val HAPTICS_ENABLED = "haptics_enabled"

    fun draft(userId: String) = "${userId}_draft"
}

interface PreferencesStore {
    fun boolean(key: String): Boolean?

    fun set(key: String, value: Boolean)
}

class InMemoryPreferencesStore(values: Map<String, Boolean> = emptyMap()) : PreferencesStore {
    private val values = values.toMutableMap()

    override fun boolean(key: String): Boolean? = values[key]

    override fun set(key: String, value: Boolean) {
        values[key] = value
    }
}

/** Semantic feedback events. Callers say what happened, not which constant to fire. */
enum class HapticEvent { Selection, Success, Failure }

fun interface HapticsPlayer {
    fun play(event: HapticEvent)
}

/** Hand-offs that leave the app: opening a URL, the share sheet. */
interface ExternalActions {
    fun open(url: String)

    fun share(text: String)
}

object NoExternalActions : ExternalActions {
    override fun open(url: String) = Unit

    override fun share(text: String) = Unit
}
