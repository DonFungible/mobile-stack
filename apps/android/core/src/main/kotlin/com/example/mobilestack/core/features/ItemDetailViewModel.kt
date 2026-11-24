package com.example.mobilestack.core.features

import com.example.mobilestack.core.app.Dependencies
import com.example.mobilestack.core.base.LoadStatus
import com.example.mobilestack.core.base.Outcome
import com.example.mobilestack.core.base.ViewModel
import com.example.mobilestack.core.config.FeatureFlag
import com.example.mobilestack.core.services.AnalyticsEvent
import com.example.mobilestack.data.Item
import com.example.mobilestack.data.QueryKeys

class ItemDetailViewModel internal constructor(itemId: Int, private val dependencies: Dependencies) :
    ViewModel<ItemDetailViewModel.State, ItemDetailViewModel.Action, ItemDetailViewModel.Message>(
        State(itemId, canShare = dependencies.flags.isEnabled(FeatureFlag.ItemSharing)),
        dependencies.scope,
    ) {
    data class State(
        val itemId: Int,
        val canShare: Boolean,
        val status: LoadStatus = LoadStatus.Loading,
        val item: Item? = null,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
    )

    sealed interface Action {
        data object Refresh : Action

        data object Share : Action
    }

    sealed interface Message {
        data class ViewAction(val action: Action) : Message

        data object Started : Message

        data class Loaded(val outcome: Outcome<Item>) : Message
    }

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun start() = dispatch(Message.Started)

    override fun update(state: State, message: Message): State = when (message) {
        Message.Started -> state.also {
            dependencies.analytics.track(AnalyticsEvent.ItemViewed(state.itemId))
            load(state.itemId, force = false)
        }

        is Message.Loaded -> when (val outcome = message.outcome) {
            is Outcome.Success ->
                state.copy(status = LoadStatus.Loaded, item = outcome.value, isRefreshing = false, errorMessage = null)

            is Outcome.Failure ->
                if (state.status == LoadStatus.Loaded) {
                    state.copy(isRefreshing = false)
                } else {
                    state.copy(
                        status = LoadStatus.Failed,
                        isRefreshing = false,
                        errorMessage = outcome.error.userMessage,
                    )
                }
        }

        is Message.ViewAction -> when (message.action) {
            Action.Refresh -> {
                load(state.itemId, force = true)
                if (state.status == LoadStatus.Loaded) {
                    state.copy(isRefreshing = true)
                } else {
                    state.copy(status = LoadStatus.Loading, errorMessage = null)
                }
            }

            Action.Share -> state.also {
                if (state.canShare && state.item != null) dependencies.external.share(state.item.title)
            }
        }
    }

    private fun load(id: Int, force: Boolean) {
        run({ dependencies.queries.fetch(QueryKeys.item(id), force) { dependencies.items.item(id) } }, Message::Loaded)
    }
}
