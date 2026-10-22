package dev.dimvlachos.lab.core.presentation.components.stickynote

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Pattern2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/** The notes' paper colours, as the prototype's palette. */
private val Palette = listOf("#fbe49a", "#f7c6cd", "#c6dcf4", "#cfe7c0", "#f8d2ab", "#dccff1")

private fun channels(hex: String): IntArray {
    val n = hex.substring(1).toInt(16)
    return intArrayOf(n shr 16 and 255, n shr 8 and 255, n and 255)
}

// The prototype's shade(): each channel times f.
private fun shade(hex: String, f: Float): String {
    val c = channels(hex)
    return "rgb(${(c[0] * f).roundToInt()},${(c[1] * f).roundToInt()},${(c[2] * f).roundToInt()})"
}

// The prototype's mixWhite(): each channel f of the way to white.
private fun mixWhite(hex: String, f: Float): String {
    val c = channels(hex)
    fun ch(v: Int) = (v + (255 - v) * f).roundToInt()
    return "rgb(${ch(c[0])},${ch(c[1])},${ch(c[2])})"
}

/** The canvas's colours by day and by night, the prototype's `theme()`, as its CSS strings. */
internal class NoteInks
private constructor(
    val dark: Boolean,
    val bg: String,
    val glow: String,
    val note: List<String>,
    val back: List<String>,
    val ink: String,
    val meta: String,
    val shadow: String,
    val flapShadow: String,
    val archive: String,
    val snooze: String,
    val label: String,
    val dash: String,
    val hintInk: String,
) {
    internal companion object {
        val Day =
            NoteInks(
                dark = false,
                bg = "#f3efe8",
                glow = "rgba(255,236,200,.55)",
                note = Palette,
                back = Palette.map { mixWhite(it, .28f) },
                ink = "#2b2418",
                meta = "rgba(43,36,24,.6)",
                shadow = "rgba(84,60,20,.22)",
                flapShadow = "rgba(70,48,14,.34)",
                archive = "#3d8a68",
                snooze = "#b5741f",
                label = "#7a7166",
                dash = "rgba(60,40,10,.18)",
                hintInk = "rgba(60,40,10,.35)",
            )
        val Dusk =
            NoteInks(
                dark = true,
                bg = "#1a1917",
                glow = "rgba(255,196,130,.07)",
                note = Palette.map { shade(it, .8f) },
                back = Palette.map { shade(it, .9f) },
                ink = "#2a241b",
                meta = "rgba(42,36,27,.62)",
                shadow = "rgba(0,0,0,.55)",
                flapShadow = "rgba(0,0,0,.6)",
                archive = "#2f6e53",
                snooze = "#a86e22",
                label = "#a59c8f",
                dash = "rgba(255,255,255,.14)",
                hintInk = "rgba(255,255,255,.3)",
            )
    }
}

/** The words the canvas writes: the action panels' and the pads'. */
internal class NoteWords(
    val archive: String,
    val snooze: String,
    val tomorrow: String,
    val done: String,
    val later: String,
    val swipeLeft: String,
    val swipeRight: String,
)

/**
 * The prototype's `makePaper()`: a 160 square of specks and short fibres that the notes' paper is
 * tiled with, from a fixed seed.
 */
internal fun notePaper(seed: Int = 1022): Pattern2D {
    val rnd = Random(seed)
    fun rand(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)
    val c = Canvas2D.create(160, 160)
    repeat(2400) {
        // Colour objects, not CSS strings: 2,400 distinct alphas would fill the parse cache.
        c.fillStyle =
            if (rnd.nextFloat() < .5f) Speck.copy(alpha = rand(.02f, .07f))
            else Color.White.copy(alpha = rand(.04f, .12f))
        c.fillRect(rnd.nextFloat() * 160, rnd.nextFloat() * 160, rand(.6f, 1.6f), rand(.6f, 1.6f))
    }
    repeat(40) {
        c.strokeStyle = Fibre.copy(alpha = rand(.02f, .05f))
        c.lineWidth = .6f
        val a = rnd.nextFloat() * 160
        val b = rnd.nextFloat() * 160
        val an = rand(-.4f, .4f)
        val l = rand(4f, 12f)
        c.beginPath()
        c.moveTo(a, b)
        c.lineTo(a + kotlin.math.cos(an) * l, b + kotlin.math.sin(an) * l)
        c.stroke()
    }
    c.bitmap.freeze()
    return c.createPattern(c.bitmap)
}

private val Speck = Color(90, 70, 30)
private val Fibre = Color(110, 85, 40)

/** The fonts the canvas names: the notes' handwriting and the system's sans. */
internal const val HandFont = "hand"
internal const val SansFont = "sans"

/**
 * Draws the reminders list as the prototype's `render()` does, split in its two canvases:
 * [drawBase] under the app bar, and [drawLift] over it, for the notes that are peeling or flying,
 * so their paper covers the bar's text. Units are dp; the canvas comes scaled by [density].
 *
 * It keeps its scratch geometry between frames, so a frame allocates no shapes of its own.
 */
internal class StickyNotePainter(
    private val paper: Pattern2D,
    private val words: NoteWords,
    private val density: Float,
) {
    private lateinit var c: Canvas2D
    private lateinit var th: NoteInks
    private val pose = FlightPose()

    // The peel's geometry: the pulled corner C, where it is pulled to (P), the fold's middle M and
    // normal n, the part still stuck down and the flap.
    private var gx = 0f
    private var gy = 0f
    private var mxp = 0f
    private var myp = 0f
    private var nx = 0f
    private var ny = 0f
    private val keepX = FloatArray(8)
    private val keepY = FloatArray(8)
    private var keepN = 0
    private val flapX = FloatArray(8)
    private val flapY = FloatArray(8)
    private var flapN = 0
    private val rx = FloatArray(4)
    private val ry = FloatArray(4)

    fun drawBase(canvas: Canvas2D, s: StickyNoteState, inks: NoteInks) {
        c = canvas
        th = inks
        val w = s.width
        val h = s.height
        c.fillStyle = th.bg
        c.fillRect(0f, 0f, w, h)
        val gl = c.createRadialGradient(w * .15f, h * .05f, 0f, w * .15f, h * .05f, h * .9f)
        gl.addColorStop(0f, th.glow)
        gl.addColorStop(1f, "rgba(255,255,255,0)")
        c.fillStyle = gl
        c.fillRect(0f, 0f, w, h)
        for (gh in s.ghosts) {
            drawAction(gh.kind, s.mx, gh.y, s.noteW, s.rh, gh.a)
        }
        // The rows that lie flat first, then the panels under the ones peeling, as the
        // prototype's sort puts them.
        for (r in s.rows) {
            if (r.hidden || abs(r.dx) > .5f) continue
            drawNote(r.item, s.mx, r.y, s.noteW, s.rh, r.dx, r.lift)
        }
        for (r in s.rows) {
            if (r.hidden || abs(r.dx) <= .5f) continue
            val kind = if (r.dx < 0) NoteAction.Archive else NoteAction.Snooze
            drawAction(kind, s.mx, r.y, s.noteW, s.rh, 1f)
        }
        drawPad(s, NoteAction.Archive)
        drawPad(s, NoteAction.Snooze)
    }

    fun drawLift(canvas: Canvas2D, s: StickyNoteState, inks: NoteInks) {
        c = canvas
        th = inks
        for (r in s.rows) {
            if (r.hidden || abs(r.dx) <= .5f) continue
            drawNote(r.item, s.mx, r.y, s.noteW, s.rh, r.dx, r.lift)
        }
        for (f in s.flights) drawFlight(s, f)
    }

    // ---------- Peel geometry ----------

    // Clips the rectangle (rx, ry) to one side of the line through M with normal n, into (ox, oy).
    private fun clipHalf(keepPos: Boolean, ox: FloatArray, oy: FloatArray): Int {
        var k = 0
        for (i in 0 until 4) {
            val ax = rx[i]
            val ay = ry[i]
            val bx = rx[(i + 1) % 4]
            val by = ry[(i + 1) % 4]
            val sA = (ax - mxp) * nx + (ay - myp) * ny
            val sB = (bx - mxp) * nx + (by - myp) * ny
            val inA = if (keepPos) sA >= 0 else sA < 0
            val inB = if (keepPos) sB >= 0 else sB < 0
            if (inA) {
                ox[k] = ax
                oy[k] = ay
                k++
            }
            if (inA != inB) {
                val t = sA / (sA - sB)
                ox[k] = ax + (bx - ax) * t
                oy[k] = ay + (by - ay) * t
                k++
            }
        }
        return k
    }

    // The prototype's peelGeom(): false when the note lies flat.
    private fun peelGeom(x: Float, y: Float, w: Float, h: Float, dx: Float): Boolean {
        if (abs(dx) < .5f) return false
        val cx = if (dx < 0) x + w else x
        val cy = y + h
        gx = cx + dx
        gy = cy - abs(dx) * StickyNoteState.K
        val vx = gx - cx
        val vy = gy - cy
        val len = hypot(vx, vy)
        nx = vx / len
        ny = vy / len
        mxp = (cx + gx) / 2
        myp = (cy + gy) / 2
        rx[0] = x
        ry[0] = y
        rx[1] = x + w
        ry[1] = y
        rx[2] = x + w
        ry[2] = y + h
        rx[3] = x
        ry[3] = y + h
        keepN = clipHalf(true, keepX, keepY)
        flapN = clipHalf(false, flapX, flapY)
        for (i in 0 until flapN) {
            val sd = (flapX[i] - mxp) * nx + (flapY[i] - myp) * ny
            flapX[i] -= 2 * sd * nx
            flapY[i] -= 2 * sd * ny
        }
        return true
    }

    private fun path(px: FloatArray, py: FloatArray, n: Int) {
        c.beginPath()
        for (i in 0 until n) if (i == 0) c.moveTo(px[i], py[i]) else c.lineTo(px[i], py[i])
        c.closePath()
    }

    private fun rectPath(x: Float, y: Float, w: Float, h: Float) {
        c.beginPath()
        c.moveTo(x, y)
        c.lineTo(x + w, y)
        c.lineTo(x + w, y + h)
        c.lineTo(x, y + h)
        c.closePath()
    }

    // ---------- Drawing ----------

    // The notes' font at a size, made again only when the size changes: most notes share one.
    private var fontSize = -1f
    private var fontName = ""

    private fun handFont(fs: Float): String {
        if (fs != fontSize) {
            fontSize = fs
            fontName = "600 ${fs}px $HandFont"
        }
        return fontName
    }

    private fun noteText(item: Reminder, x: Float, y: Float, w: Float, h: Float, color: String?) {
        var fs = (h * .36f).coerceIn(19f, 27f)
        c.font = handFont(fs)
        val tw = c.measureText(item.title)
        if (tw > w - 34) {
            fs = max(16f, fs * (w - 34) / tw)
            c.font = handFont(fs)
        }
        val block = fs * .82f + 6 + 12
        val by = y + (h - block) / 2 + fs * .8f + 1
        c.fillStyle = color ?: th.ink
        c.textBaseline = "alphabetic"
        c.textAlign = "left"
        c.fillText(item.title, x + 16, by)
        // Under 0.6x (pad stacks, the end of a flight) the meta line would be unreadable, so it
        // fades out by then.
        val sc = c.transformScale / density
        val ma = ((sc - .6f) / .1f).coerceIn(0f, 1f)
        if (ma <= 0f) return
        c.save()
        c.globalAlpha *= ma
        c.font = "500 11.5px $SansFont"
        c.fillStyle = color ?: th.meta
        c.fillText(item.meta, x + 17, by + 18)
        c.restore()
    }

    private fun noteSurface(item: Reminder, x: Float, y: Float, w: Float, h: Float) {
        c.fillStyle = th.note[item.color]
        c.fillRect(x - 1, y - 1, w + 2, h + 2)
        c.save()
        c.globalAlpha = if (th.dark) .5f else .9f
        c.fillStyle = paper
        c.fillRect(x, y, w, h)
        c.restore()
        // Light falls from the upper left across the sheet.
        var gr = c.createLinearGradient(x, y, x + w, y + h)
        gr.addColorStop(0f, "rgba(255,255,255,.14)")
        gr.addColorStop(1f, "rgba(0,0,0,.04)")
        c.fillStyle = gr
        c.fillRect(x, y, w, h)
        // The glue strip lies flatter and a touch glossier.
        gr = c.createLinearGradient(0f, y, 0f, y + h * .24f)
        gr.addColorStop(0f, "rgba(255,255,255,.16)")
        gr.addColorStop(1f, "rgba(255,255,255,0)")
        c.fillStyle = gr
        c.fillRect(x, y, w, h * .24f)
        // The free bottom edge curls up a little and catches the light.
        gr = c.createLinearGradient(0f, y + h - 14, 0f, y + h)
        gr.addColorStop(0f, "rgba(255,255,255,0)")
        gr.addColorStop(.75f, "rgba(255,255,255,.2)")
        gr.addColorStop(1f, "rgba(120,90,40,.1)")
        c.fillStyle = gr
        c.fillRect(x, y + h - 14, w, 14f)
        noteText(item, x, y, w, h, null)
    }

    private fun drawNote(
        item: Reminder,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        dx: Float,
        lift: Float,
    ) {
        val peeled = peelGeom(x, y, w, h, dx)
        // Shadow sizes are in dp at the note's own scale; the canvas wants device px, so they are
        // multiplied by the transform's scale (density × note scale). shadowBlur is 2 sigma.
        val ds = c.transformScale
        c.save()
        c.shadowColor = th.shadow
        c.shadowBlur = (2 + lift * 6) * ds
        c.shadowOffsetY = (.75f + lift * 2.5f) * ds
        c.fillStyle = th.note[item.color]
        if (peeled) path(keepX, keepY, keepN) else rectPath(x, y, w, h)
        c.fill()
        c.restore()
        c.save()
        if (peeled) path(keepX, keepY, keepN) else rectPath(x, y, w, h)
        c.clip()
        noteSurface(item, x, y, w, h)
        if (peeled) {
            // A soft crease shade where the paper starts to lift.
            val gr = c.createLinearGradient(mxp, myp, mxp + nx * 16, myp + ny * 16)
            gr.addColorStop(0f, "rgba(60,40,10,.22)")
            gr.addColorStop(1f, "rgba(60,40,10,0)")
            c.fillStyle = gr
            c.fillRect(x - 2, y - 2, w + 4, h + 4)
        }
        c.restore()
        if (!peeled || flapN < 3) return
        val p = (abs(dx) / w).coerceIn(0f, 1f)
        // The flap's shadow falls across the sheet and whatever it uncovered.
        c.save()
        c.shadowColor = th.flapShadow
        c.shadowBlur = (3.5f + p * 9) * ds
        c.shadowOffsetX = nx * (1 + p * 3) * ds
        c.shadowOffsetY = (1 + p * 4) * ds
        c.fillStyle = th.back[item.color]
        path(flapX, flapY, flapN)
        c.fill()
        c.restore()
        c.save()
        path(flapX, flapY, flapN)
        c.clip()
        c.fillStyle = th.back[item.color]
        c.fill()
        c.save()
        c.globalAlpha = if (th.dark) .35f else .6f
        c.fillStyle = paper
        c.fill()
        c.restore()
        // Ink shows through the thin paper, mirrored.
        val md = mxp * nx + myp * ny
        val fnx = nx
        val fny = ny
        val fmx = mxp
        val fmy = myp
        val fpx = gx
        val fpy = gy
        c.save()
        c.transform(
            1 - 2 * fnx * fnx,
            -2 * fnx * fny,
            -2 * fnx * fny,
            1 - 2 * fny * fny,
            2 * md * fnx,
            2 * md * fny,
        )
        c.globalAlpha = .07f
        noteText(item, x, y, w, h, th.ink)
        c.restore()
        val gr = c.createLinearGradient(fmx, fmy, fpx, fpy)
        gr.addColorStop(0f, "rgba(255,255,255,.42)")
        gr.addColorStop(.14f, "rgba(255,255,255,.08)")
        gr.addColorStop(.55f, "rgba(0,0,0,0)")
        gr.addColorStop(1f, "rgba(70,50,15,.13)")
        c.fillStyle = gr
        path(flapX, flapY, flapN)
        c.fill()
        c.restore()
        // A bright line along the bend itself.
        var a0 = -1
        var a1 = -1
        for (i in 0 until flapN) {
            if (abs((flapX[i] - fmx) * fnx + (flapY[i] - fmy) * fny) < .8f) {
                if (a0 < 0) a0 = i else if (a1 < 0) a1 = i
            }
        }
        if (a1 >= 0) {
            c.save()
            c.strokeStyle = "rgba(255,255,255,.55)"
            c.lineWidth = 1f
            c.beginPath()
            c.moveTo(flapX[a0], flapY[a0])
            c.lineTo(flapX[a1], flapY[a1])
            c.stroke()
            c.restore()
        }
    }

    private fun icon(kind: NoteAction, cx: Float, cy: Float, s: Float, col: String) {
        c.save()
        c.strokeStyle = col
        c.lineWidth = 1.8f
        c.lineCap = "round"
        c.lineJoin = "round"
        c.beginPath()
        if (kind == NoteAction.Archive) {
            c.rect(cx - s, cy - s * .55f, s * 2, s * .5f)
            c.moveTo(cx - s * .82f, cy - s * .05f)
            c.lineTo(cx - s * .82f, cy + s * .8f)
            c.lineTo(cx + s * .82f, cy + s * .8f)
            c.lineTo(cx + s * .82f, cy - s * .05f)
            c.moveTo(cx - s * .3f, cy + s * .25f)
            c.lineTo(cx + s * .3f, cy + s * .25f)
        } else {
            c.arc(cx, cy, s * .9f, 0f, TAU)
            c.moveTo(cx, cy - s * .5f)
            c.lineTo(cx, cy)
            c.lineTo(cx + s * .4f, cy + s * .25f)
        }
        c.stroke()
        c.restore()
    }

    private fun drawAction(
        kind: NoteAction,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        alpha: Float,
    ) {
        c.save()
        c.globalAlpha = alpha
        c.beginPath()
        c.roundRect(x, y, w, h, 5f)
        c.fillStyle = if (kind == NoteAction.Archive) th.archive else th.snooze
        c.fill()
        // Inner shade: the screen under a note sits a little in shadow.
        val gr = c.createLinearGradient(0f, y, 0f, y + h)
        gr.addColorStop(0f, "rgba(0,0,0,.16)")
        gr.addColorStop(.3f, "rgba(0,0,0,0)")
        c.fillStyle = gr
        c.fill()
        val right = kind == NoteAction.Archive
        val snz = kind == NoteAction.Snooze
        val label = if (right) words.archive else words.snooze
        // Icon and lines share one centre, 34 in from the edge or further if the widest line
        // needs it. The group never moves or grows with the pull, so the paper uncovers it in
        // place from the first pixel; arming is the tick and the haptic, not a jump in size.
        // (The inset keeps the room a 7 % grow once needed, so the words sit where they did.)
        c.font = F1
        var half = c.measureText(label) / 2
        if (snz) {
            c.font = F2
            half = max(half, c.measureText(words.tomorrow) / 2)
        }
        val inset = max(34f, half * 1.07f + 10)
        // The group runs from the icon's top (cy - 9.5) to the last baseline (cy + 20 or cy + 33);
        // that span is centred in the panel.
        val lastBase = if (snz) 33f else 20f
        val cx = if (right) x + w - inset else x + inset
        val cy = y + h / 2 - (lastBase - 9.5f) / 2
        icon(kind, cx, cy - 2, 7.5f, "rgba(255,255,255,.95)")
        c.fillStyle = "#fff"
        c.font = F1
        c.textAlign = "center"
        c.textBaseline = "alphabetic"
        c.fillText(label, cx, cy + 20)
        if (snz) {
            c.font = F2
            c.globalAlpha = alpha * .85f
            c.fillText(words.tomorrow, cx, cy + 33)
        }
        c.restore()
    }

    private fun drawPad(s: StickyNoteState, kind: NoteAction) {
        val pd = s.pad(kind)
        val px = s.padX(kind)
        val py = s.padY()
        val pw = s.pw
        val ph = s.ph
        val right = kind == NoteAction.Snooze
        c.save()
        c.font = "600 11px $SansFont"
        c.fillStyle = th.label
        c.textAlign = if (right) "right" else "left"
        c.textBaseline = "alphabetic"
        val n = pd.stack.size
        val name = (if (kind == NoteAction.Archive) words.done else words.later).uppercase()
        c.fillText(if (n > 0) "$name  ·  $n" else name, if (right) px + pw else px, py - 9)
        val b = 1 + pd.bump * .07f
        c.translate(px + pw / 2, py + ph / 2)
        c.scale(b, b)
        c.translate(-(px + pw / 2), -(py + ph / 2))
        if (n == 0 && s.flights.none { it.kind == kind }) {
            c.setLineDash(Dash)
            c.strokeStyle = th.dash
            c.lineWidth = 1.2f
            c.beginPath()
            c.roundRect(px + .5f, py + .5f, pw - 1, ph - 1, 4f)
            c.stroke()
            c.setLineDash(NoDash)
            c.fillStyle = th.hintInk
            c.textAlign = "center"
            c.font = "500 11px $SansFont"
            c.fillText(
                if (kind == NoteAction.Archive) words.swipeLeft else words.swipeRight,
                px + pw / 2,
                py + ph / 2 + 4,
            )
        } else if (n > 0) {
            val sc = s.padScale()
            val from = max(0, n - 4)
            for (i in from until n) {
                val e = pd.stack[i]
                val k = i - from
                c.save()
                c.translate(px + pw / 2 + e.ox, py + ph / 2 - k * 2.4f)
                c.rotate(e.rot)
                c.scale(sc, sc)
                drawNote(e.item, -s.noteW / 2, -s.rh / 2, s.noteW, s.rh, 0f, .2f)
                c.restore()
            }
        }
        c.restore()
    }

    private fun drawFlight(s: StickyNoteState, f: Flight) {
        val p = s.flightPose(f, StickyNoteState.ease((f.t / f.dur).coerceIn(0f, 1f)), pose)
        c.save()
        c.translate(p.cx, p.cy)
        c.rotate(p.rot)
        c.scale(p.s, p.s)
        drawNote(f.item, -s.noteW / 2, -s.rh / 2, s.noteW, s.rh, p.dx, p.lift)
        c.restore()
    }

    private companion object {
        const val F1 = "650 12.5px $SansFont"
        const val F2 = "500 10.5px $SansFont"
        val Dash = floatArrayOf(4f, 4f)
        val NoDash = floatArrayOf()
    }
}
