package com.example.mobilestack.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.mobilestack.designsystem.generated.TypeScale
import com.example.mobilestack.designsystem.generated.TypeToken

enum class AppTextStyle(internal val token: TypeToken, internal val isHeader: Boolean = false) {
    Display(TypeScale.Display, isHeader = true),
    Headline(TypeScale.Headline, isHeader = true),
    TitleL(TypeScale.TitleL, isHeader = true),
    TitleM(TypeScale.TitleM),
    Body(TypeScale.Body),
    Label(TypeScale.Label),
    LabelS(TypeScale.LabelS),
}

// Sizes are sp, so text follows the system font scale.
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.Body,
    color: Color = AppColor.TextPrimary,
    textAlign: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text,
        modifier = if (style.isHeader) modifier.semantics { heading() } else modifier,
        style = TextStyle(
            color = color,
            fontSize = style.token.size,
            lineHeight = style.token.lineHeight,
            fontWeight = FontWeight(style.token.weight),
            textAlign = textAlign,
        ),
    )
}
