package dev.dimvlachos.lab.core.presentation.components.fishing

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

    // Water whose every column sits within this many dp of rest, moving slower than this many dp
    // a second, is still.
    const val StillHeight = 0.02f
    const val StillSpeed = 0.2f
}
