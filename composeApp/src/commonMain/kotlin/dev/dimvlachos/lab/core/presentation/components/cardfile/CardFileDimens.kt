package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.unit.dp

/** The card file's sizes, angles and feel. Positions and velocities are in cards. */
internal object CardFileDimens {
    /** How far a finger drags to turn one card; a knob turns a quarter as fast. */
    val CardDrag = 56.dp
    const val KnobRatio = 0.25f

    /** How fast a fling dies away, per second: a fling of v cards/s carries v / Friction cards. */
    const val Friction = 4f
    /** How stiffly the wheel resists being dragged past either end. */
    const val RubberStiffness = 0.8f

    /** A card behind the front leans back this much per card, up to [BehindAngle]. */
    const val BehindStep = 9f
    const val BehindAngle = 40f
    /** A card turned over the top falls forward to lie at [FlippedAngle]. */
    const val FlippedAngle = 95f

    /** How many cards either side of the front are composed. */
    const val Window = 4

    const val TickGapMs = 45L

    /** A pulled-out card pushed down past this share of its height is filed. */
    const val FileFraction = 0.3f

    /**
     * A pulled card flicked down faster than this, in fractions of its height per second, is filed.
     */
    const val FlickFilesVelocity = 1.5f

    val CardWidth = 280.dp
    val CardHeight = 168.dp
    /** How far the rod sits under the card's bottom edge: the pivot of each card's turn. */
    val RodDrop = 22.dp
}
