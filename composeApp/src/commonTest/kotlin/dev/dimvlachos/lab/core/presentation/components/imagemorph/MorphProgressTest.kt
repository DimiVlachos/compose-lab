package dev.dimvlachos.lab.core.presentation.components.imagemorph

import kotlin.test.Test
import kotlin.test.assertEquals

class MorphProgressTest {
    private fun assertAlpha(expected: Float, actual: Float) = assertEquals(expected, actual, 0.001f)

    @Test
    fun chromeLandsWithTheContainer() {
        val start = MorphDimens.ChromeStartWidth
        assertAlpha(0f, morphChromeAlpha(widthFraction = start))
        assertAlpha(0.5f, morphChromeAlpha(widthFraction = start + (1f - start) / 2f))
        assertAlpha(1f, morphChromeAlpha(widthFraction = 1f))
        assertAlpha(0f, morphChromeAlpha(widthFraction = 0.4f))
    }

    @Test
    fun backdropFollowsProgressOnOpenAndLeavesEarlyOnClose() {
        assertAlpha(0.5f, morphBackdropAlpha(progress = 0.5f, closing = false))
        assertAlpha(1f, morphBackdropAlpha(progress = 1f, closing = false))
        assertAlpha(0.5f, morphBackdropAlpha(progress = 0.75f, closing = true))
        assertAlpha(0f, morphBackdropAlpha(progress = 0.5f, closing = true))
        assertAlpha(0f, morphBackdropAlpha(progress = 0.2f, closing = true))
    }
}
