package com.example.mobilestack

import com.example.mobilestack.core.app.MobileStackCore
import com.example.mobilestack.core.introspection.introspectionNode
import com.example.mobilestack.introspection.IntrospectionSession
import com.example.mobilestack.introspection.IntrospectionSocketServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Lets the CLI drive this running build over a local port (`adb forward tcp:4724 tcp:4724`).
 * Debug source set only: the release build gets the empty twin in `src/release`.
 */
object DebugIntrospection {
    const val PORT = 4724
    private var server: IntrospectionSocketServer? = null

    fun start(core: MobileStackCore) {
        if (server != null) return
        val session = IntrospectionSession({ core.introspectionNode })
        server =
            IntrospectionSocketServer(session, PORT) { block ->
                runBlocking(Dispatchers.Main) { block() }
            }.also { it.start() }
    }
}
