package com.example.mobilestack.cli

import com.example.mobilestack.core.app.MobileStackCore
import com.example.mobilestack.core.config.AppConfig
import com.example.mobilestack.core.fixtures.FixtureSet
import com.example.mobilestack.core.fixtures.FixtureViolations
import com.example.mobilestack.core.introspection.introspectionNode
import com.example.mobilestack.core.platform.Platform
import com.example.mobilestack.core.services.Analytics
import com.example.mobilestack.core.services.AnalyticsEvent
import com.example.mobilestack.core.services.Log
import com.example.mobilestack.introspection.IntrospectionSession
import java.io.File
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.Executors
import kotlin.system.exitProcess
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// The headless host: the same core the app runs, constructed with fixtures instead of platform services.
//
//   cli --fixtures-dir <dir> [--fixtures Name,Name] [--app-version 1.2.3]
//
// Then one command per line on stdin, one answer per line on stdout: `ls`, `do Action [json]`,
// or the JSON form the test SDK speaks. The JSON form adds `boot`, which replaces the running core
// with a fresh one on new fixtures, so one warm process can serve a whole test run.

fun main(arguments: Array<String>) {
    fun argument(name: String) = arguments.indexOf(name).takeIf { it >= 0 }?.let { arguments.getOrNull(it + 1) }

    fun fail(message: String): Nothing {
        System.err.println("cli: $message")
        exitProcess(2)
    }

    Log.sink = { _, _ -> }
    val host = Host(File(argument("--fixtures-dir") ?: fail("--fixtures-dir is required")))
    argument("--fixtures")?.let { names ->
        try {
            host.boot(names.split(",").filter(String::isNotEmpty), argument("--app-version"))
        } catch (error: IOException) {
            fail("cannot load fixtures $names: ${error.message}")
        }
    }

    generateSequence(::readlnOrNull).forEach { line ->
        println(host.handle(line))
        System.out.flush()
    }
}

private class Host(private val fixturesDirectory: File) {
    // One thread owns the view models, exactly as the main thread does in the app.
    private val coreThread = Executors
        .newSingleThreadExecutor { Thread(it, "core").apply { isDaemon = true } }
        .asCoroutineDispatcher()
    private var session: IntrospectionSession? = null

    fun boot(fixtureNames: List<String>, appVersion: String?) {
        val fixtures = FixtureSet.load(fixtureNames, fixturesDirectory)
        val violations = FixtureViolations()
        installNetworkGuard(violations)
        val core = runBlocking(coreThread) {
            MobileStackCore(
                Platform(
                    config = AppConfig.from(
                        environment = "development",
                        apiBaseUrl = "https://fixtures.invalid",
                        useFixtures = true,
                        appVersion = appVersion ?: "1.0.0",
                        urlScheme = "mobilestack",
                        universalLinkHosts = "mobilestack.example.com",
                    ),
                    storeUrl = "https://play.google.com/store/apps/details?id=com.example.mobilestack",
                    transport = fixtures.transport(violations),
                    tokens = fixtures.tokens,
                    preferences = fixtures.preferences,
                    coreDispatcher = coreThread,
                    flagProvider = fixtures.flagProvider,
                    analytics = SilentAnalytics,
                    reporter = { _, _ -> },
                ),
            )
        }
        session = IntrospectionSession({ core.introspectionNode }, { violations.all })
    }

    fun handle(line: String): String {
        val isJson = line.trimStart().startsWith("{")
        val request = if (isJson) runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull() else null
        if ((request?.get("command") as? JsonPrimitive)?.content == "boot") return boot(request)
        val current = session ?: return NOT_BOOTED
        return runBlocking(coreThread) { current.handle(line) }
    }

    private fun boot(request: JsonObject): String {
        val id = request["id"] ?: JsonNull
        val names = (request["fixtures"] as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
        return try {
            boot(names, (request["appVersion"] as? JsonPrimitive)?.takeIf { it.isString }?.content)
            handle(
                buildJsonObject {
                    put("id", id)
                    put("command", "ls")
                }.toString(),
            )
        } catch (error: IOException) {
            buildJsonObject {
                put("id", id)
                put("ok", false)
                putJsonObject("error") {
                    put("code", "bad_fixtures")
                    put("message", "cannot load $names: ${error.message}")
                }
            }.toString()
        }
    }

    private companion object {
        const val NOT_BOOTED =
            """{"ok":false,"error":{"code":"not_booted","message":"send boot, or start with --fixtures"}}"""
    }
}

/**
 * Zero network, enforced: every JVM connection asks the default proxy selector first, so refusing there
 * blocks and records any request, even if the app caught the error and recovered.
 */
private fun installNetworkGuard(violations: FixtureViolations) {
    ProxySelector.setDefault(
        object : ProxySelector() {
            override fun select(uri: URI): List<Proxy> {
                violations.record("network request attempted: $uri")
                throw IllegalStateException("network is blocked in a behavior session")
            }

            override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) = Unit
        },
    )
}

private object SilentAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit

    override fun identify(userId: String) = Unit

    override fun reset() = Unit
}
