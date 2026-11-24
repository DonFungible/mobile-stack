package com.example.mobilestack.core.features

import com.example.mobilestack.core.app.Dependencies
import com.example.mobilestack.core.app.Navigating
import com.example.mobilestack.core.base.LoadStatus
import com.example.mobilestack.core.base.Outcome
import com.example.mobilestack.core.base.ViewModel
import com.example.mobilestack.core.navigation.Route
import com.example.mobilestack.core.platform.HapticEvent
import com.example.mobilestack.data.Item
import com.example.mobilestack.data.QueryKeys

class ItemListViewModel internal constructor(
    private val dependencies: Dependencies,
    private val navigation: Navigating,
) : ViewModel<ItemListViewModel.State, ItemListViewModel.Action, ItemListViewModel.Message>(
    State(),
    dependencies.scope,
) {
    data class State(
        val status: LoadStatus = LoadStatus.Loading,
        val items: List<Item> = emptyList(),
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
    )

    sealed interface Action {
        data object Refresh : Action

        data class SelectItem(val id: Int) : Action

        data object OpenSettings : Action
    }

    sealed interface Message {
        data class ViewAction(val action: Action) : Message

        data object Started : Message

        data class Loaded(val outcome: Outcome<List<Item>>) : Message
    }

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun start() = dispatch(Message.Started)

    override fun update(state: State, message: Message): State = when (message) {
        Message.Started -> state.also { load(force = false) }

        is Message.Loaded -> when (val outcome = message.outcome) {
            is Outcome.Success -> State(LoadStatus.Loaded, outcome.value)

            is Outcome.Failure ->
                if (state.status == LoadStatus.Loaded) {
                    state.copy(isRefreshing = false)
                } else {
                    State(LoadStatus.Failed, errorMessage = outcome.error.userMessage)
                }
        }

        is Message.ViewAction -> when (val action = message.action) {
            // A refresh keeps showing the last good value; a retry after failure shows loading again.
            Action.Refresh -> {
                load(force = true)
                if (state.status == LoadStatus.Loaded) state.copy(isRefreshing = true) else State()
            }

            is Action.SelectItem -> state.also {
                if (state.items.any { it.id == action.id }) {
                    dependencies.haptics.play(HapticEvent.Selection)
                    navigation.push(Route.ItemDetail(action.id))
                }
            }

            Action.OpenSettings -> state.also { navigation.push(Route.Settings) }
        }
    }

    private fun load(force: Boolean) {
        run({ dependencies.queries.fetch(QueryKeys.items(), force) { dependencies.items.items() } }, Message::Loaded)
    }
}
