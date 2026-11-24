package com.example.mobilestack.core.network

import com.example.mobilestack.core.auth.TokenStore
import com.example.mobilestack.core.services.ErrorReporter
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class ApiClient(
    private val baseUrl: String,
    private val transport: HttpTransport,
    private val tokens: TokenStore,
    private val reporter: ErrorReporter,
    private val onUnauthorized: () -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend inline fun <reified Response> get(path: String, query: Map<String, String> = emptyMap()): Response =
        send("GET", path, query, body = null, serializer = serializer<Response>())

    suspend inline fun <reified Body, reified Response> post(path: String, body: Body): Response =
        send("POST", path, emptyMap(), json.encodeToString(serializer<Body>(), body), serializer<Response>())

    suspend inline fun <reified Body, reified Response> put(path: String, body: Body): Response =
        send("PUT", path, emptyMap(), json.encodeToString(serializer<Body>(), body), serializer<Response>())

    suspend inline fun <reified Body, reified Response> patch(path: String, body: Body): Response =
        send("PATCH", path, emptyMap(), json.encodeToString(serializer<Body>(), body), serializer<Response>())

    suspend fun delete(path: String) {
        reporting("DELETE", path) { requestId -> perform("DELETE", path, emptyMap(), body = null, requestId) }
    }

    @PublishedApi
    internal suspend fun <Response> send(
        method: String,
        path: String,
        query: Map<String, String>,
        body: String?,
        serializer: KSerializer<Response>,
    ): Response = reporting(method, path) { requestId ->
        val response = perform(method, path, query, body, requestId)
        try {
            json.decodeFromString(serializer, response.body)
        } catch (_: SerializationException) {
            throw ApiError.Decoding(requestId)
        }
    }

    private suspend fun <Result> reporting(method: String, path: String, block: suspend (String) -> Result): Result =
        try {
            block(UUID.randomUUID().toString())
        } catch (error: ApiError) {
            if (error.isReportable) {
                reporter.report(error, mapOf("method" to method, "route" to RoutePattern.normalize(path)))
            }
            throw error
        }

    private suspend fun perform(
        method: String,
        path: String,
        query: Map<String, String>,
        body: String?,
        requestId: String,
    ): HttpResponse {
        val request = HttpRequest(method, url(path, query), headers(requestId, hasBody = body != null), body)
        val response = try {
            withContext(ioDispatcher) { transport.send(request) }
        } catch (_: IOException) {
            throw ApiError.Network
        }
        return when (response.status) {
            in 200..299 -> response

            401 -> {
                onUnauthorized()
                throw ApiError.Unauthorized
            }

            else -> throw ApiError.Http(response.status, requestId)
        }
    }

    private fun url(path: String, query: Map<String, String>): String {
        val base = "${baseUrl.trimEnd('/')}/$API_VERSION/${path.trimStart('/')}"
        if (query.isEmpty()) return base
        return base + query.toSortedMap().entries.joinToString("&", prefix = "?") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
    }

    private fun headers(requestId: String, hasBody: Boolean): Map<String, String> = buildMap {
        put("x-request-id", requestId)
        put("Accept", "application/json")
        if (hasBody) put("Content-Type", "application/json")
        tokens.token?.let { put("Authorization", "Bearer $it") }
    }

    companion object {
        const val API_VERSION = "v1"
        const val TIMEOUT_SECONDS = 30L

        @PublishedApi
        internal val json = Json { ignoreUnknownKeys = true }
    }
}
