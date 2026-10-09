package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * The water, the rod and the line of a [FishingRefresh], and how far its list is pulled. Make one
 * with [rememberFishingRefreshState] and hand it to both the [FishingRefresh] and the
 * [risingFromWater] items of its list. Only whether a refresh is playing out, and the last outcome
 * to announce, are read in composition: the line, the water and the rise are read while laying out
 * and drawing, so nothing recomposes while they move.
 */
@Stable
public class FishingRefreshState internal constructor() {
    internal val rig = FishingRig(width = 360f)

    /** How far the list is pulled, as Material's pull-to-refresh tracks it. */
    internal val pull: PullToRefreshState = PullToRefreshState()

    // The density the band is laid out at: the rig works in dp, drawing in px.
    internal var density = 1f
        private set

    /** Bumped every step of the rig, so whatever draws it is drawn again. */
    internal var frame by mutableIntStateOf(0)
        private set

    /**
     * Bumped only when a catch's rise changes, so the list's items are laid out again as they rise,
     * and not on every frame of the line.
     */
    private var rising by mutableIntStateOf(0)

    /** Whether the frame loop is running: while anything moves, or the band shows. */
    internal var awake by mutableStateOf(false)

    /** Whether a refresh is playing out on the water: the band stays open, a pull isn't taken. */
    internal var busy by mutableStateOf(false)
        private set

    /** The last outcome that started to play, to announce; [FishingStatus.Idle] until one has. */
    internal var announced: FishingStatus by mutableStateOf(FishingStatus.Idle)
        private set

    /**
     * The list's items that can be caught, by their place in the list, each with a recording of its
     * drawing: the ones on a catch's line are drawn hanging from the hook from these.
     */
    internal val caught = mutableMapOf<Int, CaughtCard>()

    /** Where the fishing refresh is laid out: items' places are found against it. */
    internal var box: LayoutCoordinates? = null

    // What the rig heard, passed on to haptics.
    internal var onThreshold: () -> Unit = {}
    internal var onBite: () -> Unit = {}
    internal var onSnap: () -> Unit = {}

    // The status as it was last followed; null until the first one is seen.
    private var followed: FishingStatus? = null

    // Whether the refresh under way has had its line cast, and the landing last played, or seen
    // already when the state was made: a landing is told apart from the last by identity, so each
    // refresh's own Landed plays, even with the same outcome as the one before.
    private var castFor = false
    private var played: FishingStatus? = null

    init {
        rig.onThreshold = { onThreshold() }
        rig.onBite = { onBite() }
        rig.onSnap = { onSnap() }
        rig.onLanded = { announced = FishingStatus.Landed(it) }
    }

    /**
     * Follows the caller's [status]: a refresh starting casts the line, one landing plays its
     * outcome, and one called off reels the line in. The first status seen is where things stand: a
     * refresh already under way floats the bobber at once, and an outcome already landed has been
     * seen before, as when the screen is made again, so it doesn't play a second time.
     *
     * Nothing the caller says is lost. A refresh that lands within a frame of starting, so that
     * only its landing is ever seen, is still cast and played; and a refresh started while the last
     * one still plays out is cast as soon as it has, and its outcome played after that.
     */
    internal fun follow(status: FishingStatus) {
        if (status === followed) return
        val was = followed
        followed = status
        if (was == null) {
            when (status) {
                FishingStatus.Refreshing -> {
                    rig.floatAtOnce()
                    castFor = true
                }
                is FishingStatus.Landed -> played = status
                FishingStatus.Idle -> Unit
            }
        } else if (status == FishingStatus.Refreshing && was != FishingStatus.Refreshing) {
            castFor = false
        } else if (status == FishingStatus.Idle && was == FishingStatus.Refreshing && castFor) {
            rig.cancel()
            castFor = false
        }
        catchUp()
        busy = rig.busy
        awake = true
    }

    // Does whatever the status still asks for that the rig can take now: casts a refresh that
    // hasn't been, and lands an outcome that hasn't played, casting first for one that came in
    // without its refresh ever being seen. What the rig can't take yet waits for the next frame.
    private fun catchUp() {
        when (val status = followed) {
            FishingStatus.Refreshing -> if (!castFor && !rig.busy) cast()
            is FishingStatus.Landed -> {
                if (status === played) return
                if (!castFor && !rig.busy) cast()
                if (castFor && rig.canLand) {
                    rig.land(status.outcome)
                    played = status
                    castFor = false
                    rising++
                }
            }
            else -> Unit
        }
    }

    private fun cast() {
        rig.cast()
        castFor = true
        // The last outcome is old news: the next one, even the same, is announced afresh.
        announced = FishingStatus.Idle
        rising++
    }

    /** Lays the band out [width] px wide at [density]. */
    internal fun place(width: Int, density: Float) {
        this.density = density
        rig.resize(width / density)
        frame++
    }

    /** Steps the rig on by [seconds]; at rest, out of sight, the frame loop sleeps. */
    internal fun advance(seconds: Float) {
        rig.pullTo(pull.distanceFraction)
        val wasRising = rig.phase == FishingPhase.Rising || rig.carrying
        rig.advance(seconds)
        catchUp()
        frame++
        if (wasRising || rig.phase == FishingPhase.Rising || rig.carrying) rising++
        busy = rig.busy
        if (rig.atRest && pull.distanceFraction == 0f) awake = false
    }

    /** How far the band is open, 0 shut to 1 fully open. Read in layout or drawing. */
    internal val open: Float
        get() = pull.distanceFraction.coerceIn(0f, 1f)

    /** How far list item [index] has risen out of the water, 0 to 1. Read in layout or drawing. */
    internal fun riseFor(index: Int): Float {
        rising
        return rig.riseFor(index)
    }

    /**
     * Whether list item [index] is on a catch's line, not yet in its place: drawn on the hook by
     * the fishing refresh, not in the list. Read in layout or drawing.
     */
    internal fun hooked(index: Int): Boolean = riseFor(index) < 1f

    /** Where [at], a point of the rig in dp, is in px. Read in drawing, after [frame]. */
    internal fun px(at: Offset): Offset = at * density
}

/**
 * A [FishingRefreshState] for a [FishingRefresh] and its list's [risingFromWater] items.
 *
 * Nothing in it is saved: the status that says where a refresh stands is the caller's, and it
 * outlives the screen with the caller. A state made again mid-refresh, as on a rotation, reads it:
 * the bobber floats at once rather than being cast again, and an outcome that already landed isn't
 * played, or announced, a second time.
 */
@Composable
public fun rememberFishingRefreshState(): FishingRefreshState = remember { FishingRefreshState() }
