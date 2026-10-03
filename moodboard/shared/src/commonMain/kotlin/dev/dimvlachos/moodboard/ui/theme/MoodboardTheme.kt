package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

/** Android: the wallpaper's dynamic colors. iOS: a neutral scheme; SwiftUI owns the tint. */
@Composable expect fun platformColorScheme(dark: Boolean): ColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoodboardTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = platformColorScheme(isSystemInDarkTheme()),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
