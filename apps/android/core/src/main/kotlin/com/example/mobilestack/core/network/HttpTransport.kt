package com.example.mobilestack.core.network

import java.io.IOException

data class HttpRequest(val method: String, val url: String, val headers: Map<String, String>, val body: String? = null)

data class HttpResponse(val status: Int, val body: String)

fun interface HttpTransport {
    @Throws(IOException::class)
    fun send(request: HttpRequest): HttpResponse
}

/** Answers requests from bundled fixtures so the app and UI flows run with no backend. */
class FixtureTransport(private val handler: (method: String, path: String) -> HttpResponse?) : HttpTransport {
    override fun send(request: HttpRequest): HttpResponse {
        val path = java.net.URI(request.url).path.orEmpty()
        return handler(request.method, path) ?: HttpResponse(status = 404, body = "")
    }
}
