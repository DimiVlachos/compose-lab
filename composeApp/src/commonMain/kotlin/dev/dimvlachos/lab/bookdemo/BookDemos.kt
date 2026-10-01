package dev.dimvlachos.lab.bookdemo

import dev.dimvlachos.lab.bookdemo.presentation.components.BookDemo
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DragMove
import dev.dimvlachos.lab.core.demo.PageDrag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_book_turn
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object BookDemos {
    // A finger takes the paper under it and keeps it there; a leaf let go short of about two
    // fifths of a turn falls back. Taken near the outer edge, a drag across a fifth of the book
    // lifts it about a third of the way.
    private val tour =
        demoScript(holdEnd = 1_800.milliseconds) {
            // The top corner taken and peeled slowly, held, and let go: it sinks back, the corner
            // straightening as it falls.
            at(0.4.seconds) {
                dragPage(
                    PageDrag(
                        listOf(
                            DragMove(-0.17f, 1.2.seconds, toFractionY = 0.05f),
                            DragMove(-0.17f, 0.5.seconds, toFractionY = 0.05f),
                        ),
                        x = 0.9f,
                        y = 0.1f,
                    )
                )
            }
            // A short, fast flick from the middle of the edge is enough to turn it; let go, the
            // paper whips over.
            at(3.0.seconds) {
                dragPage(PageDrag(listOf(DragMove(-0.12f, 0.14.seconds)), releaseSpeed = -2.4f))
            }
            at(3.8.seconds) { select(1) } // already open; keeps the script's index with the book
            at(4.6.seconds) { select(2) } // a tap on the right page
            // The bottom corner pulled most of the way over, then back: the leaf leans after the
            // corner, its bow swings over with the finger, and it falls back.
            at(6.2.seconds) {
                dragPage(
                    PageDrag(
                        listOf(
                            DragMove(-0.45f, 1.0.seconds, toFractionY = -0.08f),
                            DragMove(-0.1f, 0.9.seconds, toFractionY = -0.02f),
                        ),
                        x = 0.9f,
                        y = 0.88f,
                    )
                )
            }
            // A quick hand riffles three pages on, each let go while the last is still in the
            // air, then five back to the start.
            at(9.2.seconds) { select(5) }
            at(11.6.seconds) { select(0) }
        }

    val all: List<Demo> =
        listOf(
            Demo("book.turn", Res.string.demo_book_turn, tour, landscape = true) { BookDemo(it) }
        )
}
