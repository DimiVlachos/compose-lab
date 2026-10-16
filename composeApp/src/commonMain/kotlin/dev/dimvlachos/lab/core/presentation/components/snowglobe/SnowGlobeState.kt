package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * The globe's motion. The glass follows the finger on a spring, its acceleration stirs the water,
 * and the stir is the swirl [energy], which the glitter feels and which fades when left alone.
 * Above [SnowGlobeDimens.ShakeThreshold] it counts as a shake, once; once it has calmed under
 * [SnowGlobeDimens.SettleThreshold] and no refresh is pending, as settled, once.
 *
 * Only [frame] is snapshot state: a frame bumps it, and the draw lambdas that read it redraw
 * without anything recomposing.
 */
@Stable
internal class SnowGlobeState(seed: Int = 7) {
    val glitter = GlitterField(SnowGlobeDimens.Flakes, seed)

    var energy = 0f
        private set

    var offset = Offset.Zero
        private set

    private var velocity = Offset.Zero
    private var target = Offset.Zero

    /** The glass leans with its velocity, as if carried. */
    val tilt: Float
        get() = (velocity.x * 0.04f).coerceIn(-0.12f, 0.12f)

    var time = 0f
        private set

    var frame by mutableLongStateOf(0L)
        private set

    var reducedMotion = false

    var sceneFrom = GlobeScene.Sunny
        private set

    var sceneTo = GlobeScene.Sunny
        private set

    var fade = 1f
        private set

    /** Plays the swish, as loud as given; set by the composable. */
    var onSwish: ((Float) -> Unit)? = null

    private var swirl = 1f
    private var armed = true
    private var waitingToSettle = false
    private var sinceSwish = 1f

    // Only a finger on the glass stirs the water through the spring: a rock or a fling swinging
    // back alone mustn't add up to a shake.
    private var dragging = false

    fun drag(by: Offset) {
        dragging = true
        val pulled = target + by
        val reach = pulled.getDistance()
        target =
            if (reach > SnowGlobeDimens.MaxDrag) pulled * (SnowGlobeDimens.MaxDrag / reach)
            else pulled
        if (by.x != 0f) swirl = sign(by.x)
    }

    fun release() {
        dragging = false
        target = Offset.Zero
    }

    /** A real shake of the phone, its linear acceleration in m/s². */
    fun nudge(acceleration: Offset) {
        // With reduced motion a shake of the phone still stirs the water, but the glass stays put.
        if (!reducedMotion) velocity += acceleration * 0.02f
        energy =
            (energy + acceleration.getDistance() * 0.03f).coerceAtMost(SnowGlobeDimens.MaxEnergy)
        if (acceleration.x != 0f) swirl = sign(acceleration.x)
    }

    /**
     * A keyboard or scripted shake, as hard as [strength]. With reduced motion it still stirs the
     * water enough to refresh, but the glass stays still.
     */
    fun shake(strength: Float = 1f) {
        energy =
            (energy + SnowGlobeDimens.ShakeEnergy * strength).coerceAtMost(
                SnowGlobeDimens.MaxEnergy
            )
        if (!reducedMotion) velocity += Offset(SnowGlobeDimens.RockKick * swirl, 0f)
        swirl = -swirl
    }

    /**
     * The globe carried to another day: a rock toward [direction], a little stir, no refresh. Still
     * with reduced motion.
     */
    fun rock(direction: Int) {
        if (reducedMotion) return
        velocity += Offset(SnowGlobeDimens.RockKick * direction, 0f)
        energy = maxOf(energy, SnowGlobeDimens.RockEnergy)
    }

    fun showScene(scene: GlobeScene) {
        if (scene == sceneTo) return
        sceneFrom = if (fade >= 1f) sceneTo else sceneFrom
        sceneTo = scene
        fade = 0f
    }

    fun step(dt: Float, refreshPending: Boolean, onShaken: () -> Unit, onSettled: () -> Unit) {
        time += dt
        val before = energy

        // The glass on its spring; its acceleration is what stirs the water.
        val acceleration =
            (target - offset) * SnowGlobeDimens.SpringStiffness -
                velocity * SnowGlobeDimens.SpringDamping
        velocity += acceleration * dt
        offset += velocity * dt
        if (dragging) {
            val push = sqrt(acceleration.x * acceleration.x + acceleration.y * acceleration.y)
            energy += push * SnowGlobeDimens.AccelToEnergy * dt * 60f
        }

        energy *= exp(-SnowGlobeDimens.EnergyDecay * dt)
        if (refreshPending && !armed) energy = maxOf(energy, SnowGlobeDimens.PendingFloor)
        energy = energy.coerceAtMost(SnowGlobeDimens.MaxEnergy)

        if (armed && energy >= SnowGlobeDimens.ShakeThreshold) {
            armed = false
            waitingToSettle = true
            onShaken()
        } else if (!armed && energy < SnowGlobeDimens.SettleThreshold) {
            armed = true
        }
        if (waitingToSettle && energy < SnowGlobeDimens.SettleThreshold && !refreshPending) {
            waitingToSettle = false
            onSettled()
        }

        sinceSwish += dt
        if (energy - before > 0.05f && sinceSwish > 0.35f) {
            sinceSwish = 0f
            onSwish?.invoke(energy.coerceIn(0.2f, 1f))
        }

        if (fade < 1f) {
            fade = (fade + dt / SnowGlobeDimens.SceneFadeSeconds).coerceAtMost(1f)
            if (fade >= 1f) sceneFrom = sceneTo
        }

        glitter.step(dt, time, if (reducedMotion) 0f else energy, swirl)
        frame++
    }
}

@Composable internal fun rememberSnowGlobeState(): SnowGlobeState = remember { SnowGlobeState() }
