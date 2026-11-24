package com.example.mobilestack.core.navigation

import java.net.URI
import java.net.URISyntaxException

class DeepLinkParser(private val scheme: String, private val universalLinkHosts: Set<String>) {
    /** Returns `null` for anything unrecognized; unknown links are ignored, never guessed at. */
    fun route(url: String): Route? {
        val segments = segments(url) ?: return null
        return when {
            segments == listOf("home") -> Route.Home
            segments == listOf("settings") -> Route.Settings
            segments == listOf("settings", "about") -> Route.About
            segments.size == 2 && segments[0] == "items" -> itemId(segments[1])?.let(Route::ItemDetail)
            else -> null
        }
    }

    private fun itemId(segment: String): Int? = segment.toIntOrNull()?.takeIf { it > 0 && it.toString() == segment }

    private fun segments(url: String): List<String>? {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return null
        }
        val host = uri.host.orEmpty()
        val path = uri.path.orEmpty().split("/").filter(String::isNotEmpty)
        return when (uri.scheme) {
            scheme -> if (host.isEmpty()) null else listOf(host) + path
            "https" -> if (host in universalLinkHosts) path else null
            else -> null
        }
    }
}
