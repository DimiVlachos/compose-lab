package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A pull-cord lamp and the screen it lights: [lit] or not. A finger takes the bead with [grab],
 * pulls it with [dragTo] and lets go with [release], each at a point in the lamp's own pixels, as a
 * pointer reports it; pulled far enough down, the cord clicks and the lamp switches, once a pull.
 * [toggle] switches it without the cord, and [snapTo] sets it without a switch at all. Only [lit],
 * and whether a new look is still spreading, are read in composition: the cord, the shade and the
 * light are read while drawing, so a swinging cord never recomposes.
 */
@Stable
public class PullCordState internal constructor(lit: Boolean, private val scope: CoroutineScope) {
    /** Whether the lamp is on, and content is shown lit. */
    public var lit: Boolean by mutableStateOf(lit)
        private set

    internal val rig = LampRig(Offset.Zero) { toggle() }

    // The density the lamp is laid out at: the rig works in dp, a pointer in px.
    internal var density = 1f
        private set

    /** Bumped every step of the cord, so whatever draws it is drawn again. */
    internal var frame by mutableIntStateOf(0)
        private set

    /** Bumped each time the lamp is hung somewhere new, so whatever is placed by it moves. */
    private var hung by mutableIntStateOf(0)

    /** Whether the cord or the shade is moving, or held: the frame loop runs while it is. */
    internal var awake by mutableStateOf(false)
        private set

    /** Whether the new look is still spreading over the old one: both are on the screen. */
    internal var revealing by mutableStateOf(false)
        private set

    /** How far the new look has spread, 0 at the bulb to 1 over the whole screen. */
    internal val reveal = Animatable(1f)

    /**
     * Which look is on top, clipped to the circle: the new one as it spreads, or, switched back
     * while it spread, the one going as its circle shrinks back into the bulb.
     */
    internal var revealTop by mutableStateOf(lit)
        private set

    /** Where the new look spreads from: where the bulb was when it switched, in px. */
    internal var revealFrom = Offset.Zero
        private set

    /** How bright the bulb is, 0 off to 1 on: it warms up smoothly as it comes on. */
    internal val brightness = Animatable(if (lit) 1f else 0f)

    // How bright the bulb was as it was switched off: the light it throws fades from there.
    private var offFrom = 1f

    /**
     * How bright the light the lamp throws is, 0 to 1: the bulb's brightness, but going out, it
     * fades with the lit look, by the share of it the other look has yet to cover, rather than
     * going out before the look it falls on has gone. Read in a layer or drawing.
     */
    internal val glow: Float
        get() {
            if (lit || !revealing) return brightness.value
            // The lit look is the one going: under the day look as that spreads, or on top as its
            // own circle shrinks back into the bulb.
            val left = if (revealTop) reveal.value else 1f - reveal.value
            return offFrom * left
        }

    /** Called once the lamp has switched, with whether it is now lit: a tick of haptics, say. */
    internal var onSwitch: ((Boolean) -> Unit)? = null

    private var revealJob: Job? = null
    private var brightnessJob: Job? = null

    /**
     * Takes the bead if [at] is on it, and says whether it did. Refuses while a finger already has
     * it.
     */
    public fun grab(at: Offset): Boolean {
        val taken = rig.grab(at / density)
        if (taken) awake = true
        return taken
    }

    /** Moves a held bead's finger to [at]; past the click, the lamp switches. */
    public fun dragTo(at: Offset) {
        if (!rig.held) return
        rig.dragTo(at / density)
        awake = true
    }

    /**
     * Lets go of the bead: the cord springs back up and sways. Does nothing if no finger has it.
     */
    public fun release() {
        rig.release()
    }

    /**
     * Switches the lamp: the new look spreads out from the bulb over the old. Switched again while
     * it spreads, its circle turns round from where it got to. Each switch plays a tick of haptics
     * and then calls the lamp's onSwitch, as a pull's click does.
     */
    public fun toggle() {
        lit = !lit
        revealJob?.cancel()
        // Undispatched, so the circle is at its start before the next frame draws either look.
        if (revealing) {
            // Switched again mid-spread: the circle turns round from where it got to. Back to
            // the look under it, it shrinks into the bulb; back to its own, it spreads on. Either
            // way, nothing jumps.
            val to = if (revealTop == lit) 1f else 0f
            revealJob =
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    reveal.animateTo(
                        to,
                        tween(
                            (PullCordDimens.RevealMs * abs(to - reveal.value)).toInt(),
                            easing = FastOutSlowInEasing,
                        ),
                    )
                    revealing = false
                }
        } else {
            revealFrom = bulb()
            revealTop = lit
            revealing = true
            revealJob =
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    reveal.snapTo(0f)
                    reveal.animateTo(
                        1f,
                        tween(PullCordDimens.RevealMs, easing = FastOutSlowInEasing),
                    )
                    revealing = false
                }
        }
        if (!lit) offFrom = brightness.value
        brightnessJob?.cancel()
        brightnessJob = scope.launch {
            if (lit) {
                // On with a short, smooth warm-up, from however bright it still is: no flash, and
                // switched back on as it goes out, it doesn't drop to dark first.
                brightness.animateTo(
                    1f,
                    tween(PullCordDimens.WarmUpMs, easing = FastOutSlowInEasing),
                )
            } else {
                brightness.animateTo(0f, tween(PullCordDimens.OffMs))
            }
        }
        // Last, so whatever it does sees the switch complete.
        onSwitch?.invoke(lit)
    }

    /**
     * Sets the lamp [lit] or not at once: no spread, no warm-up, no haptics and no onSwitch. For a
     * lamp put back as it was, or a state set from elsewhere.
     */
    public fun snapTo(lit: Boolean) {
        revealJob?.cancel()
        brightnessJob?.cancel()
        this.lit = lit
        revealTop = lit
        revealing = false
        revealJob =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                reveal.snapTo(1f)
                brightness.snapTo(if (lit) 1f else 0f)
            }
    }

    // How wide the stage is, in px, and where across it the lamp hangs, as a share of its width.
    private var width = 0
    private var across = 0.5f

    /** Lays the lamp out on a stage [width] px wide at [density]. */
    internal fun place(width: Int, density: Float) {
        this.width = width
        this.density = density
        hang()
    }

    /**
     * Hangs the lamp [across] the stage's width instead, as a share of it, kept to the stage; a
     * share that is no number at all is ignored.
     */
    internal fun hangAcross(across: Float) {
        if (!across.isFinite()) return
        val share = across.coerceIn(0f, 1f)
        if (share == this.across) return
        this.across = share
        hang()
    }

    private fun hang() {
        rig.moveTo(Offset(width * across / density, 0f))
        // Mid-spread, the circle follows the bulb to where it now hangs.
        if (revealing) revealFrom = bulb()
        frame++
        hung++
    }

    /** Steps the cord and the shade on by [seconds]; at rest, the frame loop sleeps. */
    internal fun advance(seconds: Float) {
        rig.advance(seconds)
        frame++
        if (rig.atRest) awake = false
    }

    /** Where the bulb is, in px. */
    internal fun bulb(): Offset =
        rig.onShade(Offset(0f, PullCordDimens.Rod.value + PullCordDimens.ShadeHeight.value)) *
            density

    /** Where the bead is, in px: for a fingertip drawn on it. Read in layout or drawing. */
    internal val bead: Offset
        get() {
            frame
            return rig.bead * density
        }

    /**
     * Where the bead hangs at rest, in px: it moves only when the lamp is hung somewhere new, not
     * as the cord swings. Read in layout or drawing.
     */
    internal val restingBead: Offset
        get() {
            hung
            return rig.restingBead * density
        }

    internal companion object {
        // Only whether it is lit outlives the screen: the cord hangs still in a new one.
        fun saver(scope: CoroutineScope): Saver<PullCordState, Boolean> =
            Saver(save = { it.lit }, restore = { PullCordState(it, scope) })
    }
}

/**
 * A [PullCordState], [lit] to begin with. Whether it is lit is saved, so it survives the screen
 * being made again, as on a rotation.
 */
@Composable
public fun rememberPullCordState(lit: Boolean = false): PullCordState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(saver = PullCordState.saver(scope)) { PullCordState(lit, scope) }
}
