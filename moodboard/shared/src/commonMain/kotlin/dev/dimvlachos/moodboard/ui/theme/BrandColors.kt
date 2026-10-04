package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Generated from the brand seed #1E6FC0 (Aegean blue) with Material's Fidelity scheme, which keeps
// the seed's colour instead of greying it as the default tonal spot does. Android uses these as its
// whole scheme, in place of wallpaper colours, so both apps share one blue; iOS tints with tones
// of the same palette (Brand.IosTint*).

/** The brand's accent tints for iOS: primary palette tones 45 (light) and 65 (dark). */
object Brand {
    val Seed = Color(0xFF1E6FC0)
    val IosTintLight = Color(0xFF186CBD)
    val IosTintDark = Color(0xFF5DA0F4)
}

internal val BrandLightColors =
    lightColorScheme(
        primary = Color(0xFF00569D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF1E6FC0),
        onPrimaryContainer = Color(0xFFECF1FF),
        inversePrimary = Color(0xFFA4C9FF),
        secondary = Color(0xFF496080),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC1D9FF),
        onSecondaryContainer = Color(0xFF485E7F),
        tertiary = Color(0xFF864300),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFAA5700),
        onTertiaryContainer = Color(0xFFFFEDE3),
        background = Color(0xFFF8F9FF),
        onBackground = Color(0xFF191C21),
        surface = Color(0xFFF8F9FF),
        onSurface = Color(0xFF191C21),
        surfaceVariant = Color(0xFFDDE2EF),
        onSurfaceVariant = Color(0xFF414751),
        surfaceTint = Color(0xFF005FAD),
        inverseSurface = Color(0xFF2E3036),
        inverseOnSurface = Color(0xFFEFF0F8),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        outline = Color(0xFF717783),
        outlineVariant = Color(0xFFC1C7D3),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF8F9FF),
        surfaceContainer = Color(0xFFECEDF5),
        surfaceContainerHigh = Color(0xFFE6E8EF),
        surfaceContainerHighest = Color(0xFFE1E2E9),
        surfaceContainerLow = Color(0xFFF2F3FB),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFD8DAE1),
    )

internal val BrandDarkColors =
    darkColorScheme(
        primary = Color(0xFFA4C9FF),
        onPrimary = Color(0xFF00315D),
        primaryContainer = Color(0xFF1E6FC0),
        onPrimaryContainer = Color(0xFFECF1FF),
        inversePrimary = Color(0xFF005FAD),
        secondary = Color(0xFFB1C8ED),
        onSecondary = Color(0xFF19314F),
        secondaryContainer = Color(0xFF334A69),
        onSecondaryContainer = Color(0xFFA3BADE),
        tertiary = Color(0xFFFFB783),
        onTertiary = Color(0xFF4F2500),
        tertiaryContainer = Color(0xFFAA5700),
        onTertiaryContainer = Color(0xFFFFEDE3),
        background = Color(0xFF101319),
        onBackground = Color(0xFFE1E2E9),
        surface = Color(0xFF101319),
        onSurface = Color(0xFFE1E2E9),
        surfaceVariant = Color(0xFF414751),
        onSurfaceVariant = Color(0xFFC1C7D3),
        surfaceTint = Color(0xFFA4C9FF),
        inverseSurface = Color(0xFFE1E2E9),
        inverseOnSurface = Color(0xFF2E3036),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF8B919D),
        outlineVariant = Color(0xFF414751),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF36393F),
        surfaceContainer = Color(0xFF1D2025),
        surfaceContainerHigh = Color(0xFF272A30),
        surfaceContainerHighest = Color(0xFF32353A),
        surfaceContainerLow = Color(0xFF191C21),
        surfaceContainerLowest = Color(0xFF0B0E13),
        surfaceDim = Color(0xFF101319),
    )
