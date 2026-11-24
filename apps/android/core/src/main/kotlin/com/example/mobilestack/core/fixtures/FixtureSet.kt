package com.example.mobilestack.core.fixtures

import com.example.mobilestack.core.auth.InMemoryTokenStore
import com.example.mobilestack.core.auth.TokenStore
import com.example.mobilestack.core.config.FeatureFlag
import com.example.mobilestack.core.config.FeatureFlagProvider
import com.example.mobilestack.core.network.ApiClient
import com.example.mobilestack.core.network.FixtureTransport
import com.example.mobilestack.core.network.HttpResponse
import com.example.mobilestack.core.platform.InMemoryPreferencesStore
import com.example.mobilestack.core.platform.PreferencesStore
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** One scenario file from `packages/contracts/fixtures`. Both platforms read the same files. */
@Serializable
data class FixtureScenario(
    val session: String? = null,
    val http: List<Response> = emptyList(),
    val flags: Map<String, Boolean> = emptyMap(),
    val preferences: Map<String, Boolean> = emptyMap(),
) {
    @Serializable
    data class Response(val method: String, val path: String, val status: Int, val body: JsonElement? = null)
}

/** Scenarios merged in order: a later one wins for the same request, flag or preference. */
class FixtureSet(private val scenarios: List<FixtureScenario>) {
    val tokens: TokenStore get() = InMemoryTokenStore(scenarios.mapNotNull { it.session }.lastOrNull())

    val preferences: PreferencesStore
        get() = InMemoryPreferencesStore(
            scenarios.fold(emptyMap()) { merged, scenario ->
                merged + scenario.preferences
            },
        )

    val flagProvider: FeatureFlagProvider
        get() {
            val flags = scenarios.fold(emptyMap<String, Boolean>()) { merged, scenario -> merged + scenario.flags }
            return FeatureFlagProvider { flag: FeatureFlag -> flags[flag.key] }
        }

    /**
     * With a [recorder], a request no scenario answers is a violation: the suite stays deterministic
     * because nothing can reach, or silently miss, the network.
     */
    fun transport(recorder: FixtureViolations? = null): FixtureTransport {
        val responses = scenarios.flatMap { it.http }
        return FixtureTransport { method, path ->
            val segments = path.split("/").filter(String::isNotEmpty)
            val endpoint = segments.drop(segments.indexOf(ApiClient.API_VERSION) + 1).joinToString("/")
            val match = responses.lastOrNull { it.method == method && it.path == endpoint }
            if (match == null) recorder?.record("no fixture for $method $endpoint")
            match?.let { HttpResponse(it.status, it.body?.toString().orEmpty()) }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(scenarios: List<String>) = FixtureSet(scenarios.map { json.decodeFromString<FixtureScenario>(it) })

        fun load(names: List<String>, directory: File) = parse(names.map { File(directory, "$it.json").readText() })
    }
}

class FixtureViolations {
    private val recorded = CopyOnWriteArrayList<String>()

    val all: List<String> get() = recorded.toList()

    fun record(violation: String) {
        recorded += violation
    }
}
