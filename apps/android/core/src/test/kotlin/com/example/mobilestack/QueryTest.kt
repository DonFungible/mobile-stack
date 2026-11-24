package com.example.mobilestack

import com.example.mobilestack.core.network.ApiError
import com.example.mobilestack.core.query.QueryClient
import com.example.mobilestack.core.query.QueryKey
import com.example.mobilestack.data.QueryKeys
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

class QueryTest {
    private class Counter {
        var count = 0

        suspend fun next(): Int {
            delay(1)
            return ++count
        }
    }

    @Test
    fun `trailing nulls make a key a prefix`() {
        assertEquals(QueryKey("items"), QueryKey("items", null))
        assertTrue(QueryKeys.item(4).hasPrefix(QueryKeys.item()))
        assertFalse(QueryKey("users", "4").hasPrefix(QueryKey("items")))
    }

    @Test
    fun `fetch serves the cache until forced`() = runTest {
        val counter = Counter()
        val client = QueryClient()

        val first = client.fetch(QueryKeys.items(), fetcher = counter::next)
        val second = client.fetch(QueryKeys.items(), fetcher = counter::next)
        val forced = client.fetch(QueryKeys.items(), force = true, fetcher = counter::next)

        assertEquals(listOf(1, 1, 2), listOf(first, second, forced))
    }

    @Test
    fun `concurrent callers share one request`() = runTest {
        val counter = Counter()
        val client = QueryClient()

        val results = awaitAll(
            async { client.fetch(QueryKeys.items(), force = true, fetcher = counter::next) },
            async { client.fetch(QueryKeys.items(), force = true, fetcher = counter::next) },
        )

        assertEquals(listOf(1, 1), results)
        assertEquals(1, counter.count)
    }

    @Test
    fun `invalidating a prefix drops only matching entries`() {
        val client = QueryClient()
        client.set(QueryKeys.item(4), 1)
        client.set(QueryKey("users"), 2)

        client.invalidate(QueryKeys.item())

        assertNull(client.cached<Int>(QueryKeys.item(4)))
        assertEquals(2, client.cached<Int>(QueryKey("users")))
    }

    @Test
    fun `a failed fetch caches nothing`() = runTest {
        val client = QueryClient()
        assertFailsWith<ApiError.Network> { client.fetch<Int>(QueryKeys.items()) { throw ApiError.Network } }
        assertNull(client.cached<Int>(QueryKeys.items()))
    }
}
