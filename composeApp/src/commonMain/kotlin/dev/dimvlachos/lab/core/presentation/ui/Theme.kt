package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf { AppColors() }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalMotion = staticCompositionLocalOf { Motion() }

/** The lab's design tokens, read inside a [LabTheme] composable. */
public object LabTheme {
    /** The colours in use. */
    public val colors: AppColors
        @Composable get() = LocalAppColors.current

    /** The spacing scale in use. */
    public val spacing: Spacing
        @Composable get() = LocalSpacing.current

    /** The animation timing in use. */
    public val motion: Motion
        @Composable get() = LocalMotion.current

    /** The corner shapes. */
    public val shapes: AppShape
        get() = AppShape

    /** The text styles. */
    public val typography: AppTextStyle
        get() = AppTextStyle
}

/** Provides the lab's colours, spacing and motion to [content]. */
@Composable
public fun LabTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides AppColors(),
        LocalSpacing provides Spacing(),
        LocalMotion provides Motion(),
    ) {
        content()
    }
}
