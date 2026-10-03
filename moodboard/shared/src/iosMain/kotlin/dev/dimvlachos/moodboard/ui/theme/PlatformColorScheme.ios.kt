package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun platformColorScheme(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme() else lightColorScheme()
