package com.example.mobilestack.features.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.mobilestack.core.features.WelcomeViewModel
import com.example.mobilestack.designsystem.AppButton
import com.example.mobilestack.designsystem.AppColor
import com.example.mobilestack.designsystem.AppText
import com.example.mobilestack.designsystem.AppTextStyle
import com.example.mobilestack.designsystem.generated.Spacing

@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.Md),
    ) {
        Spacer(Modifier.weight(1f))
        AppText("MobileStack", style = AppTextStyle.Display)
        AppText("A native starting point with the boring decisions already made.", color = AppColor.TextSecondary)
        Spacer(Modifier.weight(1f))
        AppButton("Continue as guest", modifier = Modifier.testTag("welcome-continue")) {
            viewModel.send(WelcomeViewModel.Action.ContinueAsGuest)
        }
    }
}
