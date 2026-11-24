package com.example.mobilestack

import com.example.mobilestack.core.auth.InMemoryTokenStore
import com.example.mobilestack.core.network.ApiError
import com.example.mobilestack.core.network.HttpResponse
import com.example.mobilestack.core.network.RoutePattern
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Test

class ApiClientTest {
    @Serializable
    private data class Named(val name: String)

    @Test
    fun `get builds a versioned authenticated request`() = runTest {
        val transport = StubTransport(HttpResponse(200, """{"name":"a"}"""))

        val result: Named = client(transport, tokens = InMemoryTokenStore("secret")).get("items", mapOf("page" to "2"))

        val request = transport.requests.single()
        assertEquals(Named("a"), result)
        assertEquals("https://api.example.com/v1/items?page=2", request.url)
        assertEquals("Bearer secret", request.headers["Authorization"])
        assertFalse(request.headers["x-request-id"].isNullOrEmpty())
    }

    @Test
    fun `signed out requests carry no authorization header`() = runTest {
        val transport = StubTransport(HttpResponse(200, """{"name":"a"}"""))
        client(transport).get<Named>("items")
        assertNull(transport.requests.single().headers["Authorization"])
    }

    @Test
    fun `post encodes the body as json`() = runTest {
        val transport = StubTransport(HttpResponse(200, """{"name":"b"}"""))
        client(transport).post<Named, Named>("items", Named("b"))

        val request = transport.requests.single()
        assertEquals("POST", request.method)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("""{"name":"b"}""", request.body)
    }

    @Test
    fun `unknown response fields do not break decoding`() = runTest {
        val transport = StubTransport(HttpResponse(200, """{"name":"a","added_later":true}"""))
        assertEquals(Named("a"), client(transport).get<Named>("items"))
    }

    @Test
    fun `delete accepts an empty body`() = runTest {
        val transport = StubTransport(HttpResponse(204, ""))
        client(transport).delete("items/1")
        assertEquals("DELETE", transport.requests.single().method)
    }

    @Test
    fun `unauthorized notifies the session and is not reported`() = runTest {
        val reporter = RecordingReporter()
        var notified = false
        val api =
            client(StubTransport(HttpResponse(401, "{}")), reporter = reporter, onUnauthorized = { notified = true })

        assertFailsWith<ApiError.Unauthorized> { api.get<Named>("items") }
        assertTrue(notified)
        assertTrue(reporter.contexts.isEmpty())
    }

    @Test
    fun `server errors are reported once with a normalized route`() = runTest {
        val reporter = RecordingReporter()
        val api = client(StubTransport(HttpResponse(503, "{}")), reporter = reporter)

        assertFailsWith<ApiError.Http> { api.get<Named>("items/42") }
        assertEquals(listOf(mapOf("method" to "GET", "route" to "items/:id")), reporter.contexts)
    }

    @Test
    fun `client errors are not reported`() = runTest {
        val reporter = RecordingReporter()
        val api = client(StubTransport(HttpResponse(404, "{}")), reporter = reporter)

        assertFailsWith<ApiError.Http> { api.get<Named>("items/42") }
        assertTrue(reporter.contexts.isEmpty())
    }

    @Test
    fun `a broken contract is reported as a decoding error`() = runTest {
        val reporter = RecordingReporter()
        val api = client(StubTransport(HttpResponse(200, """{"title":1}""")), reporter = reporter)

        assertIs<ApiError.Decoding>(runCatching { api.get<Named>("items") }.exceptionOrNull())
        assertEquals(1, reporter.contexts.size)
    }

    @Test
    fun `transport failures become network errors`() = runTest {
        val api = client(StubTransport(IOException("offline")))
        assertFailsWith<ApiError.Network> { api.get<Named>("items") }
    }

    @Test
    fun `route patterns collapse identifiers`() {
        assertEquals("items/:id", RoutePattern.normalize("items/42"))
        assertEquals("users/:id/items", RoutePattern.normalize("/users/3F2504E0-4F89-11D3-9A0C-0305E82C3301/items"))
        assertEquals("min-version", RoutePattern.normalize("min-version"))
    }
}
