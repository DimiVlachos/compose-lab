package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A photo card on the table, in dp from the table's top left. [spot] is where it first lay, as a
 * share of the table's width and height, so it keeps its place on a table of another size; [home]
 * is that spot on this table, where it slides back to when nothing holds it. [strengths] say how
 * well it matches each tag, 0 to 1; a tag it doesn't list it doesn't match at all.
 */
internal class PhotoBody(
    val id: String,
    val strengths: Map<String, Float>,
    val spot: Offset,
    val tilt: Float = 0f,
) {
    var home = Offset.Zero
    var at = Offset.Zero
    var velocity = Offset.Zero

    /** How shaded it is, 0 to 1: it fades in and out on its own as magnets come and go. */
    var shade = 0f

    /** Held by a finger: it goes where the finger takes it and feels no magnet. */
    var held = false

    /** The magnets it is stuck to, in the order it stuck: one, or two when it hangs between. */
    val stuckTo: MutableSet<String> = LinkedHashSet()

    /**
     * The magnets a hand has pulled it off: it doesn't stick to them, or feel their pull, until
     * they go back to the strip.
     */
    val refused: MutableSet<String> = HashSet()

    /**
     * Where on its cluster a stuck card sits, from the cluster's anchor; null until it is given
     * one, and again whenever the magnets it is on change.
     */
    var slot: Offset? = null

    fun strength(tag: String): Float = (strengths[tag] ?: 0f).coerceIn(0f, 1f)

    fun matches(tag: String): Boolean = strength(tag) >= MagnetDimens.StickThreshold

    /** Whether [magnet] pulls it at all: it matches the tag, and hasn't been pulled off it. */
    fun feels(magnet: Magnet): Boolean = magnet.tag !in refused && strength(magnet.tag) > 0f

    /** Whether any of [magnets] out of the strip pulls it. */
    fun feelsAny(magnets: List<Magnet>): Boolean {
        for (magnet in magnets) if (magnet.out && feels(magnet)) return true
        return false
    }
}

/** Which layer a card is drawn and touched in: loose at the bottom, then stuck, then held. */
internal fun PhotoBody.layer(): Int =
    when {
        held -> 2
        stuckTo.isNotEmpty() -> 1
        else -> 0
    }

/**
 * The photo cards on the table, stepped together. A loose card is tied home by a weak spring and
 * pulled by every magnet out, by how well it matches; touching a magnet it matches well enough, it
 * sticks, and clings to it from then on. Cards push each other apart, keep out of the magnets they
 * aren't stuck to, and stay on the table.
 */
internal class PhotoBodies(val bodies: List<PhotoBody>) {
    /** The table's size, in dp: the cards are kept on it. */
    var width = 0f
        private set

    var height = 0f
        private set

    /** How many times a photo has stuck since this was last zeroed: a tick of haptics a time. */
    var sticks = 0

    /** How well the best match that stuck since [sticks] was zeroed matched its magnet. */
    var strongest = 0f

    private fun noteStick(strength: Float) {
        sticks++
        strongest = max(strongest, strength)
    }

    private var quiet = 0

    // Where each card was at the start of a step: once the cards are pushed apart, their speed is
    // what they really moved, so a card pressed into a cluster comes to rest instead of pushing on.
    private val startX = FloatArray(bodies.size)
    private val startY = FloatArray(bodies.size)

    /** Whether every card has been still for a while, so the frame loop can sleep. */
    val atRest: Boolean
        get() = quiet >= MagnetDimens.QuietSteps

    /**
     * Lays the cards out on a table [width] × [height] dp: the first time each at its spot, after
     * that moved with the table as it grows or shrinks.
     */
    fun resize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val sx = if (this.width > 0f) width / this.width else 0f
        val sy = if (this.height > 0f) height / this.height else 0f
        for (body in bodies) {
            body.home = Offset(body.spot.x * width, body.spot.y * height)
            body.at = if (sx > 0f) Offset(body.at.x * sx, body.at.y * sy) else body.home
        }
        this.width = width
        this.height = height
        keepOnTable()
        quiet = 0
    }

    /**
     * Steps every card on by [dt] seconds among [magnets], the ones out of the strip, and says
     * whether any card stuck to or came off a magnet.
     */
    fun step(dt: Float, magnets: List<Magnet>): Boolean {
        var changed = false
        for (i in bodies.indices) {
            startX[i] = bodies[i].at.x
            startY[i] = bodies[i].at.y
        }
        for (body in bodies) {
            if (body.held) {
                body.velocity = Offset.Zero
                continue
            }
            if (rebridge(body, magnets)) changed = true
            val acceleration =
                if (body.stuckTo.isNotEmpty()) {
                    val anchor = anchor(body, magnets)
                    val slot = body.slot ?: assignSlot(body, anchor, magnets)
                    (anchor + slot - body.at) * MagnetDimens.Cling - body.velocity * ClingDamping
                } else {
                    var pull =
                        (body.home - body.at) * MagnetDimens.Leash -
                            body.velocity * MagnetDimens.Friction
                    for (magnet in magnets) {
                        if (magnet.tag in body.refused) continue
                        val toward = MagnetField.pull(body.at, magnet.at, body.strength(magnet.tag))
                        pull += if (body.matches(magnet.tag)) toward else leanOnly(toward)
                    }
                    pull
                }
            body.velocity += acceleration * dt
            body.at += body.velocity * dt
        }
        for (body in bodies) if (stick(body, magnets)) changed = true
        repeat(MagnetDimens.SeparationPasses) {
            separate(magnets)
            keepOffMagnets(magnets)
        }
        keepOnTable()
        var moving = false
        for (i in bodies.indices) {
            val body = bodies[i]
            if (body.held) continue
            body.velocity = Offset((body.at.x - startX[i]) / dt, (body.at.y - startY[i]) / dt)
            if (body.velocity.getDistance() > MagnetDimens.StillSpeed) moving = true
        }
        quiet = if (moving || changed) 0 else quiet + 1
        return changed
    }

    /**
     * Lets go of everything stuck to [tag]'s magnet, gone back to the strip, and forgets that any
     * card refused it. Says whether anything was stuck to it.
     */
    fun releaseAll(tag: String): Boolean {
        var changed = false
        for (body in bodies) {
            if (body.stuckTo.remove(tag)) {
                body.slot = null
                changed = true
            }
            body.refused.remove(tag)
        }
        quiet = 0
        return changed
    }

    /** A finger takes [body]: off whatever magnets it was on, which it now refuses. */
    fun grab(body: PhotoBody) {
        body.held = true
        body.refused += body.stuckTo
        body.stuckTo.clear()
        body.slot = null
        body.velocity = Offset.Zero
        quiet = 0
    }

    /** The finger lets [body] go where it is. */
    fun drop(body: PhotoBody) {
        body.held = false
        quiet = 0
    }

    /** The topmost card under [point], in dp, if any. */
    fun at(point: Offset): PhotoBody? {
        val halfWidth = MagnetDimens.CardWidth.value / 2f
        val halfHeight = MagnetDimens.CardHeight.value / 2f
        for (layer in 2 downTo 0) {
            for (i in bodies.indices.reversed()) {
                val body = bodies[i]
                if (body.layer() != layer) continue
                if (abs(point.x - body.at.x) <= halfWidth && abs(point.y - body.at.y) <= halfHeight)
                    return body
            }
        }
        return null
    }

    /** Keeps every card's middle far enough in that the whole card is on the table. */
    fun keepOnTable() {
        if (width <= 0f || height <= 0f) return
        val halfWidth = MagnetDimens.CardWidth.value / 2f
        val halfHeight = MagnetDimens.CardHeight.value / 2f
        for (body in bodies) {
            val x = body.at.x.coerceIn(halfWidth, max(halfWidth, width - halfWidth))
            val y = body.at.y.coerceIn(halfHeight, max(halfHeight, height - halfHeight))
            if (x != body.at.x) body.velocity = Offset(0f, body.velocity.y)
            if (y != body.at.y) body.velocity = Offset(body.velocity.x, 0f)
            body.at = Offset(x, y)
        }
    }

    private fun canStick(body: PhotoBody, magnet: Magnet): Boolean =
        magnet.tag !in body.stuckTo && magnet.tag !in body.refused && body.matches(magnet.tag)

    private fun bridged(a: Magnet, b: Magnet, stretch: Float = 1f): Boolean =
        (a.at - b.at).getDistance() <= MagnetDimens.BridgeSpan.value * stretch

    private fun stick(body: PhotoBody, magnets: List<Magnet>): Boolean {
        if (body.held) return false
        val ring = MagnetDimens.ContactRing.value
        // Matching both of two magnets close together, it comes to rest between them, out of
        // reach of either's ring: touching the point between them, it sticks to both.
        if (magnets.size == 2 && body.stuckTo.isEmpty()) {
            val a = magnets[0]
            val b = magnets[1]
            if (canStick(body, a) && canStick(body, b) && bridged(a, b)) {
                if ((body.at - (a.at + b.at) / 2f).getDistance() <= ring) {
                    body.stuckTo += a.tag
                    body.stuckTo += b.tag
                    body.slot = null
                    noteStick(max(body.strength(a.tag), body.strength(b.tag)))
                    return true
                }
            }
        }
        var changed = false
        for (magnet in magnets) {
            if (!canStick(body, magnet)) continue
            if ((body.at - magnet.at).getDistance() > ring && !touchesCluster(body, magnet.tag))
                continue
            body.stuckTo += magnet.tag
            body.slot = null
            noteStick(body.strength(magnet.tag))
            changed = true
        }
        return changed
    }

    // As pins chain on a magnet: a card that matches, kept from the magnet by the cards already
    // on it, sticks where it touches them rather than pushing them round and round.
    private fun touchesCluster(body: PhotoBody, tag: String): Boolean {
        val reach = MagnetDimens.CardSpan.value + ChainSlack
        for (other in bodies) {
            if (other === body || tag !in other.stuckTo) continue
            if ((other.at - body.at).getDistance() <= reach) return true
        }
        return false
    }

    // Stuck to one of two bridged magnets and matching the other, it is shared; on both and
    // pulled far apart, it keeps the one it matches better, or the first it reached.
    private fun rebridge(body: PhotoBody, magnets: List<Magnet>): Boolean {
        if (body.stuckTo.isEmpty() || magnets.size < 2) return false
        val a = magnets[0]
        val b = magnets[1]
        if (body.stuckTo.size == 1) {
            val other =
                when (body.stuckTo.first()) {
                    a.tag -> b
                    b.tag -> a
                    else -> return false
                }
            if (!canStick(body, other) || !bridged(a, b)) return false
            body.stuckTo += other.tag
            body.slot = null
            noteStick(body.strength(other.tag))
            return true
        }
        if (bridged(a, b, MagnetDimens.BridgeLetGo)) return false
        // Too far apart to share it, it goes with the one pulling it harder as they part: how well
        // it matches each, over how far it is from each. Drawn apart slowly it hangs near the
        // middle
        // and the better match keeps it; yanked apart, it lags behind towards the magnet that
        // stayed, and that one keeps it.
        val pullA = grip(body, a)
        val pullB = grip(body, b)
        val keep =
            when {
                pullA > pullB -> a.tag
                pullB > pullA -> b.tag
                else -> body.stuckTo.first()
            }
        body.stuckTo.clear()
        body.stuckTo += keep
        body.slot = null
        return true
    }

    // Gives a stuck card its place on the cluster: the innermost ring with room, and on it the
    // free place nearest the card, clear of the pucks it is on. Places are a card's span apart,
    // so a cluster comes to rest rather than shuffling for room.
    private fun assignSlot(body: PhotoBody, anchor: Offset, magnets: List<Magnet>): Offset {
        val inner = MagnetDimens.ClusterInner.value
        var best = Offset.Zero
        var bestRing = Int.MAX_VALUE
        var nearest = Float.MAX_VALUE
        for (k in Slots.indices) {
            val ring = SlotRings[k]
            if (ring > bestRing) break
            val offset = Slots[k]
            val at = anchor + offset
            if (magnets.any { it.tag in body.stuckTo && (at - it.at).getDistance() < inner })
                continue
            if (bodies.any { it !== body && it.slot == offset && it.stuckTo == body.stuckTo })
                continue
            val distance = (at - body.at).getDistance()
            if (ring < bestRing || distance < nearest) {
                best = offset
                bestRing = ring
                nearest = distance
            }
        }
        body.slot = best
        return best
    }

    // A weak match's pull, no stronger than its leash can hold MostLean from home: it leans in,
    // and stops short of the cluster rather than running in against it.
    private fun leanOnly(pull: Offset): Offset {
        val most = MagnetDimens.Leash * MagnetDimens.MostLean.value
        val size = pull.getDistance()
        return if (size <= most) pull else pull * (most / size)
    }

    // How hard [magnet] pulls [body] where it is, by the same law as its pull across the table.
    private fun grip(body: PhotoBody, magnet: Magnet): Float {
        val apart = body.at - magnet.at
        return body.strength(magnet.tag) /
            (apart.x * apart.x + apart.y * apart.y + MagnetDimens.Softening)
    }

    // Where a stuck card clings to: its magnet, or halfway between its two.
    private fun anchor(body: PhotoBody, magnets: List<Magnet>): Offset {
        var sum = Offset.Zero
        var count = 0
        for (magnet in magnets) {
            if (magnet.tag !in body.stuckTo) continue
            sum += magnet.at
            count++
        }
        return if (count == 0) body.at else sum / count.toFloat()
    }

    // Pushes overlapping cards apart, each half the overlap, or a card a finger holds not at all.
    // A cluster slides over a loose card that no magnet out pulls, as a magnet glides over the
    // prints on a table, rather than pushing it along.
    private fun separate(magnets: List<Magnet>) {
        val span = MagnetDimens.CardSpan.value
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val p = bodies[i]
                val q = bodies[j]
                if (p.held && q.held) continue
                if (p.stuckTo.isNotEmpty() && q.stuckTo.isEmpty() && !q.feelsAny(magnets)) continue
                if (q.stuckTo.isNotEmpty() && p.stuckTo.isEmpty() && !p.feelsAny(magnets)) continue
                val apart = q.at - p.at
                val distance = apart.getDistance()
                if (distance >= span) continue
                val normal = if (distance < Epsilon) spread(i + j) else apart / distance
                val push = span - distance
                when {
                    p.held -> q.at += normal * push
                    q.held -> p.at -= normal * push
                    else -> {
                        p.at -= normal * (push / 2f)
                        q.at += normal * (push / 2f)
                    }
                }
            }
        }
    }

    // A card a magnet pulls keeps out of its ring unless it is stuck to it, and then out of its
    // middle; a magnet slides over a card it doesn't pull.
    private fun keepOffMagnets(magnets: List<Magnet>) {
        for (i in bodies.indices) {
            val body = bodies[i]
            if (body.held) continue
            for (magnet in magnets) {
                if (magnet.tag !in body.stuckTo && !body.feels(magnet)) continue
                val inner =
                    if (magnet.tag in body.stuckTo) MagnetDimens.ClusterInner.value
                    else MagnetDimens.ContactRing.value
                val apart = body.at - magnet.at
                val distance = apart.getDistance()
                if (distance >= inner) continue
                val normal = if (distance < Epsilon) spread(i) else apart / distance
                body.at = magnet.at + normal * inner
            }
        }
    }

    internal companion object {
        /**
         * Where [count] cards first lie, as shares of the table: a jittered grid three across, in a
         * shuffled order, so the table looks strewn but no two cards start on top of each other.
         */
        fun scatter(count: Int, random: Random): List<Offset> {
            val columns = 3
            val rows = (count + columns - 1) / columns
            val cells = (0 until count).shuffled(random)
            return List(count) { i ->
                val cell = cells[i]
                val column = cell % columns
                val row = cell / columns
                Offset(
                    (column + 0.5f + (random.nextFloat() - 0.5f) * Jitter) / columns,
                    (row + 0.5f + (random.nextFloat() - 0.5f) * Jitter) / rows,
                )
            }
        }

        // How far a card may lie from the middle of its cell, as a share of the cell.
        private const val Jitter = 0.3f

        // The places on a cluster, from its anchor: the anchor itself, which a single magnet's
        // puck covers but two magnets' midpoint leaves free, then rings a card's span apart, each
        // with as many places as fit a span apart round it. Ordered ring by ring.
        private val SlotRadii = floatArrayOf(0f, 46f, 108f, 170f)
        private val Slots: List<Offset>
        private val SlotRings: IntArray

        init {
            val slots = mutableListOf<Offset>()
            val rings = mutableListOf<Int>()
            for ((ring, radius) in SlotRadii.withIndex()) {
                val count =
                    if (radius == 0f) 1
                    else (2f * PI.toFloat() * radius / (MagnetDimens.CardSpan.value + 2f)).toInt()
                for (k in 0 until count) {
                    val angle = 2f * PI.toFloat() * k / count - PI.toFloat() / 2f
                    slots += Offset(cos(angle) * radius, sin(angle) * radius)
                    rings += ring
                }
            }
            Slots = slots
            SlotRings = rings.toIntArray()
        }
    }
}

private const val Epsilon = 1e-3f

// How far past touching, in dp, a card still counts as touching a cluster.
private const val ChainSlack = 2f

// The golden angle: directions spread evenly for cards that land exactly on each other.
private const val GoldenAngle = 2.3999631f

private fun spread(k: Int): Offset = Offset(cos(k * GoldenAngle), sin(k * GoldenAngle))

private val ClingDamping = 2f * sqrt(MagnetDimens.Cling)
