package com.example.mobilestack.features.items.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.mobilestack.data.Item
import com.example.mobilestack.designsystem.AppColor
import com.example.mobilestack.designsystem.AppPressable
import com.example.mobilestack.designsystem.AppText
import com.example.mobilestack.designsystem.AppTextStyle
import com.example.mobilestack.designsystem.Card
import com.example.mobilestack.designsystem.generated.Spacing

@Composable
fun ItemRow(item: Item, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    AppPressable(onClick = onSelect, modifier = modifier.testTag("item-row-${item.id}")) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                AppText(item.title, style = AppTextStyle.TitleM)
                AppText(item.summary, color = AppColor.TextSecondary)
            }
        }
    }
}
