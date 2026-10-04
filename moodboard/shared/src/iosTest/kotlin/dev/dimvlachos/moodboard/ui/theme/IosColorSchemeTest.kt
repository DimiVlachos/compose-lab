package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IosColorSchemeTest {
    private val schemes = listOf(IosLightColors, IosDarkColors)

    @Test
    fun ticksAndHandlesOnTheTintAreLegible() {
        // UI components need 3:1 (WCAG 1.4.11): a Switch handle, the editor's check.
        schemes.forEach { assertTrue(contrast(it.primary, it.onPrimary) >= 3.0, "$it") }
    }

    @Test
    fun noRoleTheSharedScreensDrawWithIsMaterialsPurpleDefault() {
        val baseline = lightColorScheme()
        schemes.forEach {
            assertNotEquals(baseline.surfaceVariant, it.surfaceVariant)
            assertNotEquals(baseline.surfaceContainerHighest, it.surfaceContainerHighest)
            assertNotEquals(baseline.onSurfaceVariant, it.onSurfaceVariant)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }
}
