package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.ShapeTokens

internal object MorphDimens {
    val CardRadius = ShapeTokens.Large
    val DetailRadius = 0.dp

    // The bounds tweens, on Material's emphasized curve: it leaves slowly and lands softly, where
    // FastOutSlowIn leaves in a hurry and reads as a snap. No springs anywhere in the morph: an
    // overshoot on the bounds would drag the clip and the crop past the destination with it.
    const val OpenMs = 500
    const val CloseMs = 400
    val MorphEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // A rising radius outruns the bounds on purpose: the backdrop fades over this window, and a
    // card still rounding when the bounds land floats square over it.
    const val RiseMs = 200

    // The chrome is a function of the container's own width on each frame, as a fraction of its
    // final width, so it settles on the very frame the bounds do. A parallel clock, even one with
    // the same tween, starts a frame apart from the bounds (those begin once the match is found in
    // layout) and lands a frame early. It comes in over the last stretch of the container's growth
    // and leaves over the first stretch of its shrink; a card is about 0.46 of the stage width.
    const val ChromeStartWidth = 0.65f

    // The timed fade that sits on top of the geometric one comes in over this long, outlasting
    // the 500 ms open so the chrome keeps settling for a beat after the photo has landed, and
    // leaves fast on a close.
    const val ChromeFadeInMs = 1000
    const val ChromeFadeOutMs = 120

    // The backdrop sits outside the node and rides the bounds' progress instead. On open it follows
    // progress; on close it is gone by half way, so the grid is there when the photo lands.
    const val BackdropExitFraction = 0.5f
    const val Columns = 2
}
