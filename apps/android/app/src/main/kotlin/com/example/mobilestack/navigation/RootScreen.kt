package com.example.mobilestack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.mobilestack.core.app.AppViewModel
import com.example.mobilestack.core.app.StackNavigator
import com.example.mobilestack.designsystem.LoadingState
import com.example.mobilestack.features.items.ItemDetailScreen
import com.example.mobilestack.features.items.ItemListScreen
import com.example.mobilestack.features.settings.AboutScreen
import com.example.mobilestack.features.settings.SettingsScreen
import com.example.mobilestack.features.system.UpdateRequiredScreen
import com.example.mobilestack.features.welcome.WelcomeScreen

/** Draws whichever world the app root says the user is in. The decision itself lives in the core. */
@Composable
fun RootScreen(app: AppViewModel, modifier: Modifier = Modifier) {
    val state by app.state.collectAsStateWithLifecycle()

    when (state.phase) {
        AppViewModel.Phase.Checking -> LoadingState(modifier)
        AppViewModel.Phase.UpdateRequired -> UpdateRequiredScreen(app.updateRequired, modifier)
        AppViewModel.Phase.SignedOut -> WelcomeScreen(app.welcome, modifier)
        AppViewModel.Phase.Main -> MainStack(app.stack, modifier)
    }
}

@Composable
private fun MainStack(navigator: StackNavigator, modifier: Modifier = Modifier) {
    val state by navigator.state.collectAsStateWithLifecycle()
    if (state.entries.isEmpty()) return LoadingState(modifier)

    // The navigator owns the stack. The system back gesture reports itself as an action.
    NavDisplay(
        backStack = state.entries,
        modifier = modifier,
        onBack = { navigator.send(StackNavigator.Action.GoBack) },
        entryProvider = { entry ->
            NavEntry(entry) { Destination(entry.screen, onBack = { navigator.send(StackNavigator.Action.GoBack) }) }
        },
    )
}

@Composable
private fun Destination(screen: StackNavigator.Screen, onBack: () -> Unit) {
    when (screen) {
        is StackNavigator.Screen.ItemList -> ItemListScreen(screen.viewModel)
        is StackNavigator.Screen.ItemDetail -> ItemDetailScreen(screen.viewModel, onBack)
        is StackNavigator.Screen.Settings -> SettingsScreen(screen.viewModel, onBack)
        is StackNavigator.Screen.About -> AboutScreen(screen.viewModel, onBack)
    }
}
