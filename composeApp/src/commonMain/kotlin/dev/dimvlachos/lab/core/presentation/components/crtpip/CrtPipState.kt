package dev.dimvlachos.lab.core.presentation.components.crtpip

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Where the video is: the full player, the little set in a corner, or on its way between them. */
internal enum class PipMode {
    Full,
    ToMini,
    Mini,
    ToFull,
    /** The set flicked off a side edge, switching off. */
    ToOff,
    /** Playback stopped; the player's space stays collapsed. */
    Stopped,
    /** Undo: the set pops back into its corner and warms up. */
    WarmUp,
}

/** Something the set does that is heard or felt. */
internal enum class PipEvent {
    /** The tube switching off; the strength is its volume. */
    Off,
    /** The dot setting off on its flight. */
    Fly,
    /** The set warming up, with a crackle of static. */
    On,
    /** A swipe on the player committed to the change. */
    Commit,
    /** The set came to rest in a corner. */
    Snap,
    /** Playback stopped by a flick off the edge. */
    Stop,
    /** Undo brought the set back. */
    Undo,
}

/** What to tell a screen reader, as the prototype's live region says it. */
internal enum class PipAnnouncement {
    None,
    Mini,
    Full,
    Stopped,
}

/** Where keyboard focus should go next, as the prototype's `focusTo`: it follows the video. */
internal enum class PipFocus {
    None,
    Set,
    Player,
    Undo,
}

/** What the set's tube shows this frame. */
internal enum class PipScreen {
    /** Dark glass. */
    None,
    /** The show, squeezed to [PipPose.screenSy] and brightened by [PipPose.screenBright]. */
    Picture,
    /** A glowing line [PipPose.screenLine] wide. */
    Line,
    /** The show opening with static of [PipPose.screenNoise] over it. */
    Warming,
}

/**
 * What one frame draws, worked out by [CrtPipState.step] from the prototype's `draw()`: the set
 * (scaled about its screen and faded as one piece) and, over it, the full player's picture, the
 * line, the dot in flight and the dot on its own.
 */
internal class PipPose {
    var cab = false
    var cabScale = 1f
    var cabAlpha = 1f
    var screen = PipScreen.None
    var screenSy = 1f
    var screenBright = 0f
    var screenLine = 0f
    var screenNoise = 0f

    /** The full player's picture, clipped to the slot. */
    var picture = false
    var pictureCy = 0f
    var pictureSy = 1f
    var pictureSx = 1f
    var pictureBright = 0f

    var line = false
    var lineCx = 0f
    var lineCy = 0f
    var lineW = 0f

    var flight = false
    var fromX = 0f
    var fromY = 0f
    var toX = 0f
    var toY = 0f
    var flightF = 0f

    var dot = false
    var dotX = 0f
    var dotY = 0f
    var dotR = 0f
    var dotA = 1f

    internal fun clear() {
        cab = false
        cabScale = 1f
        cabAlpha = 1f
        screen = PipScreen.None
        picture = false
        line = false
        flight = false
        dot = false
    }

    internal fun cab(scale: Float, alpha: Float, screen: PipScreen) {
        cab = alpha > 0f
        cabScale = scale
        cabAlpha = alpha
        this.screen = screen
    }
}

/**
 * The CRT picture-in-picture, ported from the prototype's `st`, `frame()`, `draw()`, its changes
 * (`minimize`, `expand`, `stopPlayback`, `undo`, `finish`) and its pointer handlers. Units are dp,
 * which play the prototype's CSS pixels; [layout] gives the screen it fills.
 *
 * [now] is its own clock in ms, which [step] advances; every transition is only "ms into the
 * change", and the prototype's `later()` timers run on it, so a test steps it as the screen does.
 */
@Stable
internal class CrtPipState {
    var reducedMotion = false

    /** Hears what the set does, and how strongly. */
    var onEvent: ((PipEvent, Float) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something starts moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    var mode by mutableStateOf(PipMode.Full)
        private set

    /** The player's slot, in dp: 16:9 when full, nothing while mini or stopped. */
    var slotH by mutableFloatStateOf(0f)
        private set

    /** The set's top-left corner, in dp. */
    var cabX by mutableFloatStateOf(0f)
        private set

    var cabY by mutableFloatStateOf(0f)
        private set

    /** The player's controls are showing: after a tap, for 2.6 s. */
    var controlsOn by mutableStateOf(false)
        private set

    /** The progress track shows: only on the full player, at rest. */
    var trackOn by mutableStateOf(true)
        private set

    /** The whole second of the show, for the time code. */
    var second by mutableIntStateOf(StartAt.toInt())
        private set

    var toastOn by mutableStateOf(false)
        private set

    /** The toast went away at once (Undo), so it doesn't fade over the set coming back. */
    var toastInstant = false
        private set

    var announcement by mutableStateOf(PipAnnouncement.None)
        private set

    /** Bumped with each announcement, so the same words said twice are heard twice. */
    var announced by mutableIntStateOf(0)
        private set

    var focusRequest by mutableStateOf(PipFocus.None)

    // ---- Layout ----
    var width = 0f
        private set

    var height = 0f
        private set

    var slotTop = 0f
        private set

    var navTop = 0f
        private set

    /** The page's dp scale. */
    var k = 1f
        private set

    /** The set's own unit (124 dp wide where it used to be 150), so its parts shrink together. */
    var ck = 1f
        private set

    var fullH = 0f
        private set

    val cabW
        get() = 150 * ck

    val cabH
        get() = 98 * ck

    /** The set's screen: its top-left, width and height. */
    val screenX
        get() = cabX + 10 * ck

    val screenY
        get() = cabY + 10 * ck

    val screenW
        get() = 98 * ck

    val screenH
        get() = cabH - 20 * ck

    /** How far the toast stands above the screen's foot. */
    val toastBottom
        get() = height - navTop + 10 * k

    // ---- Simulation ----
    var now = 0.0
        private set

    /** The show's time, in seconds. */
    var vt = StartAt
        private set

    var corner = "br"
        private set

    private var posX = 0f
    private var posY = 0f
    private var velX = 0f
    private var velY = 0f

    /** The set is springing into [corner]. */
    var spring = false
        private set

    private var offX0 = 0f
    private var offX1 = 0f
    private var t0 = 0.0
    private var sy0 = 1f
    private var ty0 = 0f

    /** The drag's pre-squeeze, 0 to 1, and how far the picture follows the finger down. */
    var pre = 0f
        private set

    var preY = 0f
        private set

    private var ctlUntil = 0.0
    private var focusTo = PipFocus.None

    /** The phases a change passed through, in order: for tests. */
    val seen = mutableListOf<String>()
    private var phase = "full"

    val pose = PipPose()

    private class Timer(val at: Double, val fn: () -> Unit)

    private val timers = mutableListOf<Timer>()
    private var toastTok = 0

    // The spring-back after an early release: 240 ms on easeOutBack from where it was let go.
    private var backAt = -1.0
    private var backPre = 0f
    private var backY = 0f

    private class Drag(val x0: Float, val y0: Float, var x: Float, var y: Float, var t: Double) {
        var v = 0f
        var moved = false
        var scripted = false
    }

    private class SetDrag(
        val x0: Float,
        val y0: Float,
        var x: Float,
        var y: Float,
        val t0: Double,
        var t: Double,
        val px: Float,
        val py: Float,
    ) {
        var vx = 0f
        var vy = 0f
        var n = 0
        var moved = false
        var scripted = false
    }

    private var drag: Drag? = null
    private var setDrag: SetDrag? = null

    /** A finger (or the script) is on the player. */
    val dragging
        get() = drag != null

    /** A finger (or the script) is on the set and has moved it. */
    val draggingSet
        get() = setDrag?.moved == true

    /** The player's middle, in dp. */
    val playerX
        get() = width / 2

    val playerY
        get() = slotTop + fullH / 2

    /** The set's screen's middle, in dp. */
    val setX
        get() = screenX + screenW / 2

    val setY
        get() = screenY + screenH / 2

    /** At rest: stopped with nothing pending, so the frame loop can sleep. */
    val settled
        get() = mode == PipMode.Stopped && timers.isEmpty() && !spring

    /**
     * The screen it fills: [w] by [h], the player's slot starting [top] down (under the app bar)
     * and the nav bar starting [nav] down.
     */
    fun layout(w: Float, h: Float, top: Float, nav: Float) {
        if (w == width && h == height && top == slotTop && nav == navTop) return
        width = w
        height = h
        slotTop = top
        navTop = nav
        k = (w / 360f).coerceIn(.85f, 1.3f)
        fullH = kotlin.math.round(w * 9 / 16)
        ck = k * 124 / 150
        if (setDrag == null && mode != PipMode.ToOff) {
            val c = cornerX(corner) to cornerY(corner)
            posX = c.first
            posY = c.second
            spring = false
        }
        place()
        slotH =
            when (mode) {
                PipMode.Mini,
                PipMode.Stopped,
                PipMode.ToOff,
                PipMode.WarmUp -> 0f
                PipMode.Full -> fullH
                else -> slotH
            }
        pose()
        wake++
        frame++
    }

    /** One of the four places the set can rest, `tl`, `tr`, `bl` or `br`: its left edge. */
    fun cornerX(c: String) = if (c[1] == 'l') 14 * k else width - 14 * k - cabW

    /**
     * Its top: the antenna tips clear of the top bar, or the legs and shadow clear of the nav bar.
     */
    fun cornerY(c: String) =
        if (c[0] == 't') slotTop + 10 * k + (Antenna + 2.2f) * ck
        else navTop - 14 * k - 8 * ck - cabH

    private fun place() {
        cabX = posX
        cabY = posY
    }

    // ---- The loop ----

    /** Advances [dt] seconds: the show, the corner spring, due timers, then the change in hand. */
    fun step(dt: Float) {
        now += dt * 1000.0
        if (mode != PipMode.ToOff && mode != PipMode.Stopped) vt = (vt + dt) % Duration
        second = vt.toInt()
        stepSpring(dt)
        var i = 0
        while (i < timers.size) {
            if (timers[i].at <= now) {
                val t = timers.removeAt(i)
                t.fn()
            } else i++
        }
        if (backAt >= 0) {
            val p = ((now - backAt) / BackMs).toFloat().coerceIn(0f, 1f)
            if (drag == null && mode == PipMode.Full) {
                val e = easeOutBack(p)
                pre = max(0f, backPre * (1 - e))
                preY = backY * (1 - e)
            }
            if (p >= 1f) backAt = -1.0
        }
        advance()
        pose()
        controlsOn = mode == PipMode.Full && now < ctlUntil && pre == 0f
        trackOn = mode == PipMode.Full && pre < .05f
        frame++
    }

    // A damped spring on the simulated clock (dampingRatio .72), so it is stepped as the rest.
    private fun stepSpring(dt: Float) {
        if (!spring || mode != PipMode.Mini || setDrag != null) return
        val tx = cornerX(corner)
        val ty = cornerY(corner)
        var left = dt
        while (left > 0f) {
            val h = min(.004f, left)
            velX += (-SpringK * (posX - tx) - SpringC * velX) * h
            velY += (-SpringK * (posY - ty) - SpringC * velY) * h
            posX += velX * h
            posY += velY * h
            left -= .004f
        }
        if (hypot(posX - tx, posY - ty) < .3f && hypot(velX, velY) < 6f) {
            posX = tx
            posY = ty
            velX = 0f
            velY = 0f
            spring = false
            emit(PipEvent.Snap)
        }
        place()
    }

    private val el
        get() = (now - t0).toFloat()

    // What the prototype's draw() changes as it draws: the set gliding out as it switches off, the
    // slot's height, and each change finishing.
    private fun advance() {
        when (mode) {
            PipMode.ToOff -> {
                val a2 = Off.sq + Off.ln
                posX = lerp(offX0, offX1, easeOut3((el / a2).coerceIn(0f, 1f)))
                place()
                if (el >= a2 + Off.glow) finishStop()
            }
            PipMode.WarmUp -> if (el >= Wu.pop + Wu.warm + Wu.open) finish(PipMode.Mini)
            PipMode.ToMini -> {
                val t = ToMiniT
                if (el >= t.total) finish(PipMode.Mini)
                else slotH = fullH * (1 - ease(((el - t.sq) / SlotMs).coerceIn(0f, 1f)))
            }
            PipMode.ToFull -> {
                val t = ToFullT
                if (el >= t.total) finish(PipMode.Full)
                else slotH = fullH * ease(((el - t.sq - t.ln) / SlotMs).coerceIn(0f, 1f))
            }
            else -> Unit
        }
    }

    private fun pose() {
        val p = pose
        p.clear()
        val pcx = playerX
        val pcy = playerY
        val mcx = setX
        val mcy = setY
        var ph = mode.name.lowercase()
        when (mode) {
            PipMode.Full -> {
                p.picture = true
                p.pictureCy = pcy + preY
                p.pictureSy = 1 - .55f * pre
                p.pictureSx = 1 + .02f * pre
                p.pictureBright = .35f * pre
            }
            PipMode.Mini -> {
                p.cab(1f, 1f, PipScreen.Picture)
                p.screenSy = 1f
                p.screenBright = 0f
            }
            PipMode.Stopped -> Unit
            PipMode.ToOff -> {
                val a1 = Off.sq
                val a2 = a1 + Off.ln
                val e = el
                when {
                    e < a1 -> squeezeScreen(e / a1)
                    e < a2 -> {
                        p.cab(1f, 1f, PipScreen.Line)
                        p.screenLine = lerp(screenW, 5 * k, easeIn3((e - a1) / Off.ln))
                    }
                    else -> {
                        val f = (e - a2) / Off.fold
                        if (f < 1) {
                            p.cab(
                                1 - .92f * easeIn3(f),
                                1 - ((f - .8f) / .2f).coerceIn(0f, 1f),
                                PipScreen.None,
                            )
                        }
                        val g = (e - a2) / Off.glow
                        val fade = 1 - easeIn3(((g - .2f) / .8f).coerceIn(0f, 1f))
                        dot(mcx.coerceIn(16 * k, width - 16 * k), mcy, lerp(4f, 2.4f, g) * k, fade)
                    }
                }
            }
            PipMode.WarmUp -> {
                val a1 = Wu.pop
                val a2 = a1 + Wu.warm
                val e = el
                when {
                    e < a1 -> {
                        val q = e / a1
                        p.cab(.55f + .45f * easeOutBack(q), min(1f, q * 4), PipScreen.None)
                    }
                    e < a2 -> warmLine((e - a1) / Wu.warm)
                    else -> openScreen((e - a2) / Wu.open)
                }
            }
            PipMode.ToMini -> {
                val t = ToMiniT
                val a1 = t.sq
                val a2 = a1 + t.ln
                val a3 = a2 + t.fly
                val a4 = a3 + t.warm
                val e = el
                // The set pops up once the dot is on its way, so it is waiting when the dot lands.
                val cq = ((e - (a2 + t.fly * .35f)) / 300f).coerceIn(0f, 1f)
                if (cq > 0 && e < a3) {
                    p.cab(.55f + .45f * easeOutBack(cq), min(1f, cq * 4), PipScreen.None)
                }
                when {
                    e < a1 -> {
                        ph = "squeeze"
                        val a = e / a1
                        p.picture = true
                        p.pictureCy = pcy + ty0 * (1 - a)
                        p.pictureSy = lerp(sy0, .012f, easeIn3(a))
                        p.pictureSx = lerp(1 + .02f * (1 - sy0) / .55f, 1.03f, a)
                        p.pictureBright = lerp(.35f * (1 - sy0) * 2, .95f, a.pow(.7f))
                    }
                    e < a2 -> {
                        ph = "line"
                        fxLine(pcx, pcy, lerp(width * 1.03f, 5 * k, easeIn3((e - a1) / t.ln)))
                    }
                    e < a3 -> {
                        ph = "dot"
                        flight(pcx, pcy, mcx, mcy, (e - a2) / t.fly)
                    }
                    e < a4 -> {
                        ph = "warm"
                        val gq = (e - a3) / t.warm
                        warmLine(gq)
                        if (gq < .3f) dot(mcx, mcy, 4 * k, 1 - gq / .3f)
                    }
                    else -> {
                        ph = "open"
                        openScreen((e - a4) / t.open)
                    }
                }
            }
            PipMode.ToFull -> {
                val t = ToFullT
                val a1 = t.sq
                val a2 = a1 + t.ln
                val a3 = a2 + t.fly
                val a4 = a3 + t.warm
                val e = el
                when {
                    e < a1 -> {
                        ph = "squeeze"
                        squeezeScreen(e / a1)
                    }
                    e < a2 -> {
                        ph = "line"
                        p.cab(1f, 1f, PipScreen.Line)
                        p.screenLine = lerp(screenW, 5 * k, easeIn3((e - a1) / t.ln))
                    }
                    e < a3 -> {
                        ph = "dot"
                        val f = (e - a2) / t.fly
                        val out = (f / .55f).coerceIn(0f, 1f)
                        // The set collapses into its own screen (the pop-in reversed) and only
                        // fades in the last fifth, once it is small, so it never hangs as a
                        // see-through box.
                        if (out < 1) {
                            p.cab(
                                1 - .92f * easeIn3(out),
                                1 - ((out - .8f) / .2f).coerceIn(0f, 1f),
                                PipScreen.None,
                            )
                        }
                        flight(mcx, mcy, pcx, pcy, f)
                    }
                    e < a4 -> {
                        ph = "warm"
                        val gq = (e - a3) / t.warm
                        fxLine(pcx, pcy, lerp(5 * k, width, easeOut3(gq)))
                        if (gq < .3f) dot(pcx, pcy, 4 * k, 1 - gq / .3f)
                    }
                    else -> {
                        ph = "open"
                        val hq = (e - a4) / t.open
                        p.picture = true
                        p.pictureCy = pcy
                        p.pictureSy = lerp(.02f, 1f, easeOutBack(hq))
                        p.pictureSx = 1f
                        p.pictureBright = .85f * (1 - hq).pow(1.6f)
                    }
                }
            }
        }
        // A slow frame can jump a short phase; every phase it passed is kept, in order.
        if (ph != phase) {
            val i0 = Phases.indexOf(phase)
            val i1 = Phases.indexOf(ph)
            if (i1 >= 0) for (i in i0 + 1..i1) seen += Phases[i]
            phase = ph
        }
    }

    private fun squeezeScreen(a: Float) {
        pose.cab(1f, 1f, PipScreen.Picture)
        pose.screenSy = lerp(1f, .015f, easeIn3(a))
        pose.screenBright = a.pow(.7f) * .95f
    }

    private fun warmLine(gq: Float) {
        pose.cab(1f, 1f, PipScreen.Line)
        pose.screenLine = lerp(5 * k, screenW, easeOut3(gq))
    }

    private fun openScreen(hq: Float) {
        pose.cab(1f, 1f, PipScreen.Warming)
        pose.screenSy = lerp(.02f, 1f, easeOutBack(hq))
        pose.screenBright = .85f * (1 - hq).pow(1.6f)
        pose.screenNoise = .45f * (1 - hq)
    }

    private fun fxLine(cx: Float, cy: Float, w: Float) {
        pose.line = true
        pose.lineCx = cx
        pose.lineCy = cy
        pose.lineW = w
    }

    private fun dot(x: Float, y: Float, r: Float, a: Float) {
        pose.dot = true
        pose.dotX = x
        pose.dotY = y
        pose.dotR = r
        pose.dotA = a
    }

    private fun flight(fx: Float, fy: Float, tx: Float, ty: Float, f: Float) {
        pose.flight = true
        pose.fromX = fx
        pose.fromY = fy
        pose.toX = tx
        pose.toY = ty
        pose.flightF = f
    }

    // ---- Changes ----

    /** Switches the full player off into the set, as the prototype's `minimize()`. */
    fun minimize(keyboard: Boolean = false) {
        if (mode != PipMode.Full) return
        focusTo = if (keyboard) PipFocus.Set else PipFocus.None
        sy0 = 1 - .55f * pre
        ty0 = preY
        pre = 0f
        preY = 0f
        backAt = -1.0
        ctlUntil = 0.0
        seen.clear()
        posX = cornerX(corner)
        posY = cornerY(corner)
        spring = false
        place()
        wake++
        if (reducedMotion) {
            slotH = 0f
            finish(PipMode.Mini)
            pose()
            return
        }
        mode = PipMode.ToMini
        t0 = now
        phase = ""
        emit(PipEvent.Off, 1f)
        later(ToMiniT.sq + ToMiniT.ln) { emit(PipEvent.Fly) }
        later(ToMiniT.sq + ToMiniT.ln + ToMiniT.fly) { emit(PipEvent.On) }
        pose()
    }

    /** Switches the set off and back into the full player, as the prototype's `expand()`. */
    fun expand(keyboard: Boolean = false) {
        if (mode != PipMode.Mini) return
        spring = false
        velX = 0f
        velY = 0f
        focusTo = if (keyboard) PipFocus.Player else PipFocus.None
        seen.clear()
        wake++
        if (reducedMotion) {
            slotH = fullH
            finish(PipMode.Full)
            pose()
            return
        }
        mode = PipMode.ToFull
        t0 = now
        phase = ""
        emit(PipEvent.Off, .7f)
        later(ToFullT.sq + ToFullT.ln) { emit(PipEvent.Fly) }
        later(ToFullT.sq + ToFullT.ln + ToFullT.fly) { emit(PipEvent.On) }
        pose()
    }

    /**
     * Flicked off a side edge ([dir] -1 is left): playback stops, the player's space stays
     * collapsed, and a toast offers Undo.
     */
    fun stopPlayback(dir: Int, keyboard: Boolean = false) {
        if (mode != PipMode.Mini) return
        val cx = posX + cabW / 2
        // Undo brings it back to the corner on the side it left from, at the height it left.
        val mid = (cornerY("tl") + cornerY("bl")) / 2
        corner = (if (posY < mid) "t" else "b") + (if (dir < 0) "l" else "r")
        // It settles with its screen on the edge it was flicked at, half off, so the switch-off
        // stays in view even if the finger carried it further.
        offX0 = posX
        offX1 = posX + if (dir < 0) -cx else width - cx
        spring = false
        if (keyboard) focusRequest = PipFocus.Undo
        emit(PipEvent.Stop)
        wake++
        if (reducedMotion) {
            finishStop()
            showToast()
            pose()
            return
        }
        mode = PipMode.ToOff
        t0 = now
        emit(PipEvent.Off, .8f)
        later(Off.sq + Off.ln + Off.fold) { showToast() }
        pose()
    }

    private fun finishStop() {
        mode = PipMode.Stopped
        phase = "stopped"
        announce(PipAnnouncement.Stopped)
    }

    private fun showToast() {
        if (mode != PipMode.ToOff && mode != PipMode.Stopped) return
        val tok = ++toastTok
        toastInstant = false
        toastOn = true
        later(ToastMs) { if (tok == toastTok) hideToast(false) }
    }

    private fun hideToast(instant: Boolean) {
        toastTok++
        toastInstant = instant
        toastOn = false
    }

    /** Undo on the toast: the set pops back into the corner it left from and warms up. */
    fun undo(keyboard: Boolean = false) {
        if (mode != PipMode.Stopped && mode != PipMode.ToOff) return
        hideToast(true)
        posX = cornerX(corner)
        posY = cornerY(corner)
        place()
        focusTo = if (keyboard) PipFocus.Set else PipFocus.None
        emit(PipEvent.Undo)
        wake++
        if (reducedMotion) {
            finish(PipMode.Mini)
            pose()
            return
        }
        mode = PipMode.WarmUp
        t0 = now
        emit(PipEvent.On)
        pose()
    }

    private fun finish(m: PipMode) {
        if (mode == PipMode.ToMini || mode == PipMode.ToFull) {
            for (i in Phases.indexOf(phase) + 1 until Phases.size) seen += Phases[i]
        }
        mode = m
        phase = m.name.lowercase()
        if (m == PipMode.Mini) {
            slotH = 0f
            announce(PipAnnouncement.Mini)
        } else {
            slotH = fullH
            announce(PipAnnouncement.Full)
        }
        if (focusTo != PipFocus.None) focusRequest = focusTo
        focusTo = PipFocus.None
    }

    private fun announce(a: PipAnnouncement) {
        announcement = a
        announced++
    }

    /**
     * Back to the full player at rest, the set's corner bottom-right, as the prototype's `reset()`.
     */
    fun reset() {
        timers.clear()
        drag = null
        setDrag = null
        backAt = -1.0
        mode = PipMode.Full
        pre = 0f
        preY = 0f
        ctlUntil = 0.0
        seen.clear()
        phase = "full"
        corner = "br"
        spring = false
        hideToast(true)
        announcement = PipAnnouncement.None
        focusTo = PipFocus.None
        focusRequest = PipFocus.None
        if (width > 0) {
            posX = cornerX("br")
            posY = cornerY("br")
            place()
        }
        slotH = fullH
        pose()
        wake++
        frame++
    }

    /** The next minimize goes to the usual corner again, as the demo leaves it. */
    fun resetCorner() {
        if (mode == PipMode.Full) corner = "br"
    }

    // ---- The player's gesture ----

    /** A finger down on the player at ([x], [y]) dp, at [t] ms. */
    fun playerDown(x: Float, y: Float, t: Double, scripted: Boolean = false) {
        if (mode != PipMode.Full) return
        drag = Drag(x, y, x, y, t).also { it.scripted = scripted }
        wake++
    }

    fun playerMove(x: Float, y: Float, t: Double) {
        val d = drag ?: return
        val dt = max(1.0, t - d.t).toFloat()
        d.v = lerp(d.v, (y - d.y) / dt, .6f)
        d.x = x
        d.y = y
        d.t = t
        val dy = y - d.y0
        if (!d.moved && hypot(x - d.x0, dy) > Slop) d.moved = true
        if (d.moved) {
            val dd = max(0f, dy)
            pre = (dd / 260f).coerceIn(0f, 1f)
            preY = dd * .32f
            backAt = -1.0
        }
    }

    /** The finger lifts: decided from the release alone, how far down it ended or how fast. */
    fun playerUp(x: Float, y: Float, t: Double) {
        if (drag == null) return
        playerMove(x, y, t)
        val d = drag ?: return
        drag = null
        val dy = y - d.y0
        if (!d.moved) {
            ctlUntil = now + 2600
            return
        }
        if (dy > Commit || (d.v > .6f && dy > 24)) {
            emit(PipEvent.Commit)
            minimize()
            return
        }
        springBack()
    }

    /** A cancelled swipe still finishes, from the last point seen. */
    fun playerCancel(t: Double) {
        val d = drag ?: return
        playerUp(d.x, d.y, t)
    }

    private fun springBack() {
        if (reducedMotion) {
            pre = 0f
            preY = 0f
            return
        }
        backAt = now
        backPre = pre
        backY = preY
    }

    // ---- The set's gesture ----

    fun setDown(x: Float, y: Float, t: Double, scripted: Boolean = false) {
        if (mode != PipMode.Mini) return
        setDrag = SetDrag(x, y, x, y, t, t, posX, posY).also { it.scripted = scripted }
        spring = false
        velX = 0f
        velY = 0f
        wake++
    }

    fun setMove(x: Float, y: Float, t: Double) {
        val d = setDrag ?: return
        val dt = max(1.0, t - d.t).toFloat()
        // A long gap between samples means the finger rested, so that sample counts fully and a
        // pause kills the fling.
        val w = (dt / 50f).coerceIn(.6f, 1f)
        d.vx = lerp(d.vx, (x - d.x) / dt, w)
        d.vy = lerp(d.vy, (y - d.y) / dt, w)
        d.x = x
        d.y = y
        d.t = t
        d.n++
        if (!d.moved && hypot(x - d.x0, y - d.y0) > Slop) d.moved = true
        if (!d.moved) return
        // Free sideways (so it can be flicked off); top and bottom rubber-band past the corners.
        val top = cornerY("tl")
        val bot = cornerY("bl")
        val v = d.py + y - d.y0
        posX = d.px + x - d.x0
        posY =
            when {
                v < top -> top - (top - v) * .35f
                v > bot -> bot + (v - bot) * .35f
                else -> v
            }
        place()
    }

    /**
     * The finger lifts off the set: a tap expands it; a drag springs it to the corner it was
     * heading for, or stops playback if it was flicked off a side edge.
     */
    fun setUp(x: Float, y: Float, t: Double) {
        if (setDrag == null) return
        setMove(x, y, t)
        val d = setDrag ?: return
        setDrag = null
        if (!d.moved) {
            expand()
            return
        }
        // Too few samples to trust the running velocity: the whole gesture's.
        if (d.n < 3) {
            val tt = max(16.0, t - d.t0).toFloat()
            d.vx = (x - d.x0) / tt
            d.vy = (y - d.y0) / tt
        }
        val cx = posX + cabW / 2
        val cy = posY + cabH / 2
        val px = cx + d.vx * 180
        val py = cy + d.vy * 120
        if ((px < -cabW * .1f || px > width + cabW * .1f) && abs(d.vx) >= abs(d.vy) * .6f) {
            stopPlayback(if (px < 0) -1 else 1)
            return
        }
        val mid = (cornerY("tl") + cornerY("bl")) / 2 + cabH / 2
        corner = (if (py < mid) "t" else "b") + (if (px < width / 2) "l" else "r")
        if (reducedMotion) {
            posX = cornerX(corner)
            posY = cornerY(corner)
            place()
            emit(PipEvent.Snap)
            return
        }
        velX = d.vx * 1000
        velY = d.vy * 1000
        spring = true
        wake++
    }

    fun setCancel(t: Double) {
        val d = setDrag ?: return
        setUp(d.x, d.y, t)
    }

    /**
     * The script stopped: a drag it started and never released would leave the picture squeezed or
     * the set held, so it lets go of them.
     */
    fun releaseScripted() {
        if (drag?.scripted == true) {
            drag = null
            springBack()
        }
        if (setDrag?.scripted == true) {
            setDrag = null
            if (mode == PipMode.Mini) spring = true
        }
    }

    // ---- Keys ----

    /** Delete or Backspace on the set: stopped toward the side it sits on. */
    fun stopFromKeys() = stopPlayback(if (posX + cabW / 2 < width / 2) -1 else 1, keyboard = true)

    private fun later(ms: Float, fn: () -> Unit) {
        timers += Timer(now + ms, fn)
        wake++
    }

    private fun emit(e: PipEvent, strength: Float = 1f) {
        onEvent?.invoke(e, strength)
    }

    /** The phases of a change, and their lengths in ms. */
    internal class Timeline(
        val sq: Float,
        val ln: Float,
        val fly: Float,
        val warm: Float,
        val open: Float,
    ) {
        val total
            get() = sq + ln + fly + warm + open
    }

    internal object Off {
        const val sq = 150f
        const val ln = 110f
        const val fold = 230f
        const val glow = 900f
    }

    internal object Wu {
        const val pop = 240f
        const val warm = 130f
        const val open = 230f
    }

    internal companion object {
        // To mini: squeeze, line, dot flight, warm-up line, open. To full: the same, mirrored.
        val ToMiniT = Timeline(190f, 150f, 420f, 130f, 230f)
        val ToFullT = Timeline(150f, 110f, 420f, 150f, 240f)
        const val ToastMs = 4000f
        const val SlotMs = 440f
        const val BackMs = 240.0
        const val Slop = 8f
        const val Commit = 72f
        const val Antenna = 27f

        /** The show runs 18:40 and opens 4:12 in. */
        const val Duration = 1120f
        const val StartAt = 252f
        const val SpringK = 420f
        val SpringC = 2 * .72f * sqrt(420f)
        val Phases = listOf("squeeze", "line", "dot", "warm", "open")

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun easeIn3(t: Float) = t * t * t

        fun clamp01(t: Float) = t.coerceIn(0f, 1f)

        fun easeOut3(t: Float) = 1 - (1 - t).pow(3)

        fun easeOutBack(t: Float): Float {
            val c = 1.9f
            return 1 + (c + 1) * (t - 1).pow(3) + c * (t - 1).pow(2)
        }

        /** The quadratic Bézier the dot flies on, as the prototype's `bez`. */
        fun bez(a: Float, c: Float, b: Float, t: Float) =
            (1 - t) * (1 - t) * a + 2 * (1 - t) * t * c + t * t * b
    }
}
