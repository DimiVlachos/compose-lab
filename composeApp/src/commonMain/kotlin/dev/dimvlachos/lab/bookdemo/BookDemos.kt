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
    // A drag of DragSpan (0.62) of the book's width is a whole turn, and a page let go under 0.42
    // of a turn falls back: 0.2 of the width lifts it a third of the way.
    private val tour =
        demoScript(holdEnd = 1_400.milliseconds) {
            // The corner lifted slowly, held, and let go: it sinks back.
            at(0.4.seconds) {
                dragPage(
                    PageDrag(listOf(DragMove(-0.2f, 1.1.seconds), DragMove(-0.2f, 0.4.seconds)))
                )
            }
            // A short, fast flick is enough to turn it.
            at(3.0.seconds) {
                dragPage(PageDrag(listOf(DragMove(-0.12f, 0.14.seconds)), releaseSpeed = -2.4f))
            }
            at(3.8.seconds) { select(1) } // already open; keeps the script's index with the book
            at(4.6.seconds) { select(2) } // a tap on the right page
            // Most of the way over, then back: the bow flips with the finger and the page falls
            // back.
            at(6.2.seconds) {
                dragPage(
                    PageDrag(listOf(DragMove(-0.5f, 1.0.seconds), DragMove(-0.12f, 0.9.seconds)))
                )
            }
            at(9.4.seconds) { select(3) }
            at(10.9.seconds) { select(2) } // taps on the left page, back to the start
            at(12.0.seconds) { select(1) }
            at(13.1.seconds) { select(0) }
        }

    val all: List<Demo> =
        listOf(
            Demo("book.turn", Res.string.demo_book_turn, tour, landscape = true) { BookDemo(it) }
        )
}
