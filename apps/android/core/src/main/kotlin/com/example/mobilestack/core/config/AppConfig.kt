package com.example.mobilestack.core.config

import java.net.URI
import java.net.URISyntaxException

data class AppConfig(
    val environment: Environment,
    val apiBaseUrl: String,
    val useFixtures: Boolean,
    val appVersion: String,
    val urlScheme: String,
    val universalLinkHosts: Set<String>,
) {
    enum class Environment(val value: String) {
        Development("development"),
        Staging("staging"),
        Production("production"),
    }

    class InvalidConfig(key: String, value: String) :
        IllegalStateException("Invalid build configuration: $key=\"$value\"")

    val isProduction: Boolean get() = environment == Environment.Production

    companion object {
        /** Fails at launch on a bad build setting instead of at the first request. */
        fun from(
            environment: String,
            apiBaseUrl: String,
            useFixtures: Boolean,
            appVersion: String,
            urlScheme: String,
            universalLinkHosts: String,
        ): AppConfig {
            val parsedEnvironment = Environment.entries.firstOrNull { it.value == environment }
                ?: throw InvalidConfig("APP_ENVIRONMENT", environment)
            if (!isHttpsUrl(apiBaseUrl)) throw InvalidConfig("API_BASE_URL", apiBaseUrl)
            if (useFixtures && parsedEnvironment == Environment.Production) throw InvalidConfig("USE_FIXTURES", "true")
            if (appVersion.isBlank()) throw InvalidConfig("versionName", appVersion)
            if (urlScheme.isBlank()) throw InvalidConfig("URL_SCHEME", urlScheme)
            return AppConfig(
                environment = parsedEnvironment,
                apiBaseUrl = apiBaseUrl,
                useFixtures = useFixtures,
                appVersion = appVersion,
                urlScheme = urlScheme,
                universalLinkHosts = universalLinkHosts.split(",").map(String::trim).filter(String::isNotEmpty).toSet(),
            )
        }

        private fun isHttpsUrl(value: String): Boolean = try {
            val uri = URI(value)
            uri.scheme == "https" && !uri.host.isNullOrEmpty()
        } catch (_: URISyntaxException) {
            false
        }
    }
}
