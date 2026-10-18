package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShelfLayoutTest {

    private fun spec(id: Int, thickness: Float = 30f) =
        ShelfBookSpec(id = id, thickness = thickness, height = 220f, depth = 150f)

    @Test
    fun aPlankHoldsAtMostThePerPlankCap() {
        val books = (1..20).map { spec(it, thickness = 10f) }

        val layout = layoutShelf(width = 2000f, height = 800f, planks = listOf(books))

        assertEquals(MAX_BOOKS_PER_PLANK, layout.planks.single().slots.size)
    }

    @Test
    fun aPlankStopsAtTheFirstBookThatWouldOverflowTheWidth() {
        val books = (1..9).map { spec(it, thickness = 40f) }

        val layout = layoutShelf(width = 300f, height = 800f, planks = listOf(books))
        val plank = layout.planks.single()

        assertTrue(plank.slots.size < 9)
        val last = plank.slots.last()
        assertTrue(last.x + last.thickness <= 300f - plank.margin)
    }

    @Test
    fun theRowIsCentred() {
        val layout =
            layoutShelf(width = 400f, height = 800f, planks = listOf(listOf(spec(1), spec(2))))
        val plank = layout.planks.single()
        val left = plank.slots.first().x
        val right = 400f - (plank.slots.last().x + plank.slots.last().thickness)

        assertEquals(left, right, 1f)
    }

    @Test
    fun twoPlanksShareOneScaleAndStackTopToBottom() {
        val layout =
            layoutShelf(
                width = 400f,
                height = 800f,
                planks = listOf(listOf(spec(1)), listOf(spec(2))),
            )

        assertEquals(2, layout.planks.size)
        assertTrue(layout.planks[0].plankY < layout.planks[1].plankY)
        val top = layout.planks[0].slots.single()
        val bottom = layout.planks[1].slots.single()
        assertEquals(top.height, bottom.height, 1e-3f)
    }

    @Test
    fun booksNeverStandAboveThePlankAboveThem() {
        val layout =
            layoutShelf(
                width = 400f,
                height = 800f,
                planks = listOf(listOf(spec(1)), listOf(spec(2))),
            )
        val lower = layout.planks[1]

        assertTrue(lower.plankY - lower.slots.single().height > layout.planks[0].plankY)
    }

    @Test
    fun placingAShelfOnALargerStageMovesEveryBookWithIt() {
        val shelf =
            layoutShelf(width = 400f, height = 300f, planks = listOf(listOf(spec(1), spec(2))))

        val staged = shelf.onStage(x = 10f, y = 120f, stageWidth = 400f, stageHeight = 900f)

        assertEquals(900f, staged.height, 0f)
        assertEquals(shelf.scale, staged.scale, 0f)
        assertEquals(shelf.planks[0].plankY + 120f, staged.planks[0].plankY, 1e-3f)
        assertEquals(
            shelf.planks[0].slots.map { it.x + 10f },
            staged.planks[0].slots.map { it.x },
        )
        assertEquals(shelf.planks[0].left + 10f, staged.planks[0].left, 1e-3f)
    }

    @Test
    fun bothPlanksShareOneWidthAndAShortRowStillGetsAProperPlank() {
        val layout =
            layoutShelf(
                width = 400f,
                height = 800f,
                planks = listOf((1..6).map { spec(it) }, listOf(spec(7), spec(8))),
            )
        val (top, bottom) = layout.planks

        assertEquals(top.left, bottom.left, 1e-3f)
        assertEquals(top.right, bottom.right, 1e-3f)
        assertTrue(bottom.right - bottom.left >= 400f * 0.72f - 1e-3f)
        assertTrue(bottom.slots.all { it.x >= bottom.left && it.x + it.thickness <= bottom.right })
    }

    @Test
    fun anEmptyPlankListLaysOutNothing() {
        assertTrue(layoutShelf(400f, 800f, emptyList()).planks.isEmpty())
    }

    @Test
    fun longerBooksGetThickerSpinesClampedAtBothEnds() {
        assertEquals(MIN_SPINE_THICKNESS, spineThicknessFor(durationSeconds = null), 1e-3f)
        assertEquals(MIN_SPINE_THICKNESS, spineThicknessFor(durationSeconds = 600.0), 1e-3f)
        assertEquals(MAX_SPINE_THICKNESS, spineThicknessFor(durationSeconds = 90_000.0), 1e-3f)
        assertTrue(spineThicknessFor(5 * 3600.0) < spineThicknessFor(10 * 3600.0))
    }
}
