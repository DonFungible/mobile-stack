package com.example.mobilestack.core.base

import com.example.mobilestack.core.network.ApiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Action in, state out. A screen reads [state] and calls [send]; that is the whole contract.
 *
 * Everything else here is internal to the core module on purpose: a screen cannot write state, dispatch a
 * message, or call [update], because it cannot see them.
 */
abstract class ViewModel<State, Action, Message>(initial: State, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(initial)
    private val queue = ArrayDeque<Message>()
    private var isUpdating = false

    val state: StateFlow<State> = mutableState.asStateFlow()

    fun send(action: Action) = dispatch(viewAction(action))

    /** Every action enters the reducer wrapped as a message, alongside results from services. */
    internal abstract fun viewAction(action: Action): Message

    /** The only place state ever changes. */
    internal abstract fun update(state: State, message: Message): State

    /** Called once, when the screen enters the tree. */
    internal open fun start() {}

    /** Messages dispatched while an update is running are queued, so [update] never re-enters itself. */
    internal fun dispatch(message: Message) {
        queue.addLast(message)
        if (isUpdating) return
        isUpdating = true
        try {
            while (queue.isNotEmpty()) {
                mutableState.value = update(mutableState.value, queue.removeFirst())
            }
        } finally {
            isUpdating = false
        }
    }

    /** Runs async work off the reducer and feeds its result back in as a message. */
    internal fun <Value> run(work: suspend () -> Value, then: (Outcome<Value>) -> Message) {
        scope.launch {
            val outcome = try {
                Outcome.Success(work())
            } catch (error: ApiError) {
                Outcome.Failure(error)
            }
            dispatch(then(outcome))
        }
    }
}

sealed interface Outcome<out Value> {
    data class Success<Value>(val value: Value) : Outcome<Value>

    data class Failure(val error: ApiError) : Outcome<Nothing>
}

enum class LoadStatus(val value: String) {
    Loading("loading"),
    Loaded("loaded"),
    Failed("failed"),
}
