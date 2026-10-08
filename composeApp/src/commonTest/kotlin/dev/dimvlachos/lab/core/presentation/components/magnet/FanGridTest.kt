package dev.dimvlachos.lab.core.presentation.components.magnet

import kotlin.math.abs
import kotlin.test.Test
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
}
