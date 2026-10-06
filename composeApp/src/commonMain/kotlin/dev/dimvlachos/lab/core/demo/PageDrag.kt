package dev.dimvlachos.lab.core.demo

import kotlin.time.Duration

/**
 * A scripted finger on a page. It goes down at ([x], [y]), as fractions of the book across and down
 * (null: where a thumb would take the page), makes each of [moves] in turn and lets go moving at
 * [releaseSpeed] book widths a second (negative is leftwards). A release that fast is a flick.
 */
internal class PageDrag(
    val moves: List<DragMove>,
    val releaseSpeed: Float = 0f,
    val x: Float? = null,
    val y: Float? = null,
)

/**
 * Moves the finger to [toFraction] of the book's width from where it went down (negative is
 * leftwards, towards the next spread) and [toFractionY] of its height (negative is up), over
 * [duration].
 */
internal class DragMove(val toFraction: Float, val duration: Duration, val toFractionY: Float = 0f)
