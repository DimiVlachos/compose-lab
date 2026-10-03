package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// UIKit's systemBlue and the grouped grays, so the shared Compose bits sit quietly among native
// controls instead of carrying Material's default purple.
private val Light =
    lightColorScheme(
        primary = Color(0xFF007AFF),
        onPrimary = Color.White,
        secondaryContainer = Color(0xFFE5E5EA),
        onSecondaryContainer = Color.Black,
        surface = Color.Transparent,
        onSurface = Color.Black,
        outline = Color(0xFFC7C7CC),
        outlineVariant = Color(0xFFD1D1D6),
    )

private val Dark =
    darkColorScheme(
        primary = Color(0xFF0A84FF),
        onPrimary = Color.White,
        secondaryContainer = Color(0xFF3A3A3C),
        onSecondaryContainer = Color.White,
        surface = Color.Transparent,
        onSurface = Color.White,
        outline = Color(0xFF545458),
        outlineVariant = Color(0xFF48484A),
    )

@Composable actual fun platformColorScheme(dark: Boolean): ColorScheme = if (dark) Dark else Light
