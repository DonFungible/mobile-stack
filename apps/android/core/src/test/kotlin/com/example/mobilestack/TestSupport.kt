package com.example.mobilestack

import com.example.mobilestack.core.auth.InMemoryTokenStore
import com.example.mobilestack.core.auth.TokenStore
import com.example.mobilestack.core.network.ApiClient
import com.example.mobilestack.core.network.HttpRequest
import com.example.mobilestack.core.network.HttpResponse
import com.example.mobilestack.core.network.HttpTransport
import com.example.mobilestack.core.services.ErrorReporter
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers

class StubTransport(private vararg val responses: Any) : HttpTransport {
    val requests = mutableListOf<HttpRequest>()

    override fun send(request: HttpRequest): HttpResponse {
        requests += request
        return when (val next = responses.getOrNull(requests.size - 1)) {
            is HttpResponse -> next
            is IOException -> throw next
            else -> HttpResponse(200, "{}")
        }
    }
}

class RecordingReporter : ErrorReporter {
    val contexts = mutableListOf<Map<String, String>>()

    override fun report(error: Throwable, context: Map<String, String>) {
        contexts += context
    }
}

fun client(
    transport: HttpTransport,
    tokens: TokenStore = InMemoryTokenStore(),
    reporter: ErrorReporter = RecordingReporter(),
    onUnauthorized: () -> Unit = {},
) = ApiClient(
    baseUrl = "https://api.example.com",
    transport = transport,
    tokens = tokens,
    reporter = reporter,
    onUnauthorized = onUnauthorized,
    ioDispatcher = Dispatchers.Unconfined,
)

fun contractFile(name: String) = File(System.getProperty("contracts.dir"), name)
