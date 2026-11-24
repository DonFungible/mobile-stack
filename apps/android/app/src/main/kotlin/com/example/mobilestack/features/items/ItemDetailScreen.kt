package com.example.mobilestack.features.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobilestack.common.LoadStatusContent
import com.example.mobilestack.core.features.ItemDetailViewModel
import com.example.mobilestack.data.Item
import com.example.mobilestack.designsystem.AppButton
import com.example.mobilestack.designsystem.AppButtonVariant
import com.example.mobilestack.designsystem.AppColor
import com.example.mobilestack.designsystem.AppText
import com.example.mobilestack.designsystem.AppTextStyle
import com.example.mobilestack.designsystem.ScreenContainer
import com.example.mobilestack.designsystem.generated.Spacing

@Composable
fun ItemDetailScreen(viewModel: ItemDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenContainer(title = "Item", onBack = onBack) {
        LoadStatusContent(
            status = state.status,
            errorMessage = state.errorMessage,
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.send(ItemDetailViewModel.Action.Refresh) },
        ) {
            state.item?.let { item ->
                ItemDetail(item, state.canShare, onShare = { viewModel.send(ItemDetailViewModel.Action.Share) })
            }
        }
    }
}

@Composable
private fun ItemDetail(item: Item, canShare: Boolean, onShare: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.Md),
    ) {
        AppText(item.title, modifier = Modifier.testTag("item-detail-title"), style = AppTextStyle.Headline)
        AppText(item.summary, color = AppColor.TextSecondary)
        AppText(
            "Updated ${item.updatedAt.take(DATE_LENGTH)}",
            style = AppTextStyle.Label,
            color = AppColor.TextSecondary,
        )
        if (canShare) {
            AppButton(
                "Share",
                modifier = Modifier.testTag("item-detail-share"),
                variant = AppButtonVariant.Secondary,
                onClick = onShare,
            )
        }
    }
}

private const val DATE_LENGTH = 10
