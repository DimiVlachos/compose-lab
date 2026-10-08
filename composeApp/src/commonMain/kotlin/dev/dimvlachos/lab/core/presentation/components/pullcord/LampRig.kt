package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import dev.dimvlachos.lab.core.presentation.components.physics.Pendulum
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * The lamp's moving parts, in dp: a shade hanging as a pendulum from [pivot] on the ceiling, and a
 * cord hanging from its side with a bead on the end. A finger takes the bead with [grab], pulls it
 * about with [dragTo] and lets go with [release]; pulled [PullCordDimens.ClickPull] down from where
 * it was taken, the switch clicks, [onClick], once a pull. The cord's pull tilts the shade, and the
 * shade's swing carries the cord. [advance] steps it all on at a fixed rate.
 */
internal class LampRig(pivot: Offset, private val onClick: () -> Unit) {
    /** Where the lamp hangs from. */
    var pivot: Offset = pivot
        private set

    private val length = PullCordDimens.CordLength.value
    private val rope =
        CordRope(PullCordDimens.CordPoints, length / (PullCordDimens.CordPoints - 1)).apply {
            hang(attachment(0f))
        }

    // The shade, a damped pendulum from the ceiling.
    private val shade =
        Pendulum(PullCordDimens.ShadeSwingHz, PullCordDimens.ShadeDamping, PullCordDimens.MostTilt)

    /** How far the shade has turned about the pivot, in radians: clockwise is positive. */
    val tilt: Float
        get() = shade.angle

    private var carried = 0f
    private var quiet = 0

    // The finger's place, unspecified while no finger has the bead, and where it was as of the
    // last step: a frame's move is spread over that frame's steps, so a quick flick keeps its
    // speed however many steps a frame takes. Also where it took the bead, and the bead's offset
    // from it at the time, so a bead taken off-centre doesn't jump to the fingertip.
    private var finger = Offset.Unspecified
    private var stepFinger = Offset.Unspecified
    private var grabbedAt = Offset.Zero
    private var hold = Offset.Zero
    private var clicked = false

    // Let go of before its last move was stepped: the next steps carry the cord there, then let
    // it go.
    private var lettingGo = false

    /** Whether a finger has the bead. */
    val held: Boolean
        get() = finger.isSpecified && !lettingGo

    /** Whether nothing is moving: no finger, the cord still and the shade level. */
    var atRest = true
        private set

    /** Where the bead is. Read in layout or drawing. */
    val bead: Offset
        get() = rope[rope.size - 1]

    /** Where the bead hangs when the lamp is at rest. */
    val restingBead: Offset
        get() = attachment(0f) + Offset(0f, length)

    /** The cord's points, top first. */
    val points: Int
        get() = rope.size

    fun point(i: Int): Offset = rope[i]

    /** Where a point on the shade, [local] from the pivot as the shade hangs level, is now. */
    fun onShade(local: Offset): Offset = pivot + rotate(local, tilt)

    /**
     * Takes the bead if [at] is on it, and says whether it did. Refuses while a finger already has
     * it.
     */
    fun grab(at: Offset): Boolean {
        // One hand on the cord at a time: a second can't take over a pull, or re-arm its click.
        if (held) return false
        if ((at - bead).getDistance() > PullCordDimens.GrabRadius.value) return false
        finger = at
        stepFinger = at
        lettingGo = false
        grabbedAt = at
        hold = bead - at
        clicked = false
        atRest = false
        quiet = 0
        return true
    }

    fun dragTo(at: Offset) {
        if (!held) return
        finger = at
        atRest = false
        quiet = 0
        // Down, not along: a sideways swing that sags as far still only swings.
        val down = at.y - grabbedAt.y
        val along = abs(at.x - grabbedAt.x)
        if (!clicked && down >= PullCordDimens.ClickPull.value && down >= 2f * along) {
            clicked = true
            onClick()
        }
    }

    /**
     * Lets go of the bead, from where the finger last was: a move not yet stepped is stepped first,
     * so a flick's last move isn't lost. Does nothing if no finger has the bead.
     */
    fun release() {
        if (!held) return
        if (finger == stepFinger) letGo() else lettingGo = true
    }

    private fun letGo() {
        finger = Offset.Unspecified
        stepFinger = Offset.Unspecified
        lettingGo = false
        rope.letGo()
    }

    /**
     * Hangs the lamp from [to] instead, as it is: the screen or the lamp's place across it has
     * changed.
     */
    fun moveTo(to: Offset) {
        val by = to - pivot
        pivot = to
        rope.shift(by)
        // A finger stays where it is on the screen, and so does where it took the bead: only the
        // lamp moves under it, so a pull straight down still clicks.
    }

    /**
     * A breath of air through the room, as when a door opens: the cord is pushed sideways at
     * [speed] dp a second at its bead, and the shade with it, and the physics plays it out from
     * there. A held cord is the hand's, and a breath doesn't move it.
     */
    fun stir(speed: Float) {
        if (finger.isSpecified) return
        rope.push(speed, PullCordDimens.StepSeconds)
        shade.swing += speed * PullCordDimens.ShadeStir
        atRest = false
        quiet = 0
    }

    /**
     * Steps the lamp on by [seconds], in fixed steps; what is left over waits for the next call.
     */
    fun advance(seconds: Float) {
        if (atRest) return
        carried += seconds
        val dt = PullCordDimens.StepSeconds
        var steps = 0
        while (carried >= dt) {
            carried -= dt
            steps++
        }
        for (k in 1..steps) {
            // The finger is where it was at the last step, the share of the way through this
            // frame's move that this step is.
            step(dt, k / steps.toFloat())
            val level =
                !finger.isSpecified &&
                    rope.still &&
                    rope.stretch == 1f &&
                    shade.isStill(PullCordDimens.LevelTilt, PullCordDimens.LevelSwing)
            quiet = if (level) quiet + 1 else 0
        }
        if (steps > 0 && finger.isSpecified) {
            stepFinger = finger
            if (lettingGo) letGo()
        }
        atRest = quiet >= PullCordDimens.QuietSteps
        if (atRest) carried = 0f
    }

    private fun step(dt: Float, share: Float) {
        if (finger.isSpecified) {
            // Past its length the cord gives, less and less: it never stretches more than so far.
            val top = rope[0]
            val wanted = lerp(stepFinger, finger, share) + hold - top
            val reach = wanted.getDistance()
            val most = PullCordDimens.MostStretch.value
            val over = reach - length
            if (over > 0f) {
                val give = most * (1f - exp(-over / most))
                rope.hold(top + wanted * ((length + give) / reach), (length + give) / length)
            } else {
                rope.hold(top + wanted, 1f)
            }
        } else if (rope.stretch != 1f) {
            rope.stretch = 1f + (rope.stretch - 1f) * exp(-dt / PullCordDimens.Recoil)
            if (rope.stretch - 1f < 1e-4f) rope.stretch = 1f
        }
        swingShade(dt)
        rope.pin(attachment(tilt))
        rope.step(dt)
    }

    // The shade as a damped pendulum, turned by the cord's pull on its side: the more the cord is
    // stretched, the harder it pulls, along its first segment.
    private fun swingShade(dt: Float) {
        val lever = rope[0] - pivot
        val along = rope[1] - rope[0]
        val reach = along.getDistance()
        val pull = (rope.stretch - 1f) * length
        val turn =
            if (reach > 0f && pull > 0f) {
                (lever.x * along.y - lever.y * along.x) / reach * pull * PullCordDimens.TiltPerPull
            } else {
                0f
            }
        shade.step(dt, turn)
    }

    // Where the cord hangs from the shade turned by [angle].
    private fun attachment(angle: Float): Offset =
        pivot +
            rotate(
                Offset(
                    PullCordDimens.ShadeRim.value / 2f - PullCordDimens.CordInset.value,
                    PullCordDimens.Rod.value + PullCordDimens.ShadeHeight.value,
                ),
                angle,
            )
}

// [local] turned by [angle] radians, clockwise on the screen.
private fun rotate(local: Offset, angle: Float): Offset {
    val c = cos(angle)
    val s = sin(angle)
    return Offset(local.x * c - local.y * s, local.x * s + local.y * c)
}
