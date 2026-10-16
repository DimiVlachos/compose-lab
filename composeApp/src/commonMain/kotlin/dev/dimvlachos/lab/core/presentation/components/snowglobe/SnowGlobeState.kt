package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh

/** What the hint under the globe says about a shake in progress. */
internal enum class GlobeHint {
    Idle,
    KeepShaking,
    LetItSettle,
    Settling,
}

/**
 * The globe's motion: the prototype's `step()`, ported line by line and stepped at its fixed 1/120
 * s. The glass sits on its base on a spring, stiff while held and wobbly when let go, and leans on
 * a second spring; its acceleration stirs the water's turbulence [turb] and [swirl], which lift and
 * carry the [glitter]. Swinging it back and forth three times, far or hard enough, is a shake:
 * [step]'s onShaken, once; when the finger has let go and the water has calmed, and no refresh is
 * pending, onSettled, once.
 *
 * Lengths are the prototype's CSS pixels: [configure] gives the screen's scale against a 360 px
 * design and the sphere's radius in them. Only [frame] and [hint] are snapshot state: a frame bumps
 * [frame], and the draw lambdas that read it redraw without anything recomposing.
 */
@Stable
internal class SnowGlobeState {
    /** The flakes, once the scene's [useFloor] says where they can rest. */
    var glitter: GlitterField? = null
        private set

    var reducedMotion = false

    var scale = 1f
        private set

    var radius = 144f
        private set

    private var gx = 0f
    private var gvx = 0f
    private var gA = 0f
    private var tiltAngle = 0f
    private var tiltV = 0f
    private var extA = 0f

    var turb = 0f
        private set

    var swirl = 0f
        private set

    private var shakeAcc = 0f
    private var pathAcc = 0f
    private var reversals = 0
    private var lastSign = 0f
    private var motionSign = 0f
    private var pending = false

    private var dragging = false
    private var dragDx = 0f
    private var dragTx = 0f
    private var dragPtx = Float.NaN
    private var autoShake = -1f

    private var carry = 0f

    var time = 0f
        private set

    var frame by mutableLongStateOf(0L)
        private set

    var hint by mutableStateOf(GlobeHint.Idle)
        private set

    var sceneFrom = GlobeScene.Sunny
        private set

    var sceneTo = GlobeScene.Sunny
        private set

    var fade = 1f
        private set

    /** The windmill sails' turn, in radians, and how fast they turn: brisk in the meltemi. */
    var sails = 0f
        private set

    private var sailSpeed = .5f

    /** Plays the swish, as loud as given; set by the composable. */
    var onSwish: ((Float) -> Unit)? = null

    /** The glass's slide on its base, in CSS px: sideways, lifting a little as it goes. */
    val offset: Offset
        get() = if (reducedMotion) Offset.Zero else Offset(gx, -abs(gx) * .06f)

    /** The glass's lean, in radians. */
    val tilt: Float
        get() = if (reducedMotion) 0f else tiltAngle

    /** The glass's slide over its radius, which turns the resting flakes to the light. */
    val shift: Float
        get() = if (reducedMotion) 0f else gx / radius

    fun configure(scale: Float, radius: Float) {
        this.scale = scale
        this.radius = radius
    }

    fun useFloor(floorAt: (Float) -> Float) {
        if (glitter == null) glitter = GlitterField(floorAt)
    }

    /**
     * The prototype's `resetState`: everything still, the glitter seeded again, the first scene.
     */
    fun reset() {
        gx = 0f
        gvx = 0f
        gA = 0f
        tiltAngle = 0f
        tiltV = 0f
        extA = 0f
        turb = 0f
        swirl = 0f
        shakeAcc = 0f
        pathAcc = 0f
        reversals = 0
        lastSign = 0f
        motionSign = 0f
        pending = false
        dragging = false
        autoShake = -1f
        sceneFrom = sceneTo
        fade = 1f
        glitter?.seed()
        hint = GlobeHint.Idle
    }

    private fun rubber(d: Float) = 74 * scale * tanh(d / (74 * scale))

    private fun press() {
        dragging = true
        dragDx = 0f
        dragTx = gx
        dragPtx = Float.NaN
        reversals = 0
        lastSign = 0f
    }

    /** The finger on the glass moved [dx] CSS px sideways; the glass follows on a rubber band. */
    fun drag(dx: Float) {
        if (!dragging || autoShake >= 0f) {
            autoShake = -1f
            press()
        }
        dragDx += dx
        dragTx = rubber(dragDx)
    }

    fun release() {
        dragging = false
        autoShake = -1f
    }

    /**
     * The keyboard's shake, Enter or Space or a click: the prototype's `autoShake`, 1.1 s of it.
     */
    fun shake() {
        press()
        autoShake = 0f
    }

    /** A real shake of the phone, its linear acceleration in m/s²: the prototype's devicemotion. */
    fun nudge(acceleration: Offset) {
        val ax = acceleration.x
        if (abs(ax) < 5) return
        extA = (-ax * 14).coerceIn(-500f, 500f)
        if (!reducedMotion) gvx += -ax * 5 * scale
        shakeAcc += abs(extA) / 60
        pathAcc += abs(ax) * 2 * scale
        val sg = sign(ax)
        if (abs(ax) > 9 && sg != motionSign) {
            if (motionSign != 0f) reversals++
            motionSign = sg
        }
    }

    /** The globe carried to another day: a rock toward [direction], a little stir, no refresh. */
    fun rock(direction: Int) {
        if (reducedMotion) return
        tiltV += direction * 1.05f
        gvx += direction * 90 * scale
        turb = maxOf(turb, .22f)
        swirl = maxOf(swirl, .12f)
        onSwish?.invoke(.3f)
    }

    fun showScene(scene: GlobeScene) {
        if (scene == sceneTo) return
        sceneFrom = if (fade >= .5f) sceneTo else sceneFrom
        sceneTo = scene
        fade = if (reducedMotion) 1f else 0f
        if (fade >= 1f) sceneFrom = sceneTo
    }

    fun step(dt: Float, refreshPending: Boolean, onShaken: () -> Unit, onSettled: () -> Unit) {
        carry += dt
        while (carry >= Step - 1e-6f) {
            advance(Step, refreshPending, onShaken, onSettled)
            carry -= Step
        }
        val now =
            when {
                pending && dragging -> GlobeHint.LetItSettle
                pending -> GlobeHint.Settling
                dragging && reversals > 0 -> GlobeHint.KeepShaking
                else -> GlobeHint.Idle
            }
        if (hint != now) hint = now
        frame++
    }

    private fun advance(
        dt: Float,
        refreshPending: Boolean,
        onShaken: () -> Unit,
        onSettled: () -> Unit,
    ) {
        time += dt
        if (autoShake >= 0f) {
            autoShake += dt
            val p = autoShake / AutoShakeSeconds
            if (p >= 1f) {
                release()
            } else {
                dragTx = rubber(62 * scale * sin(p * TAU * 3.2f) * sin(p * PI.toFloat()))
            }
        }

        // The globe on its base: a stiff spring while held, a wobbly one when let go.
        val target = if (dragging) dragTx else 0f
        val k = if (dragging) 900f else 150f
        val cd = if (dragging) 55f else 10f
        val ax = (target - gx) * k - gvx * cd
        gvx += ax * dt
        gx += gvx * dt
        gA = ax
        val tiltT = (gvx * .00024f + if (dragging) gx * .001f else 0f).coerceIn(-.24f, .24f)
        tiltV += ((tiltT - tiltAngle) * 150 - tiltV * 8.5f) * dt
        tiltAngle += tiltV * dt
        var aN = gA / radius + extA
        extA *= exp(-dt * 12)
        if (reducedMotion) aN = 0f
        val am = min(abs(aN), 650f)
        turb = min(1.7f, turb + am * .017f * dt)
        swirl = min(1.3f, swirl + am * .0065f * dt)
        turb *= exp(-dt * if (dragging) .25f else .62f)
        swirl *= exp(-dt * .48f)
        if (dragging) {
            val d = if (dragPtx.isNaN()) 0f else abs(dragTx - dragPtx)
            dragPtx = dragTx
            pathAcc += d
        }
        pathAcc *= exp(-dt * .7f)
        if (dragging || abs(extA) > 1) {
            shakeAcc += am * dt
            val sg = sign(gvx)
            if (abs(gvx) > 200 * scale && sg != lastSign) {
                if (lastSign != 0f) {
                    reversals++
                    onSwish?.invoke((am / 400).coerceIn(.2f, 1f))
                }
                lastSign = sg
            }
        }
        shakeAcc *= exp(-dt * .7f)
        if (!pending && reversals >= 3 && (pathAcc > 210 * scale || shakeAcc > 60)) {
            pending = true
            onShaken()
        }
        // Not while the phone is still being shaken, either: with reduced motion the water never
        // clouds, and a long shake would otherwise settle and refresh between its jolts.
        if (pending && !dragging && turb < .34f && abs(extA) < 1 && !refreshPending) {
            pending = false
            shakeAcc = 0f
            pathAcc = 0f
            reversals = 0
            onSettled()
        }
        if (!dragging && !pending && shakeAcc < 5 && pathAcc < 20 * scale) reversals = 0
        if (fade < 1f) {
            fade = min(1f, fade + dt / SnowGlobeDimens.SceneFadeSeconds)
            if (fade >= 1f) sceneFrom = sceneTo
        }
        val sailTarget =
            when (sceneTo) {
                GlobeScene.Sunny -> .45f
                GlobeScene.Windy -> 3.4f
                GlobeScene.Shower -> 1.1f
            }
        sailSpeed += (sailTarget - sailSpeed) * min(1f, dt * 1.5f)
        sails += sailSpeed * dt

        if (reducedMotion) return
        glitter?.step(dt, time, turb, swirl, am, aN, tiltAngle)
    }

    private companion object {
        const val Step = 1f / 120f
        const val AutoShakeSeconds = 1.1f
    }
}

@Composable internal fun rememberSnowGlobeState(): SnowGlobeState = remember { SnowGlobeState() }
