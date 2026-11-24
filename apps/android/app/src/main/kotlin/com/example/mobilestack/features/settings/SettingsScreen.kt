package com.example.mobilestack.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobilestack.core.features.SettingsViewModel
import com.example.mobilestack.designsystem.AppButton
import com.example.mobilestack.designsystem.AppButtonVariant
import com.example.mobilestack.designsystem.AppPressable
import com.example.mobilestack.designsystem.AppText
import com.example.mobilestack.designsystem.AppToggle
import com.example.mobilestack.designsystem.Card
import com.example.mobilestack.designsystem.ScreenContainer
import com.example.mobilestack.designsystem.generated.Spacing

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenContainer(title = "Settings", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.Lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.Md),
        ) {
            Card {
                AppToggle(
                    "Haptic feedback",
                    checked = state.hapticsEnabled,
                    modifier = Modifier.testTag("settings-haptics"),
                ) { viewModel.send(SettingsViewModel.Action.SetHaptics(it)) }
            }
            AppPressable(
                onClick = { viewModel.send(SettingsViewModel.Action.OpenAbout) },
                modifier = Modifier.fillMaxWidth().testTag("settings-about"),
            ) {
                Card { AppText("About") }
            }
            Spacer(Modifier.weight(1f))
            AppButton(
                "Sign out",
                modifier = Modifier.testTag("settings-sign-out"),
                variant = AppButtonVariant.Destructive,
            ) { viewModel.send(SettingsViewModel.Action.SignOut) }
        }
    }
}
