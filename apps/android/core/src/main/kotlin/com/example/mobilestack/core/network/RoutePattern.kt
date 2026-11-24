package com.example.mobilestack.core.network

object RoutePattern {
    private val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    /** Collapses identifiers so one failing endpoint groups as one issue instead of one per id. */
    fun normalize(path: String): String = path.split("/")
        .filter { it.isNotEmpty() }
        .joinToString("/") { segment ->
            if (segment.all(Char::isDigit) || uuid.matches(segment)) ":id" else segment
        }
}
