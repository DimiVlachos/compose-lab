package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf { AppColors() }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalMotion = staticCompositionLocalOf { Motion() }

object LabTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current

    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val motion: Motion
        @Composable get() = LocalMotion.current

    val shapes: AppShape
        get() = AppShape

    val typography: AppTextStyle
        get() = AppTextStyle
}

@Composable
fun LabTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides AppColors(),
        LocalSpacing provides Spacing(),
        LocalMotion provides Motion(),
    ) {
        content()
    }
}
