package com.example.mobilestack.core.services

/** The seam for Sentry, Crashlytics or similar. Features never import a vendor SDK directly. */
fun interface ErrorReporter {
    fun report(error: Throwable, context: Map<String, String>)
}

class ConsoleErrorReporter : ErrorReporter {
    override fun report(error: Throwable, context: Map<String, String>) {
        val details = context.toSortedMap().entries.joinToString(" ") { "${it.key}=${it.value}" }
        Log.error("reported: $error $details")
    }
}
