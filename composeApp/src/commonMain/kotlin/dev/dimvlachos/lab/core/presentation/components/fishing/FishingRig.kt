package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Biting
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Casting
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Idle
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Pulling
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Reeling
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Rising
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Snapped
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Waiting
import dev.dimvlachos.lab.core.presentation.components.physics.VerletRope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Where a refresh is on the water. */
internal enum class FishingPhase {
    /** Nothing under way: the line dangles from the tip, out of sight unless pulled. */
    Idle,
    /** The list is being pulled: the rod bends and the line tightens. */
    Pulling,
    /** Let go past the threshold: the bobber flies out to the water. */
    Casting,
    /** The bobber floats, bobbing, until an outcome lands and it has floated long enough. */
    Waiting,
    /** A catch: the bobber is pulled under. */
    Biting,
    /** The line comes back to the tip: with a catch, an empty hook, or nothing asked for. */
    Reeling,
    /** A catch's cards rise out of the water. */
    Rising,
    /** A failure: the line has snapped and the bobber drifts off. */
    Snapped,
}

/**
 * A rod, its line and a bobber over a band of water [width] dp wide, as a refresh plays out on it:
 * [pullTo] as the list is pulled, [cast] as a refresh starts, [land] with its outcome and [cancel]
 * if it is called off. [advance] steps it all on at a fixed rate. Every position is in dp, from the
 * band's top left corner, and nothing here knows about Compose's state: it is stepped and read by
 * [FishingRefreshState].
 *
 * The hooks hear what a person would: [onThreshold] as a pull first reaches the threshold,
 * [onSplash] as the bobber lands, [onBite] as a catch bites, [onSnap] as the line snaps, and
 * [onLanded] as an outcome starts to play.
 */
internal class FishingRig(width: Float) {
    var onThreshold: () -> Unit = {}
    var onSplash: () -> Unit = {}
    var onBite: () -> Unit = {}
    var onSnap: () -> Unit = {}
    var onLanded: (FishingOutcome) -> Unit = {}

    var phase = Idle
        private set

    // How wide the band is, and how long the line can be at most: as long as the band is wide.
    private var width = width
    private var longest = width

    val water = WaterSurface(width = width)

    var line = newLine()
        private set

    /** How far the list is pulled, in thresholds: 1 at the threshold, at most 2. */
    var pull = 0f
        private set

    // Whether this pull has ticked at the threshold yet, and whether the band is still closing
    // after an outcome: then a falling pull isn't a new pull, and doesn't load the rod.
    private var ticked = false
    private var closing = false

    /** How far the rod is bent, in thresholds of pull, and how fast that is changing. */
    var bend = 0f
        private set

    private var bendSpeed = 0f

    /** Where the bobber is: on the line's end, unless the line has snapped. */
    var bobber = Offset.Zero
        private set

    /** How much of the bobber shows: it fades out as it drifts off on a snapped line. */
    var bobberAlpha = 1f
        private set

    // Whether the line has snapped, so its end hangs free until the next cast.
    private var broken = false

    // How long the current phase has gone on, and how long the bobber has floated.
    private var t = 0f
    private var floated = 0f
    private var bobs = 0

    // Where the bobber lands, where a phase moved it from, and how long the line was then.
    private var castTo = Offset.Zero
    private var from = Offset.Zero
    private var fromStretch = 1f

    // The outcome waiting to play, once the bobber has floated long enough.
    private var landing: Landing = Landing.None

    /** How many cards the latest catch brought: these rise out of the water. */
    var catchCount = 0
        private set

    // Whether the latest catch has finished rising.
    private var risen = true

    // Time not yet stepped: the rig steps at a fixed rate, whatever the frame rate.
    private var owed = 0f

    init {
        line.hang(tip)
        bobber = dangle()
    }

    /** Spreads the band over [width] dp instead: the line is hung afresh, the water keeps on. */
    fun resize(width: Float) {
        if (width == this.width || width <= 0f) return
        this.width = width
        longest = width
        water.resize(width)
        line = newLine().apply { hang(tip) }
        if (phase == Waiting) castTo = Offset(width * FishingDimens.CastAt, waterLevel)
    }

    private fun newLine() =
        VerletRope(
            points = FishingDimens.LinePoints,
            segment = longest / (FishingDimens.LinePoints - 1),
            gravity = FishingDimens.LineGravity,
            damping = FishingDimens.LineDamping,
            passes = FishingDimens.LinePasses,
            stillStep = FishingDimens.LineStill,
        )

    /** Whether a refresh is playing out: the band stays open, and a new pull isn't taken. */
    val busy: Boolean
        get() = phase != Idle && phase != Pulling

    /** Whether everything has come to rest, out of sight: the frame loop can sleep. */
    val atRest: Boolean
        get() =
            phase == Idle &&
                pull == 0f &&
                water.still &&
                line.still &&
                abs(bend) < FishingDimens.StraightBend &&
                abs(bendSpeed) < FishingDimens.StraightSpeed

    /** Where the rod's tip is, bent as it is. */
    val tip: Offset
        get() = tipAt(bend)

    /** Where the rod's butt is: it never moves. */
    val butt: Offset
        get() = Offset(FishingDimens.RodButtX.value, FishingDimens.RodButtY.value)

    private fun tipAt(bend: Float): Offset {
        val reach = FishingDimens.RodLength.value
        return butt +
            Offset(
                reach * cos(FishingDimens.RodRise) - FishingDimens.RodBack.value * bend,
                -reach * sin(FishingDimens.RodRise) + FishingDimens.RodDip.value * bend,
            )
    }

    private val waterLevel: Float
        get() = FishingDimens.WaterLevel.value

    /**
     * The list is pulled [fraction] thresholds down. A new pull bends the rod and tightens the
     * line; the first time a pull reaches the threshold, [onThreshold] hears it. While a refresh
     * plays out, or the band closes after one, it is only remembered.
     */
    fun pullTo(fraction: Float) {
        val to = fraction.coerceIn(0f, FishingDimens.MostBend)
        pull = to
        if (to == 0f) {
            closing = false
            ticked = false
            if (phase == Pulling) phase = Idle
            return
        }
        if (busy || closing) return
        if (phase == Idle) {
            phase = Pulling
            broken = false
            bobberAlpha = 1f
        }
        if (to >= 1f && !ticked) {
            ticked = true
            onThreshold()
        }
    }

    /** A refresh starts: the line is cast from wherever the bobber is, out onto the water. */
    fun cast() {
        if (busy) return
        startOver()
        from = bobber
        castTo = Offset(width * FishingDimens.CastAt, waterLevel)
        bendSpeed += FishingDimens.CastFlick
        enter(Casting)
    }

    /**
     * A refresh already under way when the rig is made, as after a rotation: the bobber floats on
     * the water at once, with no cast and no splash.
     */
    fun floatAtOnce() {
        startOver()
        castTo = Offset(width * FishingDimens.CastAt, waterLevel)
        bobber = castTo
        line.hold(bobber, lineFor(bobber))
        enter(Waiting)
    }

    private fun startOver() {
        broken = false
        bobberAlpha = 1f
        closing = false
        ticked = true
        landing = Landing.None
        catchCount = 0
        risen = true
        floated = 0f
        bobs = 0
    }

    /**
     * The refresh came out as [outcome]: it plays once the bobber has floated long enough. Only one
     * outcome is taken a refresh, and none when no refresh is under way.
     */
    fun land(outcome: FishingOutcome) {
        if (phase != Casting && phase != Waiting) return
        if (landing != Landing.None) return
        landing = Landing.Outcome(outcome)
        if (outcome is FishingOutcome.Caught) {
            catchCount = outcome.count
            risen = false
        }
    }

    /** The refresh was called off: the line is reeled in, with no outcome to tell. */
    fun cancel() {
        if (phase != Casting && phase != Waiting) return
        landing = Landing.None
        reelIn()
    }

    /**
     * How far card [index] of the list, from the top, has risen out of the water, 0 under it to 1
     * in its place. Only the latest catch's cards rise: they stay under from the moment it lands
     * until it is reeled in, then rise one after another. Every other card is in its place.
     */
    fun riseFor(index: Int): Float {
        if (index < 0 || index >= catchCount) return 1f
        return when {
            phase == Rising ->
                ((t - index * FishingDimens.RiseStagger) / FishingDimens.RiseSeconds).coerceIn(
                    0f,
                    1f,
                )
            risen -> 1f
            else -> 0f
        }
    }

    /** Steps it all on by [seconds]: at a fixed rate, however long the frame was. */
    fun advance(seconds: Float) {
        owed += seconds.coerceIn(0f, FishingDimens.MostFrameSeconds)
        while (owed >= FishingDimens.StepSeconds) {
            step(FishingDimens.StepSeconds)
            owed -= FishingDimens.StepSeconds
        }
    }

    private fun step(dt: Float) {
        t += dt
        when (phase) {
            Idle,
            Pulling -> holdStill(dt)
            Casting -> flyOut()
            Waiting -> float(dt)
            Biting -> bite()
            Reeling -> reel()
            Rising -> rise()
            Snapped -> drift(dt)
        }
        if (phase != Pulling) springRod(dt)
        line.pin(tip)
        if (!broken) line.hold(bobber, lineStretch())
        line.step(dt)
        water.step(dt)
    }

    // Pulled, the rod is bent by the pull itself and the bobber is drawn back and down below its
    // tip; otherwise the rod springs straight and the bobber dangles.
    private fun holdStill(dt: Float) {
        if (phase == Pulling) {
            bend = pull
            bendSpeed = 0f
        }
        if (!broken) bobber = dangle()
    }

    private fun dangle(): Offset {
        val drawn = if (phase == Pulling) pull.coerceAtMost(1f) else 0f
        val hang = FishingDimens.DangleLength.value * 0.9f
        return tip +
            Offset(
                -FishingDimens.DangleBack.value * drawn,
                hang + (FishingDimens.DangleDrop.value - hang) * drawn,
            )
    }

    private fun flyOut() {
        val p = (t / FishingDimens.CastSeconds).coerceAtMost(1f)
        val arc = 4f * p * (1f - p) * FishingDimens.ArcHeight.value
        bobber = from + (castTo - from) * p - Offset(0f, arc)
        if (p >= 1f) {
            bobber = castTo
            water.disturb(castTo.x, FishingDimens.SplashPush, FishingDimens.SplashSpread.value)
            onSplash()
            enter(Waiting)
        }
    }

    private fun float(dt: Float) {
        floated += dt
        val bob = sin(2f * PI.toFloat() * FishingDimens.BobHz * floated)
        bobber = floating(bob * FishingDimens.BobDepth.value)
        // A ripple each time it bobs down, sent out at the bottom of each bob.
        val down = floor(floated * FishingDimens.BobHz + 0.75f).toInt()
        if (down > bobs) {
            bobs = down
            water.disturb(castTo.x, FishingDimens.BobPush, FishingDimens.BobSpread.value)
        }
        val outcome = (landing as? Landing.Outcome)?.outcome ?: return
        if (floated < FishingDimens.MinWaitSeconds) return
        onLanded(outcome)
        when (outcome) {
            is FishingOutcome.Caught -> {
                water.disturb(castTo.x, FishingDimens.BitePush, FishingDimens.BiteSpread.value)
                bendSpeed += FishingDimens.BiteJerk
                onBite()
                enter(Biting)
            }
            FishingOutcome.NothingNew -> reelIn()
            is FishingOutcome.Failed -> {
                broken = true
                line.letGo()
                bendSpeed += FishingDimens.SnapFlick
                from = bobber
                onSnap()
                enter(Snapped)
            }
        }
    }

    private fun floating(dip: Float): Offset =
        Offset(castTo.x, waterLevel + water.heightAt(castTo.x) * FishingDimens.RideSwell + dip)

    private fun bite() {
        val p = (t / FishingDimens.BiteSeconds).coerceAtMost(1f)
        bobber = floating(sin(PI.toFloat() * p) * FishingDimens.BiteDepth.value)
        if (p >= 1f) reelIn()
    }

    private fun reelIn() {
        from = bobber
        fromStretch = lineFor(bobber)
        if (bobber.y >= waterLevel - 1f) {
            water.disturb(bobber.x, FishingDimens.BobPush, FishingDimens.BobSpread.value)
        }
        enter(Reeling)
    }

    private fun reel() {
        val p = (t / FishingDimens.ReelSeconds).coerceAtMost(1f)
        // Quick out of the water, slowing as it comes up to the tip.
        val eased = 1f - (1f - p) * (1f - p) * (1f - p)
        val to = dangle()
        bobber = from + (to - from) * eased
        if (p < 1f) return
        if (landing is Landing.Outcome && catchCount > 0 && !risen) {
            enter(Rising)
        } else {
            finish()
        }
    }

    private fun rise() {
        bobber = dangle()
        val lasts = FishingDimens.RiseSeconds + (catchCount - 1) * FishingDimens.RiseStagger
        if (t < lasts) return
        risen = true
        finish()
    }

    private fun drift(dt: Float) {
        val p = (t / FishingDimens.DriftSeconds).coerceAtMost(1f)
        val x = from.x + FishingDimens.DriftSpeed * t
        bobber = Offset(x, waterLevel + water.heightAt(x) * FishingDimens.RideSwell)
        bobberAlpha = 1f - p
        if (p >= 1f) finish()
    }

    private fun finish() {
        landing = Landing.None
        closing = pull > 0f
        enter(Idle)
    }

    private fun enter(next: FishingPhase) {
        phase = next
        t = 0f
    }

    // Out of the pull, the rod springs back straight, and swings about it a while.
    private fun springRod(dt: Float) {
        val omega = 2f * PI.toFloat() * FishingDimens.RodHz
        bendSpeed +=
            (-omega * omega * bend - 2f * FishingDimens.RodDamping * omega * bendSpeed) * dt
        bend += bendSpeed * dt
    }

    // How long the line is, as a share of the longest it can be: short while it dangles, paid out
    // to the bobber while it is out, and coming back in as it is reeled.
    private fun lineStretch(): Float =
        when (phase) {
            Idle,
            Pulling,
            Rising -> FishingDimens.DangleLength.value / longest
            Reeling -> {
                val p = (t / FishingDimens.ReelSeconds).coerceAtMost(1f)
                val short = FishingDimens.DangleLength.value / longest
                fromStretch + (short - fromStretch) * p
            }
            else -> lineFor(bobber)
        }

    private fun lineFor(end: Offset): Float =
        ((end - tip).getDistance() * FishingDimens.LineSlack / longest).coerceIn(
            FishingDimens.DangleLength.value / longest,
            1f,
        )

    private sealed interface Landing {
        data object None : Landing

        data class Outcome(val outcome: FishingOutcome) : Landing
    }
}
