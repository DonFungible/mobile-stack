package com.example.mobilestack.core.config

enum class FeatureFlag(val key: String) {
    ItemSharing("item_sharing"),
}

fun interface FeatureFlagProvider {
    /** `null` means the provider has no answer yet. */
    fun value(flag: FeatureFlag): Boolean?
}

class FeatureFlags(
    private val provider: FeatureFlagProvider,
    private val environment: AppConfig.Environment,
    private val stagingOverrides: Set<FeatureFlag> = emptySet(),
) {
    /** Fails closed: unfinished work stays hidden when the flag service is unreachable. */
    fun isEnabled(flag: FeatureFlag): Boolean {
        if (environment != AppConfig.Environment.Production && flag in stagingOverrides) return true
        return provider.value(flag) == true
    }
}
