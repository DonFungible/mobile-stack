package com.example.mobilestack.core.features

import com.example.mobilestack.core.app.Dependencies
import com.example.mobilestack.core.base.ViewModel
import com.example.mobilestack.core.platform.HapticEvent
import com.example.mobilestack.core.services.AnalyticsEvent
import java.util.UUID

class WelcomeViewModel internal constructor(private val dependencies: Dependencies) :
    ViewModel<WelcomeViewModel.State, WelcomeViewModel.Action, WelcomeViewModel.Message>(State, dependencies.scope) {
    data object State

    sealed interface Action {
        data object ContinueAsGuest : Action
    }

    sealed interface Message {
        data class ViewAction(val action: Action) : Message
    }

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun update(state: State, message: Message): State = when (message) {
        is Message.ViewAction -> when (message.action) {
            Action.ContinueAsGuest -> {
                dependencies.haptics.play(HapticEvent.Success)
                dependencies.session.signIn("guest-${UUID.randomUUID()}")
                dependencies.analytics.track(AnalyticsEvent.SignedIn(method = "guest"))
                state
            }
        }
    }
}
