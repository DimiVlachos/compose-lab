package dev.dimvlachos.lab.agenticdemo.presentation.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

/**
 * The Basic Catalog draws with Material 3, so the lab's tokens are handed to it through a
 * MaterialTheme: the agent's cards and inputs pick up the app's look without knowing it.
 */
@Composable
fun ConciergeTheme(content: @Composable () -> Unit) {
    val colors = LabTheme.colors
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                primaryContainer = colors.accentSoft,
                secondaryContainer = colors.accentSoft,
                background = colors.background,
                onBackground = colors.textPrimary,
                surface = colors.surface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.bar,
                onSurfaceVariant = colors.textMuted,
                surfaceContainer = colors.surface,
                surfaceContainerHigh = colors.bar,
                surfaceContainerHighest = colors.bar,
                outline = colors.textMuted,
            )
    ) {
        // Basic Catalog Text takes LocalContentColor, which is black outside a Material Surface:
        // a heading the agent puts straight on a surface, outside any Card, would vanish.
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary, content = content)
    }
}
