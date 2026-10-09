package dev.dimvlachos.lab.core.presentation.components.magnet

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelPlacementTest {
    @Test
    fun aLabelIsCentredOverItsMagnetButKeptOnTheStage() {
        // Room to spare: centred.
        assertEquals(150f, leftAcross(centre = 200f, width = 100f, stage = 400f))
        // Its magnet at the left edge: kept on, flush with it.
        assertEquals(0f, leftAcross(centre = 10f, width = 100f, stage = 400f))
        // At the right edge.
        assertEquals(300f, leftAcross(centre = 395f, width = 100f, stage = 400f))
        // Wider than the stage: from its left edge.
        assertEquals(0f, leftAcross(centre = 200f, width = 500f, stage = 400f))
    }
}
