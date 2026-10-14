package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.ui.unit.dp

/**
 * The smoke's sizes, timings, motion and loudness: lengths in dp, speeds in dp a second, times in
 * seconds, volumes from 0 to 1.
 */
internal object SmokeDimens {
    // The burn front is this many columns across a row, each stamped with the moment the front
    // passed it, so a particle knows how long it has been smoke from its column alone.
    const val Columns = 64

    // A frame longer than this (a stall, a test clock's jump) is taken as this long.
    const val MostFrameSeconds = 0.05f

    // A row is cut into about this many particles: every pixel of ink is kept at InkShare of the
    // budget at most, the card's own colour making up the rest, and never more than MostPerRow.
    // While more than MostLive are in the air, a new row is thinned so the total stays near it.
    const val PerRow = 2_600
    const val MostPerRow = 3_000
    const val InkShare = 0.62f
    const val MostLive = 11_000
    const val LeastPerRow = 700

    // How far a pixel must be from the card's colour, summed over its channels out of 765, to be
    // taken as ink rather than the card.
    const val InkDistance = 54

    // The row's colours are cut down to at most this many, the card's first, so its particles can
    // be drawn in a batch a colour.
    const val MostColours = 8

    // A smoke particle lives this long, plus up to LifeSpread more, the card's own colour lasting
    // BackgroundLife as long as ink; a mist particle MistLife as long again. Each starts up to
    // Jitter after its column is crossed, so a column doesn't lift off as one.
    const val Life = 2.6f
    const val LifeSpread = 1.5f
    const val BackgroundLife = 0.7f
    const val MistLife = 1.25f
    const val Jitter = 0.07f

    // The path a particle takes is this many steps through the curl-noise field, which is this
    // many dp across a cell, with a second octave this much finer at this share.
    const val PathSteps = 3
    val NoiseCell = 92.dp
    const val FineOctave = 2.7f
    const val FineShare = 0.55f

    // Smoke kicks off away from the burning edge at KickSpeed dp/s, fading at KickFade a second,
    // and rises at RiseSpeed dp/s, gaining RiseGain dp/s every second; the noise turns it at up to
    // CurlSpeed dp/s once it has lifted clear. Sparks rise SparkRise faster.
    const val KickSpeed = 13f
    const val KickFade = 3f
    const val RiseSpeed = 30f
    const val RiseGain = 80f
    const val CurlSpeed = 50f
    const val SparkRise = 0.6f

    // Mist barely rises and drifts the way it was swiped instead, softer and wider.
    const val MistRise = 5f
    const val MistRiseGain = 9f
    const val MistDrift = 34f
    const val MistDriftGain = 18f
    const val MistCurl = 38f

    // As it ages a particle wanders this many dp off its path, the ink keeping its shape longest.
    const val Wander = 40f

    // And drifts off its neighbours by up to this many dp over its first second, on its own.
    const val Diffuse = 22f

    // About this share of the ink stays lit as sparks: brighter, smaller, quicker to rise.
    const val SparkShare = 0.03f

    // Ember glows through the first EmberSeconds of a particle's life, and the smoke takes over
    // its colour from SmokeFrom to SmokeTo seconds.
    const val EmberSeconds = 0.22f
    const val SmokeFrom = 0.12f
    const val SmokeTo = 1.1f

    // A row burns on by itself at AutoSpeed dp/s to start, gaining AutoGain a second, up to
    // AutoTop; a release faster than FlingSpeed dp/s finishes it whatever its distance.
    const val AutoSpeed = 380f
    const val AutoGain = 2_300f
    const val AutoTop = 1_150f
    const val FlingSpeed = 650f

    // A swipe burns the row this many times as far as the finger has moved, and starts once the
    // finger has gone Slop across, and more across than up. Let go past Commit of the row, it
    // burns the rest of the way; short of it, the smoke is drawn back in, over ReformShort.
    const val Pull = 1.35f
    val Slop = 7.dp
    const val Commit = 1f / 3f
    const val ReformShort = 0.52f

    // Undo breathes the smoke back over ReformUndo, starting ReformDelay after the row has begun
    // to open again. Reforming starts each particle from no older than ReformFrom of its life.
    const val ReformUndo = 1.15f
    const val ReformDelay = 0.38f
    const val ReformFrom = 0.5f

    // Burnt, a row waits CloseDelay and then its height springs to nothing at this stiffness and
    // damping, as does a row coming back, the other way.
    const val CloseDelay = 0.15f
    const val HeightStiffness = 330f
    const val HeightDamping = 31f

    // The burning edge is ragged over this many points down the row, flickering up to Ragged dp
    // either side; it is drawn as a char band, a hot rim and a white-hot core.
    const val EdgePoints = 12
    val Ragged = 3.4.dp
    val CharWidth = 12.dp
    val GlowWidth = 9.dp
    val RimWidth = 3.6.dp
    val CoreWidth = 1.3.dp
    val MistEdgeWidth = 14.dp
    val GlowInner = 5.dp
    val MistLine = 2.dp

    // How far behind the front each stroke of the edge lies, on the row's side: the char furthest
    // back, then the rim and the core; mist's haze a little back.
    val CharBack = 5.dp
    val RimBack = 0.6.dp
    val CoreBack = 0.3.dp
    val MistBack = 3.dp

    // A finger that goes this far up or down before it has gone across is scrolling the list. The
    // focused row has a ring this wide inside its edge.
    val VerticalGiveUp = 12.dp
    val FocusRing = 2.dp

    // How soft the smoke's body and its oldest haze are drawn.
    val SoftBlur = 2.5.dp
    val HazeBlur = 7.dp

    // As they thin out, ink and the card's own colour are drawn this much more opaque than their
    // fading alone would leave them, so a few thousand particles keep the smoke's body; mist is
    // this see-through; sparks are this many dp across.
    const val InkBoost = 0.6f
    const val CardBoost = 0.3f
    const val MistOpacity = 0.8f
    val SparkSize = 1.7.dp

    // A row's height springs in steps no longer than this.
    const val HeightStep = 1f / 120f

    // The burn loop's volume follows how fast the rows are burning, this loud at AutoTop, with a
    // little hiss for a finger on a row that isn't moving.
    const val LoopVolume = 0.9f
    const val HoldVolume = 0.12f

    // The burn loop stops this long after the last row stops burning. The mist's whoosh and the
    // undo's inhale play this loud; a smoke drawn back in short of the commit inhales quieter and
    // quicker, and a wave of rows coming back breathes once in this long, not once each.
    const val LoopTail = 0.3f
    const val MistVolume = 0.8f
    const val InhaleVolume = 0.85f
    const val ShortInhaleVolume = 0.6f
    const val ShortInhaleRate = 1.4f
    const val InhaleGap = 0.6f
}
