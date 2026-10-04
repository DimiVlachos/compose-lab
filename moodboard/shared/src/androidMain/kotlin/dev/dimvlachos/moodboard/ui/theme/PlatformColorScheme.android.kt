package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** The brand's Aegean blue scheme, not the wallpaper's: both apps share one accent. */
@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme =
    if (dark) BrandDarkColors else BrandLightColors
