package dev.dimvlachos.lab.core.presentation.components.conch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConchGeometryTest {
    @Test
    fun theLiftedShellsShadowEndsAboveTheComposer() {
        for (width in listOf(320f, 390f, 412f, 600f)) {
            val c = ConchState().apply { layout(width, width - 32f, 845f, 818f) }
            val s = c.stageSize
            val sy = contactShadowY(c.stageY, s, 1f, c.composerTop)
            assertTrue(c.stageY + sy + contactShadowR(s, 1f) * .28f <= c.composerTop, "at $width")
        }
    }

    @Test
    fun inTheSlotTheShadowSitsWhereThePrototypeHasIt() {
        assertEquals(38f * .52f, contactShadowY(845f, 38f, 0f, Float.NaN))
        assertEquals(38f * .44f, contactShadowR(38f, 0f))
    }
}
