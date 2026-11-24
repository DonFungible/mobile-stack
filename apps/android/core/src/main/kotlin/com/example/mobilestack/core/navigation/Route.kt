package com.example.mobilestack.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data class ItemDetail(val id: Int) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object About : Route

    /** Matches `route` in packages/contracts/deeplinks.json, shared with iOS. */
    val name: String
        get() = when (this) {
            Home -> "home"
            is ItemDetail -> "itemDetail"
            Settings -> "settings"
            About -> "about"
        }

    /** The screens pushed above the root, so a deep link lands with a working back stack. */
    val stack: List<Route>
        get() = when (this) {
            Home -> emptyList()
            is ItemDetail, Settings -> listOf(this)
            About -> listOf(Settings, About)
        }
}
