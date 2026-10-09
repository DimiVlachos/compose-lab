package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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

    /**
     * Pushed past a nudge by a magnet or its cluster, it has slipped under them, and slides home
     * beneath them until it is back where it lay.
     */
    var slipping = false

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

    /**
     * Whether [magnet] draws it in: it matches well enough to stick, and hasn't been pulled off it.
     * Only what a magnet will hold moves for it; a weaker match stays where it lies.
     */
    fun drawnTo(magnet: Magnet): Boolean = magnet.tag !in refused && matches(magnet.tag)

    /** Whether any of [magnets] out of the strip draws it in. */
    fun drawnToAny(magnets: List<Magnet>): Boolean {
        for (magnet in magnets) if (magnet.out && drawnTo(magnet)) return true
        return false
    }

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

    // How fast each card was going on its own, before anything pushed it this step.
    private val ownSpeed = FloatArray(bodies.size)

    // Which cards are on their way to a magnet this step.
    private val travelling = BooleanArray(bodies.size)

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
    fun step(dt: Float, magnets: List<Magnet>, joined: Boolean = false): Boolean {
        this.joined = joined && magnets.size == 2
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
            if (regroup(body, magnets)) changed = true
            val acceleration =
                if (body.stuckTo.isNotEmpty()) {
                    val anchor = anchor(body, magnets)
                    val slot = body.slot ?: assignSlot(body, anchor, magnets)
                    (anchor + slot - body.at) * MagnetDimens.Cling - body.velocity * ClingDamping
                } else {
                    gather(body, magnets)
                }
            body.velocity += acceleration * dt
            body.at += body.velocity * dt
        }
        for (i in bodies.indices) ownSpeed[i] = bodies[i].velocity.getDistance()
        for (i in bodies.indices) travelling[i] = drawn(bodies[i], magnets)
        for (body in bodies) if (stick(body, magnets)) changed = true
        repeat(MagnetDimens.SeparationPasses) {
            separate()
            keepOffMagnets(magnets)
        }
        keepOnTable()
        slipUnder(magnets)
        var moving = false
        for (i in bodies.indices) {
            val body = bodies[i]
            if (body.held) continue
            body.velocity = Offset((body.at.x - startX[i]) / dt, (body.at.y - startY[i]) / dt)
            // A card nudged by a magnet it isn't drawn to is moved, not thrown: the shove leaves
            // it no faster than it was going on its own, or a slow slide.
            if (body.stuckTo.isEmpty() && !drawsAny(body, magnets)) {
                val speed = body.velocity.getDistance()
                val most = max(ownSpeed[i], MagnetDimens.NudgeSpeed)
                if (speed > most) body.velocity *= most / speed
            }
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

    /** Takes [tag] off every card on it and another magnet too: they stay with the other. */
    fun leave(tag: String) {
        for (body in bodies) {
            if (body.stuckTo.size < 2 || !body.stuckTo.remove(tag)) continue
            body.slot = null
        }
        quiet = 0
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

    // Two magnets joined are one search: they draw in, and hold, only what matches both.
    private var joined = false

    // A loose card is tied home by its leash; one a magnet out would hold is let off it and drawn
    // in, by every such magnet within reach, or from beyond them all by the nearest, so a search
    // finds every match on the table, near or far.
    private fun gather(body: PhotoBody, magnets: List<Magnet>): Offset {
        val friction = body.velocity * MagnetDimens.Friction
        var pull = Offset.Zero
        var nearest: Magnet? = null
        var nearestDistance = Float.MAX_VALUE
        var inReach = false
        val reach = MagnetDimens.Reach.value
        for (magnet in magnets) {
            if (!draws(body, magnet, magnets)) continue
            val distance = (magnet.at - body.at).getDistance()
            if (distance <= reach) {
                inReach = true
                pull += MagnetField.pull(body.at, magnet.at, body.strength(magnet.tag))
            }
            if (distance < nearestDistance) {
                nearest = magnet
                nearestDistance = distance
            }
        }
        if (nearest == null) return (body.home - body.at) * MagnetDimens.Leash - friction
        if (!inReach) pull = (nearest.at - body.at) / nearestDistance * MagnetDimens.GatherPull
        return pull - friction
    }

    /** Whether a magnet out would draw [body] in and hold it: it is on its way to one. */
    fun drawn(body: PhotoBody, magnets: List<Magnet>): Boolean =
        body.stuckTo.isEmpty() && !body.held && drawsAny(body, magnets)

    private fun draws(body: PhotoBody, magnet: Magnet, magnets: List<Magnet>): Boolean =
        if (joined) magnets.all { body.drawnTo(it) } else body.drawnTo(magnet)

    private fun drawsAny(body: PhotoBody, magnets: List<Magnet>): Boolean =
        if (joined) magnets.all { body.drawnTo(it) } else body.drawnToAny(magnets)

    private fun canStick(body: PhotoBody, magnet: Magnet): Boolean =
        magnet.tag !in body.stuckTo && magnet.tag !in body.refused && body.matches(magnet.tag)

    private fun stick(body: PhotoBody, magnets: List<Magnet>): Boolean {
        if (body.held) return false
        val ring = MagnetDimens.ContactRing.value
        // Joined, the pair holds what matches both, touching either magnet, the point between
        // them, or the cards already on them.
        if (joined) {
            val a = magnets[0]
            val b = magnets[1]
            if (body.stuckTo.isNotEmpty() || !canStick(body, a) || !canStick(body, b)) return false
            val middle = (a.at + b.at) / 2f
            val touching =
                (body.at - a.at).getDistance() <= ring ||
                    (body.at - b.at).getDistance() <= ring ||
                    (body.at - middle).getDistance() <= ring ||
                    touchesCluster(body, a.tag)
            if (!touching) return false
            body.stuckTo += a.tag
            body.stuckTo += b.tag
            body.slot = null
            noteStick(min(body.strength(a.tag), body.strength(b.tag)))
            return true
        }
        for (magnet in magnets) {
            if (body.stuckTo.isNotEmpty() || !canStick(body, magnet)) continue
            if ((body.at - magnet.at).getDistance() > ring && !touchesCluster(body, magnet.tag))
                continue
            body.stuckTo += magnet.tag
            body.slot = null
            noteStick(body.strength(magnet.tag))
            // Apart, the magnets are two searches: it is the first one's, and no one else's.
            return true
        }
        return false
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

    // As two magnets join, what is on either joins the pair if it matches both, and falls off
    // to slide home if it doesn't: the pair is one search, for both tags.
    private fun regroup(body: PhotoBody, magnets: List<Magnet>): Boolean {
        if (!joined || body.stuckTo.isEmpty() || body.stuckTo.size == 2) return false
        val a = magnets[0]
        val b = magnets[1]
        if (body.drawnTo(a) && body.drawnTo(b)) {
            body.stuckTo += a.tag
            body.stuckTo += b.tag
        } else {
            body.stuckTo.clear()
        }
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

    // A loose card no magnet out draws in, pushed further than a nudge from where it lay, slips
    // under them rather than being carried off; home again, it can be nudged again.
    private fun slipUnder(magnets: List<Magnet>) {
        val most = MagnetDimens.MostNudge.value
        val home = MagnetDimens.HomeAgain.value
        for (body in bodies) {
            if (body.held || body.stuckTo.isNotEmpty() || drawsAny(body, magnets)) {
                body.slipping = false
                continue
            }
            val away = (body.at - body.home).getDistance()
            if (!body.slipping && away > most) body.slipping = true
            // Back home, it comes out from under only once nothing lies on it: a cluster resting
            // over its spot would shove it straight out again, and it would kick for ever.
            else if (body.slipping && away < home && clearOfClusters(body, magnets))
                body.slipping = false
        }
    }

    private fun clearOfClusters(body: PhotoBody, magnets: List<Magnet>): Boolean {
        val span = MagnetDimens.CardSpan.value
        for (other in bodies) {
            if (other.stuckTo.isEmpty()) continue
            if ((other.at - body.at).getDistance() < span) return false
        }
        val reach = MagnetDimens.MagnetBody.value
        for (magnet in magnets) {
            if (magnet.out && (magnet.at - body.at).getDistance() < reach) return false
        }
        return true
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

    private fun under(i: Int): Boolean = bodies[i].slipping || travelling[i]

    // Pushes overlapping cards apart, each half the overlap, or a card a finger holds not at all:
    // a cluster carried into a print nudges it aside, and the print slides home once it has gone.
    private fun separate() {
        val span = MagnetDimens.CardSpan.value
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val p = bodies[i]
                val q = bodies[j]
                if (p.held && q.held) continue
                // A card that has slipped under a cluster passes under its cards too, as does one
                // on its way to a magnet, so no cluster in between stops it.
                if (under(i) && q.stuckTo.isNotEmpty() || under(j) && p.stuckTo.isNotEmpty())
                    continue
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

    // A card a magnet draws in keeps out of its ring unless it is stuck to it, and then out of its
    // middle; any other card is only nudged aside by the magnet's body, where it touches it.
    private fun keepOffMagnets(magnets: List<Magnet>) {
        for (i in bodies.indices) {
            val body = bodies[i]
            if (body.held) continue
            for (magnet in magnets) {
                val inner =
                    when {
                        magnet.tag in body.stuckTo -> MagnetDimens.ClusterInner.value
                        draws(body, magnet, magnets) -> MagnetDimens.ContactRing.value
                        body.slipping -> continue
                        else -> MagnetDimens.MagnetBody.value
                    }
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
