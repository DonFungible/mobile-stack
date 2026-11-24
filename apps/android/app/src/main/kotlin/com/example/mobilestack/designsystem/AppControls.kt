package com.example.mobilestack.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role

@Composable
fun AppToggle(title: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    // The whole row is the control, so TalkBack reads the title and state together.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = HitTarget.Minimum)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AppColor.Accent,
                checkedThumbColor = AppColor.OnAccent,
                uncheckedTrackColor = AppColor.SurfaceRaised,
                uncheckedThumbColor = AppColor.TextSecondary,
                uncheckedBorderColor = AppColor.Stroke,
            ),
        )
    }
}

/** [label] is required: an icon-only control is invisible to TalkBack without one. */
@Composable
fun AppIconButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppPressable(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = label, tint = AppColor.TextPrimary)
    }
}
