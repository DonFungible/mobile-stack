package com.example.mobilestack

import com.example.mobilestack.core.base.Outcome
import com.example.mobilestack.core.base.ViewModel
import com.example.mobilestack.core.fixtures.FixtureSet
import com.example.mobilestack.core.fixtures.FixtureViolations
import com.example.mobilestack.core.network.ApiError
import com.example.mobilestack.core.network.HttpRequest
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ViewModelTest {
    private data class State(val count: Int = 0, val log: List<String> = emptyList())

    private sealed interface Action {
        data object Increment : Action

        data object IncrementTwice : Action

        data object Load : Action
    }

    private sealed interface Message {
        data class ViewAction(val action: Action) : Message

        data class Loaded(val outcome: Outcome<Int>) : Message
    }

    private class Counter(scope: CoroutineScope, private val work: suspend () -> Int = { 10 }) :
        ViewModel<State, Action, Message>(State(), scope) {
        override fun viewAction(action: Action): Message = Message.ViewAction(action)

        override fun update(state: State, message: Message): State = when (message) {
            is Message.ViewAction -> when (message.action) {
                Action.Increment -> State(state.count + 1, state.log + "increment")

                Action.IncrementTwice -> {
                    send(Action.Increment)
                    send(Action.Increment)
                    state.copy(log = state.log + "twice")
                }

                Action.Load -> state.also { run(work, Message::Loaded) }
            }

            is Message.Loaded -> when (val outcome = message.outcome) {
                is Outcome.Success -> state.copy(count = outcome.value)
                is Outcome.Failure -> state.copy(log = state.log + "failed")
            }
        }
    }

    @Test
    fun `an action changes state through update`() {
        val viewModel = Counter(TestScope())
        viewModel.send(Action.Increment)
        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `messages sent during an update run after it in order`() {
        val viewModel = Counter(TestScope())
        viewModel.send(Action.IncrementTwice)
        assertEquals(State(2, listOf("twice", "increment", "increment")), viewModel.state.value)
    }

    @Test
    fun `service results come back as messages`() = runTest {
        val viewModel = Counter(this)
        viewModel.send(Action.Load)
        assertEquals(0, viewModel.state.value.count)

        testScheduler.advanceUntilIdle()

        assertEquals(10, viewModel.state.value.count)
    }

    @Test
    fun `a failure reaches the reducer as an api error`() = runTest {
        val viewModel = Counter(this) { throw ApiError.Network }
        viewModel.send(Action.Load)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("failed"), viewModel.state.value.log)
    }

    @Test
    fun `fixtures merge in order and report what they cannot answer`() {
        val names = listOf("App.default", "Items.three", "Items.empty", "Session.signedIn")
        val fixtures = FixtureSet.load(names, contractFile("fixtures"))
        val violations = FixtureViolations()
        val transport = fixtures.transport(violations)
        fun get(path: String) = transport.send(HttpRequest("GET", "https://fixtures.invalid/v1/$path", emptyMap()))

        assertEquals("[]", get("items").body)
        assertEquals(404, get("items/nope").status)
        assertEquals(listOf("no fixture for GET items/nope"), violations.all)
        assertEquals("fixture-token", fixtures.tokens.token)
    }
}
