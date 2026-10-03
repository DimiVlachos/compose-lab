package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme {
    val context = LocalContext.current
    val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return scheme.withSurfaceBackground()
}
