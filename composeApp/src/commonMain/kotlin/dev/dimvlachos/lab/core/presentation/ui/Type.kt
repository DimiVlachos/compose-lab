package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The lab's text styles. Read them through [LabTheme.typography]. */
public object AppTextStyle {
    /** 28 sp bold: a screen's title. */
    public val title: TextStyle =
        TextStyle(fontFamily = FontFamily.Default, fontSize = 28.sp, fontWeight = FontWeight.Bold)

    /** 18 sp semi-bold: a heading under the title. */
    public val subtitle: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

    /** 16 sp: running text. */
    public val body: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
        )

    /** 12 sp on a 16 sp line: the nav bar's labels. */
    public val label: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.sp,
        )

    /** 13 sp monospace: a short line of small print. */
    public val caption: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
        )
}
