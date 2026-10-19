package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShelfSceneLayoutTest {
    private fun specs(from: Int, count: Int) =
        (from until from + count).map { id ->
            ShelfBookSpec(id, thickness = 22f + id % 20, height = 198f + id * 53 % 42, depth = 140f)
        }

    private val scene =
        ShelfSceneLayout.of(
            width = 412f,
            viewportHeight = 732f,
            ring = specs(1, 8),
            planks = listOf(specs(9, 8), specs(17, 8)),
        )

    @Test
    fun theRingTakesTheTopFortyFivePercentOfTheViewport() {
        assertEquals(0f, scene.ringTop)
        assertEquals(732f * 0.45f, scene.ringBottom, 1e-3f)
        assertEquals(scene.ringBottom, scene.ring.height, 1e-3f)
    }

    @Test
    fun everyPlankBookStandsBelowTheRing() {
        for (plank in scene.planks.planks) {
            for (slot in plank.slots) {
                assertTrue(plank.plankY - slot.height > scene.ringBottom, "book ${slot.bookId}")
            }
        }
    }

    @Test
    fun theSceneIsTallEnoughForTheLastPlank() {
        val lastPlank = scene.planks.planks.last().plankY
        assertTrue(scene.totalHeight > lastPlank)
        assertEquals(scene.ringBottom + 412f * 640f / 412f, scene.totalHeight, 1e-3f)
    }

    @Test
    fun aPlankHoldsAtMostNineBooks() {
        val crowded =
            ShelfSceneLayout.of(412f, 732f, specs(1, 8), listOf(specs(9, 12), specs(21, 3)))

        assertTrue(crowded.planks.planks[0].slots.size <= MAX_BOOKS_PER_PLANK)
    }

    @Test
    fun bothPlanksAreLaidOut() {
        assertEquals(2, scene.planks.planks.size)
        assertTrue(scene.planks.planks[0].plankY < scene.planks.planks[1].plankY)
    }
}
