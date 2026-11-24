package com.example.mobilestack.core.query

/**
 * A cache key that doubles as a prefix: trailing `null`s are dropped, so `QueryKey("items", null)`
 * matches every cached item when invalidating.
 */
class QueryKey(vararg parts: String?) {
    val parts: List<String> = parts.toList().dropLastWhile { it == null }.map { it.orEmpty() }

    fun hasPrefix(prefix: QueryKey): Boolean = parts.take(prefix.parts.size) == prefix.parts

    override fun equals(other: Any?): Boolean = other is QueryKey && other.parts == parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String = parts.joinToString("/")
}
