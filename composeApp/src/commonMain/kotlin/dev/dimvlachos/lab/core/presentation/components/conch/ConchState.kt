package dev.dimvlachos.lab.core.presentation.components.conch

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** What the conch is doing: at rest in the composer, recording, sinking back, or flying to the chat. */
internal enum class ConchMode {
    Idle,
    Rec,
    Back,
    Fly,
}

/** Something the conch does that is heard, felt or announced. */
internal enum class ConchEvent {
    /** Held: it lifts and starts recording. */
    Start,
    /** Slid far enough left that letting go cancels. */
    Arm,
    /** Slid back, so letting go sends after all. */
    Disarm,
    /** Let go: the note is sent and the shell flies into the chat. */
    Sent,
    /** The flying shell lands in its bubble. */
    Landed,
    /** Let go while armed (or Escape): the note is thrown away in a puff of sand. */
    Canceled,
    /** A tap too quick to record: the hint shows. */
    Hint,
}

/**
 * A voice note in the chat: its voice [levels] (0 to 1, one every 110 ms), how long it is in [ms],
 * whose it is, and when it was sent. [pos] is how far it has played, 0 to 1, and [bounce] its
 * shell's wobble; both change every frame and are read as the shell is drawn.
 */
@Stable
internal class VoiceNote(
    val levels: FloatArray,
    val ms: Float,
    val mine: Boolean,
    val at: String,
) {
    /** Playing now. */
    var playing by mutableStateOf(false)
        internal set

    /** Kept out of sight while its shell is still flying in. */
    var hidden by mutableStateOf(false)
        internal set

    /** The seconds its label shows: the length, or what is left while it plays. */
    var shownSeconds by mutableIntStateOf(seconds(ms))
        internal set

    var pos = 0f
        internal set

    var bounce = 0f
        internal set

    internal var bounceV = 0f
    internal var t0 = 0f

    /** Its voice level [p] of the way through, between the stored samples. */
    fun levelAt(p: Float): Float {
        val l = levels
        if (l.isEmpty()) return 0f
        val f = p.coerceIn(0f, .9999f) * (l.size - 1)
        val i = floor(f).toInt()
        return lerp(l[i], l[min(l.size - 1, i + 1)], f - i)
    }
}

/** A ring spreading from the shell's mouth: when it started and how loud the voice was. */
internal class ConchRing(val t0: Float, val a: Float)

/** A grain of sand thrown up as a note is canceled. */
internal class ConchPuff(
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    val t0: Float,
    val life: Float,
    val r: Float,
)

/** The shell's flight from where it was to its bubble in the chat. */
internal class ConchFlight(
    val t0: Float,
    val ms: Float,
    val x0: Float,
    val y0: Float,
    val s0: Float,
    val note: VoiceNote,
)

/**
 * The conch voice note, ported from the prototype's `st`, `step()` and `tick()`: the shell's lift
 * out of the composer, its slide toward cancel, its regrowth after a send, the rings and the sand,
 * the flight into the chat, the voice notes and their playback. The voice is simulated, the
 * prototype's made-up syllables, words and pauses; nothing listens to a microphone.
 *
 * Everything is in dp, in the coordinates of the screen the composer sits in, which [layout]
 * gives. [time] is the conch's own clock in ms, which [step] advances.
 */
@Stable
internal class ConchState(
    /** When a sent note says it was sent. */
    private val sentAt: String = "5:21 PM",
) {
    var reducedMotion = false

    /** Hears what the conch does. */
    var onEvent: ((ConchEvent, VoiceNote?) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something starts moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    var mode by mutableStateOf(ConchMode.Idle)
        private set

    /** Slid far enough to cancel. */
    var armed by mutableStateOf(false)
        private set

    /** The hint "Hold the shell to record" is showing. */
    var hint by mutableStateOf(false)
        private set

    /** The recording's length so far, in whole seconds, as the composer shows it. */
    var recSeconds by mutableIntStateOf(0)
        private set

    /** The note playing now, if any. */
    var playing by mutableStateOf<VoiceNote?>(null)
        private set

    /** Every voice note in the chat, oldest first. */
    val notes = mutableStateListOf<VoiceNote>()

    var time = 0f
        private set

    var lift = 0f
        private set

    var off = 0f
        private set

    var dragX = 0f
        private set

    var level = 0f
        private set

    /** The voice level, smoothed: what the glow and the rings follow. */
    var levelS = 0f
        private set

    var regrow = 1f
        private set

    var fly: ConchFlight? = null
        private set

    val rings = ArrayList<ConchRing>()
    val puffs = ArrayList<ConchPuff>()

    var sent = 0
        private set

    var cancels = 0
        private set

    // The flying note's shell, where the chat has laid it out: the centre of its 40 × 44 canvas.
    private var targetX = Float.NaN
    private var targetY = Float.NaN

    /** The playing note's voice level now, for the hum's loudness. */
    var humLevel = 0f
        private set

    private var liftV = 0f
    private var offV = 0f
    private var regrowV = 0f
    private var recStart = 0f
    private val levels = ArrayList<Float>()
    private var lastSample = 0f
    private var lastRing = 0f
    private var hintUntil = 0f
    private var seed = 7L

    // ---- Layout ----

    /** The screen's width. */
    var width = 0f
        private set

    private var slotX = 0f
    private var slotY = 0f

    /** The top of the composer, which the lifted shell's shadow and rings stay above. */
    var composerTop = 0f
        private set

    /** The screen is [width] wide, the conch button is centred at ([buttonX], [buttonY]), the composer starts at [top]. */
    fun layout(width: Float, buttonX: Float, buttonY: Float, top: Float) {
        if (width == this.width && buttonX == slotX && buttonY + 1f == slotY && top == composerTop) return
        this.width = width
        slotX = buttonX
        slotY = buttonY + 1f
        composerTop = top
        wake++
        frame++
    }

    /** The flying note's shell is centred at ([x], [y]) in its bubble. */
    fun flyTarget(x: Float, y: Float) {
        targetX = x
        targetY = y + 1f
    }

    val slotCenterX get() = slotX
    val slotCenterY get() = slotY
    val slotSize get() = SlotSize

    /** The lifted shell's size, as big as the stage gives it. */
    val stageSize: Float
        get() = (width * .5f).coerceIn(130f, 190f)

    val stageX: Float
        get() = width * .5f

    val stageY: Float
        get() = composerTop - stageSize * .62f - 22f

    /** How far left the slide must go to cancel. */
    val cancelDistance: Float
        get() = (width * .3f).coerceIn(90f, 120f)

    // Where the big shell is now: lifted between the slot and the stage, plus the slide.
    var shellX = 0f
        private set

    var shellY = 0f
        private set

    var shellS = 0f
        private set

    var shellP = 0f
        private set

    private fun shellNow() {
        val p = lift.coerceIn(-.1f, 1.1f)
        shellX = lerp(slotX, stageX, p) + off
        shellY = lerp(slotY, stageY, p)
        shellS = lerp(SlotSize, stageSize, p.coerceIn(0f, 1.1f))
        shellP = p
    }

    /** Where the flight is now, [flyX], [flyY] at [flyS], [flyP] of the way (eased). */
    var flyX = 0f
        private set

    var flyY = 0f
        private set

    var flyS = 0f
        private set

    var flyP = 0f
        private set

    private fun flyNow() {
        val f = fly ?: return
        val p = ease(((time - f.t0) / f.ms).coerceIn(0f, 1f))
        val tx = if (targetX.isNaN()) slotX else targetX
        val ty = if (targetY.isNaN()) slotY else targetY
        val arc = sin(p * PI.toFloat()) * -40f
        flyX = lerp(f.x0, tx, p)
        flyY = lerp(f.y0, ty, p) + arc
        flyS = lerp(f.s0, FlySize, p)
        flyP = p
    }

    /** How cool (grey-blue) the shell has turned toward cancel, 0 to 1. */
    val cool: Float
        get() =
            if (mode != ConchMode.Rec) 0f
            else (-dragX / cancelDistance).coerceIn(0f, 1f) * (-off / 20f).coerceIn(0f, 1f)

    // ---- Actions ----

    /** Back to the start: no notes but Maya's, at rest, the random numbers from their seed. */
    fun reset() {
        playing?.let { stopPlay(it, false) }
        notes.clear()
        seed = 7L
        val l = FloatArray(40) { (simLevel(it * .11f + 3f) * .9f + .08f).coerceIn(0f, 1f) }
        notes += VoiceNote(l, 4400f, mine = false, at = "5:20 PM")
        mode = ConchMode.Idle
        lift = 0f
        liftV = 0f
        off = 0f
        offV = 0f
        dragX = 0f
        armed = false
        levelS = 0f
        rings.clear()
        puffs.clear()
        fly = null
        regrow = 1f
        regrowV = 0f
        hint = false
        hintUntil = 0f
        targetX = Float.NaN
        targetY = Float.NaN
        wake++
        frame++
    }

    /** Held down: the shell lifts and records. */
    fun startRec() {
        if (mode == ConchMode.Rec || mode == ConchMode.Fly) return
        mode = ConchMode.Rec
        recStart = time
        recSeconds = 0
        levels.clear()
        lastSample = time
        armed = false
        dragX = 0f
        rings.clear()
        hint = false
        hintUntil = 0f
        playing?.let { stopPlay(it, false) }
        wake++
        emit(ConchEvent.Start)
    }

    /** The finger is [dx] dp right of where it went down (negative is left). */
    fun dragTo(dx: Float) {
        if (mode != ConchMode.Rec) return
        dragX = dx
        val now = dx < -cancelDistance
        if (now != armed) {
            armed = now
            emit(if (now) ConchEvent.Arm else ConchEvent.Disarm)
        }
    }

    /** Let go: cancels if armed, shows the hint if it was only a tap, otherwise sends. */
    fun finishRec() {
        if (mode != ConchMode.Rec) return
        val ms = time - recStart
        if (armed) {
            cancelRec()
            return
        }
        if (ms < 450f) {
            mode = ConchMode.Back
            hint = true
            hintUntil = time + 1800f
            emit(ConchEvent.Hint)
            return
        }
        send(ms)
    }

    /** Throws the recording away: the shell sinks back with a puff of sand. */
    fun cancelRec() {
        if (mode != ConchMode.Rec) return
        mode = ConchMode.Back
        armed = false
        cancels++
        repeat(14) {
            puffs +=
                ConchPuff(
                    x = slotX + (rnd() - .5f) * 20f,
                    y = slotY + 8f,
                    vx = (rnd() - .5f) * 90f,
                    vy = -40f - rnd() * 70f,
                    t0 = time,
                    life = 600f + rnd() * 400f,
                    r = 1f + rnd() * 2f,
                )
        }
        wake++
        emit(ConchEvent.Canceled)
    }

    private fun send(ms: Float) {
        val lv = if (levels.isEmpty()) floatArrayOf(.3f, .5f, .4f) else levels.toFloatArray()
        val note = VoiceNote(lv, ms, mine = true, at = sentAt)
        note.hidden = true
        notes += note
        shellNow()
        targetX = Float.NaN
        targetY = Float.NaN
        fly = ConchFlight(time, if (reducedMotion) 1f else 560f, shellX, shellY, shellS, note)
        flyNow()
        mode = ConchMode.Fly
        sent++
        rings.clear()
        wake++
        emit(ConchEvent.Sent, note)
    }

    /** Plays [note], or pauses it if it is playing; any other note stops. */
    fun togglePlay(note: VoiceNote) {
        playing?.let { if (it !== note) stopPlay(it, false) }
        if (note.playing) {
            stopPlay(note, false)
            return
        }
        if (note.pos >= .999f) note.pos = 0f
        note.t0 = time - note.pos * note.ms
        note.playing = true
        playing = note
        note.bounceV += 4f
        wake++
    }

    private fun stopPlay(note: VoiceNote, ended: Boolean) {
        note.playing = false
        if (playing === note) {
            playing = null
            humLevel = 0f
        }
        if (ended) {
            note.pos = 0f
            note.shownSeconds = seconds(note.ms)
        }
    }

    /** Takes the notes sent here back out of the chat, leaving Maya's. */
    fun removeSent() {
        playing?.let { if (it.mine) stopPlay(it, false) }
        notes.removeAll { it.mine }
        wake++
    }

    // ---- Frames ----

    /**
     * Whether nothing would move on the next frame: at rest in the slot, no rings, sand, flight,
     * playback or hint, so the frame loop can sleep until [wake] changes.
     */
    val settled: Boolean
        get() =
            mode == ConchMode.Idle &&
                fly == null &&
                playing == null &&
                !hint &&
                rings.isEmpty() &&
                puffs.isEmpty() &&
                abs(regrow - 1f) < Rest &&
                abs(regrowV) < Rest &&
                abs(off) < Rest &&
                abs(offV) < Rest &&
                levelS < Rest &&
                notesAtRest()

    private fun notesAtRest(): Boolean {
        for (i in notes.indices) {
            val n = notes[i]
            if (abs(n.bounce) >= Rest || abs(n.bounceV) >= Rest) return false
        }
        return true
    }

    /** Advances [seconds]: the springs in small fixed substeps, then the rest once. */
    fun step(seconds: Float) {
        time += seconds * 1000f
        var left = seconds
        while (left > 0f) {
            val h = min(left, Substep)
            substep(h)
            left -= h
        }
        tick(seconds)
        shellNow()
        flyNow()
        frame++
    }

    private fun substep(dt: Float) {
        val rec = mode == ConchMode.Rec
        // The shell follows the slide, but never past the left edge of the screen.
        val offT =
            if (rec) (dragX * .55f).coerceIn(-max(0f, width * .5f - stageSize * .42f), 0f) else 0f
        if (reducedMotion) {
            lift = if (rec) 1f else 0f
            liftV = 0f
            off = offT
            offV = 0f
        } else {
            spring(lift, liftV, if (rec) 1f else 0f, if (rec) 170f else 210f, if (rec) .62f else .8f, dt)
            lift = sx
            liftV = sv
            spring(off, offV, offT, 300f, .8f, dt)
            off = sx
            offV = sv
        }
        spring(regrow, regrowV, 1f, 220f, .55f, dt)
        regrow = sx
        regrowV = sv
        if (mode == ConchMode.Back && lift < .02f) {
            mode = ConchMode.Idle
            lift = 0f
        }
        for (i in notes.indices) {
            val n = notes[i]
            spring(n.bounce, n.bounceV, 0f, 260f, .25f, dt)
            n.bounce = sx
            n.bounceV = sv
        }
    }

    private fun tick(dt: Float) {
        if (mode == ConchMode.Rec) {
            level = simLevel((time - recStart) / 1000f)
            levelS += (level - levelS) * min(1f, dt * 16f)
            if (time - lastSample >= 110f) {
                levels += levelS
                lastSample = time
            }
            recSeconds = seconds(time - recStart)
            if (
                !reducedMotion &&
                    levelS > .22f &&
                    time - lastRing > 300f - levelS * 140f &&
                    !armed
            ) {
                rings += ConchRing(time, levelS)
                lastRing = time
            }
        } else {
            levelS += (0f - levelS) * min(1f, dt * 8f)
            if (levelS < 1e-4f) levelS = 0f
        }
        for (i in rings.indices.reversed()) if (time - rings[i].t0 > 1300f) rings.removeAt(i)
        for (i in puffs.indices.reversed()) {
            val p = puffs[i]
            p.vy += 160f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (time - p.t0 > p.life) puffs.removeAt(i)
        }
        fly?.let { f ->
            if ((time - f.t0) / f.ms >= 1f) {
                f.note.hidden = false
                f.note.bounceV += 7f
                fly = null
                mode = ConchMode.Idle
                lift = 0f
                liftV = 0f
                off = 0f
                offV = 0f
                regrow = if (reducedMotion) 1f else 0f
                regrowV = 0f
                emit(ConchEvent.Landed, f.note)
            }
        }
        playing?.let { n ->
            val p = (time - n.t0) / n.ms
            n.pos = p.coerceIn(0f, 1f)
            n.shownSeconds = seconds(n.ms * (1f - n.pos))
            humLevel = n.levelAt(n.pos)
            if (p >= 1f) stopPlay(n, true)
        }
        if (hint && time > hintUntil) hint = false
    }

    /** The prototype's made-up voice: syllables, words and short pauses. */
    private fun simLevel(t: Float): Float {
        val syl = max(0f, sin(t * 13.1f + sin(t * 3.7f) * 2.2f))
        val word = .55f + .45f * sin(t * 2.3f + 1f)
        val pause = if (sin(t * .85f) > -.55f) 1f else .08f
        return (syl.pow(.7f) * word * pause + (rnd() - .5f) * .06f).coerceIn(0f, 1f)
    }

    // The prototype's seeded random numbers (Park–Miller), so the voice is the same every time.
    private fun rnd(): Float {
        seed = (seed * 16807L) % 2147483647L
        return (seed.toDouble() / 2147483647.0).toFloat()
    }

    private fun emit(event: ConchEvent, note: VoiceNote? = null) {
        onEvent?.invoke(event, note)
    }

    // One semi-implicit Euler step of a damped spring, its result in sx and sv, so a frame
    // allocates nothing.
    private var sx = 0f
    private var sv = 0f

    private fun spring(x: Float, v: Float, target: Float, k: Float, z: Float, dt: Float) {
        val c = 2f * sqrt(k) * z
        sv = v + (k * (target - x) - c * v) * dt
        sx = x + sv * dt
    }

    internal companion object {
        /** The small shell in the composer button. */
        const val SlotSize = 38f

        /** The shell in a voice-note bubble. */
        const val FlySize = 34f

        private const val Substep = 1f / 240f
        private const val Rest = .002f
    }
}

/** A length in ms as the prototype rounds it, to whole seconds. */
internal fun seconds(ms: Float): Int = max(0, (ms / 1000f).roundToInt())

/** Whole seconds as m:ss. */
internal fun clockText(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

internal fun ease(t: Float) = if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
