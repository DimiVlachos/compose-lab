package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.ui.unit.dp

/** The pull-cord lamp's sizes, timings and physics, in dp, seconds and milliseconds. */
internal object PullCordDimens {
    // The lamp hangs on a rod from a small canopy on the ceiling; the shade is a dome this wide at
    // its rim and this tall, and the bulb shows just under the rim.
    val Rod = 64.dp
    val CanopyWidth = 26.dp
    val CanopyHeight = 6.dp
    val ShadeTop = 18.dp
    val ShadeRim = 76.dp
    val ShadeHeight = 40.dp
    val BulbRadius = 11.dp

    // The cord hangs from the shade's right-hand side, inside the rim, so it clears the bulb.
    val CordInset = 24.dp

    // The cord: this many points, this long in all, with a bead on its end. A finger within this
    // much of the bead takes it: a 48 dp target, and no more, so a tap just below it still reaches
    // the screen.
    const val CordPoints = 12
    val CordLength = 110.dp
    val CordWidth = 1.6.dp
    val BeadRadius = 7.dp
    val GrabRadius = 24.dp

    // A finger this far down from where it took the bead clicks the switch.
    val ClickPull = 48.dp

    // Pulled past its length the cord gives at most this much, harder the further it goes, as a
    // cord with a spring switch at the top does.
    val MostStretch = 30.dp

    // The cord is stepped at a fixed rate, whatever the frame rate, so it moves the same on a
    // 60 Hz and a 120 Hz screen; its lengths are put right this many times a step, down the cord
    // and back up it in turn, so neither end is favoured.
    const val StepSeconds = 1f / 120f
    const val ConstraintPasses = 24

    // A frame longer than this (a stall, a test clock's jump) is stepped as this long.
    const val MostFrameSeconds = 0.05f

    // Gravity on the cord, in dp a second squared: a light cord, quicker than a real one would
    // fall at this size, so it swings at about the pace a real one does in the hand.
    const val Gravity = 3_200f

    // The share of its speed the cord keeps losing each second: a swing dies away in a few.
    const val CordDamping = 2.2f

    // Let go, the stretch comes out of the cord this quickly (a time constant, in seconds): fast
    // enough to throw the bead up past where it hangs.
    const val Recoil = 0.035f

    // The shade is a pendulum from the ceiling: it sways at this many swings a second, losing this
    // share of its swing as it goes, and never tilts past this many radians.
    const val ShadeSwingHz = 1.3f
    const val ShadeDamping = 0.18f
    const val MostTilt = 0.35f

    // How hard the cord's pull turns the shade, per dp of stretch and of lever.
    const val TiltPerPull = 0.016f

    // Moving less than this a step, in dp, the cord is still; a shade swinging less than this, in
    // radians and radians a second, is level. Both for this many steps in a row, the lamp is at
    // rest: a swing stands still for a moment at either end, and that is not rest.
    const val StillStep = 0.004f
    const val LevelTilt = 0.002f
    const val LevelSwing = 0.01f
    const val QuietSteps = 30

    // Switched on, the light spreads out over the screen from the bulb in this long; the bulb
    // flickers twice as it comes on, and goes out in this long. The light it throws goes out with
    // the lit look, as the other look covers it.
    const val RevealMs = 520
    const val FlickerMs = 300
    const val OffMs = 120

    // The flicker: full at the first time, down to the first dip at the next, full again, down to
    // the second dip, and full from then on, at FlickerMs.
    const val FlickerFullMs = 40
    const val FirstDipMs = 90
    const val FirstDip = 0.3f
    const val FlickerBackMs = 140
    const val SecondDipMs = 200
    const val SecondDip = 0.45f

    // The cone of light the lamp throws, either side of straight down, and how bright it is at
    // its brightest, at the bulb.
    const val ConeHalfAngle = 0.62f
    const val ConeGlow = 0.34f
    const val HaloGlow = 0.55f
    val HaloRadius = 46.dp
}
