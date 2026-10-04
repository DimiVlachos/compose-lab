package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// UIKit's grouped grays tinted with the brand blue (the same tones SwiftUI tints with), so the
// shared Compose bits sit among native controls instead of carrying Material's default purple.
private val Light =
    lightColorScheme(
        primary = Brand.IosTintLight,
        onPrimary = Color.White,
        secondaryContainer = Color(0xFFE5E5EA),
        onSecondaryContainer = Color.Black,
        surface = Color.Transparent,
        onSurface = Color.Black,
        // UIKit's secondaryLabel; Material's default is a purple-tinted gray.
        onSurfaceVariant = Color(0x993C3C43),
        outline = Color(0xFFC7C7CC),
        outlineVariant = Color(0xFFD1D1D6),
    )

private val Dark =
    darkColorScheme(
        primary = Brand.IosTintDark,
        onPrimary = Color.White,
        secondaryContainer = Color(0xFF3A3A3C),
        onSecondaryContainer = Color.White,
        surface = Color.Transparent,
        onSurface = Color.White,
        onSurfaceVariant = Color(0x99EBEBF5),
        outline = Color(0xFF545458),
        outlineVariant = Color(0xFF48484A),
    )

@Composable actual fun platformColorScheme(dark: Boolean): ColorScheme = if (dark) Dark else Light
