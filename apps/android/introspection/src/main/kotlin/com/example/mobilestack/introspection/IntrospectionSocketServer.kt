package com.example.mobilestack.introspection

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * The same protocol over a local TCP port, for driving a running debug build. Never part of a release build.
 *
 * [onCoreThread] runs a block on the thread the view models live on and returns its result.
 */
class IntrospectionSocketServer(
    private val session: IntrospectionSession,
    private val port: Int,
    private val onCoreThread: (() -> String) -> String,
) {
    fun start() {
        thread(name = "introspection-server", isDaemon = true) {
            try {
                ServerSocket(port, 4, InetAddress.getLoopbackAddress()).use { server ->
                    while (true) serve(server)
                }
            } catch (_: IOException) {
                // The port is taken, usually by another debug build. The app works the same without the server.
            }
        }
    }

    private fun serve(server: ServerSocket) {
        val socket = server.accept()
        thread(name = "introspection-client", isDaemon = true) {
            try {
                socket.use {
                    val output = it.getOutputStream().bufferedWriter()
                    it.getInputStream().bufferedReader().forEachLine { line ->
                        output.write(onCoreThread { session.handle(line) })
                        output.newLine()
                        output.flush()
                    }
                }
            } catch (_: IOException) {
                // The client went away.
            }
        }
    }
}
