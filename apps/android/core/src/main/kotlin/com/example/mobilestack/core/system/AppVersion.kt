package com.example.mobilestack.core.system

import kotlinx.serialization.Serializable

class AppVersion private constructor(private val components: List<Int>) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int =
        components.zip(other.components).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a.compareTo(b) } ?: 0

    companion object {
        private const val FIELDS = 3

        fun parse(value: String): AppVersion? {
            val fields = value.split(".")
            val parts = fields.mapNotNull { it.toIntOrNull()?.takeIf { part -> part >= 0 && it.all(Char::isDigit) } }
            if (fields.size > FIELDS || parts.size != fields.size) return null
            return AppVersion(parts + List(FIELDS - parts.size) { 0 })
        }
    }
}

@Serializable
data class MinimumVersion(val ios: String, val android: String)

object UpdatePolicy {
    /** An unreadable version never locks users out: only a well-formed, higher minimum forces an update. */
    fun isUpdateRequired(current: String, minimum: String): Boolean {
        val currentVersion = AppVersion.parse(current) ?: return false
        val minimumVersion = AppVersion.parse(minimum) ?: return false
        return currentVersion < minimumVersion
    }
}
