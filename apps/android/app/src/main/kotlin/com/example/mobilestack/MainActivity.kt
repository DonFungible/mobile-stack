package com.example.mobilestack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.example.mobilestack.core.app.AppViewModel
import com.example.mobilestack.navigation.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark-only, so system bar icons are always light.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        val host = (application as MobileStackApp).host
        host.haptics.attach(window.decorView)
        DebugIntrospection.start(host.core)
        if (savedInstanceState == null) open(intent, host.core.app)
        setContent {
            // Exposes testTag as resource-id so UI flows share ids with iOS.
            RootScreen(host.core.app, modifier = Modifier.semantics { testTagsAsResourceId = true })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(intent, (application as MobileStackApp).host.core.app)
    }

    private fun open(intent: Intent, app: AppViewModel) {
        intent.dataString?.let { app.send(AppViewModel.Action.OpenLink(it)) }
    }
}
