package dev.dimvlachos.lab.core.presentation.components.stickynote

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/** A reminder written on a note: its [title], the [meta] line under it, and its paper [color]. */
internal class Reminder(val title: String, val meta: String, val color: Int)

/** Where a note goes: left to the Done pad, archived, or right to the Later pad, snoozed. */
internal enum class NoteAction {
    Archive,
    Snooze;

    val sign: Float
        get() = if (this == Archive) -1f else 1f
}

/** Something the notes do that is heard, felt or announced. */
internal enum class StickyNoteEvent {
    /** The paper crackles as it peels; the value is how fast, 0 to 1. */
    Grain,
    /** The pull crossed the line where letting go commits, either way. */
    Tick,
    /** A note came off. */
    Rip,
    /** A note landed on a pad. */
    Stick,
    /** A note pressed back flat. */
    Pat,
    /** A pad's top note lifted off to go back. */
    Lift,
    /** A tapped note lifted its corner. */
    Peek,
    /** A note was archived; the event carries it. */
    Archived,
    /** A note was snoozed; the event carries it. */
    Snoozed,
    /** A note was put back; the event carries it. */
    PutBack,
}

/** A row's state, as the prototype's `row.state`. */
internal enum class RowState {
    Rest,
    Drag,
    Return,
    Tween,
    Gone,
}

/** A note in the list: where it is ([y], gliding to [ty]), how far it is peeled ([dx]). */
internal class NoteRow(val item: Reminder, y: Float) {
    var y = y
    var ty = y
    var dx = 0f
    var vx = 0f
    var state = RowState.Rest
    internal var anim: Tween? = null
    var flut = 0f
    var ft = 0f
    var hidden = false

    /** How much the flutter lifts the note now, as the prototype's `flut * |cos(ft * 17)|`. */
    val lift: Float
        get() = flut * abs(cos(ft * 17f))
}

/** A timed move of a row's [NoteRow.dx]: eased [from] → [to], or the peek's lift and drop. */
internal class Tween(
    val dur: Float,
    val from: Float,
    val to: Float,
    val peek: Boolean,
    val done: () -> Unit,
) {
    var t = 0f
}

/** A note on a pad: turned by [rot], off centre by [ox], and the list index it came from. */
internal class PadNote(val item: Reminder, val rot: Float, val ox: Float, val idx: Int)

/** A pad: its notes, newest last, and how much it bumps from a landing. */
internal class Pad {
    val stack = mutableListOf<PadNote>()
    var bump = 0f
}

/** A note in the air: out to a pad, or in from one back to its [row]. */
internal class Flight(
    val item: Reminder,
    val kind: NoteAction,
    val out: Boolean,
    val dur: Float,
    val y0: Float,
    val dx0: Float,
    val rot: Float,
    val ox: Float,
    val idx: Int,
    val row: NoteRow? = null,
) {
    var t = 0f
}

/** An action panel left behind by a note that came off, fading. */
internal class Ghost(val kind: NoteAction, val y: Float) {
    var a = 1f
}

/** Where a flying note is: its centre, scale, turn, peel, lift, and the bounds it was kept in. */
internal class FlightPose {
    var cx = 0f
    var cy = 0f
    var s = 1f
    var rot = 0f
    var dx = 0f
    var lift = 0f
    var hw = 0f
    var lo = 0f
    var hi = 0f
}

/**
 * The reminders list as the prototype's model: rows of sticky notes, the pads they land on, the
 * notes in flight and the action panels fading where a note came off. Ported from its `layout()`,
 * `update()`, gesture, `commit`/`launch`/`land`/`undo`/`peek` and `flightPose()`; units are the
 * prototype's CSS px, which are dp here, and its times are in ms.
 *
 * [step] advances it in substeps; [frame] is bumped by every step and every change, so a drawing
 * that reads it redraws.
 */
@Stable
internal class StickyNoteState(
    val items: List<Reminder>,
    private val random: Random = Random.Default,
) {
    var reducedMotion = false

    /** Hears what the notes do: the event, the note it is about, if any, and an amount. */
    var onEvent: ((StickyNoteEvent, Reminder?, Float) -> Unit)? = null

    var frame by mutableIntStateOf(0)
        private set

    /** Bumped when the rows or the pads' stacks change, for what lists them. */
    var listVersion by mutableIntStateOf(0)
        private set

    // ---- Layout, as the prototype's layout() ----
    var width = 0f
        private set

    var height = 0f
        private set

    var top0 = 80f
        private set

    var mx = 16f
        private set

    var noteW = 300f
        private set

    var rh = 72f
        private set

    var gap = 10f
        private set

    var pw = 140f
        private set

    var ph = 30f
        private set

    var rows = mutableListOf<NoteRow>()
        private set

    val flights = mutableListOf<Flight>()
    val ghosts = mutableListOf<Ghost>()
    val archive = Pad()
    val snooze = Pad()

    fun pad(kind: NoteAction) = if (kind == NoteAction.Archive) archive else snooze

    private var laidOut = false

    init {
        reset()
    }

    fun slotY(i: Int) = top0 + i * (rh + gap)

    /**
     * Lays the list out in a [w] by [h] screen whose list starts at [top]: the app bar's foot plus
     * 10. Tall screens grow the rows up to 96 so the list sits 40 above the pad labels.
     */
    fun layout(w: Float, h: Float, top: Float) {
        if (laidOut && w == width && h == height && top == top0) return
        laidOut = true
        width = w
        height = h
        top0 = top
        mx = 16f
        noteW = w - mx * 2
        val avail = h - top0 - 100
        val n = items.size
        pw = min(150f, (noteW - 24) / 2)
        val fit = (h - top0 - 26 - 20 - 40 - (n - 1) * 12) / (n + pw / noteW)
        rh = max(((avail - 50) / 6).coerceIn(54f, 80f), min(96f, fit))
        gap = (rh * .15f).coerceIn(8f, 12f)
        ph = pw * rh / noteW
        rows.forEachIndexed { i, r ->
            r.ty = slotY(i)
            r.y = r.ty
        }
        changed()
    }

    // ---- Pads ----

    /** Where [kind]'s pad is: 60 under the full list, or 26 off the foot of a short screen. */
    fun padX(kind: NoteAction) = if (kind == NoteAction.Archive) mx else width - mx - pw

    fun padY() = min(height - 26 - ph, slotY(items.size) - gap + 60)

    fun padCenterX(kind: NoteAction) = padX(kind) + pw / 2

    fun padCenterY() = padY() + ph / 2

    /** The slot the next note lands on, or the top note leaves from: a pad shows 4, so k ≤ 3. */
    fun padTopY(kind: NoteAction): Float {
        val n = min(pad(kind).stack.size, 3)
        return padY() + ph / 2 - n * 2.4f
    }

    fun padScale() = pw / noteW

    // Half the width of a note's bounding box at scale [s], turned by [r].
    private fun halfW(s: Float, r: Float) = (noteW * cos(r) + rh * abs(sin(r))) * s / 2

    /** Where flight [f] is at eased progress [e], into [out]: the prototype's `flightPose()`. */
    fun flightPose(f: Flight, e: Float, out: FlightPose = FlightPose()): FlightPose {
        val slotX = mx + noteW / 2
        val slotYc = (f.row?.y ?: f.y0) + rh / 2
        val padXc = padCenterX(f.kind) + f.ox
        val padYc = padTopY(f.kind)
        val padS = padScale()
        val ax: Float
        val ay: Float
        val aS: Float
        val aRot: Float
        val bx: Float
        val by: Float
        val bS: Float
        val bRot: Float
        val rot: Float
        var dx = 0f
        if (f.out) {
            ax = mx + noteW / 2
            ay = f.y0 + rh / 2
            aS = 1f
            aRot = 0f
            bx = padXc
            by = padYc
            bS = padS
            bRot = f.rot
            rot = f.rot * e
            dx = f.dx0 * (1 - (e / .38f).coerceIn(0f, 1f))
        } else {
            ax = padXc
            ay = padYc
            aS = padS
            aRot = f.rot
            bx = slotX
            by = slotYc
            bS = 1f
            bRot = 0f
            rot = f.rot * (1 - e)
        }
        // Pad notes are drawn with lift .2, so the flight starts or ends on that shadow.
        val lift =
            sin(PI.toFloat() * e) * 2.4f + if (f.out) (1 - e) * .8f + e * .2f else (1 - e) * .2f
        val cxp = (ax + bx) / 2 + if (f.kind == NoteAction.Archive) -30f else 30f
        val cyp = min(ay, by) - 60
        val u = 1 - e
        var cx = u * u * ax + 2 * u * e * cxp + e * e * bx
        val cy = u * u * ay + 2 * u * e * cyp + e * e * by
        val s = (aS + (bS - aS) * e) * (1 + sin(PI.toFloat() * e) * .04f)
        // Keep the note inside the 16 side margins; an end that already sits past one (a pad
        // note's offset) eases the bound out to it.
        val la = min(0f, ax - halfW(aS, aRot) - mx)
        val lb = min(0f, bx - halfW(bS, bRot) - mx)
        val ra = max(0f, ax + halfW(aS, aRot) - (width - mx))
        val rb = max(0f, bx + halfW(bS, bRot) - (width - mx))
        val lo = mx + la + (lb - la) * e
        val hi = width - mx + ra + (rb - ra) * e
        val hw = halfW(s, rot)
        cx = if (hi - lo <= 2 * hw) (lo + hi) / 2 else cx.coerceIn(lo + hw, hi - hw)
        out.cx = cx
        out.cy = cy
        out.s = s
        out.rot = rot
        out.dx = dx
        out.lift = lift
        out.hw = hw
        out.lo = lo
        out.hi = hi
        return out
    }

    // ---- Model ----

    private fun newRow(item: Reminder, i: Int) = NoteRow(item, slotY(i))

    /** Every note back in the list in order, the pads empty, nothing in the air. */
    fun reset() {
        rows = items.mapIndexed { i, it -> newRow(it, i) }.toMutableList()
        flights.clear()
        ghosts.clear()
        gest = null
        archive.stack.clear()
        snooze.stack.clear()
        archive.bump = 0f
        snooze.bump = 0f
        listChanged()
    }

    /** Whether nothing moves: the frame loop can sleep. */
    val settled: Boolean
        get() =
            flights.isEmpty() &&
                ghosts.isEmpty() &&
                archive.bump == 0f &&
                snooze.bump == 0f &&
                rows.all {
                    it.state != RowState.Return &&
                        it.state != RowState.Tween &&
                        it.flut == 0f &&
                        it.y == it.ty
                }

    private fun retarget() = rows.forEachIndexed { i, r -> r.ty = slotY(i) }

    /** Takes [row] off toward [kind]'s pad: the peel runs on to 62 %, then it flies. */
    fun commit(row: NoteRow, kind: NoteAction, fromKey: Boolean = false) {
        if (row.state == RowState.Tween || row.state == RowState.Gone) return
        if (row !in rows) return
        val to = kind.sign * noteW * .62f
        if (reducedMotion) {
            row.dx = to
            launch(row, kind)
            return
        }
        row.state = RowState.Tween
        row.anim =
            Tween(if (fromKey) 240f else 150f, row.dx, to, peek = false) { launch(row, kind) }
        changed()
    }

    private fun launch(row: NoteRow, kind: NoteAction) {
        val idx = rows.indexOf(row)
        if (idx < 0) return
        row.state = RowState.Gone
        rows.removeAt(idx)
        retarget()
        if (!reducedMotion) ghosts += Ghost(kind, row.y)
        val f =
            Flight(
                row.item,
                kind,
                out = true,
                dur = 640f,
                y0 = row.y,
                dx0 = row.dx,
                rot = kind.sign * (.02f + random.nextFloat() * .03f),
                ox = -5f + random.nextFloat() * 10f,
                idx = idx,
            )
        emit(StickyNoteEvent.Rip)
        emit(
            if (kind == NoteAction.Archive) StickyNoteEvent.Archived else StickyNoteEvent.Snoozed,
            row.item,
        )
        listChanged()
        if (reducedMotion) land(f) else flights += f
    }

    private fun land(f: Flight) {
        if (f.out) {
            val pad = pad(f.kind)
            pad.stack += PadNote(f.item, f.rot, f.ox, f.idx)
            pad.bump = if (reducedMotion) 0f else 1f
            emit(StickyNoteEvent.Stick)
            listChanged()
        } else {
            val row = f.row ?: return
            row.hidden = false
            row.flut = if (reducedMotion) 0f else .9f
            row.ft = 0f
            emit(StickyNoteEvent.Pat)
            listChanged()
        }
    }

    /** Puts [kind]'s top note back where it came from; an empty pad only bumps. */
    fun undo(kind: NoteAction) {
        val pd = pad(kind)
        if (pd.stack.isEmpty()) {
            pd.bump = if (reducedMotion) 0f else .6f
            changed()
            return
        }
        val top = pd.stack.removeAt(pd.stack.size - 1)
        val idx = min(top.idx, rows.size)
        val row = newRow(top.item, idx).also { it.hidden = true }
        rows.add(idx, row)
        retarget()
        pd.bump = if (reducedMotion) 0f else .5f
        emit(StickyNoteEvent.PutBack, top.item)
        listChanged()
        val f =
            Flight(
                top.item,
                kind,
                out = false,
                dur = 560f,
                y0 = row.y,
                dx0 = 0f,
                rot = top.rot,
                ox = top.ox,
                idx = idx,
                row = row,
            )
        if (reducedMotion) {
            land(f)
            return
        }
        emit(StickyNoteEvent.Lift)
        flights += f
    }

    /** A tapped note lifts its corner a little and lays it down, to show that it peels. */
    fun peek(row: NoteRow) {
        if (row.state != RowState.Rest) return
        emit(StickyNoteEvent.Peek)
        if (reducedMotion) return
        row.state = RowState.Tween
        row.anim =
            Tween(460f, 0f, 0f, peek = true) {
                row.state = RowState.Rest
                row.dx = 0f
                row.flut = .5f
                row.ft = 0f
            }
        changed()
    }

    /** Advances everything by [dt] seconds, in substeps of at most 1/240 s. */
    fun step(dt: Float) {
        var left = dt * 1000f
        while (left > 0f) {
            val h = min(left, SubstepMs)
            update(h)
            left -= h
        }
        changed()
    }

    // The prototype's update(dt), dt in ms.
    private fun update(dt: Float) {
        val s = dt / 1000f
        // A row can come off in a tween's done(), so the walk steps on only past a row still there.
        var i = 0
        while (i < rows.size) {
            val r = rows[i]
            if (reducedMotion) r.y = r.ty else r.y += (r.ty - r.y) * (1 - exp(-s * 15))
            if (abs(r.ty - r.y) < .01f) r.y = r.ty
            if (r.state == RowState.Return) {
                val sign = sign(r.dx)
                r.vx += (-260 * r.dx - 26 * r.vx) * s
                r.dx += r.vx * s
                if (sign == 0f || sign(r.dx) != sign || abs(r.dx) < .4f) {
                    r.dx = 0f
                    r.vx = 0f
                    r.state = RowState.Rest
                    r.flut = if (reducedMotion) 0f else 1f
                    r.ft = 0f
                    emit(StickyNoteEvent.Pat)
                }
            } else if (r.state == RowState.Tween) {
                val a = r.anim
                if (a != null) {
                    a.t += dt
                    val p = (a.t / a.dur).coerceIn(0f, 1f)
                    r.dx =
                        if (a.peek) -26 * sin(PI.toFloat() * p) * (1 - p * .2f)
                        else a.from + (a.to - a.from) * easeOut(p)
                    if (p >= 1f) {
                        r.anim = null
                        a.done()
                    }
                }
            }
            if (r.flut > .001f) {
                r.ft += s
                r.flut *= exp(-s * 4.5f)
            } else r.flut = 0f
            if (rows.getOrNull(i) === r) i++
        }
        for (f in flights) f.t += dt
        for (i in flights.indices.reversed()) {
            if (flights[i].t >= flights[i].dur) land(flights.removeAt(i))
        }
        for (i in ghosts.indices.reversed()) {
            ghosts[i].a -= dt / 280f
            if (ghosts[i].a <= 0f) ghosts.removeAt(i)
        }
        archive.bump = max(0f, archive.bump - s * 4)
        snooze.bump = max(0f, snooze.bump - s * 4)
    }

    // ---- Gesture, as the prototype's onDown / onMove / onUp ----

    private class Gesture(
        val scripted: Boolean,
        val x0: Float,
        val y0: Float,
        val row: NoteRow?,
        val pad: NoteAction?,
    ) {
        var mode = Mode.Pending
        var base = 0f
        val ts = ArrayDeque<Double>()
        val xs = ArrayDeque<Float>()
    }

    private enum class Mode {
        Pending,
        Drag,
        None,
    }

    private var gest: Gesture? = null
    private var lastGrain = -1e9

    /** Whether a scripted finger has hold of the list. */
    val scriptedTouch: Boolean
        get() = gest?.scripted == true

    fun hitRow(x: Float, y: Float): NoteRow? {
        if (x < mx || x > mx + noteW) return null
        return rows.firstOrNull {
            !it.hidden && it.state != RowState.Gone && y >= it.y && y <= it.y + rh
        }
    }

    fun hitPad(x: Float, y: Float): NoteAction? {
        val py = padY()
        for (k in NoteAction.entries) {
            val px = padX(k)
            if (x >= px - 12 && x <= px + pw + 12 && y >= py - 28 && y <= py + ph + 14) return k
        }
        return null
    }

    /** A finger down at ([x], [y]) at [now] ms; [scripted] for the demo's. */
    fun down(x: Float, y: Float, now: Double, scripted: Boolean = false) {
        val row = hitRow(x, y)
        val g =
            Gesture(
                scripted,
                x,
                y,
                row?.takeIf { it.state == RowState.Rest || it.state == RowState.Return },
                if (row == null) hitPad(x, y) else null,
            )
        g.ts += now
        g.xs += x
        gest = g
        val r = g.row
        if (r != null && r.state == RowState.Return) {
            g.mode = Mode.Drag
            r.state = RowState.Drag
            r.vx = 0f
            g.base = r.dx
        }
        changed()
    }

    fun move(x: Float, y: Float, now: Double) {
        val g = gest ?: return
        g.ts += now
        g.xs += x
        while (g.ts.size > 2 && now - g.ts.first() > 100) {
            g.ts.removeFirst()
            g.xs.removeFirst()
        }
        val ddx = x - g.x0
        val ddy = y - g.y0
        val row = g.row
        if (g.mode == Mode.Pending) {
            if (row != null && abs(ddx) > 8 && abs(ddx) > abs(ddy) * 1.1f) {
                g.mode = Mode.Drag
                row.state = RowState.Drag
                g.base = -ddx + sign(ddx) * 2
            } else if (abs(ddy) > 12 || abs(ddx) > 12) g.mode = Mode.None
        }
        if (g.mode == Mode.Drag && row != null) {
            val lim = noteW * .72f
            var raw = g.base + ddx
            if (abs(raw) > lim) raw = sign(raw) * (lim + (abs(raw) - lim) * .3f)
            val wasArmed = abs(row.dx) >= Commit * noteW
            peelSound(abs(raw - row.dx), now)
            row.dx = raw
            if (wasArmed != (abs(row.dx) >= Commit * noteW)) emit(StickyNoteEvent.Tick)
        }
        changed()
    }

    // Peeling crackles: grains of noise, more of them the faster the paper comes away.
    private fun peelSound(amount: Float, now: Double) {
        if (now - lastGrain < 22 || amount < .6f) return
        lastGrain = now
        emit(StickyNoteEvent.Grain, amount = min(.16f, .02f + amount * .012f) / .16f)
    }

    fun up() {
        val g = gest ?: return
        gest = null
        val row = g.row
        if (g.mode == Mode.Drag && row != null) {
            val t0 = g.ts.first()
            val t1 = g.ts.last()
            val v = if (t1 > t0) ((g.xs.last() - g.xs.first()) / (t1 - t0)).toFloat() else 0f
            val far = abs(row.dx) >= Commit * noteW
            val flung = abs(v) > Fling && sign(v) == sign(row.dx) && abs(row.dx) > 22
            if (far || flung) {
                commit(row, if (row.dx < 0) NoteAction.Archive else NoteAction.Snooze)
            } else {
                row.state = RowState.Return
                row.vx = 0f
                if (reducedMotion) {
                    row.dx = 0f
                    row.state = RowState.Rest
                }
            }
        } else if (g.mode == Mode.Pending) {
            if (g.pad != null) undo(g.pad) else if (row != null) peek(row)
        }
        changed()
    }

    /** Lets go of a scripted finger's hold, as the prototype's `stopDemo` does. */
    fun releaseScripted() {
        if (gest?.scripted == true) up()
    }

    private fun emit(event: StickyNoteEvent, item: Reminder? = null, amount: Float = 1f) {
        onEvent?.invoke(event, item, amount)
    }

    private fun changed() {
        frame++
    }

    private fun listChanged() {
        listVersion++
        changed()
    }

    internal companion object {
        /** How far the corner rises per unit of pull. */
        const val K = .3f

        /** The share of the width that commits. */
        const val Commit = .42f

        /** A fling, in dp per ms. */
        const val Fling = .55f

        private const val SubstepMs = 1000f / 240f

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun easeOut(t: Float) = 1 - (1 - t).pow(3)
    }
}
