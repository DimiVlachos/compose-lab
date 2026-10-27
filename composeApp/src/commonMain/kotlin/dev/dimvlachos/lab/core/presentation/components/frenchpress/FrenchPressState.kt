package dev.dimvlachos.lab.core.presentation.components.frenchpress

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** What the press is doing, as the prototype's `st.mode`. */
internal enum class PressMode {
    Idle,
    /** Words pouring in and the plunger going down. */
    Forward,
    /** Let go short of halfway: the words fly home. */
    Cancel,
    /** The summary is up. */
    Brewed,
    /** The summary drains back and the article returns. */
    Reverse,
}

/** Something the press does that is heard or felt, as the prototype's `snd()` kinds. */
internal enum class FrenchPressEvent {
    /** The words start pouring in. */
    Pour,
    /** The grounds stir up and the words start out. */
    Rise,
    /** Every sixth word lands in the glass. */
    Tick,
    /** The mesh reaches the bottom. */
    Tink,
    /** The summary glugs out of the spout, or back in; the value is which glug, 0 to 5. */
    Glug,
    /** The plunger starts up. */
    Lift,
}

/** What is announced, as the prototype's `say()`. */
internal enum class PressSaid {
    None,
    Pressing,
    Ready,
    Kept,
    Full,
}

/** A word or letter in flight, as the prototype's parts. */
internal class PressPart {
    var word = true
    var into = true
    var seed = 0
    var index = 0
    var x0 = 0f
    var y0 = 0f
    var x1 = 0f
    var y1 = 0f
    var t0 = 0.0
    var dur = 0f
    var spin = 0f
}

/** A word in the glass: on its swirl, or pressed into the pile, as the prototype's grains. */
internal class PressGrain {
    var word = 0
    var th = 0f
    var om = 0f
    var rad = 0f
    var sy = 0f
    var px = 0f
    var py = 0f
    var rot = 0f
    var e = 0f
}

/**
 * The French press over a story, as the prototype's model: its geometry from `layout()`, the
 * plunger's position [p] trailing its target, the words in flight and the grounds in the glass, and
 * the sequences that press the story into a summary and pour it back. Ported from its `step()`,
 * `held()`, `beginForward`, `commit`, `openCardEarly`, `finish`, `wordsOut`, `cancelForward`,
 * `reverse`, `tap` and gesture; units are the prototype's CSS px, which are dp here, and its times
 * are in ms.
 *
 * The words and letters are laid out by the screen, which gives their boxes here: each word's
 * centre relative to the body's origin ([setWord]) and each letter's relative to the summary's
 * ([setLetter]), with where those origins are on the screen now ([placeBody], [placeSummary]).
 * [frame] is bumped by every frame that moves anything and every change, so a drawing that reads it
 * is drawn again.
 */
@Stable
internal class FrenchPressState(paragraphs: List<List<String>>, points: List<String>) {
    /** Every word of the story, in order; [paragraphOf] says which paragraph each is in. */
    val words: List<String> = paragraphs.flatten()
    val paragraphOf: IntArray =
        IntArray(words.size).also { a ->
            var i = 0
            paragraphs.forEachIndexed { p, ws -> repeat(ws.size) { a[i++] = p } }
        }

    /** Every letter of the summary's points, spaces included, and the point each is in. */
    val letters: CharArray = points.joinToString("").toCharArray()
    val pointOf: IntArray =
        IntArray(letters.size).also { a ->
            var i = 0
            points.forEachIndexed { p, s -> repeat(s.length) { a[i++] = p } }
        }
    private val inkLetters: IntArray = letters.indices.filter { letters[it] != ' ' }.toIntArray()

    /** How many letters are ink, not space. */
    val inkCount: Int
        get() = inkLetters.size

    // ---- Boxes, from the screen's layout ----
    private val wordCx = FloatArray(words.size)
    private val wordCy = FloatArray(words.size)
    private val letterCx = FloatArray(letters.size)
    private val letterCy = FloatArray(letters.size)
    private var wordBoxH = 20f
    private var bodyX = 0f
    private var bodyY = 0f
    private var summaryX = 0f
    private var summaryY = 0f
    private var articleTop = 0f

    /** The full article button's centre on the screen, where the script taps it. */
    var fullX = 0f
        private set

    var fullY = 0f
        private set

    /** Word [i]'s box: its centre ([cx], [cy]) from the body's origin, as the prototype's span. */
    fun setWord(i: Int, cx: Float, cy: Float) {
        wordCx[i] = cx
        wordCy[i] = cy
    }

    /** How tall a word's box is (the font's ascent and descent), for the words kept still. */
    fun setWordHeight(h: Float) {
        wordBoxH = h
    }

    fun setLetter(i: Int, cx: Float, cy: Float) {
        letterCx[i] = cx
        letterCy[i] = cy
    }

    /** Where the body's origin is on the screen now, and where the article starts. */
    fun placeBody(x: Float, y: Float, article: Float) {
        bodyX = x
        bodyY = y
        articleTop = article
    }

    fun placeSummary(x: Float, y: Float) {
        summaryX = x
        summaryY = y
    }

    fun placeFull(x: Float, y: Float) {
        fullX = x
        fullY = y
    }

    fun wordX(i: Int) = bodyX + wordCx[i]

    fun wordY(i: Int) = bodyY + wordCy[i]

    fun letterX(i: Int) = summaryX + letterCx[i]

    fun letterY(i: Int) = summaryY + letterCy[i]

    // ---- Layout ----
    var width = 0f
        private set

    var height = 0f
        private set

    var k = 1f
        private set

    var kp = 1f
        private set

    var ox = 0f
        private set

    var oy = 0f
        private set

    var cw = 0f
        private set

    var ch = 0f
        private set

    var gb = 0f
        private set

    var rimY = 0f
        private set

    var level0 = 0f
        private set

    var meshTop = 0f
        private set

    var pileH = 0f
        private set

    var meshBot = 0f
        private set

    var rodL = 0f
        private set

    var pvx = 0f
        private set

    var spoutLx = 0f
        private set

    var spoutLy = 0f
        private set

    /** Where the story's text ends, faded, above the knob: the press's corner is kept clear. */
    var clearY = 0f
        private set

    /** The press's hit box on the screen. */
    var hitX = 0f
        private set

    var hitY = 0f
        private set

    var hitW = 0f
        private set

    var hitH = 0f
        private set

    /** Where the press's label ends (its right edge) and its middle. */
    var tagX = 0f
        private set

    var tagY = 0f
        private set

    /** Bumped when the layout changes, so the screen lays out what depends on it again. */
    var layoutVersion by mutableIntStateOf(0)
        private set

    /** The prototype's `layout()`: the press in the screen's bottom-right corner, [w] by [h]. */
    fun layout(w: Float, h: Float, safe: Float = 0f) {
        if (w <= 0f || h <= 0f) return
        if (w == width && h == height) return
        width = w
        height = h
        k = min(w / 360f, h / 740f).coerceIn(.85f, 1.2f)
        // The press is drawn 1.25× the base scale so it reads as an object, not an icon.
        kp = k * PRESS_SCALE
        cw = 56 * kp
        ch = 86 * kp
        gb = -12 * kp
        rimY = gb - ch
        level0 = rimY + 16 * kp
        meshTop = rimY + 12 * kp
        pileH = 14 * kp
        meshBot = gb - pileH
        rodL = meshBot - (rimY - 15 * kp)
        pvx = -cw / 2 - 1 * kp
        spoutLx = -cw / 2 - 7 * kp
        spoutLy = rimY - 2 * kp
        ox = w - 18 * kp - 14 * kp - cw / 2
        oy = h - 22 * kp - safe
        val top = oy + knobY(0f) - 12 * kp
        clearY = top - 4 * kp
        hitX = ox - cw / 2 - 12 * kp
        hitY = top
        hitW = cw + 30 * kp
        hitH = oy + 6 * kp - top
        tagX = ox - cw / 2 - 10 * kp
        tagY = oy - 7 * kp
        layoutVersion++
        frame++
    }

    fun meshY(p: Float) = lerp(meshTop, meshBot, p)

    fun knobY(p: Float) = meshY(p) - rodL

    val travel: Float
        get() = meshBot - meshTop

    /** The press frame to the screen, tipped about the left foot: x. */
    fun screenX(lx: Float, ly: Float, t: Float = tilt) =
        ox + pvx + (lx - pvx) * cos(t) - ly * sin(t)

    fun screenY(lx: Float, ly: Float, t: Float = tilt) = oy + (lx - pvx) * sin(t) + ly * cos(t)

    val spoutX: Float
        get() = screenX(spoutLx, spoutLy)

    val spoutY: Float
        get() = screenY(spoutLx, spoutLy)

    val knobX: Float
        get() = screenX(0f, knobY(p))

    val knobScreenY: Float
        get() = screenY(0f, knobY(p))

    // ---- State ----
    var frame by mutableIntStateOf(0)
        private set

    var mode by mutableStateOf(PressMode.Idle)
        private set

    /** How far open the summary card is, 0 to 1: its height is this much of what it measures. */
    var cardOpen by mutableFloatStateOf(0f)
        private set

    /** What was last announced, and a count that changes with every announcement. */
    var said by mutableStateOf(PressSaid.None)
        private set

    var saidCount by mutableIntStateOf(0)
        private set

    /** Reduced motion: the press jumps straight to the summary and back. */
    var reducedMotion: Boolean
        get() = clock.reduced
        set(value) {
            clock.reduced = value
        }

    /** Hears what the press does: the event and, for a glug, which one. */
    var onEvent: ((FrenchPressEvent, Int) -> Unit)? = null

    private val clock = PressScheduler()

    /** The simulated time, ms. */
    val now: Double
        get() = clock.now

    var p = 0f
        private set

    var pt = 0f
        private set

    var speedP = 0f
        private set

    var arrived = 0
        private set

    var launched = 0
        private set

    var total = 0
        private set

    private var cancelled = false
    var committed = false
        private set

    private var finishing = false
    var tilt = 0f
        private set

    var clarity = 0f
        private set

    var charsOut = 0
        private set

    private var cardEarly = false

    /** The furthest the plunger went this press. */
    var maxP = 0f
        private set

    /** Whether word i is off the page (flying, in the press, or kept still at the fade). */
    val gone = BooleanArray(words.size)

    /** Whether letter i is written in the card. */
    val shown = BooleanArray(letters.size)

    val grains = ArrayList<PressGrain>(words.size)
    val parts = ArrayList<PressPart>(words.size)
    private val grainPool = ArrayList<PressGrain>()
    private val partPool = ArrayList<PressPart>()
    private var seq = 0

    private class Drag(
        val x0: Float,
        val y0: Float,
        val from: PressMode,
        val scripted: Boolean,
    ) {
        var x = x0
        var y = y0
        var moved = false
    }

    private var drag: Drag? = null

    /** Bumped whenever a real touch, a key or the button takes over from the script. */
    var takenOver by mutableIntStateOf(0)
        private set

    /** Whether nothing moves: the frame loop can sleep. */
    val settled: Boolean
        get() =
            !clock.busy &&
                parts.isEmpty() &&
                abs(held(pt) - p) < 1e-6f &&
                grains.all { it.e >= 1f } &&
                (mode == PressMode.Idle || mode == PressMode.Brewed) &&
                (grains.isEmpty() || p >= 1f)

    val brewed: Boolean
        get() = mode == PressMode.Brewed

    private fun snd(e: FrenchPressEvent, i: Int = 0) = onEvent?.invoke(e, i)

    private fun say(s: PressSaid) {
        said = s
        saidCount++
    }

    private fun setCard(f: Float) {
        cardOpen = f
    }

    // ---- Words into the press ----
    private fun part(): PressPart =
        if (partPool.isEmpty()) PressPart() else partPool.removeAt(partPool.size - 1)

    private fun launchIn(w: Int) {
        gone[w] = true
        launched++
        parts +=
            part().apply {
                word = true
                into = true
                seed = w
                index = w
                x0 = wordX(w)
                y0 = wordY(w)
                t0 = clock.now
                dur = (WORD_IN_MS + (w * 37 % 120)).toFloat()
                spin = ((w * 53 % 17) / 17f - .5f) * 3
            }
    }

    private fun addGrain(w: Int, settled: Boolean) {
        fun r(n: Int) = ((w + 1) * n % 997) / 997f
        grains +=
            (if (grainPool.isEmpty()) PressGrain() else grainPool.removeAt(grainPool.size - 1))
                .apply {
                    word = w
                    th = r(131) * PI.toFloat() * 2
                    om = 2 + r(277) * 1.6f
                    rad = .2f + r(389) * .75f
                    sy = lerp(meshTop + 6 * kp, gb - 4 * kp, r(521))
                    px = (r(613) * 2 - 1) * (cw / 2 - 5 * kp)
                    py = lerp(meshBot + 1.8f * kp, gb - 1.6f * kp, r(733))
                    rot = r(859) * PI.toFloat() * 2
                    e = if (settled) 1f else 0f
                }
    }

    /**
     * A grain's place in the press frame, written into [out] as x, y and its depth: on its swirl,
     * pulled toward the pile as the plunger goes down.
     */
    fun grainPos(g: PressGrain, out: FloatArray) {
        val pp = p
        val sw = (cw / 2 - 5 * kp) * g.rad
        val tx = sin(g.th) * sw
        val ty = g.sy + sin(g.th * 1.3f + g.rot) * 2 * kp * (1 - pp)
        var x = lerp(tx, g.px, pp)
        var y = lerp(ty, g.py, pp)
        if (g.e < 1f) {
            val e = easeOut(g.e)
            val ex = -cw / 2 + 6 * kp
            val ey = level0 + 2 * kp
            x = lerp(ex, x, e)
            y = lerp(ey, y, e)
        }
        out[0] = x
        out[1] = y
        out[2] = cos(g.th) * (1 - pp)
    }

    private fun launchOut(g: PressGrain) {
        grains.remove(g)
        grainPool += g
        val w = g.word
        parts +=
            part().apply {
                word = true
                into = false
                seed = w
                index = w
                x0 = spoutX
                y0 = spoutY
                x1 = wordX(w)
                y1 = wordY(w)
                t0 = clock.now
                dur = (WORD_OUT_MS + (w * 37 % 100)).toFloat()
                spin = ((w * 53 % 17) / 17f - .5f) * 3
            }
    }

    private fun launchLetter(c: Int, back: Boolean) {
        if (back) {
            shown[c] = false
            parts +=
                part().apply {
                    word = false
                    into = true
                    seed = c
                    index = c
                    x0 = letterX(c)
                    y0 = letterY(c)
                    t0 = clock.now
                    dur = CHAR_MS * .75f
                    spin = 0f
                }
        } else {
            charsOut++
            parts +=
                part().apply {
                    word = false
                    into = false
                    seed = c
                    index = c
                    x0 = spoutX
                    y0 = spoutY
                    x1 = letterX(c)
                    y1 = letterY(c)
                    t0 = clock.now
                    dur = CHAR_MS.toFloat()
                    spin = 0f
                }
        }
    }

    private fun land(pp: PressPart) {
        if (pp.word) {
            if (pp.into) {
                addGrain(pp.index, false)
                arrived++
                if (arrived % 6 == 0) snd(FrenchPressEvent.Tick)
            } else {
                gone[pp.index] = false
            }
        } else {
            if (!pp.into) shown[pp.index] = true else charsOut = max(0, charsOut - 1)
        }
    }

    private fun flying(word: Boolean): Boolean {
        for (i in parts.indices) if (parts[i].word == word) return true
        return false
    }

    // ---- Actions ----
    private fun measureVisible(): IntArray {
        val vis = ArrayList<Int>(words.size)
        val bottom = min(min(height - 2, height), clearY - 20)
        for (w in words.indices) {
            val top = wordY(w) - wordBoxH / 2
            // Words in the soft fade at the story's bottom edge stay put (hidden until the end), so
            // nothing pops from faded to full ink.
            val off = top + wordBoxH > bottom || top < articleTop
            if (off) gone[w] = true else vis += w
        }
        return vis.toIntArray()
    }

    private fun beginForward(auto: Boolean) {
        if (mode != PressMode.Idle) return
        val tok = seq
        mode = PressMode.Forward
        arrived = 0
        launched = 0
        cancelled = false
        committed = false
        finishing = false
        maxP = 0f
        cardEarly = false
        val vis = measureVisible()
        total = vis.size
        if (clock.reduced) {
            for (w in vis) {
                gone[w] = true
                addGrain(w, true)
            }
            arrived = total
            launched = total
            p = 1f
            pt = 1f
            finishInstant()
            return
        }
        snd(FrenchPressEvent.Pour)
        vis.forEachIndexed { i, w ->
            clock.later(i * POUR_SPREAD.toDouble() / vis.size) {
                if (tok != seq || cancelled) return@later
                launchIn(w)
            }
        }
        say(PressSaid.Pressing)
        if (auto) {
            committed = true
            clock.later(260.0) {
                if (tok != seq) return@later
                val p0 = pt
                clock.animateJob(1150.0) { t ->
                    if (tok == seq && mode == PressMode.Forward) pt = lerp(p0, 1f, ease(t))
                }
            }
        }
    }

    private fun commit() {
        if (mode != PressMode.Forward || committed) return
        committed = true
        val tok = seq
        val p0 = pt
        clock.animateJob(lerp(520f, 160f, p0).toDouble()) { t ->
            if (tok == seq && mode == PressMode.Forward) pt = lerp(p0, 1f, ease(t))
        }
    }

    private fun finishInstant() {
        clarity = 1f
        charsOut = inkLetters.size
        tilt = 0f
        shown.fill(true)
        setCard(1f)
        mode = PressMode.Brewed
        say(PressSaid.Ready)
    }

    private fun openCardEarly() {
        cardEarly = true
        val tok = seq
        val f0 = cardOpen
        clock.animateJob(520.0) { t -> if (tok == seq && cardEarly) setCard(lerp(f0, 1f, ease(t))) }
    }

    private suspend fun finish() {
        val tok = seq
        val ok = { tok == seq }
        finishing = true
        snd(FrenchPressEvent.Tink)
        for (i in letters.indices) if (letters[i] == ' ') shown[i] = true
        if (!cardEarly) openCardEarly()
        clock.animate(400.0) { clarity = ease(it) }
        if (!ok()) return
        clock.animate(380.0) { tilt = -TILT * ease(it) }
        if (!ok()) return
        val n = inkLetters.size
        val spread = 900.0
        inkLetters.forEachIndexed { i, c ->
            clock.later(i * spread / n) { if (ok()) launchLetter(c, false) }
        }
        for (i in 0 until 6) clock.later(i * 160.0) { if (ok()) snd(FrenchPressEvent.Glug, i) }
        clock.wait(spread + 40)
        if (!ok()) return
        clock.animate(380.0) { tilt = -TILT * (1 - ease(it)) }
        if (!ok()) return
        clock.until { !flying(word = false) }
        if (!ok()) return
        setCard(1f)
        mode = PressMode.Brewed
        say(PressSaid.Ready)
    }

    private suspend fun wordsOut(ok: () -> Boolean): Boolean {
        val gs = grains.sortedBy { it.word }
        if (gs.isNotEmpty()) snd(FrenchPressEvent.Rise)
        gs.forEachIndexed { i, g ->
            clock.later(i * POUR_SPREAD.toDouble() / gs.size) { if (ok()) launchOut(g) }
        }
        clock.until { grains.isEmpty() && !flying(word = true) }
        if (!ok()) return false
        gone.fill(false)
        return true
    }

    private suspend fun cancelForward() {
        val tok = seq
        val ok = { tok == seq }
        cancelled = true
        mode = PressMode.Cancel
        pt = 0f
        // If the card had started to open (past halfway, then dragged back up and let go), it
        // closes again before the words fly home.
        if (cardEarly || cardOpen > 0f) {
            cardEarly = false
            val f0 = cardOpen
            clock.animate(300.0) { if (ok()) setCard(f0 * (1 - ease(it))) }
            if (!ok()) return
            setCard(0f)
        }
        clock.until { !flying(word = true) }
        if (!ok()) return
        clock.until { p < .01f }
        if (!ok()) return
        if (!wordsOut(ok)) return
        mode = PressMode.Idle
        p = 0f
        pt = 0f
        say(PressSaid.Kept)
    }

    private suspend fun reverse() {
        if (mode != PressMode.Brewed) return
        val tok = seq
        val ok = { tok == seq }
        mode = PressMode.Reverse
        if (clock.reduced) {
            shown.fill(false)
            setCard(0f)
            charsOut = 0
            clarity = 0f
            grainPool += grains
            grains.clear()
            p = 0f
            pt = 0f
            gone.fill(false)
            mode = PressMode.Idle
            say(PressSaid.Full)
            return
        }
        clock.animate(320.0) { tilt = -TILT * ease(it) }
        if (!ok()) return
        val back = inkLetters.reversedArray()
        val spread = 620.0
        back.forEachIndexed { i, c ->
            clock.later(i * spread / back.size) { if (ok()) launchLetter(c, true) }
        }
        for (i in 0 until 4) clock.later(i * 150.0) { if (ok()) snd(FrenchPressEvent.Glug, 5 - i) }
        clock.until { !flying(word = false) && charsOut == 0 }
        if (!ok()) return
        shown.fill(false)
        // The coffee murks back while the press rights itself, and the words start out while the
        // plunger is still rising, so the page doesn't sit blank between the card closing and the
        // first word coming home.
        clock.animateJob(300.0) { if (ok()) setCard(1 - ease(it)) }
        clock.animateJob(340.0) { if (ok()) clarity = 1 - ease(it) }
        clock.animate(340.0) { tilt = -TILT * (1 - ease(it)) }
        if (!ok()) return
        setCard(0f)
        clarity = 0f
        snd(FrenchPressEvent.Lift)
        val lift = clock.animateJob(720.0) { if (ok()) pt = 1 - ease(it) }
        clock.wait(260.0)
        if (!ok()) return
        val out = clock.async { wordsOut(ok) }
        lift.await()
        if (!ok()) return
        clock.until { p < .01f }
        if (!ok()) return
        if (!out.await()) return
        mode = PressMode.Idle
        p = 0f
        pt = 0f
        say(PressSaid.Full)
    }

    /** The prototype's tap: summarize from the full article, or give it back from the summary. */
    fun tap() {
        when (mode) {
            PressMode.Idle -> beginForward(true)
            PressMode.Brewed -> clock.launch { reverse() }
            else -> {}
        }
        changed()
    }

    /** Full article, the card's button (and the reverse a key or the script asks for). */
    fun showFull() {
        clock.launch { reverse() }
        changed()
    }

    // ---- Input ----
    /** A finger (or, [scripted], the demo's) comes down on the press at ([x], [y]). */
    fun pressDown(x: Float, y: Float, scripted: Boolean = false) {
        // A second finger (or the demo's drag still in hand) never replaces the drag in progress.
        if (drag != null) return
        if (mode != PressMode.Idle && mode != PressMode.Brewed) return
        drag = Drag(x, y, mode, scripted)
    }

    /** Whether a real finger holds the press, so another is ignored. */
    val heldByFinger: Boolean
        get() = drag?.scripted == false

    fun pressMove(x: Float, y: Float) {
        val d = drag ?: return
        d.x = x
        d.y = y
        if (!d.moved && hypot(x - d.x0, y - d.y0) > SLOP) d.moved = true
        // Starts the pour on the first move that heads down past the slop, even if the drag set
        // off upward or sideways.
        if (d.moved && d.from == PressMode.Idle && mode == PressMode.Idle && y - d.y0 > SLOP)
            beginForward(false)
        if (d.moved && d.from == PressMode.Idle && mode == PressMode.Forward && !committed) {
            pt = ((y - d.y0) / travel).coerceIn(0f, 1f)
        }
        changed()
    }

    fun pressUp(x: Float, y: Float) {
        val d = drag ?: return
        drag = null
        // Decided from where the finger lets go, so a flick with no move events still works.
        if (hypot(x - d.x0, y - d.y0) <= SLOP) {
            tap()
            return
        }
        if (d.from == PressMode.Idle) {
            if (mode == PressMode.Idle) {
                if (y - d.y0 > SLOP) beginForward(false) else return changed()
            }
            if (mode != PressMode.Forward) return changed()
            val at = ((y - d.y0) / travel).coerceIn(0f, 1f)
            if (at >= .5f) {
                pt = max(pt, at)
                commit()
            } else {
                clock.launch { cancelForward() }
            }
        } else if (d.from == PressMode.Brewed && d.y0 - y > SLOP) {
            clock.launch { reverse() }
        }
        changed()
    }

    /** A touch cancelled lets go where the finger last was, as the prototype's pointercancel. */
    fun pressCancel() {
        val d = drag ?: return
        pressUp(d.x, d.y)
    }

    /**
     * The prototype's `demoStop()` for the press: a real touch, a key or the button takes over, and
     * the demo's drag is let go where its finger was, so the press never sits half-pushed.
     */
    fun takeOver() {
        takenOver++
        releaseScripted()
    }

    /** Lets go of the demo's drag, if it holds one: where it was if it moved, else dropped. */
    fun releaseScripted() {
        val d = drag ?: return
        if (!d.scripted) return
        if (d.moved) pressUp(d.x, d.y) else drag = null
    }

    /** The prototype's `reset()`: the full article, the press empty, nothing pending. */
    fun reset() {
        seq++
        clock.clear()
        drag = null
        mode = PressMode.Idle
        p = 0f
        pt = 0f
        speedP = 0f
        arrived = 0
        launched = 0
        total = 0
        cancelled = false
        committed = false
        finishing = false
        tilt = 0f
        clarity = 0f
        charsOut = 0
        cardEarly = false
        maxP = 0f
        grainPool += grains
        grains.clear()
        partPool += parts
        parts.clear()
        gone.fill(false)
        shown.fill(false)
        setCard(0f)
        said = PressSaid.None
        changed()
    }

    /** Whether the story is back as it began: the full article, nothing in the press. */
    val asBegun: Boolean
        get() =
            mode == PressMode.Idle &&
                gone.none { it } &&
                grains.isEmpty() &&
                parts.isEmpty() &&
                p == 0f &&
                tilt == 0f &&
                clarity == 0f &&
                cardOpen == 0f &&
                shown.none { it }

    private fun changed() {
        clock.drain()
        frame++
    }

    // ---- Loop ----
    private fun held(target: Float): Float {
        if (mode != PressMode.Forward || arrived >= total) return target
        val lim = .3f * arrived / max(1, total)
        return if (target <= lim) target else lim + (target - lim) * RESIST
    }

    private fun step(dt: Float) {
        // The plunger trails its target. Until every word is in it resists like a rubber band:
        // past a limit that grows with the words in (30% when they're all in), it moves only a
        // third as far as the finger.
        val want = held(pt)
        val p0 = p
        p += (want - p) * (1 - exp(-dt * 11))
        if (abs(want - p) < 2e-4f) p = want
        speedP = lerp(speedP, (p - p0) / dt, .2f)
        maxP = max(maxP, p)
        for (g in grains) {
            g.th += g.om * dt * (1 - .92f * p)
            if (g.e < 1f) g.e = min(1f, g.e + dt * 1000 / 380)
        }
        // Once every word is in and the plunger is past halfway, the empty card starts opening, so
        // the payoff begins while you press.
        val noWords = !flying(word = true)
        if (mode == PressMode.Forward && !cardEarly && arrived == total && noWords && p > .5f)
            openCardEarly()
        if (
            mode == PressMode.Forward &&
                committed &&
                !finishing &&
                arrived == total &&
                noWords &&
                p > .995f
        ) {
            p = 1f
            pt = 1f
            clock.launch { finish() }
        }
    }

    private fun updateParts() {
        var i = parts.size - 1
        while (i >= 0) {
            val pp = parts[i]
            if (clock.now - pp.t0 >= pp.dur) {
                parts.removeAt(i)
                land(pp)
                partPool += pp
            }
            i--
        }
    }

    /** The prototype's `frame()`: [dt] seconds on (at most .15), in 1/240 s substeps. */
    fun advance(dt: Float) {
        val d = dt.coerceIn(0f, .15f)
        clock.tick(d * 1000.0)
        var left = d
        while (left > 0f) {
            val h = min(left, 1f / 240)
            step(h)
            left -= h
        }
        updateParts()
        clock.checkWaiters()
        clock.drain()
        frame++
    }

    internal companion object {
        const val PRESS_SCALE = 1.25f
        const val WORD_IN_MS = 760
        const val WORD_OUT_MS = 600
        const val CHAR_MS = 620
        const val POUR_SPREAD = 1100
        const val TILT = .62f
        const val RESIST = 1f / 3
        const val SLOP = 10f

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun easeOut(t: Float) = 1 - (1 - t).pow(3)
    }
}
