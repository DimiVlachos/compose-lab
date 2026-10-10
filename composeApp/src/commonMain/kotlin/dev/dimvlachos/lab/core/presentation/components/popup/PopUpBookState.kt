package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

/**
 * Where the book lies on the screen, for gestures: the [camera], the gutter's height on the screen
 * ([gutterY]), how far a finger travels to turn a leaf over ([span]), what counts as a tap
 * ([touchSlop]) and as a touch on a tab ([tabReach]), and each spread's tab, by spread index: where
 * it is on the screen ([tabPoint], null for none) and how far it pulls out ([tabTravel]).
 */
internal class BookLayout(
    val camera: BookCamera,
    val gutterY: Float,
    val span: Float,
    val touchSlop: Float,
    val tabReach: Float,
    val tabPoint: (spread: Int) -> Offset?,
    val tabTravel: (spread: Int) -> Float,
)

/**
 * A pop-up book's leaves, tabs and sails. A leaf turns on a spring to lying open (π) or shut (0),
 * by a drag, a tap, [next] or [previous]; a finger and a script make the same calls.
 */
@Stable
public class PopUpBookState
internal constructor(
    public val spreadCount: Int,
    opened: Int = 0,
) {
    init {
        require(spreadCount >= 1) { "A pop-up book needs a spread" }
    }

    private val pi = PI.toFloat()

    // One turning leaf per spread: the cover, then a leaf between each pair of spreads.
    internal val angles = FloatArray(spreadCount) { if (it < opened) pi else 0f }
    private val speeds = FloatArray(spreadCount)
    private val open = BooleanArray(spreadCount) { it < opened }

    // A turning leaf follows a set point that glides from where the leaf was to where it is going:
    // easing in and out for a turn from rest, easing out for one already moving.
    private val glideFrom = FloatArray(spreadCount)
    private val glideClock = FloatArray(spreadCount)
    private val glideLength = FloatArray(spreadCount)
    private val glideEasesIn = BooleanArray(spreadCount)

    internal val tabTravel = FloatArray(spreadCount)
    private val tabSpeeds = FloatArray(spreadCount)
    private val pulledBefore = FloatArray(spreadCount)
    internal val sailAngle = FloatArray(spreadCount)
    private val sailSpeeds = FloatArray(spreadCount)

    /** Seconds since the book began, for a boat's bob. */
    internal var time: Float = 0f
        private set

    /** Spreads that move on their own while open, such as a bobbing boat. Set by the book. */
    internal var restless: BooleanArray = BooleanArray(spreadCount)
        set(value) {
            field = value
            // A book made again on a spread that moves by itself starts moving at once.
            settle()
        }

    internal var layout: BookLayout? = null

    /** Bumped each frame that moves something: drawing reads it, so it redraws. */
    internal var frame: Int by mutableIntStateOf(0)
        private set

    /** Whether anything moves, so the book needs frames. */
    internal var awake: Boolean by mutableStateOf(false)
        private set

    /** The spread showing: the leaves past halfway, 0 with the cover shut. */
    public var spread: Int by mutableIntStateOf(opened)
        private set

    /** Whether a leaf is moving or held. */
    public var isTurning: Boolean by mutableStateOf(false)
        private set

    /** The spread the book is headed for: the leaves set to lie open. */
    public val destination: Int
        get() = open.count { it }

    private var held = -1
    private var holdAngle = 0f
    private var holdStart = Offset.Zero
    private var holdForward = true
    private var holdMoved = false

    /** Whether the held leaf has been dragged past a tap's slop. */
    internal val isDragMoved: Boolean
        get() = held >= 0 && holdMoved

    private var tabHeld = -1
    private var tabStartTravel = 0f
    private var tabStartX = 0f

    /** Turns the next leaf over, if there is one. */
    public fun next() {
        val j = open.indexOfFirst { !it }
        if (j < 0) return
        open[j] = true
        glide(j)
        wake()
    }

    /** Turns the last leaf turned back, if there is one. */
    public fun previous() {
        val j = open.indexOfLast { it }
        if (j < 0) return
        open[j] = false
        glide(j)
        wake()
    }

    /**
     * A finger down at [position]: below the gutter it takes the next leaf, above it the last one
     * turned. Returns false if there is no such leaf.
     */
    public fun dragStart(position: Offset): Boolean {
        val layout = layout ?: return false
        // One page in hand at a time: a second finger doesn't take it over.
        if (held >= 0) return false
        val forward = position.y > layout.gutterY
        val j = if (forward) open.indexOfFirst { !it } else open.indexOfLast { it }
        if (j < 0) return false
        held = j
        holdAngle = angles[j]
        holdStart = position
        holdForward = forward
        holdMoved = false
        speeds[j] = 0f
        wake()
        return true
    }

    /** The finger holding a page moves to [position]; the page follows it over. */
    public fun dragTo(position: Offset) {
        val layout = layout ?: return
        if (held < 0) return
        if (hypot(position.x - holdStart.x, position.y - holdStart.y) > layout.touchSlop) {
            holdMoved = true
        }
        angles[held] = (holdAngle + (holdStart.y - position.y) / layout.span * pi).coerceIn(0f, pi)
    }

    /**
     * The finger lifts, moving at [velocity]: a tap turns a leaf, a drag lets it finish or fall
     * back.
     */
    public fun dragEnd(velocity: Velocity) {
        val j = held
        if (j < 0) return
        held = -1
        val layout = layout
        if (layout == null) {
            letGo(j, angles[j] > pi / 2f)
            return
        }
        if (!holdMoved) {
            if (holdForward) next() else previous()
            return
        }
        val speed = -velocity.y / layout.span * pi
        speeds[j] = speed
        letGo(j, angles[j] + PopUpDimens.CommitLead * speed > pi / 2f)
    }

    /**
     * The touch was taken away (the system cancelled it, or it was held too long to be a tap): the
     * leaf is let go where it is, to finish or fall back, and nothing is tapped.
     */
    public fun dragCancel() {
        val j = held
        if (j < 0) return
        held = -1
        letGo(j, angles[j] > pi / 2f)
    }

    // Leaf [j] is let go to lie open or shut. Leaves lie open from the cover on, so if Next or
    // Back was pressed while it was held, the leaves before it open with it, or those after it
    // shut with it, and the book can always come to rest.
    private fun letGo(j: Int, opens: Boolean) {
        val range = if (opens) 0..j else j until spreadCount
        for (k in range) {
            if (open[k] == opens && k != j) continue
            open[k] = opens
            glide(k)
        }
        wake()
    }

    /** A finger down at [position] on the open spread's tab, if it has one and is open enough. */
    public fun tabStart(position: Offset): Boolean {
        val layout = layout ?: return false
        val s = spread - 1
        if (s < 0) return false
        val point = layout.tabPoint(s) ?: return false
        if (opening(s) < PopUpDimens.TabActiveFrom) return false
        if (hypot(position.x - point.x, position.y - point.y) > layout.tabReach) return false
        tabHeld = s
        tabStartTravel = tabTravel[s]
        tabStartX = position.x
        pulledBefore[s] = tabTravel[s]
        tabSpeeds[s] = 0f
        wake()
        return true
    }

    /** The finger holding the tab moves to [position]; the tab slides out or in with it. */
    public fun tabTo(position: Offset) {
        val layout = layout ?: return
        val s = tabHeld
        if (s < 0) return
        val travel = tabStartTravel + (position.x - tabStartX) / layout.camera.pxPerUnit
        tabTravel[s] = travel.coerceIn(0f, layout.tabTravel(s))
    }

    /** The finger lets go of the tab, and a spring draws it back in. */
    public fun tabEnd() {
        tabHeld = -1
        wake()
    }

    /** Moves everything on by [seconds], in fixed steps. */
    internal fun advance(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val dt = minOf(PopUpDimens.Step, left)
            step(dt)
            left -= dt
        }
        frame++
        settle()
    }

    // Leaf [j] sets off from where it is towards where it is now headed.
    private fun glide(j: Int) {
        val target = if (open[j]) pi else 0f
        glideFrom[j] = angles[j]
        glideClock[j] = 0f
        glideLength[j] =
            PopUpDimens.TurnSeconds * (abs(target - angles[j]) / pi).coerceAtLeast(MinGlide)
        // A leaf at rest is lifted gently; one already moving keeps its way and eases down.
        glideEasesIn[j] = abs(speeds[j]) < MovingSpeed
    }

    // Where leaf [j]'s set point is now.
    private fun setPoint(j: Int, target: Float): Float {
        val length = glideLength[j]
        if (length <= 0f || glideClock[j] >= length) return target
        val x = glideClock[j] / length
        val eased =
            if (glideEasesIn[j]) x * x * x * (x * (6f * x - 15f) + 10f)
            else 1f - (1f - x) * (1f - x) * (1f - x)
        return glideFrom[j] + (target - glideFrom[j]) * eased
    }

    private fun step(dt: Float) {
        time += dt
        val damping = PopUpDimens.LeafDamping
        for (j in 0 until spreadCount) {
            if (j == held) continue
            val target = if (open[j]) pi else 0f
            glideClock[j] += dt
            val follow = setPoint(j, target)
            speeds[j] +=
                (PopUpDimens.LeafStiffness * (follow - angles[j]) - damping * speeds[j]) * dt
            angles[j] += speeds[j] * dt
            if (angles[j] < 0f) {
                angles[j] = 0f
                speeds[j] = -speeds[j] * PopUpDimens.Restitution
            } else if (angles[j] > pi) {
                angles[j] = pi
                speeds[j] = -speeds[j] * PopUpDimens.Restitution
            }
        }
        // A leaf lies under the one before it: they never pass through each other. A held leaf
        // pushes every leaf above it up ahead of it, then each leaf caps the ones below.
        if (held >= 0) {
            for (k in held - 1 downTo 0) {
                if (angles[k] >= angles[k + 1]) break
                angles[k] = angles[k + 1]
                speeds[k] = maxOf(speeds[k], 0f)
            }
        }
        for (j in 1 until spreadCount) {
            if (angles[j] <= angles[j - 1]) continue
            angles[j] = angles[j - 1]
            speeds[j] = minOf(speeds[j], speeds[j - 1])
        }
        for (s in 0 until spreadCount) {
            if (s == tabHeld) {
                val pulled = tabTravel[s] - pulledBefore[s]
                if (pulled > 0f) sailSpeeds[s] += PopUpDimens.SailGain * pulled
                pulledBefore[s] = tabTravel[s]
            } else if (tabTravel[s] > 0f || tabSpeeds[s] != 0f) {
                tabSpeeds[s] +=
                    (-PopUpDimens.TabStiffness * tabTravel[s] -
                        PopUpDimens.TabDamping * tabSpeeds[s]) * dt
                tabTravel[s] += tabSpeeds[s] * dt
                if (tabTravel[s] <= 0f) {
                    tabTravel[s] = 0f
                    tabSpeeds[s] = 0f
                }
            }
            if (sailSpeeds[s] != 0f) {
                sailSpeeds[s] *= exp(-PopUpDimens.SailFriction * dt)
                sailAngle[s] += sailSpeeds[s] * dt
                if (abs(sailSpeeds[s]) < SailRest) sailSpeeds[s] = 0f
            }
        }
    }

    // How far spread [s] is open: the angle between its two pages.
    private fun opening(s: Int): Float =
        angles[s] - (if (s + 1 < spreadCount) angles[s + 1] else 0f)

    private fun settle() {
        var turning = held >= 0
        var shown = 0
        for (j in 0 until spreadCount) {
            val target = if (open[j]) pi else 0f
            val gliding = glideClock[j] < glideLength[j]
            if (
                j != held &&
                    !gliding &&
                    abs(angles[j] - target) < LeafRest &&
                    abs(speeds[j]) < LeafRest
            ) {
                angles[j] = target
                speeds[j] = 0f
            } else {
                turning = true
            }
            if (angles[j] > pi / 2f) shown++
        }
        if (spread != shown) spread = shown
        if (isTurning != turning) isTurning = turning
        var moving = turning || tabHeld >= 0
        for (s in 0 until spreadCount) {
            if (tabTravel[s] > 0f || sailSpeeds[s] != 0f) moving = true
        }
        val showing = spread - 1
        if (showing >= 0 && showing < restless.size && restless[showing]) moving = true
        if (awake != moving) awake = moving
    }

    private fun wake() {
        if (!awake) awake = true
    }

    internal companion object {
        private const val LeafRest = 1e-3f

        // A glide over a short way still takes this share of a full turn's time.
        private const val MinGlide = 0.3f

        // Faster than this, in radians a second, a leaf is already on its way.
        private const val MovingSpeed = 0.5f
        private const val SailRest = 0.02f

        fun saver(): Saver<PopUpBookState, Any> =
            Saver(
                save = { listOf(it.spreadCount, it.destination) },
                restore = {
                    val (count, opened) = it as List<*>
                    PopUpBookState(count as Int, opened as Int)
                },
            )
    }
}

/**
 * A pop-up book's state with [spreadCount] spreads, kept across recreation, the book shut at first.
 */
@Composable
public fun rememberPopUpBookState(spreadCount: Int): PopUpBookState =
    rememberSaveable(spreadCount, saver = PopUpBookState.saver()) { PopUpBookState(spreadCount) }
