package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTextStyle {
    val title =
        TextStyle(fontFamily = FontFamily.Default, fontSize = 28.sp, fontWeight = FontWeight.Bold)

    val subtitle =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

    val body =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
        )

    val label =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.sp,
        )

    val caption =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
        )

    val pageLabel =
        TextStyle(fontFamily = FontFamily.Default, fontSize = 44.sp, fontWeight = FontWeight.Bold)

    val stat =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
        )
}
