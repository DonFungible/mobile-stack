package com.example.mobilestack.core.app

import com.example.mobilestack.core.auth.SessionStore
import com.example.mobilestack.core.base.Outcome
import com.example.mobilestack.core.base.ViewModel
import com.example.mobilestack.core.features.UpdateRequiredViewModel
import com.example.mobilestack.core.features.WelcomeViewModel
import com.example.mobilestack.core.navigation.Route
import com.example.mobilestack.core.services.AnalyticsEvent
import com.example.mobilestack.core.system.UpdatePolicy

/** The root of the tree. Decides which world the user is in: forced update, signed out, or the app. */
class AppViewModel internal constructor(private val dependencies: Dependencies) :
    ViewModel<AppViewModel.State, AppViewModel.Action, AppViewModel.Message>(State(), dependencies.scope) {
    enum class Phase(val value: String) {
        Checking("checking"),
        UpdateRequired("updateRequired"),
        SignedOut("signedOut"),
        Main("main"),
    }

    data class State(
        val phase: Phase = Phase.Checking,
        /** A link that arrived before there was a session, held for replay. */
        internal val pendingRoute: Route? = null,
    )

    sealed interface Action {
        data class OpenLink(val url: String) : Action
    }

    sealed interface Message {
        data class ViewAction(val action: Action) : Message

        data object Started : Message

        data class UpdateChecked(val isRequired: Boolean) : Message

        data class SessionChanged(val status: SessionStore.Status) : Message
    }

    val welcome = WelcomeViewModel(dependencies)
    val updateRequired = UpdateRequiredViewModel(dependencies)
    val stack = StackNavigator(dependencies)

    init {
        dependencies.session.onChange = { dispatch(Message.SessionChanged(it)) }
    }

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun start() = dispatch(Message.Started)

    override fun update(state: State, message: Message): State = when (message) {
        Message.Started -> {
            dependencies.analytics.track(AnalyticsEvent.AppOpened)
            val current = dependencies.config.appVersion
            run({ dependencies.system.minimumVersion() }) { outcome ->
                // A failed check never blocks launch; only a readable, higher minimum does.
                val minimum = (outcome as? Outcome.Success)?.value?.android
                Message.UpdateChecked(minimum != null && UpdatePolicy.isUpdateRequired(current, minimum))
            }
            state
        }

        is Message.UpdateChecked ->
            if (message.isRequired) {
                state.copy(
                    phase = Phase.UpdateRequired,
                )
            } else {
                enter(dependencies.session.status, state)
            }

        is Message.SessionChanged ->
            if (state.phase == Phase.SignedOut || state.phase == Phase.Main) enter(message.status, state) else state

        is Message.ViewAction -> when (val action = message.action) {
            is Action.OpenLink -> open(action.url, state)
        }
    }

    private fun open(url: String, state: State): State {
        val route = dependencies.deepLinks.route(url) ?: return state
        dependencies.analytics.track(AnalyticsEvent.DeepLinkOpened(route.name))
        if (state.phase != Phase.Main) return state.copy(pendingRoute = route)
        stack.reset(route.stack)
        return state
    }

    private fun enter(status: SessionStore.Status, state: State): State = when (status) {
        SessionStore.Status.Expired -> {
            dependencies.session.signOut()
            state.copy(phase = Phase.SignedOut)
        }

        SessionStore.Status.SignedOut -> {
            stack.clear()
            state.copy(phase = Phase.SignedOut)
        }

        // Screens are built only once there is a session, so nothing fetches while signed out.
        SessionStore.Status.SignedIn -> {
            stack.reset(state.pendingRoute?.stack.orEmpty())
            state.copy(phase = Phase.Main, pendingRoute = null)
        }
    }
}
