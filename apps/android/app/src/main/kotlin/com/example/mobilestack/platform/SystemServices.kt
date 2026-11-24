package com.example.mobilestack.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log as AndroidLog
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.mobilestack.core.platform.ExternalActions
import com.example.mobilestack.core.platform.HapticEvent
import com.example.mobilestack.core.platform.HapticsPlayer
import com.example.mobilestack.core.platform.PreferencesStore
import com.example.mobilestack.core.services.Log
import java.lang.ref.WeakReference

// The platform capabilities the core asks for. This package and the screens are the only code that touches android.*.

class SharedPreferencesStore(context: Context) : PreferencesStore {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    override fun boolean(key: String): Boolean? = if (prefs.contains(key)) prefs.getBoolean(key, false) else null

    override fun set(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
}

class SystemHaptics : HapticsPlayer {
    private var view = WeakReference<View>(null)

    /** Feedback is performed on a view, so the activity lends its root while it is alive. */
    fun attach(view: View) {
        this.view = WeakReference(view)
    }

    override fun play(event: HapticEvent) {
        view.get()?.performHapticFeedback(
            when (event) {
                HapticEvent.Selection -> HapticFeedbackConstants.CLOCK_TICK
                HapticEvent.Success -> HapticFeedbackConstants.CONTEXT_CLICK
                HapticEvent.Failure -> HapticFeedbackConstants.LONG_PRESS
            },
        )
    }
}

class SystemExternalActions(private val context: Context) : ExternalActions {
    override fun open(url: String) = start(Intent(Intent.ACTION_VIEW, url.toUri()))

    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        start(Intent.createChooser(send, null))
    }

    private fun start(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No app on this device can handle it; nothing useful to do.
        }
    }
}

fun routeLogsToLogcat() {
    Log.sink = { level, message ->
        val priority = when (level) {
            Log.Level.Debug -> AndroidLog.DEBUG
            Log.Level.Info -> AndroidLog.INFO
            Log.Level.Error -> AndroidLog.ERROR
        }
        AndroidLog.println(priority, "app", message)
    }
}
