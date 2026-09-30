package dev.dimvlachos.lab.core.demo

import kotlin.time.Duration

/**
 * A scripted finger on a page. It goes down, makes each of [moves] in turn and lets go moving at
 * [releaseSpeed] book widths a second (negative is leftwards). A release that fast is a flick.
 */
class PageDrag(val moves: List<DragMove>, val releaseSpeed: Float = 0f)

/**
 * Moves the finger to [toFraction] of the book's width from where it went down (negative is
 * leftwards, towards the next spread), over [duration].
 */
class DragMove(val toFraction: Float, val duration: Duration)
