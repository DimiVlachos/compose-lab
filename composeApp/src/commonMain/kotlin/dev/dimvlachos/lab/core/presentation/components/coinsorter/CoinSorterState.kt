package dev.dimvlachos.lab.core.presentation.components.coinsorter

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** What a coin is struck from: its colours, as the prototype's `.coin.nickel/.copper/.brass`. */
internal enum class Metal {
    Nickel,
    Copper,
    Brass,
}

/** One result as the sorter needs it: its price in cents, its rating and the metal of its coin. */
internal class CoinItem(val cents: Int, val rating: Double, val metal: Metal)

/**
 * The prototype's four sorts. Every one sorts the coins small to big by its value; a descending
 * sort deals the big ones to the top.
 */
internal enum class CoinSort(val desc: Boolean) {
    Best(true),
    Low(false),
    High(true),
    Top(true),
}

/** Where a result is in its trip through the tray, as the prototype's `mode`. */
internal enum class CoinMode {
    Row,
    Tip,
    Fly,
    Hopper,
    Ramp,
    Drop,
    Tube,
    Deal,
    Unfold,
}

/** What the sorter tells its screen: sounds, feel and what is announced. */
internal enum class CoinEvent {
    Tip,
    Rattle,
    Tick,
    Clink,
    Out,
    Thunk,
    MenuTick,
    MenuOpen,
    MenuClose,
    Hover,
    Pick,
    Sorting,
    Sorted,
    Already,
}

/**
 * One result: where it is in its trip ([mode]), the line it shows on ([row]), and as a coin its
 * centre, speed, roll ([ang]) and tilt ([ax], degrees), as the prototype's state.
 */
internal class CoinRow(val index: Int) {
    var mode = CoinMode.Row
    var trip = false
    var row = index
    var target = index
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var ang = 0f
    var ax = 0f
    var slot = -1
    var d = 0f
    var t0 = 0.0
    var passed = 0
    var tubeAt = 0.0
    var fromX = 0f
    var fromY = 0f
    var ctrlX = 0f
    var ctrlY = 0f
    var toX = 0f
    var toY = 0f

    // The coin's face for the sort that's running: its main line, its cents (or empty), and how
    // big it is set.
    var face = ""
    var cents = ""
    var faceSize = 0f

    /** Whether it shows as a row (tipping, flat or opening) rather than a coin. */
    val asRow: Boolean
        get() = mode == CoinMode.Row || mode == CoinMode.Tip || mode == CoinMode.Unfold

    internal fun clear(i: Int) {
        mode = CoinMode.Row
        trip = false
        row = i
        target = i
        x = 0f
        y = 0f
        vx = 0f
        vy = 0f
        ang = 0f
        ax = 0f
        slot = -1
        d = 0f
        t0 = 0.0
        passed = 0
        tubeAt = 0.0
    }
}

/**
 * The coin sorter's whole state, ported from the prototype: the tray's geometry ([layout]), the
 * sort a pick starts ([run]), the rows' trips through the tray ([step], in 1/240 s substeps on its
 * own clock), and the chip's press, slide and release onto the menu. Units are dp.
 *
 * [frame] is bumped by every step and change, so a drawing that reads it is drawn again;
 * [listVersion] by every committed order, a row landing on its new line and layout; [layoutVersion]
 * by layout alone, so the tray is drawn again only when it moves.
 */
@Stable
internal class CoinSorterState(val items: List<CoinItem>) {
    val n = items.size
    val rows = List(n) { CoinRow(it) }

    var frame by mutableIntStateOf(0)
        private set

    var listVersion by mutableIntStateOf(0)
        private set

    var layoutVersion by mutableIntStateOf(0)
        private set

    var reducedMotion = false

    /** Hears every [CoinEvent], with a volume and an index (a row, a slot or a sort). */
    var onEvent: ((CoinEvent, Float, Int) -> Unit)? = null

    /** The sort the list is in. */
    var sort by mutableStateOf(CoinSort.Best)
        private set

    /** The sort being run, while [running]. */
    var pending: CoinSort? = null
        private set

    var running by mutableStateOf(false)
        private set

    var menuOpen by mutableStateOf(false)
        private set

    /** The option a drag has lit, or -1. */
    var hot by mutableIntStateOf(-1)
        private set

    /** The list's order, top to bottom: the results' indices, committed when a sort lands. */
    var order: List<Int> = List(n) { it }
        private set

    // Clinks and thunks heard, for tests.
    var clinks = 0
        private set

    var thunks = 0
        private set

    // ---------- Layout ----------
    var width = 0f
        private set

    var height = 0f
        private set

    var k = 1f
        private set

    var trayX0 = 0f
        private set

    var trayX1 = 0f
        private set

    var trayTop = 0f
        private set

    var trayH = 0f
        private set

    var hopCx = 0f
        private set

    var hopTop = 0f
        private set

    var hopOut = 0f
        private set

    var hopTw = 0f
        private set

    var hopOw = 0f
        private set

    var railY0 = 0f
        private set

    var tubeBottom = 0f
        private set

    var listTop = 0f
        private set

    var rowH = 54f
        private set

    var rowGap = 6f
        private set

    var toolbarTop = 0f
        private set

    val slotW = FloatArray(n)
    val slotX = FloatArray(n)
    val coinD = FloatArray(n)

    /** Whether the menu, once it opens, takes keyboard focus on the current option. */
    var focusOnOpen = false

    /** Hands focus back to the chip, when a key closes the menu. */
    var refocusChip: (() -> Unit)? = null

    /** The chip's left edge and width, as laid out. */
    var chipX = 0f
    var chipW = 0f

    val chipY: Float
        get() = toolbarTop + ChipTop

    fun railY(x: Float) = railY0 + (x - hopCx) * Tan

    /**
     * Lays the tray out for a screen [w] by [h] whose toolbar starts at [toolbarTop], as the
     * prototype's `layout()`.
     */
    fun layout(w: Float, h: Float, toolbarTop: Float) {
        if (w <= 0f || h <= 0f) return
        width = w
        height = h
        this.toolbarTop = toolbarTop
        k = min(w / 360f, h / 744f).coerceIn(.85f, 1.25f)
        trayX0 = 16f
        trayX1 = w - 16f
        trayTop = toolbarTop + ToolbarH + 4 * k
        trayH = 120 * k
        hopCx = trayX0 + 34 * k
        hopTop = trayTop - 17 * k
        hopOut = trayTop + 12 * k
        hopTw = 58 * k
        hopOw = 40 * k
        railY0 = trayTop + 48 * k
        tubeBottom = trayTop + trayH - 8 * k
        // Graded slots: each 3.2 wider than the last; each coin 2.2 narrower than its own slot,
        // so it clears it but rolls over every smaller one.
        var x = hopCx + hopTw / 2 + 10 * k
        for (i in 0 until n) {
            slotW[i] = (16 + 3.2f * i) * k
            coinD[i] = slotW[i] - 2.2f * k
            slotX[i] = x + slotW[i] / 2
            x += slotW[i] + 3.5f * k
        }
        listTop = trayTop + trayH + 12 * k
        // On short screens the gaps tighten and the rows come down to 38 (the icon's height) so
        // the last row stays on screen.
        val fit = (h - 14 - listTop) / n
        rowGap = if (fit < 50) 4f else 6f
        rowH = (fit - rowGap).coerceIn(38f, 64f)
        for (s in rows) if (s.slot >= 0) setCoin(s)
        layoutVersion++
        listVersion++
        frame++
    }

    fun rowX() = 16f

    fun rowY(line: Int) = listTop + line * (rowH + rowGap)

    fun rowW() = width - 32

    // ---------- The menu's geometry ----------
    val menuX: Float
        get() = width - 16 - MenuW

    val menuY: Float
        get() = chipY + ChipH + 6

    fun optY(i: Int) = menuY + MenuPad + i * OptH

    fun optAt(x: Float, y: Float): Int {
        val ox = menuX + MenuPad
        if (x < ox || x > ox + MenuW - 2 * MenuPad) return -1
        for (i in 0 until CoinSort.entries.size) {
            val oy = optY(i)
            if (y >= oy && y <= oy + OptH) return i
        }
        return -1
    }

    // ---------- The funnel heap ----------
    /** Where a coin waits in the funnel: the first on the rail at the outlet, the rest heaped. */
    fun pileX(q: Int) = if (q == 0) hopCx else hopCx + Jit[q % Jit.size] * k

    fun pileY(q: Int, r: Float): Float {
        if (q == 0) return railY(hopCx) - r
        // The heap stays inside the funnel: past three it packs tighter instead of rising out of
        // the top, and never higher than the rim.
        val up = min(q - 1, 2) * 7 * k + max(0, q - 3) * 2 * k
        return max(hopTop + r + 1.5f * k, hopOut - r + 6 * k - up)
    }

    // A coin's size and face for the sort that's running: its price, its rating or its rank.
    private fun setCoin(s: CoinRow) {
        val d = coinD[s.slot]
        val it = items[s.index]
        s.d = d
        // Every coin carries its value. Under 20 dp the face is short ("$12", "4.4") and set a
        // little larger so it stays legible.
        val small = d < 20 * k
        s.faceSize = d * (if (small) .4f else .33f)
        when (pending ?: sort) {
            CoinSort.Low,
            CoinSort.High -> {
                s.face = "$" + it.cents / 100
                s.cents =
                    if (d >= 24 * k) "." + (it.cents % 100).toString().padStart(2, '0') else ""
            }
            CoinSort.Top -> {
                s.face = (if (d >= 26 * k) "★" else "") + rating(it.rating)
                s.cents = ""
            }
            CoinSort.Best -> {
                s.face = "#${s.index + 1}"
                s.cents = ""
            }
        }
    }

    // ---------- Sorting ----------
    private val timerAt = DoubleArray(n)
    private val timerOn = BooleanArray(n)
    private val queue = ArrayList<Int>(n)
    private var lastRel = -1
    private var nextRelease = 0.0
    var simNow = 0.0
        private set

    /** Runs [id], as the prototype's `run()`: false when one is running or it is the same sort. */
    fun run(id: CoinSort): Boolean {
        if (running) return false
        closeMenu()
        if (id == sort) {
            emit(CoinEvent.Already, 1f, id.ordinal)
            return false
        }
        val asc = (0 until n).sortedWith(compareBy<Int> { value(id, it) }.thenBy { it })
        pending = id
        asc.forEachIndexed { rank, it ->
            val s = rows[it]
            s.slot = rank
            s.target = if (id.desc) n - 1 - rank else rank
            setCoin(s)
        }
        if (reducedMotion) {
            for (s in rows) {
                s.row = s.target
                s.mode = CoinMode.Row
            }
            running = true
            finish()
            return true
        }
        running = true
        queue.clear()
        lastRel = -1
        nextRelease = simNow
        emit(CoinEvent.Sorting, 1f, id.ordinal)
        // Top to bottom, each row tips up, a little after the one above.
        order.forEachIndexed { line, it ->
            timerAt[it] = simNow + line * GatherGap
            timerOn[it] = true
        }
        for (s in rows) s.trip = true
        frame++
        return true
    }

    private fun value(id: CoinSort, i: Int): Double =
        when (id) {
            CoinSort.Best -> (n - i).toDouble()
            CoinSort.Low,
            CoinSort.High -> items[i].cents.toDouble()
            CoinSort.Top -> items[i].rating
        }

    private fun startDeal(i: Int) {
        val s = rows[i]
        val cx = rowX() + rowW() / 2
        val cy = rowY(s.target) + rowH / 2
        s.mode = CoinMode.Deal
        s.t0 = simNow
        s.fromX = s.x
        s.fromY = s.y
        s.toX = cx
        s.toY = cy
        s.ctrlX = s.x
        s.ctrlY = lerp(s.y, cy, .72f)
        emit(CoinEvent.Out, 1f, s.slot)
    }

    private fun finish() {
        running = false
        order = (0 until n).sortedBy { rows[it].row }
        sort = pending ?: sort
        pending = null
        listVersion++
        frame++
        emit(CoinEvent.Sorted, 1f, sort.ordinal)
    }

    /** Puts everything back as it began, mid-sort too. */
    fun reset() {
        running = false
        pending = null
        queue.clear()
        lastRel = -1
        timerOn.fill(false)
        rows.forEachIndexed { i, s -> s.clear(i) }
        order = List(n) { it }
        sort = CoinSort.Best
        menuOpen = false
        hot = -1
        press = false
        listVersion++
        frame++
    }

    /** Whether the list is as it began: best match, every row on its own line, at rest. */
    val asBegun: Boolean
        get() =
            !running &&
                sort == CoinSort.Best &&
                rows.all { it.mode == CoinMode.Row && it.row == it.index }

    /** Whether nothing moves: the frame loop can sleep. */
    val settled: Boolean
        get() = !running && rows.all { it.mode == CoinMode.Row } && wellsSettled()

    // ---------- Physics ----------
    /** Advances [dt] seconds, in 1/240 s substeps. */
    fun step(dt: Float) {
        if (dt <= 0f) {
            frame++
            return
        }
        var left = dt
        while (left > 0f) {
            val h = min(left, 1f / 240)
            simNow += h * 1000.0
            fireTimers()
            stepOnce(h)
            stepWells(h)
            left -= h
        }
        frame++
    }

    private fun fireTimers() {
        for (i in 0 until n) {
            if (!timerOn[i] || timerAt[i] > simNow) continue
            timerOn[i] = false
            val s = rows[i]
            if (s.mode != CoinMode.Row) continue
            s.mode = CoinMode.Tip
            s.t0 = simNow
            emit(CoinEvent.Tip, 1f, s.row)
        }
    }

    private fun stepOnce(dt: Float) {
        val v0 = 330 * k
        val acc = 700 * k
        val g = 2800 * k
        for (i in 0 until n) {
            val s = rows[i]
            val r = s.d / 2
            when (s.mode) {
                CoinMode.Tip ->
                    if (simNow - s.t0 >= TipMs) {
                        // Edge-on: the row hands over to its coin, which turns face-on as it
                        // flies up to the funnel.
                        val cx = rowX() + rowW() / 2
                        val cy = rowY(s.row) + rowH / 2
                        s.mode = CoinMode.Fly
                        s.t0 = simNow
                        s.x = cx
                        s.y = cy - 8 * k
                        s.ax = 86f
                        s.ang = 0f
                        s.fromX = cx
                        s.fromY = cy - 8 * k
                        s.toX = hopCx
                        s.toY = hopTop - 10 * k
                        s.ctrlX = lerp(s.fromX, s.toX, .3f)
                        s.ctrlY = min(s.fromY, s.toY) - 46 * k
                    }
                CoinMode.Fly -> {
                    val p = ((simNow - s.t0) / FlyMs).toFloat().coerceIn(0f, 1f)
                    curve(s, ease(p))
                    s.ax = 86 * (1 - easeOut((p / .6f).coerceIn(0f, 1f)))
                    s.ang -= dt * 7
                    if (p >= 1f) {
                        s.mode = CoinMode.Hopper
                        s.vx = 0f
                        s.vy = 160 * k
                        queue += i
                        emit(CoinEvent.Rattle, 1f, i)
                    }
                }
                CoinMode.Hopper -> {
                    val q = queue.indexOf(i).coerceAtLeast(0)
                    springX(s, pileX(q), dt)
                    springY(s, pileY(q, r), dt)
                    s.ax = 0f
                }
                CoinMode.Ramp -> {
                    s.vx += acc * dt
                    s.x += s.vx * dt
                    s.y = railY(s.x) - r
                    s.ang += s.vx * dt / r
                    // Over each smaller slot it bridges the gap with a tick; at its own it drops.
                    while (s.passed < s.slot && s.x >= slotX[s.passed]) {
                        s.passed++
                        emit(CoinEvent.Tick, .6f, s.passed)
                    }
                    if (s.x >= slotX[s.slot]) {
                        s.x = slotX[s.slot]
                        s.mode = CoinMode.Drop
                        s.vy = 40 * k
                    }
                }
                CoinMode.Drop -> {
                    s.vy += g * dt
                    s.y += s.vy * dt
                    s.ang += s.vy * dt / (r * 6)
                    val rest = tubeBottom - r
                    if (s.y >= rest) {
                        s.y = rest
                        if (s.vy > 110 * k) {
                            clinks++
                            emit(CoinEvent.Clink, (s.vy / (900 * k)).coerceIn(.2f, 1f), s.slot)
                            s.vy = -s.vy * .3f
                        } else {
                            s.vy = 0f
                            s.mode = CoinMode.Tube
                            s.tubeAt = simNow
                        }
                    }
                }
                // A moment in the tube, then out of its bottom and into its row: the list
                // refills as the coins land.
                CoinMode.Tube -> if (running && simNow - s.tubeAt >= SettleMs) startDeal(i)
                CoinMode.Deal -> {
                    val p = ((simNow - s.t0) / DealMs).toFloat().coerceIn(0f, 1f)
                    curve(s, ease(p))
                    s.ax = 86 * easeIn(((p - .42f) / .58f).coerceIn(0f, 1f))
                    s.ang += dt * 5
                    if (p >= 1f) {
                        s.mode = CoinMode.Unfold
                        s.row = s.target
                        s.t0 = simNow
                        s.trip = false
                        thunks++
                        listVersion++
                        emit(CoinEvent.Thunk, 1f, s.row)
                    }
                }
                CoinMode.Unfold -> if (simNow - s.t0 >= UnfoldMs) s.mode = CoinMode.Row
                CoinMode.Row -> Unit
            }
        }
        if (!running) return
        // The funnel lets the bottom coin out once it has settled and the one ahead has rolled
        // clear.
        if (queue.isNotEmpty() && simNow >= nextRelease) {
            val i0 = queue[0]
            val s0 = rows[i0]
            val px = pileX(0)
            val py = pileY(0, s0.d / 2)
            val l = if (lastRel >= 0) rows[lastRel] else null
            val clear =
                l == null || l.mode != CoinMode.Ramp || l.x - hopCx >= l.d / 2 + s0.d / 2 + 4 * k
            if (clear && hypot(s0.x - px, s0.y - py) < 6 * k) {
                queue.removeAt(0)
                s0.mode = CoinMode.Ramp
                s0.x = px
                s0.y = py
                s0.vx = v0
                s0.vy = 0f
                s0.passed = 0
                lastRel = i0
                nextRelease = simNow + ReleaseGap
            }
        }
        if (rows.all { it.mode == CoinMode.Row && !it.trip }) finish()
    }

    private fun curve(s: CoinRow, e: Float) {
        val w = 1 - e
        s.x = w * w * s.fromX + 2 * w * e * s.ctrlX + e * e * s.toX
        s.y = w * w * s.fromY + 2 * w * e * s.ctrlY + e * e * s.toY
    }

    private fun springX(s: CoinRow, target: Float, dt: Float) {
        val c = 2 * sqrt(SpringK) * SpringZ
        s.vx += (SpringK * (target - s.x) - c * s.vx) * dt
        s.x += s.vx * dt
    }

    private fun springY(s: CoinRow, target: Float, dt: Float) {
        val c = 2 * sqrt(SpringK) * SpringZ
        s.vy += (SpringK * (target - s.y) - c * s.vy) * dt
        s.y += s.vy * dt
    }

    // ---------- The rows' poses ----------
    /**
     * The row's pose on its line, as the prototype's `render()`: its tilt (degrees), how far it is
     * lifted and how narrow it is, into [out] as (ax, lift, scaleX).
     */
    fun rowPose(i: Int, out: FloatArray) {
        val s = rows[i]
        var ax = 0f
        var lift = 0f
        var sx = 1f
        val narrow = if (s.d > 0f) s.d / rowW() else 1f
        // Near edge-on the row narrows to its coin's width, so the handover is the same size
        // both ways; it widens again as it opens.
        if (s.mode == CoinMode.Tip) {
            val p = ((simNow - s.t0) / TipMs).toFloat().coerceIn(0f, 1f)
            ax = 86 * easeIn(p)
            lift = 8 * k * sin(p * PI.toFloat() / 2)
            sx = lerp(1f, narrow, easeIn(((p - .6f) / .4f).coerceIn(0f, 1f)))
        } else if (s.mode == CoinMode.Unfold) {
            val p = ((simNow - s.t0) / UnfoldMs).toFloat().coerceIn(0f, 1f)
            ax = 86 * (1 - backOut(p))
            sx = lerp(narrow, 1f, easeOut((p / .4f).coerceIn(0f, 1f)))
        }
        out[0] = ax
        out[1] = lift
        out[2] = sx
    }

    /**
     * In flight a coin is in front of the tray; it goes behind the acrylic the moment its lower
     * edge drops past the funnel rim.
     */
    fun flying(s: CoinRow): Boolean {
        if (s.mode != CoinMode.Fly) return false
        val r = s.d / 2
        val intoFunnel = abs(s.x - hopCx) < hopTw / 2 + r && s.y + r > hopTop - 3 * k
        return !intoFunnel
    }

    // ---------- Wells ----------
    /** How far each line's empty outline has faded in, 0 to 1. */
    val wellA = FloatArray(n)

    /** Whether no row is on [line] mid-sort, so its outline shows. */
    fun wellOn(line: Int): Boolean {
        if (!running) return false
        for (s in rows) if (s.asRow && s.row == line) return false
        return true
    }

    private fun stepWells(dt: Float) {
        for (j in 0 until n) {
            val to = if (wellOn(j)) 1f else 0f
            val a = wellA[j]
            wellA[j] =
                if (to > a) min(to, a + dt * 1000 / WellMs) else max(to, a - dt * 1000 / WellMs)
        }
    }

    private fun wellsSettled() = wellA.all { it == 0f }

    // ---------- Menu and press ----------
    private var press = false
    private var px0 = 0f
    private var py0 = 0f
    private var lx = 0f
    private var ly = 0f
    private var moved = false
    private var wasOpen = false

    fun openMenu(): Boolean {
        if (running || menuOpen) return false
        menuOpen = true
        emit(CoinEvent.MenuTick, .5f, 3)
        emit(CoinEvent.MenuOpen, 1f, 0)
        return true
    }

    fun closeMenu() {
        if (!menuOpen) return
        menuOpen = false
        hot = -1
        emit(CoinEvent.MenuClose, 1f, 0)
    }

    fun hover(i: Int) {
        if (hot == i) return
        hot = i
        if (i >= 0) emit(CoinEvent.Hover, 1f, i)
    }

    /** Picks option [i], as the prototype's `pick()`. */
    fun pick(i: Int): Boolean {
        val id = CoinSort.entries[i]
        val started = run(id)
        if (started) emit(CoinEvent.Pick, 1f, i)
        return started
    }

    fun pressDown(x: Float, y: Float) {
        press = true
        px0 = x
        py0 = y
        lx = x
        ly = y
        moved = false
        wasOpen = menuOpen
    }

    fun pressMove(x: Float, y: Float) {
        if (!press) return
        lx = x
        ly = y
        if (!moved && hypot(x - px0, y - py0) > Slop) {
            moved = true
            openMenu()
        }
        if (moved && menuOpen) hover(optAt(x, y))
    }

    fun pressUp(x: Float, y: Float) {
        if (!press) return
        press = false
        // Decided from the release point, so a press and release with no moves still works.
        if (hypot(x - px0, y - py0) <= Slop) {
            hover(-1)
            if (wasOpen) closeMenu()
            else if (!openMenu() && running) emit(CoinEvent.MenuTick, .4f, 0)
            return
        }
        if (!menuOpen) openMenu()
        val b = optAt(x, y)
        hover(-1)
        if (b >= 0) pick(b) else closeMenu()
    }

    /** A cancelled press finishes from the last point it reached, like a release there. */
    fun pressCancel() {
        if (press) pressUp(lx, ly)
    }

    private fun emit(event: CoinEvent, v: Float, i: Int) {
        onEvent?.invoke(event, v, i)
    }

    internal companion object {
        const val GatherGap = 95.0
        const val TipMs = 200.0
        const val FlyMs = 380.0
        const val ReleaseGap = 100.0
        const val SettleMs = 200.0
        const val DealMs = 440.0
        const val UnfoldMs = 360.0
        const val WellMs = 140f
        const val Tan = .044f
        const val ToolbarH = 38f
        const val ChipTop = 3f
        const val ChipH = 34f
        const val MenuW = 206f
        const val MenuPad = 6f
        const val OptH = 42f
        const val Slop = 10f
        private const val SpringK = 520f
        private const val SpringZ = .62f
        private val Jit = intArrayOf(0, -6, 5, -3, 7, -7, 2, -1)

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun easeIn(t: Float) = t * t

        fun easeOut(t: Float) = 1 - (1 - t).pow(3)

        fun backOut(t: Float): Float {
            val c = 1.7f
            return 1 + (c + 1) * (t - 1).pow(3) + c * (t - 1).pow(2)
        }

        /** A rating as the prototype prints it: 4.7, or 5 for 5.0. */
        fun rating(r: Double): String {
            val tenths = kotlin.math.round(r * 10).toInt()
            return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
        }
    }
}
