package dev.dimvlachos.lab.core.presentation.components.magnet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenShadowTest {
    @Test
    fun theCardsShadowIsWholeAsThePhotoStartsToOpen() {
        assertEquals(1f, openShadowAlpha(0f))
    }

    @Test
    fun itIsGoneOnceThePhotoIsOpen() {
        assertEquals(0f, openShadowAlpha(1f))
    }

    @Test
    fun itFadesSteadilyOnTheWay() {
        val steps = (0..20).map { openShadowAlpha(it / 20f) }
        steps.zipWithNext().forEach { (a, b) -> assertTrue(b <= a, "$a then $b") }
        assertTrue(steps.zipWithNext().all { (a, b) -> a - b < 0.1f }, "no jump: $steps")
    }
}
