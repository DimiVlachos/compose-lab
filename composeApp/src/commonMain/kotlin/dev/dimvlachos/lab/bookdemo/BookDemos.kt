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
    // lifts it about a third of the way. The book is 2.1 pages wide, so a page's height is about
    // half its width as a fraction.
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
            // Taken by the top corner, which peels first; the finger slides down the edge to the
            // bottom corner as it pulls, and the bottom corner takes the lead over to the left.
            at(4.4.seconds) {
                dragPage(
                    PageDrag(
                        listOf(
                            DragMove(-0.14f, 0.6.seconds),
                            DragMove(-0.3f, 1.0.seconds, toFractionY = 0.78f),
                            DragMove(-0.52f, 0.6.seconds, toFractionY = 0.78f),
                        ),
                        x = 0.9f,
                        y = 0.1f,
                    )
                )
            }
            at(7.0.seconds) { select(2) } // already turned by the drag
            // The bottom corner pulled up and in on a slant, most of the way over, then back: the
            // fold lies across the pull, the bow swings over with the finger, and it falls back.
            at(7.4.seconds) {
                dragPage(
                    PageDrag(
                        listOf(
                            DragMove(-0.45f, 1.0.seconds, toFractionY = -0.12f),
                            DragMove(-0.1f, 0.8.seconds, toFractionY = -0.03f),
                        ),
                        x = 0.9f,
                        y = 0.88f,
                    )
                )
            }
            // A page pulled back from the left by its top corner, down and in, and let go on its
            // way: it turns back.
            at(10.0.seconds) {
                dragPage(
                    PageDrag(
                        listOf(DragMove(0.34f, 0.7.seconds, toFractionY = 0.1f)),
                        releaseSpeed = 1.8f,
                        x = 0.1f,
                        y = 0.12f,
                    )
                )
            }
            at(11.2.seconds) { select(1) } // already turned back by the drag
            // A quick hand riffles four pages on, each let go while the last is still in the air.
            at(11.8.seconds) { select(5) }
            // One pulled back just past the spine and let go to fall the rest of the way, caught in
            // the air on its way down and sent on again.
            at(14.2.seconds) {
                dragPage(
                    PageDrag(
                        listOf(DragMove(0.33f, 0.35.seconds)),
                        x = 0.14f,
                    )
                )
            }
            at(14.68.seconds) {
                dragPage(
                    PageDrag(
                        listOf(DragMove(-0.22f, 0.22.seconds)),
                        releaseSpeed = -2.4f,
                        x = 0.6f,
                    )
                )
            }
            // And five back to the start.
            at(16.4.seconds) { select(0) }
        }

    val all: List<Demo> =
        listOf(
            Demo("book.turn", Res.string.demo_book_turn, tour, landscape = true) { BookDemo(it) }
        )
}
