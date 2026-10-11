package dev.dimvlachos.lab.core.presentation.components.magnet

import kotlin.math.abs
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FanGridTest {
    @Test
    fun theGridFitsTheTableAndNoTwoCardsOverlap() {
        for ((width, height) in listOf(300f to 508f, 400f to 700f, 700f to 300f)) {
            for (count in 1..12) {
                val grid = FanGrid(count, width, height, 76f, 56f, 12f)
                val w = 76f * grid.scale
                val h = 56f * grid.scale
                val cells = List(count) { grid.cell(it) }
                for (c in cells) {
                    assertTrue(c.x - w / 2f >= 0f && c.x + w / 2f <= width, "$count on $width: $c")
                    assertTrue(
                        c.y - h / 2f >= 0f && c.y + h / 2f <= height,
                        "$count on $height: $c",
                    )
                }
                for (i in cells.indices) for (j in i + 1 until cells.size) {
                    val apartX = abs(cells[i].x - cells[j].x) >= w
                    val apartY = abs(cells[i].y - cells[j].y) >= h
                    assertTrue(apartX || apartY, "$count: cells $i and $j overlap")
                }
                assertTrue(grid.scale <= MagnetDimens.FanScale)
            }
        }
    }

    @Test
    fun theGridLeavesRoomForItsHeaderAndATitleUnderEachCard() {
        for ((width, height) in listOf(300f to 508f, 400f to 700f, 700f to 300f)) {
            for (count in 1..12) {
                val grid = FanGrid(count, width, height, 76f, 56f, 12f, caption = 20f, header = 40f)
                val h = 56f * grid.scale
                for (k in 0 until count) {
                    val c = grid.cell(k)
                    assertTrue(c.y - h / 2f >= 40f, "$count on $height: card $k under the header")
                    assertTrue(c.y + h / 2f + 20f <= height, "$count on $height: title $k off")
                }
                for (k in 0 until count - 1) {
                    val a = grid.cell(k)
                    val b = grid.cell(k + 1)
                    if (b.y > a.y)
                        assertTrue(b.y - a.y >= h + 20f, "$count: title $k under the next row")
                }
            }
        }
    }

    @Test
    fun theHeaderSitsJustOverTheGridAndBothAreCentredTogether() {
        for (count in listOf(1, 3, 7)) {
            val grid = FanGrid(count, 400f, 700f, 76f, 56f, 12f, caption = 20f, header = 40f)
            val h = 56f * grid.scale
            val firstTop = grid.cell(0).y - h / 2f
            assertEquals(grid.top, firstTop, 0.01f)
            val bottom = grid.cell(count - 1).y + h / 2f + 20f
            // As much room over the header as under the last row's titles.
            assertEquals(700f - bottom, grid.top - 40f, 0.01f)
        }
    }

    private fun columns(count: Int, width: Float, height: Float) =
        fanColumns(count, width, height, 76f, 56f, 12f, caption = 26f, header = 48f)

    @Test
    fun aPortraitTableFansOutThreeAcross() {
        // The table in a 4:5 or 9:16 clip, under the hint's line and over the strip.
        for (width in 360..430 step 10) {
            for (frame in listOf(width * 5f / 4f, width * 16f / 9f)) {
                val height = frame - 40f - MagnetDimens.StripHeight.value
                for (count in 1..6) {
                    assertEquals(
                        min(count, MagnetDimens.FanColumns),
                        columns(count, width.toFloat(), height),
                        "$count on $width × $height",
                    )
                }
            }
        }
    }

    @Test
    fun aShortWideTableFansOutInAWiderGridWithBiggerCards() {
        for (count in 4..6) {
            assertTrue(columns(count, 800f, 158f) > MagnetDimens.FanColumns, "$count")
            val wide = FanGrid(count, 800f, 158f, 76f, 56f, 12f, caption = 26f, header = 48f)
            val three =
                fanScale(MagnetDimens.FanColumns, count, 800f, 158f, 76f, 56f, 12f, 26f, 48f)
            assertTrue(wide.scale > three, "$count: ${wide.scale} against $three")
        }
    }
}
