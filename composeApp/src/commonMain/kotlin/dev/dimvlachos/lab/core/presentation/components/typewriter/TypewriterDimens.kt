package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The typewriter's sizes, timings and physics. Its geometry is in machine units: a phone 360 dp
 * wide is 360 units across, and a wider one scales them up, so the machine keeps its proportions;
 * [unit] says how big one is. Times are in seconds unless they say otherwise.
 */
internal object TypewriterDimens {
    /** How wide the stage is in units when one unit is a dp. */
    const val StageUnits = 360f

    // A unit is never smaller than this, nor bigger: on a tablet or a phone on its side, the
    // machine stays a portable's size, in the middle of the stage.
    private val SmallestUnit = 0.85.dp
    private val BiggestUnit = 1.3.dp

    /**
     * How big a unit is on a stage [width] wide and [height] tall: on a short one, as a phone on
     * its side, the machine with a full sheet stays under half its height.
     */
    fun unit(width: Dp, height: Dp = Dp.Infinity): Dp =
        minOf(width / StageUnits, height / ShortStageUnits).coerceIn(SmallestUnit, BiggestUnit)

    // A stage is this many units tall at the least before the machine shrinks to fit it.
    private const val ShortStageUnits = 440f

    // The sheet: this many letters to a line, each this wide, lines this far apart, with this
    // much paper either side of the text and above its first line. A sheet holds this many lines,
    // and is this tall in all.
    const val Columns = 18
    const val CharWidth = 9.4f
    const val LineHeight = 21f
    const val Margin = 14f
    const val TopMargin = 9f
    const val MaxLines = 4
    const val SheetWidth = Margin * 2 + Columns * CharWidth
    const val SheetHeight = 160f

    // The letters: this big, standing this far down their line.
    const val FontSize = 16f
    const val GlyphBaseline = 14f

    /** The bell rings this many letters before the margin. */
    const val BellWarning = 5
    const val BellColumn = Columns - BellWarning

    // Where the type bars strike, across the stage and down from the machine's top: the current
    // line's foot. The platen's top is just under it, and the enamel body under that.
    const val StrikeX = 232f
    const val StrikeY = 34f
    const val PlatenTop = 38f
    const val PlatenHeight = 30f
    const val PlatenOverhang = 16f
    const val PlatenCorner = 7f
    const val KnobWidth = 22f
    const val KnobHeight = 40f
    const val KnobRise = 5f
    const val BodyTop = 48f
    const val BodyHeight = 64f
    const val BodyCorner = 26f

    /** How tall the machine is, from the top of the lever's paddle to the foot of the body. */
    const val MachineHeight = BodyTop + BodyHeight

    /** Room over the paper for the hint. */
    const val HintRoom = 34f

    /** Where the sheet's top is at the first line, from the machine's top. */
    const val SheetTop = StrikeY - 26f

    /** Where the carriage is, the sheet's left edge, with the carriage at the first column. */
    const val HomeX = StrikeX - Margin

    // The basket of type bars, a fan in the body behind the platen, and the badge on the left.
    const val BasketLeft = 152f
    const val BasketTop = 6f
    const val BasketWidth = 160f
    const val BasketHeight = 32f
    const val BadgeLeft = 22f
    const val BadgeTop = 27f

    // The return lever: it pivots this far left of the carriage, this far down from the machine's
    // top, and points up and to the left; pushed, it swings this much to the right.
    const val LeverOffset = 27f
    const val LeverPivotY = 53f
    const val LeverAngle = -102f
    const val LeverSwing = 16f
    const val LeverLength = 46f

    // The carriage is a spring towards the column it should be at, stepped at a fixed rate:
    // stiff with a small overshoot as the escapement steps it on, and, sent home, a loose one that
    // aims past the stop and bangs into it, bouncing back this share of its speed.
    const val StepSeconds = 1f / 120f
    const val MostFrameSeconds = 0.05f
    const val Stiffness = 1500f
    const val Damping = 42f
    const val ReturnStiffness = 40f
    const val ReturnDamping = 4f
    const val ReturnOvershoot = 40f
    const val Bounce = 0.15f

    /** The paper's own spring as it feeds up a line, or back down one. */
    const val FeedStiffness = 900f
    const val FeedDamping = 55f

    // A return: the lever is pushed and the paper feeds a line, then the carriage is let go, so it
    // bangs home this long after the lever was touched, where the return's sound bangs too.
    const val ReturnBang = 0.41f
    const val LeverShortest = 0.08f

    // A strike: the type bar is up at the paper and back in this long, the letter lands on the
    // paper this long after the key, and the ribbon is lifted to it and back in this long.
    const val TypebarSeconds = 0.11f
    const val LandSeconds = 0.038f
    const val RibbonSeconds = 0.17f

    // A letter backspaced over is lifted off the paper by the tape: it fades out over this long,
    // rising this far.
    const val TapeSeconds = 0.3f
    const val TapeLift = 2.5f

    // The ribbon: this wide and tall, resting under the platen, lifted to the line struck.
    const val RibbonWidth = 20f
    const val RibbonHeight = 12f
    const val RibbonLift = 13f

    // Each letter is struck a little off its place, and a little turned, by up to this much either
    // way, and its ink is at least this dark.
    const val Wobble = 0.45f
    const val Bob = 0.65f
    const val Tilt = 1.6f
    const val LeastInk = 0.68f

    // Pulling the sheet: it stretches out of the roller like a rubber band, no further than this,
    // and an empty one barely moves. Let go past this, or flung at this many units a second once
    // past that, it is pulled out; otherwise it springs back in.
    const val MostPull = 150f
    const val EmptyPull = 26f
    const val EmptyPullScale = 40f
    const val SendPull = 62f
    const val FlingSpeed = 700f
    const val FlingPull = 18f
    const val PullStiffness = 600f
    const val PullDamping = 40f

    /** The ratchet clicks each time the paper moves this far. */
    const val RatchetStep = 7f

    // Sent: the sheet zips up out of the roller in this long, and a fresh one feeds in from this
    // far down in this long, as the carriage goes home.
    const val ZipSeconds = 0.17f
    const val FeedInSeconds = 0.48f
    const val FeedIn = 64f

    /** Ctrl or ⌘ and Return pulls the sheet out this far over this long before it lets go. */
    const val AutoPull = 110f
    const val AutoPullSeconds = 0.24f

    /** Full, a sheet takes no more: the hint says so for this long. */
    const val JamSeconds = 2.2f

    // A sent sheet as a slip in the thread: trimmed to its text with this much paper round it, and
    // turned between these many degrees either way. It flies there in this long, arcing this high.
    const val SlipBottom = 8f
    const val SlipLeastTilt = 0.6f
    const val SlipMostTilt = 1.5f
    const val FlightSeconds = 0.56f
    const val FlightArc = 46f
    const val FlightSwing = 6f
    const val FlightGrow = 0.05f

    /** A message's slip waits this long for its place in the thread before it simply appears. */
    const val SlotWaitMs = 600L
}
