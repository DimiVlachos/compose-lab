package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.runtime.Immutable
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

    // Across the stage, a unit is never smaller than this, nor bigger: on a tablet the machine
    // stays a portable's size, in the middle of the stage. On a short stage, as a phone on its
    // side, it shrinks further to fit, down to the shortest.
    val SmallestUnit = 0.85.dp
    val BiggestUnit = 1.3.dp
    val ShortestUnit = 0.5.dp

    /**
     * How big a unit is, how far apart the keys are and how tall a row of them is on a stage
     * [width] wide and [height] tall over the screen's [foot], with a hint [hint] tall over the
     * paper: the machine with a sheet at its first line, the hint and the keys all fit. On a short
     * stage the machine and its keys shrink together, the keys no closer than [LeastKeyPitch] where
     * the width allows; if that is not enough the front lip runs on down into the foot, and then
     * the hint is left out. Given room to spare, a row of keys is made as tall as a thumb's target.
     */
    fun fit(width: Dp, height: Dp = Dp.Infinity, hint: Dp = 0.dp, foot: Dp = 0.dp): MachineFit {
        val unit = (width / StageUnits).coerceIn(SmallestUnit, BiggestUnit)
        val pitch = minOf(MostKeyPitch, (width - KeysSide * 2) / KeysAcross)
        fun at(scale: Float, lip: Dp, hinted: Boolean = true): MachineFit {
            val u = maxOf(unit * scale, ShortestUnit)
            val p = maxOf(pitch * scale, minOf(pitch, LeastKeyPitch))
            val room = if (hinted) hintRoom(u, hint) else u * HintRoom
            return MachineFit(u, p, p * KeyRowHeight, room, lip, hinted)
        }
        if (height == Dp.Infinity || at(1f, FrontLip).height <= height) {
            return grown(at(1f, FrontLip), height)
        }
        val lip =
            if (at(0f, FrontLip).height <= height) FrontLip else FrontLip - minOf(foot, FrontLip)
        val hinted = at(0f, lip).height <= height
        // The biggest scale at which it fits, or the smallest it goes.
        var low = 0f
        var high = 1f
        repeat(FitSteps) {
            val mid = (low + high) / 2f
            if (at(mid, lip, hinted).height <= height) low = mid else high = mid
        }
        return grown(at(low, lip, hinted), height)
    }

    // Room to spare grows the rows of keys towards a thumb's target, the keys themselves no
    // bigger.
    private fun grown(fit: MachineFit, height: Dp): MachineFit {
        if (fit.rowHeight >= KeyTarget) return fit
        val spare = if (height == Dp.Infinity) KeyTarget else (height - fit.height) / KeyRows
        if (spare <= 0.dp) return fit
        val row = minOf(KeyTarget, fit.rowHeight + spare)
        return MachineFit(fit.unit, fit.pitch, row, fit.hintRoom, fit.lip, fit.hinted)
    }

    /** The room over the paper at [unit] for a hint [hint] tall, at least [HintRoom] units. */
    fun hintRoom(unit: Dp, hint: Dp): Dp = maxOf(unit * HintRoom, unit * HintTop + hint + HintGap)

    private const val FitSteps = 14

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

    // Where the type bars strike, a little right of the stage's middle, so the return lever on the
    // carriage's left stays in reach at the end of a line; and down from the machine's top: the
    // current line's foot. The platen's top is just under it; its knobs stand out at its ends.
    const val StrikeX = 208f
    const val StrikeY = 34f
    const val PlatenTop = 38f
    const val PlatenHeight = 30f
    const val PlatenOverhang = 16f
    const val KnobWidth = 20f
    const val KnobHeight = 34f
    const val KnobRise = 2f

    // The body: its deck runs from just under the platen to the keys, as wide as this at its back
    // and widening towards the front, its shoulders this round; its front lip under the keys is
    // this tall, and the keys sit in a well this round at its corners.
    const val BodyTop = 50f
    const val DeckBottom = 134f
    const val DeckWidth = 330f
    const val ShoulderCorner = 34f
    val FrontLip = 22.dp
    val WellCorner = 14.dp
    val ShellSide = 6.dp

    /**
     * How tall the machine is over its keys, from the top of the lever's paddle to its deck's foot.
     */
    const val MachineHeight = DeckBottom

    // Room over the paper for the hint, at least; the hint's top is this far down it, and there
    // is this much room under it.
    const val HintRoom = 34f
    const val HintTop = 6f
    val HintGap = 4.dp

    /** Where the sheet's top is at the first line, from the machine's top. */
    const val SheetTop = StrikeY - 26f

    /** Where the carriage is, the sheet's left edge, with the carriage at the first column. */
    const val HomeX = StrikeX - Margin

    // The type basket: its bars lie in a fan about the printing point, from this far out to this
    // far, between these angles, seen through an opening in the deck this wide and deep.
    const val BasketY = 68f
    const val BasketInner = 18f
    const val BasketOuter = 50f
    const val BasketFrom = 18f
    const val BasketTo = 162f
    const val BasketBars = 34
    const val OpeningWidth = 124f
    const val OpeningDepth = 54f

    // The ribbon spools either side of the basket, and the ribbon's vibrator at the printing point.
    const val SpoolX = 106f
    const val SpoolY = 98f
    const val SpoolRadius = 21f
    const val VibratorY = 71f

    // The margin bell, a brass dome on the deck, and the hammer that taps it.
    const val BellX = StrikeX + 70f
    const val BellY = 122f
    const val BellRadius = 6.5f

    // The paper bail across the sheet over the line, with its scale and its two rollers.
    const val BailY = StrikeY - 19f
    const val BailHeight = 3.5f

    /** A strike shakes the whole machine this far, for this long. */
    val Shudder = 0.8.dp
    const val ShudderSeconds = 0.09f

    // The return lever: it pivots this far left of the carriage, this far down from the machine's
    // top, and points up and to the left; pushed, it swings this much to the right.
    const val LeverOffset = 12f
    const val LeverPivotY = 53f
    const val LeverAngle = -95f
    const val LeverSwing = 24f
    const val LeverLength = 46f

    // Pushed by hand: a finger this far to the right pushes the lever all the way over; the
    // line-space pawl catches, feeding a line, this share of the way, and let go past this share,
    // or flung right faster than this many travels a second, the return goes through. A finger
    // this near the lever, all along it, takes it.
    const val LeverThrow = 56f
    const val LeverPawl = 0.35f
    const val LeverCommit = 0.6f
    const val LeverFling = 3f
    val LeverReach = 24.dp

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
    // Critically damped: let go short, it settles back in from the speed it was let go at
    // without bouncing.
    const val PullDamping = 49f

    /** The ratchet clicks each time the paper moves this far. */
    const val RatchetStep = 7f

    // Sent: the sheet zips up out of the roller from the speed it was let go at, speeding up by
    // this
    // many units a second each second, and a fresh one feeds in from this far down in this long, as
    // the carriage goes home.
    const val ZipAccel = 4_000f
    const val FeedInSeconds = 0.48f
    const val FeedIn = 64f

    /**
     * Ctrl or ⌘ and Return pulls the sheet out this far over this long, speeding up, and lets go at
     * that speed.
     */
    const val AutoPull = 110f
    const val AutoPullSeconds = 0.24f

    /** Full, a sheet takes no more: the hint says so for this long. */
    const val JamSeconds = 2.2f

    // A sent sheet as a slip in the thread: trimmed to its text with this much paper round it, and
    // turned between these many degrees either way. It flies there in this long, carried on from
    // the speed it came free at and drawn in by a critically damped spring this quick (a second's
    // radians), swinging and growing a little on the way.
    const val SlipBottom = 8f
    const val SlipLeastTilt = 0.6f
    const val SlipMostTilt = 1.5f
    const val FlightSeconds = 0.56f
    const val FlightSpring = 13f
    const val FlightSwing = 6f
    const val FlightGrow = 0.05f

    /** A message's slip waits this long for its place in the thread before it simply appears. */
    const val SlotWaitMs = 600L

    // The keys: at most this far apart, and on a short stage no closer than this where the width
    // allows; ten across with this much room at either side, in four rows each this many pitches
    // tall, made as tall as a thumb's target where there is room, with this much room over and
    // under them.
    val MostKeyPitch = 48.dp
    val LeastKeyPitch = 28.dp
    val KeysSide = 6.dp
    const val KeysAcross = 10
    const val KeyRows = 4
    const val KeyRowHeight = 1.12f
    val KeyTarget = 48.dp
    val KeysPadding = 6.dp

    // A key's top is this share of its cell, stands this far up on its lever and goes this far
    // down when pressed; its chrome rim is this wide, and its lever this share of its top's width.
    const val CapShare = 0.8f
    val KeyLift = 1.5.dp
    val KeyTravel = 3.dp
    val RimWidth = 2.dp
    const val StemShare = 0.13f

    // Seen a little from above, a key's round top is squashed to this share as tall as it is
    // wide, its top this share down its cell, and its stem goes into a slot this far down. Each
    // row further back looks this much smaller.
    const val CapSquash = 0.84f
    const val CapRise = 0.03f
    const val SlotAt = 0.98f
    const val TierShrink = 0.035f

    // A key's legend is this share of its pitch tall, smaller for a word than a letter.
    const val LegendShare = 0.36f
    const val WordLegendShare = 0.26f

    // A key goes down in this long, and Delete held goes on after this long, this often.
    const val DipMs = 35
    const val RepeatAfterMs = 450L
    const val RepeatEveryMs = 140L

    /** A screen reader hears the typing once it has paused this long. */
    const val SpeechPauseMs = 600L

    /** How tall the keys are in all, rows [rowHeight] tall. */
    fun keysHeight(rowHeight: Dp): Dp = rowHeight * KeyRows + KeysPadding * 2
}

/**
 * The machine's sizes on its stage: how big a [unit] is, how far apart the keys are ([pitch]), how
 * tall a row of them is ([rowHeight]), the room over the paper for the hint ([hintRoom]), and how
 * much of the front lip under the keys stands over the screen's foot ([lip]); and whether there is
 * room for the hint at all ([hinted]).
 */
@Immutable
internal class MachineFit(
    val unit: Dp,
    val pitch: Dp,
    val rowHeight: Dp,
    val hintRoom: Dp,
    val lip: Dp = TypewriterDimens.FrontLip,
    val hinted: Boolean = true,
) {
    /** How tall the keys are in all. */
    val keysHeight: Dp
        get() = TypewriterDimens.keysHeight(rowHeight)

    /** How tall the composer is with a sheet at its first line, over the screen's foot. */
    val height: Dp
        get() = hintRoom + unit * TypewriterDimens.MachineHeight + keysHeight + lip
}
