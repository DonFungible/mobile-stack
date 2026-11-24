package com.example.mobilestack.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.mobilestack.designsystem.generated.Spacing

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().semantics {
            contentDescription = "Loading"
        },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppColor.TextSecondary)
    }
}

@Composable
fun MessageState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.Md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(title, style = AppTextStyle.TitleL)
        AppText(message, color = AppColor.TextSecondary, textAlign = TextAlign.Center)
        if (actionTitle != null && onAction != null) {
            AppButton(actionTitle, variant = AppButtonVariant.Secondary, onClick = onAction)
        }
    }
}

@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        content()
    }
}
