package com.example.mobilestack.common

import androidx.compose.runtime.Composable
import com.example.mobilestack.core.base.LoadStatus
import com.example.mobilestack.designsystem.AppPullToRefresh
import com.example.mobilestack.designsystem.LoadingState
import com.example.mobilestack.designsystem.MessageState

/** Renders the loading and failure states every loaded screen shares, so each screen only draws its content. */
@Composable
fun LoadStatusContent(
    status: LoadStatus,
    errorMessage: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    when (status) {
        LoadStatus.Loading -> LoadingState()

        LoadStatus.Failed -> MessageState(
            title = "Couldn't load",
            message = errorMessage.orEmpty(),
            actionTitle = "Try again",
            onAction = onRefresh,
        )

        LoadStatus.Loaded -> AppPullToRefresh(isRefreshing, onRefresh = onRefresh, content = content)
    }
}
