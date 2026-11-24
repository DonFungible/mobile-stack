package com.example.mobilestack.core.features

import com.example.mobilestack.core.app.Dependencies
import com.example.mobilestack.core.app.Navigating
import com.example.mobilestack.core.base.ViewModel
import com.example.mobilestack.core.navigation.Route
import com.example.mobilestack.core.platform.StorageKey
import com.example.mobilestack.core.services.AnalyticsEvent

class SettingsViewModel internal constructor(
    private val dependencies: Dependencies,
    private val navigation: Navigating,
) : ViewModel<SettingsViewModel.State, SettingsViewModel.Action, SettingsViewModel.Message>(
    State(dependencies.haptics.isEnabled),
    dependencies.scope,
) {
    data class State(val hapticsEnabled: Boolean)

    sealed interface Action {
        data class SetHaptics(val enabled: Boolean) : Action

        data object OpenAbout : Action

        data object SignOut : Action
    }

    sealed interface Message {
        data class ViewAction(val action: Action) : Message
    }

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun update(state: State, message: Message): State = when (message) {
        is Message.ViewAction -> when (val action = message.action) {
            is Action.SetHaptics -> {
                dependencies.preferences.set(StorageKey.HAPTICS_ENABLED, action.enabled)
                State(action.enabled)
            }

            Action.OpenAbout -> state.also { navigation.push(Route.About) }

            Action.SignOut -> state.also {
                dependencies.analytics.track(AnalyticsEvent.SignedOut)
                dependencies.session.signOut()
            }
        }
    }
}
