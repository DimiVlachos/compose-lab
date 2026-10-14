package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundLoop
import dev.dimvlachos.lab.core.platform.SoundPlayer
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which way a row is swiped away: [Left] burns it from its right edge, [Right] from its left. */
public enum class SmokeSide {
    Left,
    Right,
}

/**
 * What swiping a row away to one side does: it goes up in [material], and once it is all smoke
 * [onCommit] is called. [label] names it for a screen reader's actions.
 */
public class SmokeAction(
    public val label: String,
    public val material: SmokeMaterial,
    public val onCommit: () -> Unit,
)

/**
 * The smoke over a list of [SmokeRow]s: every row going up, or coming back, and the frame loop that
 * moves them. Make one with [rememberSmokeState] and hand it to the [SmokeOverlay] and its rows.
 *
 * A swipe that has burnt a third of a row, [swipeAway], or a key or a screen reader's action burns
 * it away; [reform] breathes the smoke of a row that went back in, as an undo does, for as long as
 * it is kept, until [forget]. A finger's swipe comes in through [dragStart], [dragTo] and
 * [release], which a scripted finger can call too, and [boundsOf] says where a row is. Nothing here
 * is read in composition: the rows read their front and their height while drawing and laying out,
 * and the overlay draws the particles, so nothing recomposes while the smoke moves.
 */
@Stable
public class SmokeState internal constructor() {
    internal var sound: SoundPlayer = SilentSoundPlayer

    // What the smoke heard, passed on to haptics.
    internal var onThreshold: () -> Unit = {}
    internal var onCommitted: () -> Unit = {}

    // Where rows are sampled from, and the overlay the particles are drawn in.
    internal var scope: CoroutineScope? = null
    internal var overlay: LayoutCoordinates? = null
    internal var density = 1f

    /** The smoke's own clock, in seconds: it runs only while the frame loop does. */
    internal var now = 0f
        private set

    /** Bumped every frame, so the overlay draws again. */
    internal var frame by mutableIntStateOf(0)
        private set

    /** Whether the frame loop is running: while anything moves or any smoke is in the air. */
    internal var awake by mutableStateOf(false)

    internal val rows = mutableMapOf<Any, SmokeRowState>()
    private val plumes = mutableMapOf<Any, SmokePlume>()

    // Smoke let go of, no longer anyone's to undo, still in the air until it has faded.
    private val fading = mutableListOf<SmokePlume>()
    private val stepping = mutableListOf<Pair<Any, SmokePlume>>()
    internal val painter = SmokePainter()

    // Whether a frame's particles are being worked out off the main thread, and for which plumes.
    private var filling = false
    private val jobPlumes = mutableListOf<SmokePlume>()
    private var jobReforms = FloatArray(8)

    /** The plumes in the air this frame, in the order they were started. */
    internal val drawn = mutableListOf<SmokePlume>()

    private var loop: SoundLoop? = null
    private var quietFor = 0f
    private var inhaledAt = Float.NEGATIVE_INFINITY

    // Programmed burns, waiting their turn: a wave clearing several rows starts them in turn.
    private class Queued(val at: Float, val key: Any, val side: SmokeSide)

    private val queued = mutableListOf<Queued>()

    /** The plume of the row [key], if it has one. */
    internal fun plumeOf(key: Any): SmokePlume? = plumes[key]

    /**
     * The plume of [row] itself, if it has one: not one left by the row's key on another screen.
     */
    internal fun plumeOf(row: SmokeRowState): SmokePlume? =
        plumes[row.key]?.takeIf { it.row === row }

    /**
     * Burns the row [key] away to [side], as a swipe all the way would, after [delay] seconds: the
     * row's action for that side runs once it is all smoke. Returns whether the row stands, has an
     * action that way and isn't already going, or waiting to. Burning a row now takes the place of
     * a burn it was waiting for.
     */
    public fun swipeAway(key: Any, side: SmokeSide, delay: Float = 0f): Boolean {
        val row = rows[key] ?: return false
        if (!row.present || row.actionFor(side) == null || busy(row)) return false
        if (delay > 0f) {
            if (queued.any { it.key == key }) return false
            queued += Queued(now + delay, key, side)
            awake = true
            return true
        }
        queued.removeAll { it.key == key }
        return startAuto(key, side)
    }

    // Whether [row] has smoke of its own going, or kept for an undo. Smoke its key left on another
    // screen, kept for that screen's undo, is let go: this row is the chat now.
    private fun busy(row: SmokeRowState): Boolean {
        val plume = plumes[row.key] ?: return false
        if (plume.phase == PlumePhase.Burnt && plume.row !== row) {
            forget(row.key)
            return false
        }
        return true
    }

    /** Whether row [key] is waiting for a burn [swipeAway] put off. */
    public fun isWaiting(key: Any): Boolean = queued.any { it.key == key }

    // Starts the row burning by itself, if it still stands and nothing else has it by now.
    private fun startAuto(key: Any, side: SmokeSide): Boolean {
        val row = rows[key] ?: return false
        if (!row.present || busy(row)) return false
        val action = row.actionFor(side) ?: return false
        val plume = start(row, action, side)
        plume.phase = PlumePhase.Burning
        plume.speed = SmokeDimens.AutoSpeed
        if (action.material == SmokeMaterial.Mist) {
            sound.play(Sound.MistWhoosh, SmokeDimens.MistVolume)
        }
        return true
    }

    /**
     * Breathes the smoke of the row [key] back into it, as an undo does: the row opens again, and
     * after [delay] more seconds the smoke flows back along the paths it left by. Returns false if
     * there is no smoke kept for it, when the row simply comes back.
     */
    public fun reform(key: Any, delay: Float = 0f): Boolean {
        val plume = plumes[key] ?: return false
        if (plume.phase != PlumePhase.Burnt) return false
        // The row may have been made again since, on another screen: the smoke comes back to it.
        rows[key]?.let {
            plume.row = it
            it.gone = false
        }
        plume.phase = PlumePhase.Waiting
        plume.reformAt = now + SmokeDimens.ReformDelay + delay
        plume.reformSeconds = SmokeDimens.ReformUndo
        awake = true
        return true
    }

    /**
     * Lets the smoke kept for the row [key] go: it can't be breathed back in after this, and the
     * row can burn again. What is still in the air drifts on until it has faded.
     */
    public fun forget(key: Any) {
        val plume = plumes[key] ?: return
        if (plume.phase != PlumePhase.Burnt) return
        plumes.remove(key)
        if (plume.visibleAt(now)) fading += plume
    }

    /** Stops the burn loop, as the app goes to the background; it starts again with a burn. */
    internal fun quiet() {
        loop?.stop()
        loop = null
    }

    /** Clears all the smoke at once and brings every row back, as it was. */
    public fun clear() {
        plumes.clear()
        fading.clear()
        queued.clear()
        drawn.clear()
        for (row in rows.values) {
            row.gone = false
            row.dragging = false
            row.tick++
        }
        loop?.stop()
        loop = null
        frame++
    }

    // ---- The finger's drag, from a row or a scripted fingertip.

    /**
     * A finger has started to swipe row [key] to [side], as [SmokeRow] calls it once a drag has
     * gone far enough across, or as a scripted fingertip does. Returns whether that way does
     * anything: not while the row is burning, reforming, keeping its smoke for an undo, or waiting
     * its turn in a wave, which it can't be taken out of.
     */
    public fun dragStart(key: Any, side: SmokeSide): Boolean {
        val row = rows[key] ?: return false
        if (!row.present || busy(row) || isWaiting(key)) return false
        val action = row.actionFor(side) ?: return false
        val plume = start(row, action, side)
        plume.phase = PlumePhase.Held
        row.dragging = true
        return true
    }

    /**
     * The finger on row [key] is [dx] px across from where its swipe started (negative is left),
     * moving [velocity] px a second. The row burns this far, times [SmokeDimens.Pull], from the
     * edge it burns from, and only ever onwards.
     */
    public fun dragTo(key: Any, dx: Float, velocity: Float) {
        val plume = plumes[key] ?: return
        if (plume.phase != PlumePhase.Held) return
        plume.dragged = if (plume.travel < 0f) -dx else dx
        plume.velocity = velocity
        awake = true
    }

    /**
     * The finger lets go of row [key], moving [velocity] px a second across. Once a third of the
     * row has burnt, or flung its way, the row burns the rest of the way; short of it, the smoke is
     * drawn back in and the row is whole again.
     */
    public fun release(key: Any, velocity: Float) {
        val row = rows[key]
        row?.dragging = false
        val plume = plumes[key] ?: return
        if (plume.phase != PlumePhase.Held) return
        val flung = velocity * plume.travel > SmokeDimens.FlingSpeed * density
        if (plume.front.burnt > SmokeDimens.Commit || flung) {
            plume.phase = PlumePhase.Burning
            plume.speed = maxOf(SmokeDimens.AutoSpeed * 0.8f, abs(velocity) / density * 0.8f)
            onCommitted()
            if (plume.material == SmokeMaterial.Mist) {
                sound.play(Sound.MistWhoosh, SmokeDimens.MistVolume)
            }
        } else {
            plume.phase = PlumePhase.Reforming
            plume.reformAt = now
            plume.reformSeconds = SmokeDimens.ReformShort
            plume.reform = 0f
            sound.play(Sound.Inhale, SmokeDimens.ShortInhaleVolume, SmokeDimens.ShortInhaleRate)
        }
        awake = true
    }

    /**
     * Where row [key] is in the [SmokeOverlay], in px, if both are laid out: where a scripted
     * fingertip comes down on it.
     */
    public fun boundsOf(key: Any): Rect? {
        val row = rows[key] ?: return null
        val coordinates = row.coordinates ?: return null
        val overlay = overlay ?: return null
        if (!coordinates.isAttached || !overlay.isAttached) return null
        val at = overlay.localPositionOf(coordinates, Offset.Zero)
        val size = coordinates.size
        return Rect(at.x, at.y, at.x + size.width, at.y + size.height)
    }

    // Starts a plume on [row] for [action] to [side]: the front at the edge it burns from, and
    // the row sampled into particles in the background, which join the smoke as soon as they are
    // ready. Their columns' crossing times are already stamped, so they come in where they should.
    private fun start(row: SmokeRowState, action: SmokeAction, side: SmokeSide): SmokePlume {
        val bounds = boundsOf(row.key)
        val plume = SmokePlume(action.material, fromRight = side == SmokeSide.Left)
        plume.action = action
        plume.row = row
        if (bounds != null) {
            plume.originX = bounds.left
            plume.originY = bounds.top
            plume.width = bounds.width
            plume.height = bounds.height
        }
        plumes[row.key] = plume
        val layer = row.layer
        val scope = scope
        if (layer != null && scope != null && bounds != null && bounds.width >= 1f) {
            val budget = budget()
            val seed = row.key.hashCode()
            scope.launch {
                try {
                    val bitmap = layer.toImageBitmap()
                    val width = bitmap.width
                    val height = bitmap.height
                    val sampled =
                        withContext(Dispatchers.Default) {
                            val pixels = IntArray(width * height)
                            bitmap.readPixels(pixels)
                            RowSampler.sample(pixels, width, height, budget, seed)
                        }
                    // The drawing may be at another scale than the row is laid out at.
                    val sx = plume.width / width
                    val sy = plume.height / height
                    if (sx != 1f || sy != 1f) {
                        for (i in 0 until sampled.count) {
                            sampled.x[i] *= sx
                            sampled.y[i] *= sy
                        }
                    }
                    plume.particles = sampled
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // A row that can't be drawn into a bitmap still burns; it just has no smoke.
                    log.w(e) { "Could not sample a row" }
                }
            }
        }
        row.tick++
        awake = true
        return plume
    }

    // How many particles a new row may have: thinner while many are already in the air.
    private fun budget(): Int {
        var live = 0
        for (plume in plumes.values) {
            if (plume.visibleAt(now)) live += plume.particles?.count ?: SmokeDimens.PerRow
        }
        val room = SmokeDimens.MostLive - live
        return room.coerceIn(SmokeDimens.LeastPerRow, SmokeDimens.PerRow)
    }

    // ---- The frame loop.

    /**
     * Moves everything on by [seconds]; once nothing moves and no smoke is in the air, it sleeps.
     */
    internal fun advance(seconds: Float) {
        val dt = seconds.coerceIn(0f, SmokeDimens.MostFrameSeconds)
        now += dt
        runQueued()
        var burning = 0f
        var moving = false
        stepping.clear()
        for ((key, plume) in plumes) stepping += key to plume
        for ((key, plume) in stepping) {
            val row = plume.row
            when (plume.phase) {
                PlumePhase.Held -> {
                    val width = plume.width.coerceAtLeast(1f)
                    val target = (SmokeDimens.Pull * plume.dragged / width).coerceIn(0f, 1f)
                    plume.front.advanceTo(target, now, dt)
                    if (!plume.armed && plume.front.burnt > SmokeDimens.Commit) {
                        plume.armed = true
                        onThreshold()
                    }
                    if (plume.material == SmokeMaterial.Smoke) {
                        val pace = abs(plume.velocity) / density / SmokeDimens.AutoTop
                        burning += SmokeDimens.HoldVolume + pace.coerceAtMost(1f) * 0.6f
                    }
                    moving = true
                }
                PlumePhase.Burning -> {
                    plume.speed =
                        (plume.speed + SmokeDimens.AutoGain * dt).coerceAtMost(SmokeDimens.AutoTop)
                    val width = plume.width.coerceAtLeast(1f)
                    val step = plume.speed * density * dt / width
                    plume.front.advanceTo(plume.front.burnt + step, now, dt)
                    if (plume.material == SmokeMaterial.Smoke) {
                        burning += plume.speed / SmokeDimens.AutoTop
                    }
                    if (plume.front.done) {
                        plume.phase = PlumePhase.Burnt
                        plume.burntAt = now
                        if (!plume.told) {
                            plume.told = true
                            plume.action?.onCommit?.invoke()
                        }
                    }
                    moving = true
                }
                PlumePhase.Burnt -> {
                    if (!plume.closed && now - plume.burntAt >= SmokeDimens.CloseDelay) {
                        plume.closed = true
                        row?.gone = true
                    }
                }
                PlumePhase.Waiting -> {
                    if (now >= plume.reformAt) {
                        // The row may have moved while it was away: the smoke comes back to where
                        // it is now.
                        boundsOf(key)?.let {
                            plume.originX = it.left
                            plume.originY = it.top
                        }
                        plume.phase = PlumePhase.Reforming
                        plume.reform = 0f
                        // One breath for a wave of rows coming back, not one each.
                        if (now - inhaledAt > SmokeDimens.InhaleGap) {
                            inhaledAt = now
                            sound.play(Sound.Inhale, SmokeDimens.InhaleVolume)
                        }
                    }
                    moving = true
                }
                PlumePhase.Reforming -> {
                    plume.reform = ((now - plume.reformAt) / plume.reformSeconds).coerceIn(0f, 1f)
                    if (plume.reform >= 1f) {
                        plumes.remove(key)
                        row?.gone = false
                    }
                    moving = true
                }
            }
            row?.tick?.let { row.tick = it + 1 }
        }
        for (row in rows.values) {
            if (row.stepHeight(dt, plumeOf(row))) moving = true
        }
        fading.removeAll { !it.visibleAt(now) }
        drawn.clear()
        drawn += fading
        for (plume in plumes.values) if (plume.visibleAt(now)) drawn += plume
        fill()
        follow(burning, dt)
        frame++
        if (!moving && drawn.isEmpty() && queued.isEmpty()) {
            awake = false
            quiet()
        }
    }

    // Works out where the particles in the air are now, into each plume's spare batches, and
    // draws them in place of the last once they are ready: off the main thread, in one job for
    // every plume, when there is a scope to run it in, so a wave of rows burning costs the frame
    // nothing. While the last frame's job is still going, the plumes keep the batches they have.
    private fun fill() {
        if (drawn.isEmpty()) return
        val time = now
        val scope = scope
        if (scope == null) {
            for (plume in drawn) {
                val reform = plume.reformProgress
                painter.fill(plume, plume.spare, time, reform, plume.reformAt, density)
                swap(plume, reform)
            }
            return
        }
        if (filling) return
        filling = true
        // The job's own copy of what it fills, reused: only one job runs at a time.
        val plumes = jobPlumes
        plumes.clear()
        plumes += drawn
        if (jobReforms.size < plumes.size) jobReforms = FloatArray(plumes.size * 2)
        val reforms = jobReforms
        for (i in plumes.indices) reforms[i] = plumes[i].reformProgress
        val density = density
        scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    for (i in plumes.indices) {
                        val plume = plumes[i]
                        painter.fill(plume, plume.spare, time, reforms[i], plume.reformAt, density)
                    }
                }
                for (i in plumes.indices) swap(plumes[i], reforms[i])
                frame++
            } finally {
                filling = false
            }
        }
    }

    private fun swap(plume: SmokePlume, reform: Float) {
        val shown = plume.batches
        plume.batches = plume.spare
        plume.spare = shown
        plume.batchesReform = reform
    }

    private fun runQueued() {
        if (queued.isEmpty()) return
        var i = 0
        while (i < queued.size) {
            val next = queued[i]
            if (next.at <= now) {
                queued.removeAt(i)
                startAuto(next.key, next.side)
            } else i++
        }
    }

    // The burn loop follows how fast the rows are burning, and stops a moment after the last.
    private fun follow(burning: Float, dt: Float) {
        if (burning > 0f) {
            quietFor = 0f
            val playing = loop ?: sound.loop(Sound.BurnLoop, 0f).also { loop = it }
            playing.setVolume((burning * SmokeDimens.LoopVolume).coerceAtMost(1f))
        } else if (loop != null) {
            quietFor += dt
            loop?.setVolume(0f)
            if (quietFor > SmokeDimens.LoopTail) {
                loop?.stop()
                loop = null
            }
        }
    }

    private companion object {
        val log = Logger.withTag("Smoke")
    }
}

/**
 * A row of a [SmokeOverlay], as its [SmokeRow] registers it: where it is laid out, the drawing it
 * is sampled from, what swiping it either way does, and how tall it stands, from 0 closed to 1.
 */
@Stable
internal class SmokeRowState(val key: Any, present: Boolean) {
    var coordinates: LayoutCoordinates? = null
    var layer: GraphicsLayer? = null
    var left: SmokeAction? = null
    var right: SmokeAction? = null
    var present = present

    /** Burnt away and closed up: it stays closed until it is reformed or comes back. */
    var gone = false

    /** Whether a finger holds it. */
    var dragging = false

    /** How tall it stands, 0 to 1. Read in layout. */
    var height by mutableFloatStateOf(if (present) 1f else 0f)
    private var heightSpeed = 0f

    /** Bumped whenever the row's cut or edge changes. Read in drawing. */
    var tick by mutableIntStateOf(0)

    fun actionFor(side: SmokeSide): SmokeAction? = if (side == SmokeSide.Left) left else right

    /** Springs the row's height on by [dt]; returns whether it is still moving. */
    fun stepHeight(dt: Float, plume: SmokePlume?): Boolean {
        val holding =
            plume != null &&
                (plume.phase == PlumePhase.Held ||
                    plume.phase == PlumePhase.Burning ||
                    (plume.phase == PlumePhase.Burnt && !plume.closed))
        val target = if (holding || (present && !gone)) 1f else 0f
        if (height == target && heightSpeed == 0f) return false
        var x = height
        var v = heightSpeed
        var left = dt
        while (left > 0f) {
            val h = minOf(left, SmokeDimens.HeightStep)
            v += (SmokeDimens.HeightStiffness * (target - x) - SmokeDimens.HeightDamping * v) * h
            x += v * h
            if (x < 0f) {
                x = 0f
                v = 0f
            }
            left -= h
        }
        if (abs(target - x) < 0.004f && abs(v) < 0.05f) {
            x = target
            v = 0f
        }
        height = x
        heightSpeed = v
        return true
    }
}

/**
 * A [SmokeState] for a [SmokeOverlay] and its [SmokeRow]s. Nothing in it is saved: smoke is a
 * moment, and a screen made again starts with the rows whole.
 */
@Composable public fun rememberSmokeState(): SmokeState = remember { SmokeState() }

/** A [SmokeState] that plays its burning, mist and undo through [sound]. */
@Composable
internal fun rememberSmokeState(sound: SoundPlayer): SmokeState = remember {
    SmokeState()
}
    .also { it.sound = sound }
