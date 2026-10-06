package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.util.lerp
import kotlin.coroutines.CoroutineContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Where a plane leaves from: its middle at [center], nose [heading] degrees clockwise from the
 * right, [length] long from tail to nose; the dart on a send button, as [planeTakeoff] gives it.
 */
@Immutable
public class Takeoff(public val center: Offset, public val heading: Float, public val length: Float)

/** Where the dart drawn by a [PaperPlaneIcon] laid out at [iconBounds] (in the root) lies. */
public fun planeTakeoff(iconBounds: Rect): Takeoff =
    Takeoff(
        iconPlacement(iconBounds).at,
        PaperPlaneDimens.IconHeading,
        iconBounds.width * PaperPlaneDimens.IconLength,
    )

/**
 * Paper planes in the air over a stage, each carrying a message: thrown off the send button, flown
 * up and round, and in low over the message's place, letting its letters go one by one as it passes
 * over each, before it flies on out of the stage. Places are in the root's coordinates.
 */
@Stable
public class PaperPlaneState
internal constructor(
    // Where the folds are worked out: off the main thread, so a throw never stalls a frame.
    private val planning: CoroutineContext = Dispatchers.Default
) {
    internal val flights = mutableStateListOf<Flight>()

    /** Where planes may fly, set by the stage as it is laid out. */
    internal var stage: Rect by mutableStateOf(Rect.Zero)

    /** The density planes are sized by, set where the state is remembered. */
    internal var density = 1f

    private var plan: FoldPlan? = null

    // The keys whose letters all lie in their places: their places stay open, their bubbles grown.
    private val landed = HashSet<Any>()

    /**
     * Folds the plane and makes its paper, off the main thread, so the first throw has nothing to
     * make in the frame it leaves.
     */
    internal suspend fun prepare() {
        withContext(planning) {
            if (plan == null) plan = dartPlan
            dartPlan.lighting
            dartOutline(iconPlacement(Rect(0f, 0f, 1f, 1f)))
            paperGrain
        }
    }

    /**
     * Throws the plane at [takeoff] carrying the text laid out as [text], to drop it letter by
     * letter where it is to lie, its top left at [at] in the root (read again every frame, so a
     * place that moves while the plane is up is still found). Returns once the last letter lies in
     * its place and the plane is gone.
     */
    public suspend fun launch(
        key: Any,
        takeoff: Takeoff,
        text: TextLayoutResult,
        at: () -> Offset,
    ) {
        val boxes = letterBoxes(text)
        if (boxes.isEmpty()) return
        prepare()
        val plan = checkNotNull(plan)
        // Not before the stage is laid out: a plane flies over it, and is sized by it.
        val stage = snapshotFlow { stage }.first { it != Rect.Zero }
        val flight = Flight(key, plan, takeoff, text, at, at(), boxes, stage, density)
        flights += flight
        try {
            flight.clock.animateTo(flight.endMs, tween(flight.endMs.toInt(), easing = LinearEasing))
            landed += key
        } finally {
            flights -= flight
        }
    }

    /**
     * How far the message for [key] has come down, 0 until its first letter lands to 1 once its
     * last lies in its place, and from then on: what its bubble grows in by.
     */
    public fun delivered(key: Any): Float =
        flights.firstOrNull { it.key == key }?.delivered?.value ?: if (key in landed) 1f else 0f

    /**
     * How far the place for [key]'s message has opened, 0 shut to 1 its full height: shut while its
     * letters go into the button and the plane flies round, and opening as it comes down to it, so
     * the conversation makes room just in time and never jumps; open from then on.
     */
    public fun opening(key: Any): Float =
        flights.firstOrNull { it.key == key }?.opening?.value ?: if (key in landed) 1f else 0f
}

/** One plane in the air: its folds, its throw, and the letters it carries. */
@Stable
internal class Flight(
    val key: Any,
    val plan: FoldPlan,
    private val takeoff: Takeoff,
    val text: TextLayoutResult,
    // Where the text's top left lies now, and where it lay when the plane was thrown.
    private val textAt: () -> Offset,
    thrownTo: Offset,
    boxes: List<Rect>,
    stage: Rect,
    private val density: Float,
) {
    val path: FlightPath
    val pace: Pace
    val drops: List<Drop>

    /** Milliseconds since the throw. */
    val clock = Animatable(0f)

    /** What draws this plane: it keeps the dart's shape for as long as it flies. */
    val painter = DartPainter()

    /** What its letters are drawn from. */
    val letters = LetterAtlas(text)

    // The plane's size: tail to nose, the icon's dart's to begin with.
    private val fullScale = PaperPlaneDimens.PlaneLength * density / plan.length
    private val startScale = takeoff.length / plan.length

    /** When the last letter lies in its place, and the plane is long gone. */
    val endMs: Float

    private val firstLands: Float
    private val lastLands: Float

    init {
        val middles = boxes.map { thrownTo.x + it.center.x }
        val first = middles.min()
        val span = middles.max() - first
        // Never so slow over a long message that its last letter would land late.
        val cruise = PaperPlaneDimens.Cruise * density
        val fastest = PaperPlaneDimens.DropMs - PaperPlaneDimens.FallMs
        val sweepSpeed = max(cruise, span / fastest)
        val sweep = max(span, PaperPlaneDimens.MinSweep * density)
        // Let go before it is over its place, each letter is carried on by the plane into it.
        val carry = sweepSpeed * PaperPlaneDimens.FallMs * PaperPlaneDimens.CarryShare
        val y = thrownTo.y + boxes.minOf { it.top } - PaperPlaneDimens.SkimAbove * density
        val sweepFrom = Offset(first - carry, y)
        val length = PaperPlaneDimens.PlaneLength * density
        path =
            flightPath(
                takeoff.center,
                takeoff.heading,
                sweepFrom,
                sweepFrom + Offset(sweep, 0f),
                length * PaperPlaneDimens.ExitBeyond,
                stage,
            )
        val pieces = path.pieces
        pace =
            Pace(
                approach = pieces[0].length + pieces[1].length,
                sweep = pieces[2].length,
                exit = pieces[3].length,
                approachMs = PaperPlaneDimens.ApproachMs,
                sweepSpeed = sweepSpeed,
                launchSpeed = PaperPlaneDimens.ThrowSpeed * density,
                exitSpeed = sweepSpeed * PaperPlaneDimens.ExitBoost,
            )
        drops = boxes.mapIndexed { i, box ->
            val letGo = pace.overAt((thrownTo.x + box.center.x - first) / sweep)
            // Where the plane is as it lets it go, worked out the once.
            Drop(box, letGo, spinFor(i), placement(letGo).at)
        }
        firstLands = drops.minOf { it.letGo } + PaperPlaneDimens.FallMs * PaperPlaneDimens.LandAt
        lastLands = drops.maxOf { it.letGo } + PaperPlaneDimens.FallMs
        endMs = max(pace.totalMs, lastLands)
    }

    /** Whether the plane is still on the stage: it may be gone before its last letter lands. */
    val flying: Boolean
        get() = clock.value < pace.totalMs

    /** Where the text lies now, its top left in the root. */
    fun textOrigin(): Offset = textAt()

    /**
     * How far the message's place has opened, shut till the plane comes down to it; read through
     * this, a place is only laid out again while it is opening, not every frame of the flight.
     */
    val opening: State<Float> = derivedStateOf { openingAt(clock.value) }

    /** How far its letters have come down, from the first landing to the last. */
    val delivered: State<Float> = derivedStateOf { deliveredAt(clock.value) }

    fun openingAt(ms: Float): Float =
        ease((ms - pace.approachMs + PaperPlaneDimens.OpenMs) / PaperPlaneDimens.OpenMs)

    fun deliveredAt(ms: Float): Float =
        ((ms - firstLands) / (lastLands - firstLands)).coerceIn(0f, 1f)

    /**
     * Where the plane is [ms] after the throw: along the throw, growing from the icon it left as
     * and turning from flat to its flying roll, rising off the screen and leaning into its turns,
     * down low over the message and up again as it leaves.
     */
    fun placement(ms: Float = clock.value): Placement {
        val s = pace.distance(ms)
        val t = (s / path.length).coerceIn(0f, 1f)
        val grown = ease(ms / PaperPlaneDimens.GrowMs)
        val scale = lerp(startScale, fullScale, grown)
        // Never quite steady in the air: still on the button, it flutters once it is thrown.
        val seconds = ms / 1000f
        fun wobble(amount: Float, hz: Float, phase: Float) =
            grown * amount * sin(2f * PI.toFloat() * hz * seconds + phase)
        val roll =
            // Its roll runs the other way to its bank: leaning into a turn takes the roll back
            // towards level on the inside of it.
            lerp(PaperPlaneDimens.TakeoffRoll, PaperPlaneDimens.RestRoll, grown) -
                grown * leanAt(ms) +
                wobble(PaperPlaneDimens.WobbleRoll, 1.3f, 0.4f) +
                wobble(PaperPlaneDimens.WobbleRoll * 0.4f, 2.9f, 1.7f)
        val heading = path.heading(t) + wobble(PaperPlaneDimens.WobbleYaw, 0.9f, 1.1f)
        val h = heading.toRadians()
        val across = Offset(-sin(h), cos(h))
        val drift = wobble(PaperPlaneDimens.WobbleDrift * density, 1.7f, 2.3f)
        val lift = liftAt(s) * density
        return Placement(
            path.at(t) + across * drift,
            heading,
            roll,
            scale,
            // Seen close up on the button, as its icon was, the eye that close kept as close to
            // the plane, never the stage, so as it rises it never comes up into it; eased back to
            // its own distance over the stage as it grows.
            lerp(
                lift + PaperPlaneDimens.IconEye * scale * plan.length,
                PaperPlaneDimens.Eye * density,
                grown,
            ),
            lift,
            // Nose up and away as its icon showed it, swung round to the way it is going as soon
            // as it is thrown.
            lerp(PaperPlaneDimens.TakeoffPitch, climbAt(s), ease(ms / PaperPlaneDimens.SwingMs)),
        )
    }

    // How fast it goes along the throw at [ms], in pixels a millisecond.
    private fun speedAt(ms: Float): Float {
        val from = (ms - 1f).coerceAtLeast(0f)
        return (pace.distance(ms + 1f) - pace.distance(from)) / (ms + 1f - from)
    }

    // Its bank at [ms]: a plane takes a moment to roll into a turn and out of it, so it leans as
    // the turn it has been in over the last little while asked it to.
    private fun leanAt(ms: Float): Float {
        val gravity = PaperPlaneDimens.Gravity * density
        var sum = 0f
        for (i in 0 until RollSamples) {
            val then = (ms - PaperPlaneDimens.RollLagMs * i / (RollSamples - 1)).coerceAtLeast(0f)
            val t = (pace.distance(then) / path.length).coerceIn(0f, 1f)
            sum += path.bank(t, speedAt(then), gravity)
        }
        return sum / RollSamples
    }

    // How steeply it climbs off the screen or comes down to it at [s] along the throw, in degrees:
    // its nose goes the way it is going, up towards the eye as it rises, down as it sinks.
    private fun climbAt(s: Float): Float {
        val step = 2f * density
        val rise = (liftAt(s + step) - liftAt((s - step).coerceAtLeast(0f))) * density
        return atan2(rise, s + step - (s - step).coerceAtLeast(0f)).toDegrees()
    }

    // Thrown up off the screen towards the eye, steeply at first, then gliding down the rest of
    // the loop to skim the message, and up a little as it leaves.
    private fun liftAt(s: Float): Float {
        val approach = pace.approach
        if (s < approach) {
            val q = s / approach
            val top = PaperPlaneDimens.ClimbShare
            if (q < top) return PaperPlaneDimens.FlightLift * ease(q / top)
            return lerp(
                PaperPlaneDimens.FlightLift,
                PaperPlaneDimens.SkimLift,
                ease((q - top) / (1f - top)),
            )
        }
        val out = s - approach - pace.sweep
        if (out <= 0f) return PaperPlaneDimens.SkimLift
        return PaperPlaneDimens.SkimLift +
            PaperPlaneDimens.ExitLift * ease(min(1f, out / pace.exit))
    }
}

/**
 * One letter the plane carries: its box in the text, when it is let go, how it tumbles, and where
 * the plane is, in the root, as it lets it go.
 */
internal class Drop(val box: Rect, val letGo: Float, val spin: Float, val from: Offset)

/**
 * A [PaperPlaneState] to throw planes with; draw them with a [PaperPlane]. The dart is folded off
 * the main thread as soon as it is remembered, so the first throw is ready.
 */
@Composable
public fun rememberPaperPlaneState(): PaperPlaneState = rememberPaperPlaneState(Dispatchers.Default)

/** A [PaperPlaneState] whose planes are folded in [planning]: a test folds them in its own time. */
@Composable
internal fun rememberPaperPlaneState(planning: CoroutineContext): PaperPlaneState {
    val state = remember(planning) { PaperPlaneState(planning) }
    state.density = LocalDensity.current.density
    LaunchedEffect(state) { state.prepare() }
    return state
}

// The moments a bank is averaged over, from now back to the roll's lag.
private const val RollSamples = 6
