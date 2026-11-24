package com.example.mobilestack.core.features

import com.example.mobilestack.core.app.Dependencies
import com.example.mobilestack.core.base.ViewModel

class UpdateRequiredViewModel internal constructor(private val dependencies: Dependencies) :
    ViewModel<UpdateRequiredViewModel.State, UpdateRequiredViewModel.Action, UpdateRequiredViewModel.Message>(
        State(dependencies.storeUrl),
        dependencies.scope,
    ) {
    data class State(val storeUrl: String)

    sealed interface Action {
        data object OpenStore : Action
    }

    sealed interface Message {
        data class ViewAction(val action: Action) : Message
    }

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun update(state: State, message: Message): State = when (message) {
        is Message.ViewAction -> when (message.action) {
            Action.OpenStore -> state.also { dependencies.external.open(it.storeUrl) }
        }
    }
}
