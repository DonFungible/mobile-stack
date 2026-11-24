package com.example.mobilestack.core.network

sealed class ApiError : Exception() {
    data object Network : ApiError()

    data object Unauthorized : ApiError()

    data class Http(val status: Int, val requestId: String) : ApiError()

    data class Decoding(val requestId: String) : ApiError()

    /** Only server faults and contract breaks are worth an alert; offline users and 4xx are not. */
    val isReportable: Boolean
        get() = when (this) {
            is Http -> status >= 500
            is Decoding -> true
            Network, Unauthorized -> false
        }

    val userMessage: String
        get() = when (this) {
            Network -> "You appear to be offline. Check your connection and try again."
            Unauthorized -> "Your session has expired. Please sign in again."
            is Http, is Decoding -> "Something went wrong. Please try again."
        }
}
