package com.example.mobilestack.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.example.mobilestack.designsystem.generated.Radius
import com.example.mobilestack.designsystem.generated.Spacing

/** The root of every screen: background, system-bar insets and the top bar in one place. */
@Composable
fun ScreenContainer(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(TopBarHeight).padding(horizontal = Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                AppIconButton(
                    AppIcons.Back,
                    label = "Back",
                    modifier = Modifier.testTag("back-button"),
                    onClick = onBack,
                )
            } else {
                Spacer(Modifier.size(Spacing.Sm))
            }
            AppText(title, modifier = Modifier.weight(1f).padding(horizontal = Spacing.Sm), style = AppTextStyle.TitleM)
            actions()
        }
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Xl))
            .background(AppColor.Surface)
            .padding(Spacing.Lg),
    ) {
        content()
    }
}

private val TopBarHeight = HitTarget.Minimum + Spacing.Sm
