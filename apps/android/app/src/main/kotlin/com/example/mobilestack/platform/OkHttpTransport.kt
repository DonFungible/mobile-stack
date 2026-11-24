package com.example.mobilestack.platform

import com.example.mobilestack.core.network.ApiClient
import com.example.mobilestack.core.network.HttpRequest
import com.example.mobilestack.core.network.HttpResponse
import com.example.mobilestack.core.network.HttpTransport
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpTransport(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(ApiClient.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) : HttpTransport {
    override fun send(request: HttpRequest): HttpResponse {
        val body = request.body?.toRequestBody(JSON)
        val builder = Request.Builder().url(request.url).method(request.method, body)
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        return client.newCall(builder.build()).execute().use { response ->
            HttpResponse(status = response.code, body = response.body.string())
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
