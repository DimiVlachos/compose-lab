package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import org.jetbrains.compose.resources.DrawableResource

/**
 * A photo on the magnet table: its [image], [title] for a screen reader and under it in a grid, how
 * strongly it matches each tag, 0 to 1, by the tag's id, and a [caption] shown with it opened. A
 * tag it doesn't list it doesn't match.
 */
@Immutable
public data class MagnetPhoto(
    val id: String,
    val image: DrawableResource,
    val title: String,
    val strengths: Map<String, Float>,
    val caption: String = "",
)

/**
 * A tag with a magnet of its own, waiting in the strip: its [id], the [label] it shows, and the
 * [color] its magnet's face and count badge wear, so two magnets out tell apart at a glance. A tag
 * without a colour of its own wears the theme's magnet red.
 */
@Immutable
public data class MagnetTag(
    val id: String,
    val label: String,
    val color: Color = Color.Unspecified,
)

/**
 * A steel table of photos and a strip of tag magnets. A finger takes a magnet with [place], moves
 * it with [drag] and lets it go with [release], at points in the table's own pixels, as a pointer
 * reports them. Matching photos are pulled towards a magnet out on the table, and the strong
 * matches stick: what sticks is the filter's result, [results]. Two magnets out combine: a photo
 * that matches both hangs between them, on both. Flicked back into the strip, a magnet drops what
 * is on it, and the photos slide home. [apply] and [remove] do the same without a finger, as a
 * screen reader does.
 *
 * Only [results] is read in composition, and it changes only when a photo sticks or comes off, or a
 * magnet goes out or back: the cards, the magnets, the filings and the fanned-out grid are read
 * while drawing, so nothing recomposes while they move. It is demo state: nothing is saved when the
 * screen is made again.
 */
@Stable
public class MagnetState
internal constructor(internal val photos: List<MagnetPhoto>, internal val tags: List<MagnetTag>) {
    init {
        require(tags.isNotEmpty()) { "A magnet table needs at least one tag" }
        require(tags.map { it.id }.toSet().size == tags.size) { "Tag ids must be unique" }
        require(photos.map { it.id }.toSet().size == photos.size) { "Photo ids must be unique" }
    }

    /**
     * The photos stuck to each magnet out of the strip, by tag, each in the order the photos were
     * given: the filter's results. A magnet just put down has an empty set.
     */
    public var results: Map<String, Set<String>> by mutableStateOf(emptyMap())
        private set

    /** The magnet whose photos are fanned out into a grid, if any. */
    public var fannedOut: String? by mutableStateOf(null)
        private set

    /** The photo opened from the grid, if any. */
    public var opened: String? by mutableStateOf(null)
        private set

    internal val magnets: List<Magnet> = tags.map { Magnet(it.id) }

    internal val bodies: PhotoBodies = run {
        val random = Random(LayoutSeed)
        val spots = PhotoBodies.scatter(photos.size, random)
        PhotoBodies(
            photos.mapIndexed { i, photo ->
                val tilt = (random.nextFloat() - 0.5f) * 2f * MagnetDimens.MostTilt
                PhotoBody(photo.id, photo.strengths, spots[i], tilt)
            }
        )
    }

    // The magnets out of the strip this step: kept, not made again each step.
    private val pulling = ArrayList<Magnet>(MagnetDimens.MostMagnets)

    // The density the table is laid out at, and its size in dp: the physics works in dp, a
    // pointer in px. The strip is the bottom of it, the table the rest.
    internal var density = 1f
        private set

    internal var width = 0f
        private set

    internal var height = 0f
        private set

    internal val tableHeight: Float
        get() = (height - MagnetDimens.StripHeight.value).coerceAtLeast(0f)

    /** Bumped every step, so whatever draws the cards and magnets draws them again. */
    internal var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever a magnet out of the strip moves, so the filings turn again. */
    internal var fieldVersion by mutableIntStateOf(0)
        private set

    /** Whether anything is moving or held: the frame loop runs while it is. */
    internal var awake by mutableStateOf(false)
        private set

    /** Whose photos the grid shows, as it fans out and as it folds back. */
    internal var fanShown: String? = null
        private set

    /**
     * How settled the filings are with every magnet put away, 0 just put away to 1 lying as they
     * fell, eased; 0 while any magnet is out.
     */
    internal var calm = 1f
        private set

    private var calmTime = MagnetDimens.CalmSeconds

    /** How far the photos no magnet pulls are shaded, 0 with every magnet away to 1. */
    internal var dim = 0f
        private set

    // The photos the grid shows, by index, taken as it fans out: as it folds back, even after their
    // magnet has gone, they fold from the grid to where they are rather than vanish.
    private var fanPhotos = IntArray(0)

    // A magnet asked to fan out while another's grid folds: it fans out once that has folded.
    private var nextFan: String? = null

    /** How far the opened photo has grown out of its cell, 0 in the grid to 1 open. */
    internal var openness = 0f
        private set

    /** Which photo is open, or closing back into the grid, by index; -1 for none. */
    internal var openIndex = -1
        private set

    /** How far the grid has fanned out, 0 folded to 1 out. */
    internal var fan = 0f
        private set

    /** Called after a step in which a photo stuck, at most every few steps: a tick of haptics. */
    internal var onStick: ((strength: Float) -> Unit)? = null

    /** Called as a magnet put away comes to rest in its slot: a soft tick of haptics. */
    internal var onSlot: (() -> Unit)? = null

    private var carry = 0f
    private var sinceTick = MagnetDimens.TickSteps
    private var heldPhoto: PhotoBody? = null

    /**
     * Takes the [tag] magnet, from the strip or the table, and holds it at [at]. Refuses a third
     * magnet out of the strip while two are, and says whether it took it.
     */
    public fun place(tag: String, at: Offset): Boolean {
        val magnet = magnet(tag) ?: return false
        if (width <= 0f) return false
        if (!magnet.out && magnets.count { it.out } >= MagnetDimens.MostMagnets) return false
        magnet.held = true
        magnet.velocity = Offset.Zero
        moveMagnet(magnet, at / density)
        updateResults()
        awake = true
        return true
    }

    /** Moves the held [tag] magnet to [to], kept on the table and clear of the other magnet. */
    public fun drag(tag: String, to: Offset) {
        val magnet = magnet(tag) ?: return
        if (!magnet.held) return
        moveMagnet(magnet, to / density)
        awake = true
    }

    /**
     * Lets go of the held [tag] magnet, moving at [velocity] px a second: flicked or dropped into
     * the strip, it goes back to its slot and drops what is on it; otherwise it stays on the table
     * where it is.
     */
    public fun release(tag: String, velocity: Offset) {
        val magnet = magnet(tag) ?: return
        if (!magnet.held) return
        magnet.held = false
        val landing = magnet.at + velocity / density * MagnetDimens.FlickLead
        if (landing.y >= tableHeight) {
            toStrip(magnet)
        } else {
            magnet.onTable = true
            val r = MagnetDimens.MagnetRadius.value
            magnet.at = Offset(magnet.at.x, min(magnet.at.y, max(r, tableHeight - r)))
            keepApart(magnet)
            fieldVersion++
        }
        updateResults()
        awake = true
    }

    /**
     * Pulls the photo [photoId] off whatever magnets it is on and puts it down at [to]: it is no
     * longer in their results, and won't stick to them again until they go back to the strip.
     */
    public fun pullOff(photoId: String, to: Offset) {
        val body = bodies.bodies.firstOrNull { it.id == photoId } ?: return
        bodies.grab(body)
        body.at = to / density
        bodies.keepOnTable()
        bodies.drop(body)
        updateResults()
        awake = true
    }

    /**
     * Fans the photos on the [tag] magnet out into a grid, or, with null, folds them back. A magnet
     * with nothing on it has nothing to fan out.
     */
    public fun fanOut(tag: String?) {
        if (tag != null && results[tag].isNullOrEmpty()) return
        awake = true
        if (tag == null) opened = null
        val shown = fanShown
        if (tag != null && shown != null && shown != tag && fan > 0f) {
            // Another magnet's grid is out or folding: it folds first, then this one fans out.
            nextFan = tag
            fannedOut = null
            return
        }
        nextFan = null
        fannedOut = tag
        if (tag != null) {
            fanShown = tag
            takeFanPhotos(tag)
        }
    }

    private fun takeFanPhotos(tag: String) {
        val bodies = bodies.bodies
        fanPhotos = IntArray(bodies.count { tag in it.stuckTo })
        var k = 0
        for (i in bodies.indices) if (tag in bodies[i].stuckTo) fanPhotos[k++] = i
    }

    /**
     * Opens the photo [photoId] from the fanned-out grid: it grows out of its cell into a large
     * photo with its title and caption. Only a photo the grid shows opens.
     */
    public fun open(photoId: String) {
        if (fannedOut == null) return
        val index = bodies.bodies.indexOfFirst { it.id == photoId }
        if (index < 0 || index !in fanPhotos) return
        opened = photoId
        openIndex = index
        awake = true
    }

    /** Closes the opened photo back into its cell in the grid. */
    public fun close() {
        opened = null
        awake = true
    }

    /** The photo in the fanned-out grid under [at], in px, if any. */
    internal fun fanPhotoAt(at: Offset): String? {
        if (fannedOut == null || fan < 1f) return null
        val point = at / density
        val scale = fanScale()
        val halfWidth = MagnetDimens.CardWidth.value * scale / 2f
        val halfHeight = MagnetDimens.CardHeight.value * scale / 2f
        for (index in fanPhotos) {
            val centre = photoCentre(index)
            if (abs(point.x - centre.x) <= halfWidth && abs(point.y - centre.y) <= halfHeight)
                return bodies.bodies[index].id
        }
        return null
    }

    /** Whether the [index]th photo is one the grid shows, out or folding. */
    internal fun inFan(index: Int): Boolean = fanShown != null && index in fanPhotos

    /** How much the grid's cards grow, in the grid's own layout. */
    internal fun fanScale(): Float = fanGrid()?.scale ?: 1f

    private fun fanGrid(): FanGrid? {
        if (fanPhotos.isEmpty() || width <= 0f) return null
        return FanGrid(
            fanPhotos.size,
            width,
            tableHeight,
            MagnetDimens.CardWidth.value,
            MagnetDimens.CardHeight.value,
            MagnetDimens.FanGap.value,
            caption = MagnetDimens.FanCaption.value,
            header = MagnetDimens.FanHeader.value,
        )
    }

    /**
     * Where the [index]th photo is drawn, in dp: on the table, or, fanned out, on its way to or in
     * its cell in the grid. Read in layout or drawing.
     */
    internal fun photoCentre(index: Int): Offset {
        frame
        val at = bodies.bodies[index].at
        if (fanShown == null || fan <= 0f) return at
        val k = fanPhotos.indexOf(index)
        if (k < 0) return at
        val grid = fanGrid() ?: return at
        return lerp(at, grid.cell(k), smooth(fan))
    }

    /**
     * Puts the [tag] magnet down in the middle of the table, or, with the other magnet out, beside
     * it, close enough that the two share what matches both. Says whether it could.
     */
    public fun apply(tag: String): Boolean {
        val magnet = magnet(tag) ?: return false
        if (magnet.onTable) return true
        val other = magnets.firstOrNull { it !== magnet && it.out }
        val spot =
            if (other == null) {
                Offset(width / 2f, tableHeight / 2f)
            } else {
                val apart = MagnetDimens.BridgeSpan.value * 0.6f
                other.at + Offset(if (other.at.x < width / 2f) apart else -apart, 0f)
            }
        if (!place(tag, spot * density)) return false
        release(tag, Offset.Zero)
        return true
    }

    /** Puts the [tag] magnet back in the strip: what is on it falls off and slides home. */
    public fun remove(tag: String) {
        val magnet = magnet(tag) ?: return
        if (!magnet.out) return
        toStrip(magnet)
        updateResults()
        awake = true
    }

    /** Lays the table out at [widthPx] × [heightPx] and [density]; the strip is its bottom. */
    internal fun layOut(widthPx: Int, heightPx: Int, density: Float) {
        val width = widthPx / density
        val height = heightPx / density
        if (width == this.width && height == this.height && density == this.density) return
        val oldWidth = this.width
        val oldTable = tableHeight
        this.density = density
        this.width = width
        this.height = height
        bodies.resize(width, tableHeight)
        // The magnets out move with the table as one, keeping their spacing: scaled apart, two
        // that share photos could be pulled past sharing, or squeezed into it, by a rotation.
        var middle = Offset.Zero
        var outCount = 0
        for (magnet in magnets) {
            if (!magnet.out) continue
            middle += magnet.at
            outCount++
        }
        val shift =
            if (outCount > 0 && oldWidth > 0f && oldTable > 0f && tableHeight > 0f) {
                middle /= outCount.toFloat()
                Offset(middle.x * width / oldWidth, middle.y * tableHeight / oldTable) - middle
            } else {
                Offset.Zero
            }
        magnets.forEachIndexed { i, magnet ->
            magnet.slot =
                Offset(
                    width * (i + 0.5f) / magnets.size,
                    height - MagnetDimens.StripHeight.value / 2f,
                )
            if (!magnet.out) {
                magnet.at = magnet.slot
                magnet.velocity = Offset.Zero
            } else {
                magnet.at += shift
            }
        }
        for (magnet in magnets) {
            if (!magnet.out) continue
            moveMagnet(magnet, magnet.at)
            if (magnet.onTable) {
                val r = MagnetDimens.MagnetRadius.value
                magnet.at = Offset(magnet.at.x, min(magnet.at.y, max(r, tableHeight - r)))
            }
        }
        fieldVersion++
        frame++
        awake = true
    }

    /** Steps the table on by [seconds]: at a fixed rate, a long frame cut short. */
    internal fun advance(seconds: Float) {
        if (width <= 0f) {
            awake = false
            return
        }
        carry += seconds.coerceIn(0f, MagnetDimens.MostFrameSeconds)
        var changed = false
        while (carry >= MagnetDimens.StepSeconds) {
            carry -= MagnetDimens.StepSeconds
            stepMagnets(MagnetDimens.StepSeconds)
            pulling.clear()
            for (magnet in magnets) if (magnet.out) pulling += magnet
            if (bodies.step(MagnetDimens.StepSeconds, pulling)) changed = true
            stepFan(MagnetDimens.StepSeconds)
            stepOpen(MagnetDimens.StepSeconds)
            stepCalm(MagnetDimens.StepSeconds)
            stepDim(MagnetDimens.StepSeconds)
            sinceTick++
        }
        if (changed) updateResults()
        if (bodies.sticks > 0) {
            val strongest = bodies.strongest
            bodies.sticks = 0
            bodies.strongest = 0f
            if (sinceTick >= MagnetDimens.TickSteps) {
                sinceTick = 0
                onStick?.invoke(strongest)
            }
        }
        frame++
        if (atRest()) awake = false
    }

    /** The magnet within reach of [at], in px, nearest first: one out, or one in the strip. */
    internal fun magnetAt(at: Offset): String? {
        val point = at / density
        var best: Magnet? = null
        var nearest = MagnetDimens.GrabRadius.value
        for (magnet in magnets) {
            val distance = (magnet.at - point).getDistance()
            if (distance <= nearest) {
                best = magnet
                nearest = distance
            }
        }
        return best?.tag
    }

    internal fun isOut(tag: String): Boolean = magnet(tag)?.out == true

    /** Whether a finger at [at], in px, would take a card: there is one, and no grid is out. */
    internal fun photoUnder(at: Offset): Boolean =
        fanShown == null && heldPhoto == null && bodies.at(at / density) != null

    /** Where the [tag] magnet is, in px. Read in layout or drawing. */
    internal fun magnetPosition(tag: String): Offset? = magnet(tag)?.at?.times(density)

    /** Where the [tag] magnet's slot in the strip is, in px. */
    internal fun slotPosition(tag: String): Offset? = magnet(tag)?.slot?.times(density)

    /** Where the photo [id] is, in px. */
    internal fun photoPosition(id: String): Offset? =
        bodies.bodies.firstOrNull { it.id == id }?.at?.times(density)

    /** The top left of the [index]th photo's card, in px, for its semantics node. */
    internal fun photoTopLeft(index: Int): IntOffset {
        val at = photoCentre(index)
        return IntOffset(
            ((at.x - MagnetDimens.CardWidth.value / 2f) * density).roundToInt(),
            ((at.y - MagnetDimens.CardHeight.value / 2f) * density).roundToInt(),
        )
    }

    /** The top left of the [tag] magnet's touch target, in px, for its semantics node. */
    internal fun magnetTopLeft(tag: String): IntOffset {
        frame
        val at = magnet(tag)?.at ?: Offset.Zero
        val reach = MagnetDimens.GrabRadius.value
        return IntOffset(
            ((at.x - reach) * density).roundToInt(),
            ((at.y - reach) * density).roundToInt(),
        )
    }

    /**
     * Takes the topmost card under [at], in px, off whatever it is on; returns where its middle is,
     * in px, so a finger can carry it by where it took it. No card while the grid is out.
     */
    internal fun grabPhoto(at: Offset): Offset? {
        if (fanShown != null || heldPhoto != null) return null
        val body = bodies.at(at / density) ?: return null
        bodies.grab(body)
        heldPhoto = body
        updateResults()
        awake = true
        return body.at * density
    }

    internal fun dragPhoto(to: Offset) {
        val body = heldPhoto ?: return
        body.at = to / density
        bodies.keepOnTable()
        awake = true
    }

    internal fun dropPhoto() {
        val body = heldPhoto ?: return
        heldPhoto = null
        bodies.drop(body)
        awake = true
    }

    private fun magnet(tag: String): Magnet? = magnets.firstOrNull { it.tag == tag }

    private fun moveMagnet(magnet: Magnet, to: Offset) {
        magnet.at = onStage(to)
        keepApart(magnet)
        fieldVersion++
    }

    private fun onStage(at: Offset): Offset {
        val r = MagnetDimens.MagnetRadius.value
        return Offset(at.x.coerceIn(r, max(r, width - r)), at.y.coerceIn(r, max(r, height - r)))
    }

    // Two magnets never lie on each other: one put down too close is moved off, away from the
    // other, or towards the middle if right on it.
    private fun keepApart(magnet: Magnet) {
        val gap = MagnetDimens.MagnetGap.value
        for (other in magnets) {
            if (other === magnet || !other.out) continue
            val apart = magnet.at - other.at
            val distance = apart.getDistance()
            if (distance >= gap) continue
            val normal =
                if (distance < 1e-3f) Offset(if (other.at.x < width / 2f) 1f else -1f, 0f)
                else apart / distance
            magnet.at = onStage(other.at + normal * gap)
        }
    }

    private fun toStrip(magnet: Magnet) {
        magnet.held = false
        magnet.onTable = false
        bodies.releaseAll(magnet.tag)
        if (fannedOut == magnet.tag || fanShown == magnet.tag) {
            fannedOut = null
            opened = null
        }
        if (nextFan == magnet.tag) nextFan = null
        fieldVersion++
    }

    // A magnet back in the strip slides into its slot on the same stiff spring a stuck card
    // clings with.
    private fun stepMagnets(dt: Float) {
        for (magnet in magnets) {
            if (magnet.out || magnet.at == magnet.slot) continue
            magnet.velocity +=
                ((magnet.slot - magnet.at) * MagnetDimens.Cling - magnet.velocity * SlotDamping) *
                    dt
            magnet.at += magnet.velocity * dt
            if (
                (magnet.slot - magnet.at).getDistance() < MagnetDimens.StillGap &&
                    magnet.velocity.getDistance() < MagnetDimens.StillSpeed
            ) {
                magnet.at = magnet.slot
                magnet.velocity = Offset.Zero
                onSlot?.invoke()
            }
        }
    }

    // The filings settle only once every magnet is away, and are drawn again as they do.
    private fun stepCalm(dt: Float) {
        if (magnets.any { it.out }) {
            calmTime = 0f
            calm = 0f
            return
        }
        if (calmTime >= MagnetDimens.CalmSeconds) return
        calmTime = min(MagnetDimens.CalmSeconds, calmTime + dt)
        val t = calmTime / MagnetDimens.CalmSeconds
        calm = t * t * (3f - 2f * t)
        fieldVersion++
    }

    private fun stepDim(dt: Float) {
        val target = if (magnets.any { it.out }) 1f else 0f
        val step = dt * 1_000f / MagnetDimens.DimMs
        dim = if (dim < target) min(target, dim + step) else max(target, dim - step)
    }

    private fun stepOpen(dt: Float) {
        val target = if (opened != null) 1f else 0f
        val step = dt * 1_000f / MagnetDimens.OpenMs
        openness =
            if (openness < target) min(target, openness + step) else max(target, openness - step)
        if (openness == 0f && opened == null) openIndex = -1
    }

    private fun stepFan(dt: Float) {
        val target = if (fannedOut != null) 1f else 0f
        val step = dt * 1_000f / MagnetDimens.FanMs
        fan = if (fan < target) min(target, fan + step) else max(target, fan - step)
        if (fan == 0f && fannedOut == null) {
            fanShown = null
            fanPhotos = IntArray(0)
            val next = nextFan
            nextFan = null
            if (next != null) fanOut(next)
        }
    }

    private fun atRest(): Boolean =
        heldPhoto == null &&
            magnets.none { it.held } &&
            magnets.all { it.out || it.at == it.slot } &&
            bodies.atRest &&
            (calm == 1f || magnets.any { it.out }) &&
            dim == (if (magnets.any { it.out }) 1f else 0f) &&
            fan == (if (fannedOut != null) 1f else 0f) &&
            openness == (if (opened != null) 1f else 0f) &&
            nextFan == null

    // Results are made again only when a card sticks or comes off, or a magnet goes out or back,
    // and set only if they differ: moving cards never change them.
    private fun updateResults() {
        val next = LinkedHashMap<String, Set<String>>()
        for (magnet in magnets) {
            if (!magnet.out) continue
            val stuck = LinkedHashSet<String>()
            for (body in bodies.bodies) if (magnet.tag in body.stuckTo) stuck += body.id
            next[magnet.tag] = stuck
        }
        if (next != results) results = next
        val shown = fannedOut
        if (shown != null && results[shown].isNullOrEmpty()) fannedOut = null
        val waiting = nextFan
        if (waiting != null && results[waiting].isNullOrEmpty()) nextFan = null
    }

    private companion object {
        // The same strewn table every time.
        const val LayoutSeed = 29
        val SlotDamping = 2f * sqrt(MagnetDimens.Cling)
    }
}

/**
 * A [MagnetState] for [photos] and [tags], a magnet for each tag. It is made again if either
 * changes, and it isn't saved: a table made again starts with every magnet in the strip.
 */
@Composable
public fun rememberMagnetState(photos: List<MagnetPhoto>, tags: List<MagnetTag>): MagnetState =
    remember(photos, tags) { MagnetState(photos, tags) }
