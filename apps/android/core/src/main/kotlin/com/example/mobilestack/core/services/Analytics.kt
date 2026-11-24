package com.example.mobilestack.core.services

/** The single catalog of events. Names are snake_case; properties are typed here, not at call sites. */
sealed class AnalyticsEvent(val name: String, val properties: Map<String, String> = emptyMap()) {
    data object AppOpened : AnalyticsEvent("app_opened")

    data class SignedIn(val method: String) : AnalyticsEvent("signed_in", mapOf("method" to method))

    data object SignedOut : AnalyticsEvent("signed_out")

    data class ItemViewed(val id: Int) : AnalyticsEvent("item_viewed", mapOf("item_id" to id.toString()))

    data class DeepLinkOpened(val route: String) : AnalyticsEvent("deep_link_opened", mapOf("route" to route))
}

/** The seam for PostHog, Firebase or similar. */
interface Analytics {
    fun track(event: AnalyticsEvent)

    fun identify(userId: String)

    fun reset()
}

class ConsoleAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Log.debug("analytics: ${event.name} ${event.properties}")

    override fun identify(userId: String) = Log.debug("analytics: identify $userId")

    override fun reset() = Log.debug("analytics: reset")
}
