package dev.dimvlachos.lab.core.presentation.components.cardfile

/**
 * The card file's feel, as the prototype has it. Positions and velocities are in cards; distances
 * in dp, the prototype's CSS px.
 */
internal object CardFileDimens {
    /** A knob takes this many times the drag the cards do to turn one card, for fine control. */
    const val KnobRatio = 2.6f

    /** A fling of v cards/s carries on for this long: it comes to rest v × FlingCarry cards on. */
    const val FlingCarry = .3f
    /** A release is taken at no more than this many cards a second, either way. */
    const val MaxFlingVelocity = 60f
    /** A release slower than this, in cards a second, rests on the nearest card. */
    const val MinFlingVelocity = 1.2f
    /** How stiffly the wheel resists being dragged past either end: o goes o / (1 + 1.5 o). */
    const val RubberStiffness = 1.5f

    /** The wheel's spring onto a card: stiffness in 1/s², and its damping ratio. */
    const val WheelStiffness = 70f
    const val WheelDamping = .92f

    /** The pulled card's spring out of the rail and back in. */
    const val PullStiffness = 95f
    const val PullDamping = .9f

    const val TickGapMs = 16L

    /** A pulled card let go less than this far out files itself. */
    const val FiledBelow = .72f
    /** A pulled card flicked down faster than this, in dp a second, files itself. */
    const val FlickFiles = 700f
    /** How far down a pulled card is pushed to go all the way back in, as a share of the stage. */
    const val PushTravel = .42f
    /** A pulled card can be pushed once it is this far out. */
    const val Grabbable = .85f

    /** A touch that moves less than this, in dp, and lifts within [TapMs] is a tap. */
    const val TapSlop = 8f
    const val TapMs = 350L
    /** A tap pulls the front card only when the wheel is this close to it, in cards. */
    const val TapOnCard = .15f
}
