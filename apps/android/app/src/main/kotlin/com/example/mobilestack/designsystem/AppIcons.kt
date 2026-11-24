package com.example.mobilestack.designsystem

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Icons are drawn here rather than pulled from material-icons-extended, which adds megabytes for a few glyphs. */
object AppIcons {
    val Back: ImageVector = icon("Back") {
        moveTo(15.4f, 4.6f)
        lineTo(8f, 12f)
        lineTo(15.4f, 19.4f)
        lineTo(14f, 20.8f)
        lineTo(5.2f, 12f)
        lineTo(14f, 3.2f)
        close()
    }

    val Settings: ImageVector = icon("Settings") {
        moveTo(4f, 6f)
        horizontalLineTo(20f)
        verticalLineTo(8f)
        horizontalLineTo(4f)
        close()
        moveTo(4f, 11f)
        horizontalLineTo(20f)
        verticalLineTo(13f)
        horizontalLineTo(4f)
        close()
        moveTo(4f, 16f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        horizontalLineTo(4f)
        close()
    }

    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply { path(fill = SolidColor(AppColor.TextPrimary), pathBuilder = block) }
            .build()
}
