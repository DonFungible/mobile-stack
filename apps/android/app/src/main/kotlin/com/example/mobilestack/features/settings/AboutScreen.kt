package com.example.mobilestack.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobilestack.core.features.AboutViewModel
import com.example.mobilestack.designsystem.AppColor
import com.example.mobilestack.designsystem.AppText
import com.example.mobilestack.designsystem.Card
import com.example.mobilestack.designsystem.ScreenContainer
import com.example.mobilestack.designsystem.generated.Spacing

@Composable
fun AboutScreen(viewModel: AboutViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenContainer(title = "About", onBack = onBack) {
        Card(modifier = Modifier.padding(Spacing.Lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
                AboutRow("Version", state.version)
                AboutRow("Environment", state.environment)
                AboutRow("Data", state.dataSource)
            }
        }
    }
}

@Composable
private fun AboutRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(title, color = AppColor.TextSecondary)
        AppText(value)
    }
}
