package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

/** The card file's sizes, angles and feel. Positions and velocities are in cards. */
internal object CardFileDimens {
    /** How far a finger drags to turn one card; a knob turns a quarter as fast. */
    val CardDrag = 56.dp
    const val KnobRatio = 0.25f

    /** How fast a fling dies away, per second: a fling of v cards/s carries v / Friction cards. */
    const val Friction = 4f
    /** A release is taken at no more than this many cards a second, either way. */
    const val MaxFlingVelocity = 40f
    /** How stiffly the wheel resists being dragged past either end. */
    const val RubberStiffness = 0.8f

    /** A card behind the front leans back this much per card, up to [BehindAngle]. */
    const val BehindStep = 4f
    const val BehindAngle = 24f
    /**
     * A card turned over the top falls forward past level to [FallAngle]; each card under it on the
     * pile lies [FallStep] further over, up to [FlippedAngle].
     */
    const val FallAngle = 116f
    const val FallStep = 7f
    const val FlippedAngle = 138f

    /** How many cards either side of the front are composed. */
    const val Window = 4

    const val TickGapMs = 45L

    /** A pulled-out card pushed down past this share of its height is filed. */
    const val FileFraction = 0.3f

    /**
     * A pulled card flicked down faster than this, in fractions of its height per second, is filed.
     */
    const val FlickFilesVelocity = 1.5f

    /**
     * Each card behind the front stands this much higher, up to [BehindLiftCards] cards back, as if
     * seen from a little above, so their tops peek over the front card and can be tapped.
     */
    val BehindLift = 16.dp
    const val BehindLiftCards = 4f

    /** A card spans this share of the stage's width, within [MinCardWidth]..[MaxCardWidth]. */
    const val CardWidthShare = 0.82f
    val MinCardWidth = 200.dp
    val MaxCardWidth = 420.dp
    /** An index card's height over its width: a 5 × 3 card. */
    const val CardAspect = 0.6f
    /** A card is never taller than this share of the stage's height. */
    const val CardMaxHeightShare = 0.34f
    /** The front card's centre, as a share of the stage's height from the top. */
    const val WheelCentre = 0.4f

    /** How far the rod sits under the card's bottom edge: the pivot of each card's turn. */
    val RodDrop = 14.dp
    val RodThickness = 9.dp
    /** The knurled knobs at the rod's ends, seen side-on: a short chrome drum. */
    val KnobWidth = 30.dp
    val KnobHeight = 46.dp
    /** How far the rod reaches past the cards on each side, to the knobs. */
    val RodOverhang = 14.dp
    /** How far under the rod the base sits, as a share of the card's height. */
    const val BaseDropShare = 0.5f

    /** The pulled card's top, as a share of the stage's height. */
    const val PulledTop = 0.05f
    /** The pulled card is never wider than this. */
    val MaxPulledWidth = 480.dp

    /** The size of a card on a stage of [width] × [height]. */
    fun cardSize(width: Dp, height: Dp): DpSize {
        // Room is left either side for the rod's overhang and the knobs.
        val besideKnobs = width - (RodOverhang + KnobWidth + 8.dp) * 2
        val w =
            min(
                    min(min(width * CardWidthShare, besideKnobs), MaxCardWidth),
                    height * CardMaxHeightShare / CardAspect,
                )
                .coerceAtLeast(MinCardWidth.coerceAtMost(width))
        return DpSize(w, w * CardAspect)
    }
}
