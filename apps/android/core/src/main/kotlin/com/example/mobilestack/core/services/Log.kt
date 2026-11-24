package com.example.mobilestack.core.services

object Log {
    enum class Level { Debug, Info, Error }

    /** The host decides where logs go: Logcat in the app, stderr in the CLI. */
    @Volatile
    var sink: (level: Level, message: String) -> Unit = { level, message -> System.err.println("[$level] $message") }

    fun debug(message: String) = sink(Level.Debug, message)

    fun info(message: String) = sink(Level.Info, message)

    fun error(message: String) = sink(Level.Error, message)
}
