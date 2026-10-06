package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * [toggle] switches it without the cord. Only [lit], and whether a new look is still spreading, are
 * read in composition: the cord, the shade and the light are read while drawing, so a swinging cord
 * never recomposes.
 */
@Stable
public class PullCordState internal constructor(lit: Boolean, private val scope: CoroutineScope) {
    /** Whether the lamp is on: the screen under it shows its dark look, lit by the lamp. */
    public var lit: Boolean by mutableStateOf(lit)
        private set

    internal val rig = LampRig(Offset.Zero) { toggle() }

    // The density the lamp is laid out at: the rig works in dp, a pointer in px.
    internal var density = 1f
        private set

    /** Bumped every step of the cord, so whatever draws it is drawn again. */
    internal var frame by mutableIntStateOf(0)
        private set

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

    /** How bright the bulb is, 0 off to 1 on: it flickers as it comes on. */
    internal val brightness = Animatable(if (lit) 1f else 0f)

    /** Called as the lamp switches, with whether it is now lit: a tick of haptics, say. */
    internal var onSwitch: ((Boolean) -> Unit)? = null

    private var revealJob: Job? = null
    private var brightnessJob: Job? = null

    /** Takes the bead if [at] is on it, and says whether it did. */
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

    /** Lets go of the bead: the cord springs back up and sways. */
    public fun release() {
        rig.release()
    }

    /** Switches the lamp: the new look spreads out from the bulb over the old. */
    public fun toggle() {
        lit = !lit
        onSwitch?.invoke(lit)
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
        brightnessJob?.cancel()
        brightnessJob = scope.launch {
            if (lit) {
                // On with two quick dips, as a filament catching does.
                brightness.animateTo(
                    1f,
                    keyframes {
                        durationMillis = PullCordDimens.FlickerMs
                        0f at 0
                        1f at 40
                        0.3f at 90
                        1f at 140
                        0.45f at 200
                        1f at PullCordDimens.FlickerMs
                    },
                )
            } else {
                brightness.animateTo(0f, tween(PullCordDimens.OffMs))
            }
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

    /** Hangs the lamp [across] the stage's width instead, as a share of it. */
    internal fun hangAcross(across: Float) {
        if (across == this.across) return
        this.across = across
        hang()
    }

    private fun hang() {
        rig.moveTo(Offset(width * across / density, 0f))
        // Mid-spread, the circle follows the bulb to where it now hangs.
        if (revealing) revealFrom = bulb()
        frame++
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

    /** Where the bead is, in px: for a fingertip drawn on it. Read while drawing. */
    internal val bead: Offset
        get() {
            frame
            return rig.bead * density
        }
}

/** A [PullCordState], [lit] to begin with. */
@Composable
public fun rememberPullCordState(lit: Boolean = false): PullCordState {
    val scope = rememberCoroutineScope()
    return remember { PullCordState(lit, scope) }
}
