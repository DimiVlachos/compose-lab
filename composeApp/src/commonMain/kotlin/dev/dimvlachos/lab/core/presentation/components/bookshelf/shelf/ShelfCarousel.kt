package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookDimensions
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.PlankLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Point2
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.RingPlacement
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfRing
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.TiltGesture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_carousel_next
import dev.dimvlachos.lab.resources.bookshelf_carousel_previous
import dev.dimvlachos.lab.resources.bookshelf_shelf_take_out
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private const val OVERSCROLL_RESIST = 0.35f
private const val FLING_FRICTION = 1.6f
private const val SNAP_DAMPING = 0.82f
private const val SNAP_STIFFNESS = 140f
private const val SPINE_WINDOW = 8
private const val REVEAL_WINDOW = 5
private const val REVEAL_MS = 200
private const val SHELL_WINDOW = 3
private const val EVICT_MARGIN = 4
private const val SHELL_FADE_MS = 250
private const val SHELL_RESOLUTION = 1.2f
private const val PULL_STEEPNESS = 1.5f
private const val SHADOW_RESOLUTION = 0.5f
private const val SHADOW_RESTORE = 0.5f

@Stable
internal class RingHero(scope: CoroutineScope) {
    val spin = RingState(scope)
    val textures = RingTextures()
    val painter = RingPainter()
    var light by mutableStateOf<Pair<ShelfRing, ShelfLightMaps>?>(null)
    var assets: ShelfAssets? = null
        private set

    var backdrop: GraphicsLayer? = null
    var recorded by mutableIntStateOf(0)

    val restingBookId: Int?
        get() = spin.ring?.books?.getOrNull(spin.restingIndex)?.id

    val maps: ShelfLightMaps?
        get() = light?.takeIf { it.first == spin.ring }?.second

    fun assetsFor(ring: ShelfRing?, size: IntSize): ShelfAssets? {
        val ready = light?.takeIf { it.first == ring }?.second
        val current = assets
        assets =
            when {
                ring == null || ready == null -> null
                current != null && current.maps == ready && current.size == size -> current
                else ->
                    ShelfAssets(
                        ring.heroLayout { id -> ring.rest(id, spin.scroll) },
                        ready,
                        textures.spines,
                        size,
                    )
            }
        return assets
    }
}

@Composable
internal fun rememberRingHero(
    content: ShelfContent,
    size: IntSize,
    lighting: ShelfLighting,
    kit: ShelfKit,
    coverFor: (Int) -> Bitmap?,
): RingHero {
    val densityScope = LocalDensity.current
    val density = densityScope.density
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val hero = remember(scope) { RingHero(scope) }
    val backdrop = rememberGraphicsLayer()
    hero.backdrop = backdrop
    val entries = remember(content) { content.planks.flatten() }
    val ring =
        remember(entries, size, density) {
            if (size == IntSize.Zero || entries.isEmpty()) null
            else ShelfRing(size.width / density, size.height / density, entries.map { it.spec })
        }
    LaunchedEffect(ring) { ring?.let { hero.spin.attach(it, content.readingBookId) } }
    LaunchedEffect(ring, lighting) {
        val target = ring ?: return@LaunchedEffect
        hero.light =
            target to
                withContext(Dispatchers.Default) { bakeShelfLight(target.backdrop(), lighting) }
    }
    LaunchedEffect(ring) {
        val target = ring ?: return@LaunchedEffect
        hero.textures.bake(target, entries, hero.spin, density, kit, coverFor)
    }
    hero.light
        ?.takeIf { it.first == ring }
        ?.second
        ?.let { ready ->
            remember(ready, size, lighting) {
                backdrop.compositingStrategy = CompositingStrategy.Offscreen
                backdrop.record(densityScope, layoutDirection, size) {
                    withDp(density) {
                        drawShelfBackdrop(
                            size.width / density,
                            size.height / density,
                            lighting,
                            ready,
                            kit,
                        )
                    }
                }
                hero.recorded++
            }
        }
    hero.assetsFor(ring, size)
    return hero
}

@Composable
internal fun RingShelfHero(
    content: ShelfContent,
    hero: RingHero,
    shelf: ShelfState,
    lighting: ShelfLighting,
    timeMillis: State<Long>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val sounds = LocalShelfSounds.current
    SideEffect { shelf.ringSpin = hero.spin }
    val edge = ShelfTheme.colors.background
    val entries = remember(content) { content.planks.flatten() }
    val spin = hero.spin
    val ring = spin.ring
    val maps = hero.maps
    val wallShadows = rememberGraphicsLayer()
    val contactShadows = rememberGraphicsLayer()
    val shadowsDrawn = remember(wallShadows, contactShadows) { ShadowsDrawn() }
    val centred by remember(spin) { derivedStateOf { spin.centredIndex } }
    val centredEntry = entries.getOrNull(centred)
    val browsing = shelf.mode == ShelfMode.Shelf || shelf.mode == ShelfMode.Settle

    LaunchedEffect(spin) {
        snapshotFlow { spin.centredIndex }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                sounds.play(Sound.RingTick, volume = 0.5f)
            }
    }

    fun takeOutCentred() {
        centredEntry?.let {
            spin.snapToCentre()
            shelf.openDirect(it.visual.id)
        }
    }

    val previous = stringResource(Res.string.bookshelf_carousel_previous)
    val next = stringResource(Res.string.bookshelf_carousel_next)
    val takeOut = stringResource(Res.string.bookshelf_shelf_take_out)
    // The ring shows once every book a screen holds has its spine, so none is seen arriving blank.
    val reveal = remember(hero) { Animatable(0f) }
    LaunchedEffect(hero) {
        snapshotFlow {
            val target = spin.ring
            target != null &&
                ringReady(
                    target.books.map { it.id },
                    spin.centredIndex,
                    hasLight = hero.maps != null && hero.backdrop != null,
                    baked = hero.textures.spines.keys,
                )
        }
            .first { it }
        shelf.ready = true
        reveal.animateTo(1f, tween(REVEAL_MS))
    }
    Box(modifier.clipToBounds().graphicsLayer { alpha = reveal.value }) {
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(spin, ring, shelf) {
                    val target = ring ?: return@pointerInput
                    awaitEachGesture {
                        handleRingGesture(target, spin, shelf, hero.painter, density.density)
                    }
                }
                .then(
                    if (browsing) {
                        Modifier.semantics {
                                role = Role.Button
                                liveRegion = LiveRegionMode.Polite
                                centredEntry?.let { contentDescription = it.accessibilityLabel }
                                onClick(label = takeOut) {
                                    takeOutCentred()
                                    true
                                }
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction(previous) {
                                            spin.spinBy(-1)
                                            true
                                        },
                                        CustomAccessibilityAction(next) {
                                            spin.spinBy(1)
                                            true
                                        },
                                    )
                            }
                            .onKeyEvent { event -> ringKey(event, spin, ::takeOutCentred) }
                            .focusable()
                    } else {
                        Modifier.clearAndSetSemantics {}
                    },
                ),
        ) {
            val target = ring ?: return@Canvas
            val ready = maps ?: return@Canvas
            hero.recorded
            hero.backdrop?.let { drawLayer(it) }
            val scroll = spin.scroll
            val placed = target.visible(scroll)
            val m = shelf.motion.value
            val missing = m.activeBookId
            val restore = 1f - smoothStep(0f, SHADOW_RESTORE, m.travel * 2f + abs(m.tilt))
            val shadow: (RingPlacement) -> Float = {
                if (entries.getOrNull(it.index)?.visual?.id == missing) restore else 1f
            }
            val painter = hero.painter
            val shadowKey = ShadowKey(target, scroll, missing, restore, size)
            val recordShadows = shadowKey != shadowsDrawn.key
            shadowsDrawn.key = shadowKey
            withDp(density.density) { drawLeaves(ready, leafSway(timeMillis.value)) }
            drawBlurred(wallShadows, painter.wallBlur(target), recordShadows) {
                with(painter) { drawWallShadows(target, placed, shadow) }
            }
            withDp(density.density) { with(painter) { drawRingPlank(target, lighting, scroll) } }
            drawBlurred(contactShadows, painter.contactBlur(target), recordShadows) {
                with(painter) { drawContactShadows(target, placed, shadow) }
            }
            withDp(density.density) {
                with(painter) {
                    for (p in placed) {
                        val entry = entries.getOrNull(p.index)
                        val id = entry?.visual?.id
                        if (entry == null || id == null || id == missing) continue
                        drawRingBook(
                            p,
                            lighting,
                            RingBookAssets(
                                spine = hero.textures.spines[id],
                                shell = hero.textures.shells[id],
                                shellAlpha = hero.textures.fades[id]?.value ?: 0f,
                                plain = { plainFaceColor(it, entry.visual.style) },
                            ),
                        )
                    }
                }
            }
            drawEdgeFades(edge, EDGE_FADE_DP * density.density)
        }
    }
}

private fun DrawScope.drawBlurred(
    layer: GraphicsLayer,
    radiusDp: Float,
    record: Boolean,
    block: dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas.() -> Unit,
) {
    if (record) {
        val radius = radiusDp * density * SHADOW_RESOLUTION
        layer.renderEffect = BlurEffect(radius, radius, TileMode.Decal)
        layer.record(this, layoutDirection, (size * SHADOW_RESOLUTION).toIntSize()) {
            scale(SHADOW_RESOLUTION, SHADOW_RESOLUTION, Offset.Zero) {
                withDp(this@drawBlurred.density) { block() }
            }
        }
    }
    scale(1f / SHADOW_RESOLUTION, 1f / SHADOW_RESOLUTION, Offset.Zero) { drawLayer(layer) }
}

private fun ringKey(event: KeyEvent, spin: RingState, takeOut: () -> Unit): Boolean {
    val step =
        when (event.key) {
            Key.DirectionLeft -> -1
            Key.DirectionRight -> 1
            else -> 0
        }
    if (step != 0) {
        if (event.type == KeyEventType.KeyDown) spin.spinBy(step)
        return true
    }
    val activate =
        event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter
    if (activate && event.type == KeyEventType.KeyUp) takeOut()
    return activate
}

private data class ShadowKey(
    val ring: ShelfRing,
    val scroll: Float,
    val missing: Int?,
    val restore: Float,
    val size: Size,
)

private class ShadowsDrawn {
    var key: ShadowKey? = null
}

@Stable
internal class RingState(private val scope: CoroutineScope) {
    var scroll by mutableFloatStateOf(0f)
        private set

    var ring by mutableStateOf<ShelfRing?>(null)
        private set

    var restingIndex by mutableIntStateOf(-1)
        private set

    private var job: Job? = null

    val centredIndex: Int
        get() = ring?.nearestIndex(scroll) ?: -1

    val isMoving: Boolean
        get() = job?.isActive == true

    fun attach(next: ShelfRing, focusId: Int?) {
        val previous = ring
        val keepId = previous?.books?.getOrNull(previous.nearestIndex(scroll))?.id
        ring = next
        val index = (keepId ?: focusId)?.let(next::indexOf)?.takeIf { it >= 0 } ?: 0
        stop()
        next.books.getOrNull(index)?.let { scroll = it.centre }
        restingIndex = index
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun snapToCentre() {
        val r = ring ?: return
        stop()
        scroll = r.snapTarget(scroll)
        restingIndex = r.nearestIndex(scroll)
    }

    fun dragBy(dx: Float) {
        val r = ring ?: return
        stop()
        restingIndex = -1
        val outside = scroll < r.start || scroll > r.end
        scroll -= dx * if (outside) OVERSCROLL_RESIST else 1f
    }

    fun release(velocity: Float) {
        val r = ring ?: return
        val v = -velocity
        val coasted = exponentialDecay<Float>(FLING_FRICTION).calculateTargetValue(scroll, v)
        settle(r.snapTarget(coasted.coerceIn(r.start, r.end)), v)
    }

    fun spinTo(index: Int) {
        val r = ring ?: return
        r.books.getOrNull(index)?.let { settle(it.centre, 0f) }
    }

    fun spinBy(step: Int) {
        val r = ring ?: return
        spinTo((centredIndex + step).coerceIn(0, r.books.lastIndex))
    }

    private fun settle(target: Float, velocity: Float) {
        stop()
        restingIndex = -1
        val from = scroll
        job = scope.launch {
            animate(
                from,
                target,
                initialVelocity = velocity,
                animationSpec = spring(SNAP_DAMPING, SNAP_STIFFNESS),
            ) { value, _ ->
                scroll = value
            }
            scroll = target
            restingIndex = ring?.nearestIndex(target) ?: -1
        }
    }
}

private class RingTouch(
    val bookIndex: Int?,
    val bookId: Int?,
    val centred: Boolean,
    val grabHeight: Float,
    val canTip: Boolean,
)

private suspend fun AwaitPointerEventScope.handleRingGesture(
    ring: ShelfRing,
    spin: RingState,
    shelf: ShelfState,
    painter: RingPainter,
    density: Float,
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    if (shelf.mode != ShelfMode.Shelf && shelf.mode != ShelfMode.Settle) return
    val canSpin = shelf.mode == ShelfMode.Shelf
    val wasMoving = spin.isMoving
    if (canSpin) spin.stop()
    val touch = ringTouch(ring, spin, painter, down.position / density, wasMoving)
    val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
    val slop = viewConfiguration.touchSlop
    var total = Offset.Zero
    var dragging = false
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
        tracker.addPosition(change.uptimeMillis, change.position)
        if (change.changedToUpIgnoreConsumed()) {
            when {
                dragging -> spin.release(tracker.calculateVelocity().x / density)
                abs(total.x) < slop && abs(total.y) < slop -> ringTap(touch, spin, shelf, canSpin)
                canSpin -> spin.spinBy(0)
            }
            return
        }
        val delta = change.positionChange()
        total += delta
        if (!dragging) {
            val vertical = abs(total.y) > slop && abs(total.y) > abs(total.x) * PULL_STEEPNESS
            when {
                vertical && total.y > 0f -> {
                    ringPull(down, change, touch, total, spin, shelf, canSpin, density)
                    return
                }
                vertical -> {
                    if (canSpin) spin.spinBy(0)
                    return
                }
                canSpin && abs(total.x) > slop -> dragging = true
            }
        }
        if (dragging) {
            change.consume()
            spin.dragBy(delta.x / density)
        }
    }
    if (canSpin) spin.spinBy(0)
}

private fun ringTouch(
    ring: ShelfRing,
    spin: RingState,
    painter: RingPainter,
    point: Offset,
    wasMoving: Boolean,
): RingTouch {
    val hit =
        painter.hit(ring.visible(spin.scroll), Point2(point.x, point.y))
            ?: return RingTouch(null, null, centred = false, grabHeight = 0f, canTip = false)
    val (t, h, _) = hit.pose.dimensions
    val bottom = ring.camera.project(hit.pose.map(Vec3(t / 2f, 0f, 0f))).y
    val top = ring.camera.project(hit.pose.map(Vec3(t / 2f, h, 0f))).y
    return RingTouch(
        bookIndex = hit.index,
        bookId = ring.books[hit.index].id,
        centred = !wasMoving && hit.index == spin.centredIndex,
        grabHeight = TiltGesture.lever(bottom - point.y, bottom - top),
        canTip = true,
    )
}

private fun ringTap(touch: RingTouch, spin: RingState, shelf: ShelfState, canSpin: Boolean) {
    val index = touch.bookIndex
    val id = touch.bookId
    when {
        touch.centred && id != null -> {
            spin.snapToCentre()
            shelf.openDirect(id)
        }
        !canSpin -> Unit
        index != null -> spin.spinTo(index)
        else -> spin.spinBy(0)
    }
}

@Suppress("LongParameterList")
private suspend fun AwaitPointerEventScope.ringPull(
    down: PointerInputChange,
    change: PointerInputChange,
    touch: RingTouch,
    total: Offset,
    spin: RingState,
    shelf: ShelfState,
    canSpin: Boolean,
    density: Float,
) {
    val id = touch.bookId
    when {
        total.y > 0f && touch.canTip && id != null -> {
            change.consume()
            tipBook(down, id, touch.grabHeight, change.position.y / density, shelf, density)
        }
        canSpin -> spin.spinBy(0)
    }
}

@Stable
internal class RingTextures {
    val spines = mutableStateMapOf<Int, Bitmap>()
    val shells = mutableStateMapOf<Int, BookShell>()
    val fades = mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
    private val shellCovers = HashMap<Int, Bitmap?>()

    suspend fun bake(
        ring: ShelfRing,
        entries: List<ShelfEntry>,
        state: RingState,
        density: Float,
        kit: ShelfKit,
        coverFor: (Int) -> Bitmap?,
    ) =
        kotlinx.coroutines.coroutineScope {
            spines.clear()
            shells.clear()
            fades.clear()
            shellCovers.clear()
            while (true) {
                val centre = state.centredIndex.coerceAtLeast(0)
                evict(ring, centre)
                val window = around(centre, SPINE_WINDOW, ring.books.lastIndex)
                val shellWindow = around(centre, SHELL_WINDOW, ring.books.lastIndex)
                val missing = window.filter { ring.books[it].id !in spines }
                val spineAt = missing.firstOrNull()
                val shellAt = shellWindow.firstOrNull { stale(ring.books[it].id, coverFor) }
                when {
                    // Every missing spine at once, in parallel where the platform's text bakers
                    // allow it; each is stored back here, on the ring's own thread.
                    spineAt != null ->
                        kotlinx.coroutines.coroutineScope {
                            missing.forEach { i ->
                                launch { bakeSpine(ring, i, entries[i], density, kit) }
                            }
                        }
                    shellAt != null -> {
                        val id = ring.books[shellAt].id
                        val fresh = id !in shells
                        bakeShell(ring, shellAt, entries[shellAt], density, kit, coverFor)
                        if (fresh) {
                            val fade = Animatable(0f)
                            fades[id] = fade
                            launch { fade.animateTo(1f, tween(SHELL_FADE_MS)) }
                        }
                    }
                    else -> snapshotFlow {
                            state.centredIndex != centre ||
                                shellWindow.any { stale(ring.books[it].id, coverFor) }
                        }
                            .first { it }
                }
            }
        }

    private suspend fun bakeSpine(
        ring: ShelfRing,
        index: Int,
        entry: ShelfEntry,
        density: Float,
        kit: ShelfKit,
    ) {
        val book = ring.books[index]
        val k = ring.scale
        spines[book.id] =
            withContext(ShelfTextBakers) {
                createSpineBitmap(
                        entry.visual,
                        book.thickness / k,
                        book.height / k,
                        k * density,
                        kit,
                    )
                    .apply { prepareToDraw() }
            }
    }

    private suspend fun bakeShell(
        ring: ShelfRing,
        index: Int,
        entry: ShelfEntry,
        density: Float,
        kit: ShelfKit,
        coverFor: (Int) -> Bitmap?,
    ) {
        val book = ring.books[index]
        val k = ring.scale
        val spine = spines[book.id] ?: return
        val cover = coverFor(book.id)
        shellCovers[book.id] = cover
        shells[book.id] =
            withContext(ShelfTextBakers) {
                createBookShell(
                        entry.visual,
                        BookDimensions(book.thickness / k, book.height / k, book.depth / k),
                        k * density * SHELL_RESOLUTION,
                        spine,
                        cover,
                        kit,
                    )
                    .apply { faces.values.forEach(Bitmap::prepareToDraw) }
            }
    }

    private fun evict(ring: ShelfRing, centre: Int) {
        val keep =
            around(centre, SPINE_WINDOW + EVICT_MARGIN, ring.books.lastIndex)
                .map { ring.books[it].id }
                .toSet()
        spines.keys.retainAll(keep)
        val keepShells =
            around(centre, SHELL_WINDOW + EVICT_MARGIN, ring.books.lastIndex)
                .map { ring.books[it].id }
                .toSet()
        shells.keys.retainAll(keepShells)
        fades.keys.retainAll(keepShells)
        shellCovers.keys.retainAll(keepShells)
    }

    private fun stale(id: Int, coverFor: (Int) -> Bitmap?): Boolean =
        id !in shells || coverFor(id) !== shellCovers[id]
}

/**
 * Whether the ring can be shown whole: its light is baked, and so is the spine of every book within
 * [REVEAL_WINDOW] of the one in front, all a screen shows, so no book is seen arriving blank.
 */
internal fun ringReady(ids: List<Int>, centre: Int, hasLight: Boolean, baked: Set<Int>): Boolean =
    hasLight &&
        around(centre.coerceAtLeast(0), REVEAL_WINDOW, ids.lastIndex).all { ids[it] in baked }

private fun around(centre: Int, window: Int, last: Int): List<Int> = buildList {
    add(centre)
    for (step in 1..window) {
        if (centre - step >= 0) add(centre - step)
        if (centre + step <= last) add(centre + step)
    }
}

private fun ShelfRing.backdrop(): ShelfLayout =
    ShelfLayout(
        width = width,
        height = height,
        scale = scale,
        planks = listOf(PlankLayout(plankY, 0f, emptyList(), 0f, width)),
    )
