package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The chat round the rubber band, as the prototype's `--a-*` colours: a warm paper thread by day
 * and a near black one by night. [me] is my bubbles' blue (also the caret and the send button),
 * [bar] the app bar and composer, [line] their hairlines; the `*Css` strings are the same colours
 * for the canvas.
 */
internal class RubberBandScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val me: Color,
    val meInk: Color,
    val bar: Color,
    val meCss: String,
) {
    /** The caption's "Pull the band", in the band's tan. */
    val pull = Color(0xFFA8743A)

    /** The app bar's "Active now". */
    val active = Color(0xFF2F9A5A)

    /** The other person's avatar. */
    val avatar = Color(0xFFC2574F)

    internal companion object {
        val Light =
            RubberBandScreenColors(
                background = Color(0xFFF3F1ED),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF22242B),
                muted = Color(0xFF77736C),
                line = Color(0xFFE1DDD5),
                me = Color(0xFF2F63D8),
                meInk = Color(0xFFFFFFFF),
                bar = Color(0xFFFBFAF8),
                meCss = "#2f63d8",
            )
        val Dark =
            RubberBandScreenColors(
                background = Color(0xFF16161A),
                card = Color(0xFF232328),
                ink = Color(0xFFECE9E3),
                muted = Color(0xFFA29D94),
                line = Color(0xFF34333A),
                me = Color(0xFF3F73E6),
                meInk = Color(0xFFFFFFFF),
                bar = Color(0xFF1C1C21),
                meCss = "#3f73e6",
            )
    }
}
