package com.example.mobilestack.core.query

import kotlinx.coroutines.CompletableDeferred

/**
 * The server-state cache. View models read and fetch through it, so a revisited screen starts from the
 * last good value and two screens asking for the same key share one request.
 * Confined to the core's thread, like the view models that use it.
 */
class QueryClient {
    private val cache = mutableMapOf<QueryKey, Any?>()
    private val inFlight = mutableMapOf<QueryKey, CompletableDeferred<Any?>>()

    @Suppress("UNCHECKED_CAST")
    fun <Value> cached(key: QueryKey): Value? = cache[key] as? Value

    fun set(key: QueryKey, value: Any?) {
        cache[key] = value
    }

    /** Returns the cached value unless [force] is set; concurrent callers for one key share a request. */
    @Suppress("UNCHECKED_CAST")
    suspend fun <Value> fetch(key: QueryKey, force: Boolean = false, fetcher: suspend () -> Value): Value {
        if (!force) cached<Value>(key)?.let { return it }
        inFlight[key]?.let { return it.await() as Value }
        val request = CompletableDeferred<Any?>().also { inFlight[key] = it }
        return try {
            fetcher().also {
                cache[key] = it
                request.complete(it)
            }
        } catch (error: Exception) {
            request.completeExceptionally(error)
            throw error
        } finally {
            inFlight.remove(key)
        }
    }

    /** Drops every entry under [prefix]; the next read refetches. */
    fun invalidate(prefix: QueryKey) {
        cache.keys.removeAll { it.hasPrefix(prefix) }
    }

    fun clear() {
        cache.clear()
    }
}
