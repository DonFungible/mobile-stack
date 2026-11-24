package com.example.mobilestack.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.mobilestack.designsystem.generated.Motion
import com.example.mobilestack.designsystem.generated.Radius
import com.example.mobilestack.designsystem.generated.Spacing

enum class AppButtonVariant { Primary, Secondary, Destructive }

object HitTarget {
    val Minimum = 48.dp
}

@Composable
fun AppButton(
    title: String,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.Xl)
    val foreground = when {
        !enabled -> AppColor.TextDisabled
        variant == AppButtonVariant.Primary -> AppColor.OnAccent
        variant == AppButtonVariant.Destructive -> AppColor.Danger
        else -> AppColor.TextPrimary
    }
    val background = when {
        !enabled -> AppColor.SurfaceRaised
        variant == AppButtonVariant.Primary -> AppColor.Accent
        else -> AppColor.Surface
    }
    AppPressable(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(
                if (variant ==
                    AppButtonVariant.Secondary
                ) {
                    Modifier.border(1.dp, AppColor.Stroke, shape)
                } else {
                    Modifier
                },
            ),
        enabled = enabled && !isLoading,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Lg),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(Spacing.Xl), color = foreground, strokeWidth = 2.dp)
            } else {
                AppText(title, style = AppTextStyle.TitleM, color = foreground)
            }
        }
    }
}

/** The one press treatment in the app: dim on touch, fade back on release, 48dp minimum target. */
@Composable
fun AppPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) PRESSED_ALPHA else 1f,
        animationSpec = tween(Motion.PressFadeMs),
        label = "press",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = HitTarget.Minimum, minHeight = HitTarget.Minimum)
            .alpha(alpha)
            .clickable(
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private const val PRESSED_ALPHA = 0.6f
