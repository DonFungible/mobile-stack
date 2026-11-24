package com.example.mobilestack

import com.example.mobilestack.core.config.AppConfig
import com.example.mobilestack.core.config.FeatureFlag
import com.example.mobilestack.core.config.FeatureFlags
import com.example.mobilestack.core.system.UpdatePolicy
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ConfigTest {
    private fun config(
        environment: String = "staging",
        apiBaseUrl: String = "https://staging.example.com",
        useFixtures: Boolean = true,
    ) = AppConfig.from(
        environment = environment,
        apiBaseUrl = apiBaseUrl,
        useFixtures = useFixtures,
        appVersion = "1.2.0",
        urlScheme = "mobilestack",
        universalLinkHosts = "a.example.com, b.example.com",
    )

    @Test
    fun `reads a valid configuration`() {
        val config = config()
        assertEquals(AppConfig.Environment.Staging, config.environment)
        assertEquals(setOf("a.example.com", "b.example.com"), config.universalLinkHosts)
        assertFalse(config.isProduction)
    }

    @Test
    fun `rejects an unknown environment`() {
        assertFailsWith<AppConfig.InvalidConfig> { config(environment = "qa") }
    }

    @Test
    fun `rejects a base url that is not https`() {
        for (url in listOf("http://insecure.example.com", "not a url", "https://", "")) {
            assertFailsWith<AppConfig.InvalidConfig>(url) { config(apiBaseUrl = url) }
        }
    }

    @Test
    fun `production can never run on fixtures`() {
        assertFailsWith<AppConfig.InvalidConfig> { config(environment = "production") }
    }

    @Test
    fun `flags fail closed`() {
        val flags = FeatureFlags(provider = { null }, environment = AppConfig.Environment.Production)
        assertFalse(flags.isEnabled(FeatureFlag.ItemSharing))
    }

    @Test
    fun `staging overrides never leak into production`() {
        val overrides = setOf(FeatureFlag.ItemSharing)
        val staging = FeatureFlags({ null }, AppConfig.Environment.Staging, overrides)
        val production = FeatureFlags({ null }, AppConfig.Environment.Production, overrides)
        assertTrue(staging.isEnabled(FeatureFlag.ItemSharing))
        assertFalse(production.isEnabled(FeatureFlag.ItemSharing))
    }

    @Test
    fun `update is forced only below a well formed minimum`() {
        val cases = listOf(
            Triple("1.2.0", "1.3.0", true),
            Triple("1.3.0", "1.3.0", false),
            Triple("2.0", "1.9.9", false),
            Triple("1.9", "1.10", true),
            Triple("1.0.0", "garbage", false),
            Triple("garbage", "1.0.0", false),
            Triple("1.0.0", "1.-1", false),
        )
        for ((current, minimum, expected) in cases) {
            assertEquals(expected, UpdatePolicy.isUpdateRequired(current, minimum), "$current vs $minimum")
        }
    }
}
