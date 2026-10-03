package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorSchemeTest {
    @Test
    fun screensShareTheBarsAndListsSurface() {
        // Samsung's dynamic dark scheme: a near-black background under gray bars and rows.
        val samsung =
            darkColorScheme(
                background = Color(0xFF010102),
                onBackground = Color(0xFFEEEEEE),
                surface = Color(0xFF17171A),
                onSurface = Color(0xFFE6E6E9),
            )

        val scheme = samsung.withSurfaceBackground()

        assertEquals(Color(0xFF17171A), scheme.background)
        assertEquals(Color(0xFFE6E6E9), scheme.onBackground)
        assertEquals(samsung.primary, scheme.primary)
    }
}
