package com.example.mobilestack.core.app

import com.example.mobilestack.core.base.ViewModel
import com.example.mobilestack.core.features.AboutViewModel
import com.example.mobilestack.core.features.ItemDetailViewModel
import com.example.mobilestack.core.features.ItemListViewModel
import com.example.mobilestack.core.features.SettingsViewModel
import com.example.mobilestack.core.navigation.Route

/**
 * The screens currently on the stack, each with its view model. Navigation is state: the CLI can walk
 * the whole app without a single view being created.
 */
class StackNavigator internal constructor(private val dependencies: Dependencies) :
    ViewModel<StackNavigator.State, StackNavigator.Action, StackNavigator.Message>(State(), dependencies.scope),
    Navigating {
    sealed interface Screen {
        data class ItemList(val viewModel: ItemListViewModel) : Screen

        data class ItemDetail(val viewModel: ItemDetailViewModel) : Screen

        data class Settings(val viewModel: SettingsViewModel) : Screen

        data class About(val viewModel: AboutViewModel) : Screen
    }

    class Entry(val id: Int, val route: Route, val screen: Screen) {
        override fun equals(other: Any?) = other is Entry && other.id == id

        override fun hashCode() = id
    }

    /** [entries] is empty while signed out; otherwise the first entry is the home screen. */
    data class State(val entries: List<Entry> = emptyList()) {
        val top: Entry? get() = entries.lastOrNull()
    }

    sealed interface Action {
        data object GoBack : Action

        /** Keeps [depth] screens above the root. */
        data class PopTo(val depth: Int) : Action
    }

    sealed interface Message {
        data class ViewAction(val action: Action) : Message

        data class Push(val route: Route) : Message

        data class Reset(val routes: List<Route>) : Message

        data object Clear : Message
    }

    private var nextId = 0

    override fun viewAction(action: Action): Message = Message.ViewAction(action)

    override fun push(route: Route) = dispatch(Message.Push(route))

    internal fun reset(routes: List<Route>) = dispatch(Message.Reset(routes))

    internal fun clear() = dispatch(Message.Clear)

    override fun update(state: State, message: Message): State = when (message) {
        is Message.ViewAction -> when (val action = message.action) {
            Action.GoBack -> if (state.entries.size > 1) State(state.entries.dropLast(1)) else state
            is Action.PopTo -> State(state.entries.take((action.depth + 1).coerceIn(1, maxOf(1, state.entries.size))))
        }

        is Message.Push -> State(state.entries + entry(message.route))

        is Message.Reset -> State((listOf(Route.Home) + message.routes).map(::entry))

        Message.Clear -> State()
    }

    private fun entry(route: Route): Entry {
        val screen = when (route) {
            Route.Home -> Screen.ItemList(ItemListViewModel(dependencies, this).also { it.start() })
            is Route.ItemDetail -> Screen.ItemDetail(ItemDetailViewModel(route.id, dependencies).also { it.start() })
            Route.Settings -> Screen.Settings(SettingsViewModel(dependencies, this))
            Route.About -> Screen.About(AboutViewModel(dependencies))
        }
        return Entry(++nextId, route, screen)
    }
}
