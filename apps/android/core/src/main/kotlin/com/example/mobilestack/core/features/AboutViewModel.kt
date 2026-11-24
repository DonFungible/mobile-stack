package com.example.mobilestack.core.features

import com.example.mobilestack.core.app.Dependencies
import com.example.mobilestack.core.base.ViewModel

class AboutViewModel internal constructor(dependencies: Dependencies) :
    ViewModel<AboutViewModel.State, AboutViewModel.Action, AboutViewModel.Message>(
        State(
            version = dependencies.config.appVersion,
            environment = dependencies.config.environment.value,
            dataSource = if (dependencies.config.useFixtures) "Bundled fixtures" else "Live API",
        ),
        dependencies.scope,
    ) {
    data class State(val version: String, val environment: String, val dataSource: String)

    /** Nothing to do on this screen; leaving it is the navigator's `goBack`. */
    sealed interface Action

    sealed interface Message {
        data class ViewAction(val action: Action) : Message
    }

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun update(state: State, message: Message): State = state
}
