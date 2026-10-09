package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.ui.unit.dp

/** The fishing refresh's sizes, timings and physics, in dp, seconds and milliseconds. */
internal object FishingDimens {
    // The line and the water are stepped at a fixed rate, whatever the frame rate, so they move
    // the same on a 60 Hz and a 120 Hz screen. A frame longer than this (a stall, a test clock's
    // jump) is stepped as this long.
    const val StepSeconds = 1f / 120f
    const val MostFrameSeconds = 0.05f

    // The water is this many columns across, each pulled towards its neighbours, so a disturbance
    // runs out both ways at this many dp a second, and towards rest at this rate. It loses this
    // share of its speed each second: a splash dies away in a few seconds, a bob's ripple sooner.
    const val WaterColumns = 48
    const val WaveSpeed = 420f
    const val WaterSpring = 30f
    const val WaterDamping = 2.4f

    // At most this share of a column a step is as far as a ripple may run, so the water stays
    // stable however close its columns sit.
    const val StableShare = 0.9f

    // Water whose every column sits within this many dp of rest, moving slower than this many dp
    // a second, is still.
    const val StillHeight = 0.02f
    const val StillSpeed = 0.2f

    // The band of water and air the line is fished in: this tall once fully open, which is also
    // how far the list must be pulled (after the pull's own give) to ask for a refresh. The water's
    // top lies this far down it.
    val Band = 96.dp
    val WaterLevel = 60.dp

    // The rod reaches in from the left edge, its butt just off the screen, rising gently to its
    // tip. A pull bends it: the tip dips this far, and draws back this far, for each full
    // threshold of pull, up to twice the threshold. Let go, it springs back at this many swings
    // a second, losing this share of its swing as it goes; a cast flicks it forward at this speed.
    val RodButtX = (-6).dp
    val RodButtY = 36.dp
    val RodLength = 136.dp
    const val RodRise = 0.2f
    val RodDip = 26.dp
    val RodBack = 8.dp
    const val MostBend = 2f
    const val RodHz = 2.6f
    const val RodDamping = 0.3f
    const val CastFlick = -6f

    // The line: this many points, at most as long as the stage is wide. A light line, falling
    // quicker than a real one would at this size, losing this share of its speed each second, its
    // lengths put right this many times a step, and still once it moves less than this a step.
    const val LinePoints = 20
    const val LineGravity = 900f
    const val LineDamping = 3f
    const val LinePasses = 20
    const val LineStill = 0.004f

    // Out of the water the bobber dangles this far below the tip on a short line. Pulled, it is
    // drawn back and down, so the line goes from slack to taut as the pull reaches the threshold.
    val DangleLength = 34.dp
    val DangleBack = 14.dp
    val DangleDrop = 40.dp

    // Cast, it flies this long in an arc this high, out to this share of the stage's width, with
    // the line paid out this much longer than the way to it, so it sags a little into the water.
    const val CastSeconds = 0.6f
    val ArcHeight = 34.dp
    const val CastAt = 0.64f
    const val LineSlack = 1.06f

    // Landing, the bobber pushes the water down this fast across this wide a patch. It rides the
    // water's swell by this share, so a splash rocks it without tossing it out of the band.
    const val SplashPush = 120f
    val SplashSpread = 22.dp
    const val RideSwell = 0.6f

    // Its drops fly this long, up to this high and this far out either side.
    const val SplashSeconds = 0.55f
    val SplashHeight = 22.dp
    val SplashReach = 18.dp

    // Floating, it bobs this deep this many times a second, each bob sending out a small ripple.
    const val BobHz = 0.6f
    val BobDepth = 2.2.dp
    const val BobPush = 30f
    val BobSpread = 14.dp

    // However quickly a refresh lands, the bobber floats at least this long first.
    const val MinWaitSeconds = 0.6f

    // A bite pulls it under this deep, and back, in this long, with a swirl on the water and a
    // jerk at the rod's tip.
    const val BiteSeconds = 0.4f
    val BiteDepth = 9.dp
    const val BitePush = 220f
    val BiteSpread = 18.dp
    const val BiteJerk = 5f

    // Reeled in, the line comes back to the tip in this long.
    const val ReelSeconds = 0.75f

    // A catch's cards rise out of the water in this long each, one this much after another.
    const val RiseSeconds = 0.8f
    const val RiseStagger = 0.15f

    // A snapped line's bobber drifts off this fast, fading out over this long; the rod springs up.
    const val DriftSeconds = 1.6f
    const val DriftSpeed = 70f
    const val SnapFlick = -4f
}
