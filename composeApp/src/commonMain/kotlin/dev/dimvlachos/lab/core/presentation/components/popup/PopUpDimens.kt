package dev.dimvlachos.lab.core.presentation.components.popup

import kotlin.math.PI

internal object PopUpDimens {
    // A page, along the gutter and from the gutter to its edge; the boards a little larger.
    const val PageWidth = 300f
    const val PageDepth = 190f
    const val BoardWidth = 314f
    const val BoardDepth = 198f

    // The eye: pitched 48° down onto the table, 1100 book units away, and the book's width
    // across the frame in book units, its gutter 0.41 of the way down.
    const val CameraPitch = (48.0 * PI / 180.0).toFloat()
    const val CameraDistance = 1100f
    const val UnitsAcross = 372f
    const val CentreY = 0.41f

    // The book's frame is this many times as tall as it is wide.
    const val Aspect = 1.22f

    // Leaves: springs to 0 or π, critically damped, so paper swings over in about a second and
    // eases down onto its stack instead of snapping into place; a flick that still reaches a
    // stack bounces a little off it.
    const val LeafStiffness = 36f
    const val LeafDamping = 12f
    const val Restitution = 0.15f
    const val CommitLead = 0.15f

    // A finger that moves less than this many dp is a tap.
    const val TapSlopDp = 6f

    // Tabs: a spring that draws them back in, alive only once their spread is open this far.
    const val TabStiffness = 55f
    const val TabDamping = 10f
    const val TabActiveFrom = 2.4f
    const val TabReachDp = 34f

    // Where a tab's slit is, along the gutter and out from it, and how wide the tab is.
    const val TabX = 147f
    const val TabFromGutter = 141f
    const val TabWidth = 18f
    const val TabStub = 14f

    // Sails: speed gained per unit pulled out, and how fast they coast down.
    const val SailGain = 0.09f
    const val SailFriction = 0.45f

    // A boat's bob, radians per second.
    const val BobRate = 2.2f

    // Shadows: fading in from this opening to flat, at most this dark.
    const val ShadowFrom = 2.1f
    const val ShadowAlpha = 0.34f
    const val ShadowBlur = 3f

    // How dark paper turned from the light gets.
    const val ShadeAlpha = 0.85f

    // Spreads open less than this aren't drawn.
    const val OpenFrom = 0.03f

    // Physics runs at a fixed step.
    const val Step = 1f / 120f
    const val MaxFrameSeconds = 0.05f
}
