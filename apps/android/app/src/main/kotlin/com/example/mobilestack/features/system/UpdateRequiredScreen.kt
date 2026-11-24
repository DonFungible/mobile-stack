package com.example.mobilestack.features.system

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mobilestack.core.features.UpdateRequiredViewModel
import com.example.mobilestack.designsystem.AppColor
import com.example.mobilestack.designsystem.MessageState

@Composable
fun UpdateRequiredScreen(viewModel: UpdateRequiredViewModel, modifier: Modifier = Modifier) {
    MessageState(
        title = "Update required",
        message = "This version is no longer supported. Update to keep going.",
        modifier = modifier.background(AppColor.Background),
        actionTitle = "Open Google Play",
        onAction = { viewModel.send(UpdateRequiredViewModel.Action.OpenStore) },
    )
}
