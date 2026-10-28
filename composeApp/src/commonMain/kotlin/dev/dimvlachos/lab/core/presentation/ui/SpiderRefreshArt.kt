package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The Elm Hollow neighborhood feed round the spider, as the prototype styles it: warm paper by day
 * and an ink-violet night, with a pumpkin accent. The spider's own colours follow the theme too, as
 * the prototype's `readColors()`: a dark body with a soft rim at night, its silk and legs light.
 */
internal class SpiderRefreshScreenColors(
    val background: Color,
    val card: Color,
    val cardEdge: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    /** The card's box shadows: CSS's `0 1px 2px` and `0 4px 14px -8px` (none at night). */
    val shadowNear: Color,
    val shadowFar: Color,
    /** The spider's silk, opaque: the drawing sets its alpha per stroke. */
    val silk: Color,
    val leg: Color,
    val body0: Color,
    val body1: Color,
    val rim: Color,
    val ball: Color,
    val ballEdge: Color,
    val dark: Boolean,
) {
    internal companion object {
        val Light =
            SpiderRefreshScreenColors(
                background = Color(0xFFF3F1ED),
                card = Color(0xFFFFFFFF),
                cardEdge = Color(40, 30, 20).copy(alpha = .07f),
                ink = Color(0xFF1D1B20),
                muted = Color(0xFF6D6873),
                line = Color(0xFFE3DED9),
                chip = Color(40, 30, 25).copy(alpha = .06f),
                accent = Color(0xFFD4612A),
                shadowNear = Color(40, 30, 20).copy(alpha = .06f),
                shadowFar = Color(40, 30, 20).copy(alpha = .16f),
                silk = Color(84, 78, 98),
                leg = Color(0xFF2A2433),
                body0 = Color(0xFF4A4158),
                body1 = Color(0xFF1D1824),
                rim = Color.Transparent,
                ball = Color(252, 251, 255).copy(alpha = .98f),
                ballEdge = Color(60, 50, 80).copy(alpha = .22f),
                dark = false,
            )
        val Dark =
            SpiderRefreshScreenColors(
                background = Color(0xFF121116),
                card = Color(0xFF1C1A21),
                cardEdge = Color.White.copy(alpha = .06f),
                ink = Color(0xFFEFEDF2),
                muted = Color(0xFFA29EAA),
                line = Color(0xFF2C2932),
                chip = Color.White.copy(alpha = .07f),
                accent = Color(0xFFF08A4B),
                shadowNear = Color.Black.copy(alpha = .3f),
                shadowFar = Color.Transparent,
                silk = Color(226, 222, 240),
                leg = Color(0xFFCFC6DC),
                body0 = Color(0xFF5D536C),
                body1 = Color(0xFF2C2635),
                rim = Color.White.copy(alpha = .16f),
                ball = Color(240, 238, 248).copy(alpha = .95f),
                ballEdge = Color.Black.copy(alpha = .25f),
                dark = true,
            )

        /** The spider's face, the same in both themes: pupils, cheeks, its smile and its shine. */
        val Pupil = Color(0xFF17121C)
        val Cheek = Color(255, 128, 150).copy(alpha = .38f)
        val Mouth = Color.White.copy(alpha = .75f)
        val Shine = Color.White.copy(alpha = .12f)

        /** The threads wound round the silk ball. */
        val BallLines = Color(140, 132, 160).copy(alpha = .55f)

        /** The "You're all caught up" pill, the same in both themes. */
        val Toast = Color(28, 24, 32).copy(alpha = .92f)
        val ToastInk = Color(0xFFF3F1F5)

        /** Each neighbor's avatar gradient, from its top left: the prototype's `g` pairs. */
        val Avatars: List<Pair<Color, Color>> =
            listOf(
                Color(0xFF5D8F6A) to Color(0xFF2F5B45),
                Color(0xFFC77AD6) to Color(0xFF6A3F9E),
                Color(0xFF5F86D8) to Color(0xFF2C4FA3),
                Color(0xFFD8A24A) to Color(0xFF9A5E1E),
                Color(0xFFE07A6A) to Color(0xFFA8404F),
                Color(0xFFF0A050) to Color(0xFFC45C22),
                Color(0xFFC77AD6) to Color(0xFF6A3F9E),
                Color(0xFF9BBF4F) to Color(0xFF4F7A2A),
                Color(0xFF6AB6C8) to Color(0xFF2A6F86),
                Color(0xFF8A7AD8) to Color(0xFF4A3A9E),
                Color(0xFFE0604A) to Color(0xFFA02A22),
            )
    }
}
