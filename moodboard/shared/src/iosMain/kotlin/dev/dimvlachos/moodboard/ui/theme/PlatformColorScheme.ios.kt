package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// UIKit's grouped grays tinted with the brand blue (the same tones SwiftUI tints with), so the
// shared Compose bits sit among native controls instead of carrying Material's default purple.
internal val IosLightColors =
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
        // UIKit's fills and grouped backgrounds: placeholders, a Switch's track, containers.
        surfaceVariant = Color(0xFFE5E5EA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF2F2F7),
        surfaceContainer = Color(0xFFF2F2F7),
        surfaceContainerHigh = Color(0xFFE5E5EA),
        surfaceContainerHighest = Color(0xFFE5E5EA),
    )

internal val IosDarkColors =
    darkColorScheme(
        primary = Brand.IosTintDark,
        // The dark tint is light: white on it is 2.7:1, too faint for a tick or a Switch handle.
        onPrimary = Brand.IosOnTintDark,
        secondaryContainer = Color(0xFF3A3A3C),
        onSecondaryContainer = Color.White,
        surface = Color.Transparent,
        onSurface = Color.White,
        onSurfaceVariant = Color(0x99EBEBF5),
        outline = Color(0xFF545458),
        outlineVariant = Color(0xFF48484A),
        surfaceVariant = Color(0xFF3A3A3C),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF1C1C1E),
        surfaceContainer = Color(0xFF1C1C1E),
        surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerHighest = Color(0xFF3A3A3C),
    )

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme =
    if (dark) IosDarkColors else IosLightColors
