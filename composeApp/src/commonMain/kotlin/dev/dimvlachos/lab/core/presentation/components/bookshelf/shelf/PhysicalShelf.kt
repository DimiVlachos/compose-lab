package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookDimensions
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPose
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookRest
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.FaceTexture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.HeldPlacement
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.OpenBand
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.PlankLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Point2
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfBookSpec
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfBookStyle
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfCamera
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfSlot
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.TiltGesture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.buildBookFaces
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.mix
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.onStage
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.place
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withScale
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.BookLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.Flight
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.LeafFaces
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.LeafPainter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageGeometry
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnDimens
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnState
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.RestingBook
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.stackLand
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.stackRise
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_shelf_opened
import dev.dimvlachos.lab.resources.bookshelf_shelf_returned
import dev.dimvlachos.lab.resources.bookshelf_shelf_take_out
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val BLUR_DP = 5f
private const val CHROME_FROM = 0.15f
private const val CHROME_TO = 0.85f
private const val WALL_SHADOW_FADE = 0.5f
internal const val EDGE_FADE_DP = 28f
private const val BAND_SHOWN = 0.5f
private const val RIBBON_FADE_MS = 250
private const val RIBBON_LAND_DELAY_MS = 120L
private const val RIBBON_LAY_MS = 1_700
private const val RIBBON_LAND_SETTLE_MS = 400L
private const val RIBBON_SEEK_WINDOW_MS = 1_500L
private const val WARM_UP_DELAY_MS = 1_200L
internal const val SWAY_FRAME_STRIDE = 4L
private const val LEFT_STACK_FROM = 0.85f
private const val RIGHT_STACK_TO = 0.3f
private const val REST_SHADE_GONE = 0.5f
private const val REST_SHADOW_FROM = 0.35f

internal data class ShelfEntry(
    val visual: ShelfBookVisual,
    val spec: ShelfBookSpec,
    val readFraction: Float?,
    val accessibilityLabel: String,
)

internal data class ShelfContent(val planks: List<List<ShelfEntry>>, val readingBookId: Int?) {
    val entries: Map<Int, ShelfEntry> = planks.flatten().associateBy { it.visual.id }

    val ribbonBookIds: Set<Int> = entries.filterValues { it.readFraction != null }.keys
}

internal enum class ShelfStyle {
    Planks,
    Ring,
}

internal data class ShelfMotion(
    val activeBookId: Int? = null,
    val travel: Float = 0f,
    val open: Float = 0f,
    val tilt: Float = 0f,
    val vanish: Float = 0f,
    val arch: Float = 0f,
    val push: Float = 0f,
    val coverOnly: Boolean = false,
)

internal data class HeldGeometry(
    val slot: ShelfSlot,
    val plank: PlankLayout,
    val camera: ShelfCamera,
    val held: HeldPlacement,
    val restOf: (() -> BookRest?)? = null,
) {
    val pageLeft: Float
        get() = held.centerX - slot.depth * held.scale / 2f

    val pageTop: Float
        get() = held.centerY - slot.height * held.scale / 2f

    val pageBottom: Float
        get() = held.centerY + slot.height * held.scale / 2f

    fun openPage(readFraction: Float?): RectF {
        val corners =
            buildBookFaces(pose(ShelfMotion(travel = 1f, open = 1f), 0f), readFraction)
                .firstOrNull { it.texture == FaceTexture.RightPage }
                ?.corners
                ?: return RectF(pageLeft, pageTop, pageLeft + slot.depth * held.scale, pageBottom)
        return RectF(
            corners.minOf { it.x },
            corners.minOf { it.y },
            corners.maxOf { it.x },
            corners.maxOf { it.y },
        )
    }
}

@Composable
internal fun PhysicalShelfHost(
    content: ShelfContent,
    state: ShelfState,
    pagesFor: (Int, BookPageSetting?) -> OpenBookPages?,
    modifier: Modifier = Modifier,
    style: ShelfStyle = ShelfStyle.Planks,
    coverFor: (Int) -> Bitmap? = { null },
    turns: PageTurnState? = null,
    leafFor: (Int, BookPageSetting?, LeafKey) -> PageContent? = { _, _, _ -> null },
    onBookSetting: (BookPageSetting?) -> Unit = {},
    openReserve: PaddingValues = PaddingValues(),
    spread: Boolean = false,
    overlay: @Composable (OpenPageSlot) -> Unit = {},
    backdrop: @Composable () -> Unit = {},
    chrome: @Composable (alpha: () -> Float) -> Unit = {},
    // Whether the [overlay] draws the open page's text live; if not, the page keeps its baked text.
    liveText: Boolean = true,
    body: @Composable (hero: @Composable (Modifier) -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val inspection = LocalInspectionMode.current
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val fonts = rememberShelfFonts()
    val kit = remember(fonts) { ShelfKit(fonts, ShelfNoise.create()) }
    val lighting = if (ShelfTheme.isDark) ShelfLighting.Dusk else ShelfLighting.Day
    val reducedMotion = rememberReducedMotion()
    var heroSize by remember { mutableStateOf(IntSize.Zero) }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var stageX by remember { mutableFloatStateOf(0f) }
    var contentX by remember { mutableFloatStateOf(0f) }
    val anchors = remember { ShelfAnchors() }
    val clock = remember { ShelfClock() }
    val recorded = remember { mutableIntStateOf(0) }
    val staticLayer = rememberGraphicsLayer()

    val pageMetrics = rememberPageMetrics()
    val textOnPage =
        remember(state, turns) {
            derivedStateOf {
                liveText &&
                    state.mode == ShelfMode.Open &&
                    !state.listening &&
                    state.motion.value.open >= 1f &&
                    turns?.isTurning != true &&
                    turns?.isRiffling != true
            }
        }
    val ring =
        if (style == ShelfStyle.Ring) {
            rememberRingHero(content, heroSize, lighting, kit, coverFor)
        } else {
            null
        }
    val assets =
        if (ring != null) ring.assets
        else rememberShelfAssets(content, heroSize, density.density, lighting, kit, inspection)
    val activeId = state.activeBookId
    val openBand =
        with(density) {
            OpenBand(
                top = openReserve.calculateTopPadding().value,
                bottom = stageSize.height.toDp().value - openReserve.calculateBottomPadding().value,
                left = openReserve.calculateLeftPadding(layoutDirection).value,
                right =
                    stageSize.width.toDp().value -
                        openReserve.calculateRightPadding(layoutDirection).value,
                spread = spread,
                // No hinge here to set the spine on: a spread's spine is at its middle.
                gutter = null,
            )
        }
    val textureAssets =
        remember(assets, stageSize, openBand) {
            assets?.onStage(Offset.Zero, stageSize, density.density, openBand)
        }
    val stageAssets =
        remember(assets, stageSize, activeId, openBand) {
            assets?.onStage(anchors.heroOrigin(), stageSize, density.density, openBand)
        }
    val settled = remember(state) { Animatable(if (state.mode == ShelfMode.Open) 1f else 0f) }
    val chromeMotion = ShelfTheme.motion
    LaunchedEffect(state, settled) {
        snapshotFlow { state.mode == ShelfMode.Open }
            .collect { open ->
                val spec = tween<Float>(chromeMotion.slowMs, easing = chromeMotion.ease)
                if (open && state.motion.value.open >= 1f) settled.snapTo(1f)
                else settled.animateTo(if (open) 1f else 0f, spec)
            }
    }
    val chromeAlpha =
        remember(state, settled) {
            {
                val opening =
                    if (state.mode == ShelfMode.Out) {
                        smoothStep(CHROME_FROM, CHROME_TO, state.motion.value.open)
                    } else {
                        0f
                    }
                maxOf(settled.value, opening)
            }
        }
    val chromeShown = remember(chromeAlpha) { derivedStateOf { chromeAlpha() > 0f } }
    val currentChrome by rememberUpdatedState(chrome)
    val currentBackdrop by rememberUpdatedState(backdrop)
    val chromeSlot = remember(chromeAlpha) { movableContentOf { currentChrome(chromeAlpha) } }
    val backdropSlot = remember { movableContentOf { currentBackdrop() } }
    val shell =
        rememberBookShell(
            textureAssets,
            content,
            activeId ?: ring?.restingBookId ?: content.readingBookId,
            coverFor,
            density.density,
            kit,
            inspection,
        )
    val takingOut = state.mode == ShelfMode.Tip || state.isBookOut || state.mode == ShelfMode.Back
    val openPage =
        remember(stageAssets, activeId) {
            val id = activeId ?: return@remember null
            val layout = stageAssets?.layout ?: return@remember null
            heldGeometry(layout, id)?.openPage(content.entries[id]?.readFraction)
        }
    val bookSetting = rememberBookPageSetting(openPage)
    val typeset =
        rememberPageTypeset(
            activeId?.takeIf { takingOut }?.let { pagesFor(it, bookSetting) }?.right
                as? PageContent.Passage,
            openPage,
        )
    val pages =
        rememberBookPages(
            textureAssets,
            content,
            activeId.takeIf { takingOut },
            pagesFor,
            pageMetrics,
            typeset,
            bookSetting,
            density.density,
            kit,
            inspection,
        )
    val leaves =
        rememberLeafFaces(
            textureAssets,
            content,
            activeId.takeIf { takingOut && turns != null },
            turns?.let { wantedLeaves(it) }.orEmpty(),
            leafFor,
            pageMetrics,
            bookSetting,
            density.density,
            kit,
        )
    val ribbonBand =
        rememberRibbonBand(
            textureAssets,
            content,
            activeId.takeIf { takingOut && turns != null },
            density.density,
        )
    val lift =
        rememberShadowLift(assets.takeIf { ring == null }, lighting, activeId, inspection)?.takeIf {
            it.bookId == activeId
        }
    val wall =
        remember(assets, activeId) {
            assets?.takeIf { ring == null }?.let { wallRegion(it.layout, activeId) }
        }
    if (assets != null && ring == null) {
        remember(assets, activeId, lighting, lift) {
            recordShelf(
                staticLayer,
                assets,
                lift?.maps ?: assets.maps,
                content,
                lighting,
                activeId,
                kit,
                density,
                layoutDirection,
            )
            recorded.intValue++
        }
    }

    SideEffect {
        onBookSetting(bookSetting)
        state.pagesReadyFor = pages?.takeIf { !it.pages.pending }?.bookId
        turns?.canTurn = { leaf -> turnKeys(leaf).all { leaves[it]?.exact == true } }
    }
    if (!inspection) {
        LaunchedEffect(lifecycle, reducedMotion) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var frame = 0L
                while (true) {
                    withFrameMillis { now ->
                        frame++
                        val shelfShown = !state.isBookOut && state.mode != ShelfMode.Back
                        val ticks =
                            shelfFrameTicks(clock.moving(), shelfShown, frame, reducedMotion)
                        if (ticks.physics) clock.physics.longValue = now
                        if (ticks.sway) clock.sway.longValue = now
                    }
                }
            }
        }
    }
    LaunchedEffect(state) {
        var armed = false
        snapshotFlow { state.takeOutArmed }
            .collect { now ->
                if (now && !armed) {
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
                armed = now
            }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.isBookOut || state.mode == ShelfMode.Back,
        onBackCompleted = { if (state.isBookOut) state.putBack() },
    )

    val hero: @Composable (Modifier) -> Unit = { heroModifier ->
        val measured =
            heroModifier
                .onSizeChanged { heroSize = it }
                .onGloballyPositioned {
                    anchors.hero = it
                    if (anchors.heroMoved()) state.shelfMoved()
                }
        if (ring != null) {
            RingShelfHero(content, ring, state, lighting, clock.sway, measured)
        } else {
            ShelfHero(
                content = content,
                assets = assets,
                state = state,
                staticLayer = staticLayer,
                recorded = recorded,
                lift = lift,
                wall = wall,
                timeMillis = clock.sway,
                modifier = measured,
            )
        }
    }

    Box(modifier.onGloballyPositioned { contentX = it.positionInWindow().x }) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val radius =
                        BLUR_DP * density.density * smoothStep(0f, 0.7f, state.motion.value.travel)
                    renderEffect =
                        if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Clamp) else null
                }
                .then(if (activeId != null) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            body(hero)
        }
        Box(
            Modifier.fillMaxSize()
                .bleedStart(contentX.roundToInt())
                .onSizeChanged { stageSize = it }
                .onGloballyPositioned {
                    anchors.stage = it
                    stageX = it.positionInWindow().x
                }
        ) {
            val backdropOnStage = activeId != null && chromeShown.value
            val chromeOnStage = backdropOnStage && stageAssets != null
            if (backdropOnStage) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha() }) {
                    backdropSlot()
                }
            }
            ChromeParking(inspection) {
                if (!backdropOnStage) backdropSlot()
                if (!chromeOnStage) chromeSlot()
            }
            if (stageAssets != null && activeId != null) {
                ShelfStage(
                    content = content,
                    assets = stageAssets,
                    state = state,
                    shell = shell,
                    pages = pages,
                    openPage = openPage,
                    facing = spread,
                    textOnPage = textOnPage,
                    bookSetting = bookSetting,
                    turns = turns,
                    leaves = leaves,
                    ribbonBand = ribbonBand,
                    shelfRibbons = ring == null,
                    lighting = lighting,
                    clock = clock,
                    overlay = overlay,
                    chrome = { if (chromeShown.value) chromeSlot() },
                )
            }
        }
        ShelfAnnouncer(content, state)
    }
}

@Composable
private fun ChromeParking(inspection: Boolean, content: @Composable () -> Unit) {
    var parked by remember { mutableStateOf(false) }
    LaunchedEffect(inspection) {
        if (inspection) return@LaunchedEffect
        delay(WARM_UP_DELAY_MS)
        parked = true
    }
    if (parked) {
        Box(Modifier.size(0.dp).clipToBounds().clearAndSetSemantics {}) { content() }
    }
}

@Composable
private fun ShelfHero(
    content: ShelfContent,
    assets: ShelfAssets?,
    state: ShelfState,
    staticLayer: GraphicsLayer,
    recorded: MutableIntState,
    lift: ShadowLift?,
    wall: Path?,
    timeMillis: State<Long>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val edge = ShelfTheme.colors.background
    if (assets != null) SideEffect { state.ready = true }
    // Clipped to its bounds, as the ring's hero is, so its wall paints nothing past its edges when
    // the list it sits in stretches or blurs.
    Box(
        modifier.clipToBounds().pointerInput(assets, state) {
            val ready = assets ?: return@pointerInput
            awaitEachGesture { handleShelfGesture(ready.layout, state, density) }
        }
    ) {
        val ready = assets ?: return@Box
        Canvas(Modifier.fillMaxSize()) {
            recorded.intValue
            drawLayer(staticLayer)
            val m = state.motion.value
            val restore = 1f - smoothStep(0f, WALL_SHADOW_FADE, m.travel * 2f + abs(m.tilt))
            if (lift != null && wall != null && restore > 0f) {
                withDp(density) { drawShadowRestore(lift, wall, restore) }
            }
            withDp(density) { drawLeaves(ready.maps, leafSway(timeMillis.value)) }
            drawEdgeFades(edge, EDGE_FADE_DP * density)
        }
        SpineAccessibility(content, ready, state)
    }
}

@Composable
private fun ShelfStage(
    content: ShelfContent,
    assets: ShelfAssets,
    state: ShelfState,
    shell: BookShell?,
    pages: BookPages?,
    openPage: RectF?,
    facing: Boolean,
    textOnPage: State<Boolean>,
    bookSetting: BookPageSetting?,
    turns: PageTurnState?,
    leaves: Map<LeafKey, LeafFace>,
    ribbonBand: Bitmap?,
    shelfRibbons: Boolean,
    lighting: ShelfLighting,
    clock: ShelfClock,
    overlay: @Composable (OpenPageSlot) -> Unit,
    chrome: @Composable () -> Unit,
) {
    val timeMillis = clock.physics
    val density = LocalDensity.current.density
    val blocking = state.isBookOut || state.mode == ShelfMode.Back
    val leafPainter = remember { LeafPainter() }
    val ribbonRope = remember(state.activeBookId) { RibbonRope() }
    val ribbonCord = remember(state.activeBookId) { RibbonCord() }
    val perspective = with(LocalDensity.current) { PageTurnDimens.Perspective.toPx() }
    val bookLayout =
        remember(openPage, density, perspective) {
            openPage?.let { pageTurnLayout(it, density, perspective) }
        }
    SideEffect { turns?.layout = bookLayout }
    val fresh = pages.isFresh(turns, leaves)
    val bookmark =
        rememberBookmark(turns?.bookmarkPage?.takeIf { ribbonBand != null }, turns?.chapterKey)
    val fullyOpen by
        remember(state) {
            derivedStateOf {
                state.mode == ShelfMode.Open && !state.listening && state.motion.value.open >= 1f
            }
        }
    val stow =
        rememberRibbonStow(
            state.activeBookId,
            fullyOpen,
            turns?.riffleCovered == true,
            bookmark.moves,
            turns?.seeks ?: 0,
        )
    DisposableEffect(clock, ribbonRope, ribbonCord, stow) {
        clock.moving = {
            ribbonRope.takeMoving() or
                ribbonCord.takeMoving() or
                stow.tuck.isRunning or
                stow.fade.isRunning
        }
        onDispose { clock.moving = { false } }
    }
    Box(
        Modifier.fillMaxSize()
            .then(
                if (blocking) {
                    Modifier.pointerInput(openPage, state, turns, facing) {
                        awaitEachGesture {
                            handlePageGesture(openPage, state, turns, density, facing)
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - state.motion.value.vanish }) {
            val m = state.motion.value
            val dimAlpha = lighting.dimAlpha * smoothStep(0f, 0.8f, m.travel)
            if (dimAlpha > 0f) drawRect(Color(lighting.dim.toArgb(dimAlpha)))
            val active = m.activeBookId ?: return@Canvas
            val faces = shell?.takeIf { it.bookId == active }
            val entry = content.entries[active] ?: return@Canvas
            val geometry = heldGeometry(assets.layout, active) ?: return@Canvas
            val openPages = pages?.takeIf { it.bookId == active }
            val showsText = textOnPage.value && openPages != null && fresh
            // A book pushed down is held still: no leaf flies while it is.
            val flights = turns?.flights?.takeIf { m.push == 0f && it.isNotEmpty() }
            val resting = flights?.let { turns?.restingPages(it) }
            val rightPage = resting?.second ?: turns?.page
            val rest = geometry.restOf?.takeIf { m.travel < 1f }?.invoke()
            val pose = geometry.pose(m, assets.layout.scale, rest)
            val tuck = stow.tuck.value
            val fade = stow.fade.value
            val cordShown = tuck > 0f || fade > 0f
            val bandShown = tuck == 0f && fade < 1f
            val flyingMark = tuck == 0f && flights?.any { it.leaf == bookmark.mark } == true
            val side =
                ribbonSide(bookmark.mark, rightPage, flyingMark, openPages?.pages?.ribbon == true)
            val tailUnder =
                bookmark.mark != null && (rightPage?.let(bookmark::alpha) ?: 0f) < BAND_SHOWN
            val flatTail = bookLayout?.takeIf {
                bookmark.mark != null &&
                    state.mode == ShelfMode.Open &&
                    m.open >= 1f &&
                    m.push == 0f
            }
            val tailMotion =
                TailMotion(
                    density,
                    assets.layout.scale,
                    timeMillis.longValue,
                    ribbonRope,
                    pose.dimensions.depth / assets.layout.scale,
                )
            val cordFrame =
                flatTail
                    ?.takeIf { cordShown && side != null }
                    ?.let {
                        ribbonFrame(
                            it,
                            density,
                            assets.layout.scale,
                            RIBBON_TAIL_LENGTH,
                            pose.dimensions.depth / assets.layout.scale,
                        )
                    }
            cordFrame?.let { ribbonCord.update(it, tuck, timeMillis.longValue) }
            if (tuck > 0f) ribbonRope.calm()
            fun cord() {
                if (cordFrame != null && flatTail != null) {
                    val left = side == RibbonSide.Left
                    drawRibbonCord(
                        flatTail,
                        left,
                        ribbonCord,
                        cordFrame,
                        lighting,
                        density,
                        if (tuck > 0f) 1f else fade,
                        leafPainter.restFrame(flatTail.geometry, !left, m.arch),
                    )
                }
            }
            // Arched while it rests; flat, in 3D, once a push or a cover's close has flattened it.
            val arched = bookLayout?.takeIf { m.open >= 1f && (m.push == 0f || m.arch > 0f) }
            fun restingTail() {
                if (flatTail != null && side != null && bandShown) {
                    val left = side == RibbonSide.Left
                    val frame = leafPainter.restFrame(flatTail.geometry, !left, m.arch)
                    drawLeafRibbonTail(flatTail, frame, lighting, tailMotion)
                }
            }
            if (tailUnder) restingTail()
            fun faceBitmap(face: FaceTexture): Bitmap? =
                when {
                    resting != null && face == FaceTexture.LeftPage ->
                        leaves[LeafKey(resting.first, right = false)]?.bitmap
                    resting != null && face == FaceTexture.RightPage ->
                        leaves[LeafKey(resting.second, right = true)]?.bitmap
                    (openPages == null || !fresh) && turns != null ->
                        idleLeaf(face, turns.page)?.let { leaves[it]?.bitmap }
                    else -> null
                }
                    ?: when (face) {
                        FaceTexture.LeftPage ->
                            if (showsText) openPages?.leftBlank ?: openPages?.left
                            else openPages?.left
                        FaceTexture.RightPage ->
                            if (showsText) openPages?.rightBlank ?: openPages?.right
                            else openPages?.right
                        FaceTexture.Spine -> faces?.faces?.get(face) ?: assets.spines[active]
                        else -> faces?.faces?.get(face)
                    }
            val bandAlpha =
                if (bandShown) (rightPage?.let(bookmark::alpha) ?: 0f) * (1f - fade) else 0f
            val restingBook =
                bookLayout
                    ?.takeIf { m.open > 0f }
                    ?.let {
                        restingBook(
                            turns,
                            resting,
                            flights.orEmpty(),
                            entry.readFraction,
                            faceBitmap(FaceTexture.LeftPage),
                            faceBitmap(FaceTexture.RightPage),
                            ribbonBand?.takeIf { bandAlpha > 0f },
                            bandAlpha,
                            m.arch,
                        )
                    }
            if (bookLayout != null && restingBook != null) {
                val (leftAlpha, rightAlpha) = stackAlphas(m.open, m.push)
                with(leafPainter) {
                    drawStacks(
                        bookLayout,
                        restingBook,
                        lighting.pageLight(),
                        leftAlpha = leftAlpha,
                        rightAlpha = rightAlpha,
                    )
                }
            }
            withDp(density) {
                drawHeldBook(
                    pose = pose,
                    faceBitmap = ::faceBitmap,
                    readFraction = entry.readFraction,
                    coverOnly = m.coverOnly,
                    ribbonTail = side.takeIf { flatTail == null && bandShown },
                    lighting = lighting,
                    scale = assets.layout.scale,
                    neighbours = rest?.occluders ?: geometry.neighbourSpines(),
                    shade =
                        rest?.let { mix(it.shade, 1f, smoothStep(0f, REST_SHADE_GONE, m.travel)) }
                            ?: 1f,
                    castShadow =
                        if (rest != null) smoothStep(0f, REST_SHADOW_FROM, m.travel) else 1f,
                    plainFace = { face -> plainFaceColor(face, entry.visual.style) },
                    tailUnderPages = tailUnder,
                    tailMotion = tailMotion,
                    shelfRibbonBookId =
                        active.takeIf { shelfRibbons && it in content.ribbonBookIds },
                    faceOverlay = { face ->
                        if (face == FaceTexture.RightPage && ribbonBand != null && bandAlpha > 0f) {
                            FaceOverlay(ribbonBand, bandAlpha)
                        } else {
                            null
                        }
                    },
                    hidePages = arched != null,
                )
            }
            if (arched != null && restingBook != null) {
                with(leafPainter) { drawRestingBook(arched, restingBook, lighting.pageLight()) }
            }
            if (!tailUnder) restingTail()
            cord()
            if (flights != null && bookLayout != null) {
                with(leafPainter) {
                    drawFlights(
                        bookLayout,
                        flights,
                        {
                            leafFaces(
                                it,
                                leaves,
                                ribbonBand,
                                bookmark.alpha(it).takeIf { tuck == 0f } ?: 0f,
                            )
                        },
                        lighting.pageLight(),
                    ) { flight, frame ->
                        if (flyingMark && flight.leaf == bookmark.mark) {
                            translate(-bookLayout.left, -bookLayout.top) {
                                drawLeafRibbonTail(bookLayout, frame, lighting, tailMotion)
                            }
                        }
                    }
                }
            }
        }
        OpenPageLayer(
            openPage,
            facing,
            state,
            textOnPage.value && pages?.bookId == state.activeBookId && fresh,
            bookSetting,
            overlay,
        )
        chrome()
    }
}

@Composable
private fun OpenPageLayer(
    page: RectF?,
    facing: Boolean,
    state: ShelfState,
    textOnPage: Boolean,
    bookSetting: BookPageSetting?,
    overlay: @Composable (OpenPageSlot) -> Unit,
) {
    if (state.mode != ShelfMode.Open) return
    val bookId = state.activeBookId ?: return
    if (page == null) return
    val slot =
        OpenPageSlot(
            bookId = bookId,
            left = page.left.dp,
            top = page.top.dp,
            width = page.width().dp,
            height = page.height().dp,
            showsText = textOnPage,
            bookSetting = bookSetting,
            facing = facing,
        )
    Box(Modifier.fillMaxSize()) { overlay(slot) }
}

@Composable
private fun SpineAccessibility(content: ShelfContent, assets: ShelfAssets, state: ShelfState) {
    val takeOut = stringResource(Res.string.bookshelf_shelf_take_out)
    if (state.mode != ShelfMode.Shelf && state.mode != ShelfMode.Settle) return
    for (plank in assets.layout.planks) {
        for (slot in plank.slots) {
            val label = content.entries[slot.bookId]?.accessibilityLabel ?: continue
            Box(
                Modifier.offset(slot.x.dp, (plank.plankY - slot.height).dp)
                    .size(slot.thickness.dp, slot.height.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = label
                        onClick(label = takeOut) {
                            state.openDirect(slot.bookId)
                            true
                        }
                    }
                    .onKeyEvent { event ->
                        val activate = event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (activate && event.type == KeyEventType.KeyUp) {
                            state.openDirect(slot.bookId)
                        }
                        activate
                    }
                    .focusable()
            )
        }
    }
}

/**
 * The book whose return to the shelf is announced: the one that opened, until another is tipped, so
 * a tip that only falls back announces nothing.
 */
internal fun announcedTitle(current: String?, mode: ShelfMode, activeTitle: String?): String? =
    when {
        mode == ShelfMode.Open && activeTitle != null -> activeTitle
        mode == ShelfMode.Tip -> null
        else -> current
    }

@Composable
private fun ShelfAnnouncer(content: ShelfContent, state: ShelfState) {
    var openedTitle by remember { mutableStateOf<String?>(null) }
    val activeTitle = state.activeBookId?.let { content.entries[it]?.visual?.title }
    LaunchedEffect(state.mode, activeTitle) {
        openedTitle = announcedTitle(openedTitle, state.mode, activeTitle)
    }
    val title = openedTitle
    val announcement =
        when {
            title == null -> null
            state.mode == ShelfMode.Open -> stringResource(Res.string.bookshelf_shelf_opened, title)
            state.mode == ShelfMode.Settle || state.mode == ShelfMode.Shelf ->
                stringResource(Res.string.bookshelf_shelf_returned, title)
            else -> null
        }
    Box(
        Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            if (announcement != null) contentDescription = announcement
        }
    )
}

private class ShelfAnchors {
    var stage: LayoutCoordinates? = null
    var hero: LayoutCoordinates? = null

    private var lastOrigin: Offset? = null

    fun heroOrigin(): Offset {
        val stage = stage?.takeIf { it.isAttached } ?: return Offset.Zero
        val hero = hero?.takeIf { it.isAttached } ?: return Offset.Zero
        return stage.localPositionOf(hero, Offset.Zero)
    }

    // Whether the hero has moved on the stage since it was last placed.
    fun heroMoved(): Boolean {
        val origin = heroOrigin()
        val moved = lastOrigin != null && origin != lastOrigin
        lastOrigin = origin
        return moved
    }
}

private fun ShelfAssets.onStage(
    origin: Offset,
    stage: IntSize,
    density: Float,
    openBand: OpenBand,
): ShelfAssets {
    if (stage == IntSize.Zero) return this
    return ShelfAssets(
        layout.onStage(
            origin.x / density,
            origin.y / density,
            stage.width / density,
            stage.height / density,
            openBand,
        ),
        maps,
        spines,
        size,
    )
}

private fun HeldGeometry.neighbourSpines(): List<List<Point2>> =
    plank.slots
        .filter { it.bookId != slot.bookId }
        .map {
            val top = plank.plankY - it.height
            listOf(
                Point2(it.x, top),
                Point2(it.x + it.thickness, top),
                Point2(it.x + it.thickness, plank.plankY),
                Point2(it.x, plank.plankY),
            )
        }

internal fun heldGeometry(layout: ShelfLayout, bookId: Int): HeldGeometry? {
    val plank =
        layout.planks.firstOrNull { p -> p.slots.any { it.bookId == bookId } } ?: return null
    val slot = plank.slots.first { it.bookId == bookId }
    val w = layout.width
    val h = layout.height
    val k = layout.scale
    val camera =
        layout.camera
            ?: ShelfCamera(
                vanishingX = w / 2f,
                vanishingY = plank.plankY - 150f * k,
                focal = maxOf(900f, w * 2.8f),
            )
    val placed =
        (layout.openBand ?: OpenBand(top = h * 0.19f, bottom = h * 0.75f, left = 0f, right = w))
            .place(slot.depth, slot.height)
    val held =
        HeldPlacement(
            centerX = placed.centerX,
            centerY = placed.centerY,
            scale = placed.pageWidth / slot.depth,
        )
    return HeldGeometry(
        slot,
        plank,
        camera,
        held,
        layout.rest?.let { lookup -> { lookup(bookId) } },
    )
}

private fun HeldGeometry.pose(motion: ShelfMotion, scale: Float, rest: BookRest? = null) =
    BookPose(
        dimensions = BookDimensions(slot.thickness, slot.height, slot.depth),
        slotX = rest?.x ?: slot.x,
        plankY = plank.plankY,
        shelfCamera = camera,
        held = held,
        liftArc = 40f * scale,
        travel = motion.travel,
        tilt = motion.tilt,
        open = motion.open,
        restZ = rest?.z ?: 0f,
        restYaw = rest?.yaw ?: 0f,
        row = rest?.row,
        push = (motion.push - TiltGesture.PUSH_FLATTEN).coerceAtLeast(0f),
    )

private fun recordShelf(
    staticLayer: GraphicsLayer,
    assets: ShelfAssets,
    maps: ShelfLightMaps,
    content: ShelfContent,
    lighting: ShelfLighting,
    missingBookId: Int?,
    kit: ShelfKit,
    density: Density,
    layoutDirection: LayoutDirection,
) {
    val scene =
        ShelfScene(
            assets.layout,
            content.entries.mapValues { it.value.visual },
            lighting,
            content.ribbonBookIds,
        )
    staticLayer.compositingStrategy = CompositingStrategy.Offscreen
    staticLayer.record(density, layoutDirection, assets.size) {
        withDp(density.density) {
            drawStaticShelf(scene, maps, assets.spines, missingBookId, kit)
        }
    }
}

internal inline fun DrawScope.withDp(
    density: Float,
    crossinline block:
        dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas.() -> Unit,
) {
    drawIntoCanvas { canvas ->
        dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas(canvas).withScale(
            density,
            density,
        ) {
            block()
        }
    }
}

internal fun plainFaceColor(face: FaceTexture, style: ShelfBookStyle): Int =
    when (face) {
        FaceTexture.Cover,
        FaceTexture.Back,
        FaceTexture.Spine -> argb(style.base)
        FaceTexture.Head,
        FaceTexture.Fore -> argb(PAGE_EDGE)
        FaceTexture.LeftPage,
        FaceTexture.RightPage -> argb(PAGE_PAPER)
    }

/**
 * How much of the flat page stacks shows for a cover [open] and a [push]: they fade as the cover
 * closes, the left first, and over a push's first stretch, gone before the book moves in 3D.
 */
internal fun stackAlphas(open: Float, push: Float): Pair<Float, Float> {
    val kept = 1f - smoothStep(0f, TiltGesture.PUSH_FLATTEN, push)
    return smoothStep(LEFT_STACK_FROM, 1f, open) * kept to
        smoothStep(0f, RIGHT_STACK_TO, open) * kept
}

internal fun DrawScope.drawEdgeFades(edge: Color, fade: Float) {
    drawRect(
        Brush.verticalGradient(0f to edge, 1f to edge.copy(alpha = 0f), startY = 0f, endY = fade),
        size = Size(size.width, fade),
    )
    drawRect(
        Brush.verticalGradient(
            0f to edge.copy(alpha = 0f),
            1f to edge,
            startY = size.height - fade,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height - fade),
        size = Size(size.width, fade),
    )
}

private fun ShelfLighting.pageLight(): Color = Color(faceLight(Vec3(0f, 0f, 1f), 1f).toArgb())

private fun turnKeys(leaf: Int): List<LeafKey> =
    listOf(
        LeafKey(leaf, right = true),
        LeafKey(leaf, right = false),
        LeafKey(leaf + 1, right = true),
        LeafKey(leaf - 1, right = false),
    )

private fun leafFaces(
    leaf: Int,
    leaves: Map<LeafKey, LeafFace>,
    band: Bitmap?,
    bandAlpha: Float,
): LeafFaces? {
    val front = leaves[LeafKey(leaf, right = true)]?.bitmap ?: return null
    val back = leaves[LeafKey(leaf, right = false)]?.bitmap ?: return null
    return LeafFaces(front, back, band, bandAlpha)
}

@Stable
private class Bookmark(page: Int?, var key: Int?) {
    var page by mutableStateOf(page)
    var previous by mutableStateOf<Int?>(null)
    var moves by mutableIntStateOf(0)
    val fade = Animatable(1f)
    var liveKey: Int? = key
    var livePage: Int? = page

    val mark: Int?
        get() = if (key == liveKey) page else livePage

    fun alpha(at: Int): Float =
        when {
            key != liveKey -> if (at == livePage) 1f else 0f
            at == page -> fade.value
            at == previous -> 1f - fade.value
            else -> 0f
        }
}

@Composable
private fun rememberBookmark(page: Int?, key: Int?): Bookmark {
    val bookmark = remember { Bookmark(page, key) }
    bookmark.liveKey = key
    bookmark.livePage = page
    val fadeMs = ShelfTheme.motion.mediumMs
    LaunchedEffect(page, key) {
        if (key != bookmark.key) {
            bookmark.key = key
            bookmark.page = page
            bookmark.previous = null
            bookmark.fade.snapTo(1f)
            return@LaunchedEffect
        }
        if (page == bookmark.page) return@LaunchedEffect
        bookmark.previous = bookmark.page.takeIf { page != null }
        bookmark.page = page
        if (page != null) bookmark.moves++
        bookmark.fade.snapTo(0f)
        bookmark.fade.animateTo(1f, tween(fadeMs))
        bookmark.previous = null
    }
    return bookmark
}

@Composable
private fun rememberRibbonStow(
    bookId: Int?,
    open: Boolean,
    covered: Boolean,
    moves: Int,
    seeks: Int,
): RibbonStow {
    val stow = remember(bookId) { RibbonStow(moves, seeks) }
    val motion = ShelfTheme.motion
    LaunchedEffect(stow, seeks) {
        if (seeks == stow.seeks) return@LaunchedEffect
        stow.seeks = seeks
        stow.seeking = true
        delay(RIBBON_SEEK_WINDOW_MS)
        stow.seeking = false
    }
    LaunchedEffect(stow, open, covered, moves) {
        if (!open) return@LaunchedEffect
        val rehang = moves != stow.moves && stow.seeking && stow.tuck.value == 0f
        stow.moves = moves
        if (covered || rehang) {
            stow.seeking = false
            stow.fade.snapTo(1f)
            stow.tuck.snapTo(1f)
            if (covered) return@LaunchedEffect
        }
        if (stow.tuck.value == 0f && stow.fade.value == 0f) return@LaunchedEffect
        if (stow.tuck.value > 0f) {
            delay(RIBBON_LAND_DELAY_MS)
            stow.tuck.animateTo(0f, tween(RIBBON_LAY_MS, easing = LinearEasing))
            delay(RIBBON_LAND_SETTLE_MS)
        }
        stow.fade.animateTo(0f, tween(RIBBON_FADE_MS, easing = motion.ease))
    }
    return stow
}

internal data class ShelfFrameTicks(val physics: Boolean, val sway: Boolean)

/**
 * What a frame moves on: the ribbon's physics while anything moves, so it always settles, and the
 * leaves' sway every few frames while the shelf shows, never with reduced motion.
 */
internal fun shelfFrameTicks(
    moving: Boolean,
    shelfShown: Boolean,
    frame: Long,
    reducedMotion: Boolean,
) =
    ShelfFrameTicks(
        physics = moving,
        sway = !reducedMotion && shelfShown && frame % SWAY_FRAME_STRIDE == 0L,
    )

private class ShelfClock {
    val sway = mutableLongStateOf(0L)
    val physics = mutableLongStateOf(0L)
    var moving: () -> Boolean = { false }
}

private class RibbonStow(var moves: Int, var seeks: Int) {
    val tuck = Animatable(0f)
    val fade = Animatable(0f)
    var seeking = false
}

private fun pageTurnLayout(page: RectF, density: Float, perspective: Float): BookLayout {
    val height = page.height() * density
    return BookLayout(
        left = 0f,
        top = page.top * density,
        geometry =
            PageGeometry(
                spineX = page.left * density,
                originY = height * PageTurnDimens.OriginYFraction,
                perspectivePx = maxOf(perspective, height * PageTurnDimens.PerspectivePerHeight),
                page = page.width() * density,
                height = height,
                restLift = PageTurnDimens.RestLift,
            ),
    )
}

private fun wantedLeaves(turns: PageTurnState): List<LeafKey> {
    val page = turns.page
    val riffle = turns.riffleSpan?.sortedBy { abs(it - page) }?.flatMap { turnKeys(it) }.orEmpty()
    val around =
        (page - 1..page + 2).map { LeafKey(it, right = true) } +
            (page - 2..page + 1).map { LeafKey(it, right = false) }
    val live =
        turns.livePage?.let { listOf(LeafKey(it, right = true), LeafKey(it - 1, right = false)) }
    val flying = turns.flyingLeaves.flatMap { turnKeys(it) }
    return (flying + riffle + around + live.orEmpty()).distinct()
}

@Suppress("LongParameterList")
private fun restingBook(
    turns: PageTurnState?,
    resting: Pair<Int, Int>?,
    flights: List<Flight>,
    readFraction: Float?,
    left: Bitmap?,
    right: Bitmap?,
    band: Bitmap?,
    bandAlpha: Float,
    arch: Float,
): RestingBook {
    val cap = PageTurnDimens.MaxSheets
    val leftTop = resting?.first ?: turns?.page?.minus(1)
    val rightTop = resting?.second ?: turns?.page
    val bridged = turns?.bridgeSheets
    val across = turns?.let { (it.page.toFloat() / it.lastPage.coerceAtLeast(1)).coerceIn(0f, 1f) }
    val leftSheets =
        when {
            bridged != null && across != null ->
                lerp(bridged.first.left, bridged.second.left, across)
            turns != null && leftTop != null -> leftTop.coerceIn(0, cap)
            else -> ((readFraction ?: 0f) * cap).roundToInt()
        }
    val rightSheets =
        when {
            bridged != null && across != null ->
                lerp(bridged.first.right, bridged.second.right, across)
            turns != null && rightTop != null -> (turns.lastPage - rightTop).coerceIn(0, cap)
            else -> cap - leftSheets
        }
    return RestingBook(
        left = left?.let { LeafFaces(it, it, null, 0f) },
        right = right?.let { LeafFaces(it, it, band, bandAlpha) },
        leftSheets = leftSheets,
        rightSheets = rightSheets,
        leftSink = flights.sumOf { stackLand(it.t).toDouble() }.toFloat(),
        rightSink = flights.sumOf { (1f - stackRise(it.t)).toDouble() }.toFloat(),
        arch = arch,
    )
}

private fun lerp(from: Int, to: Int, fraction: Float): Int =
    (from + (to - from) * fraction).roundToInt()

private fun idleLeaf(face: FaceTexture, page: Int): LeafKey? =
    when (face) {
        FaceTexture.LeftPage -> LeafKey(page - 1, right = false)
        FaceTexture.RightPage -> LeafKey(page, right = true)
        else -> null
    }

private fun ribbonSide(
    bookmark: Int?,
    rightPage: Int?,
    flying: Boolean,
    bakedRibbon: Boolean,
): RibbonSide? =
    when {
        bookmark == null -> RibbonSide.Right.takeIf { bakedRibbon }
        flying -> null
        rightPage != null && bookmark < rightPage -> RibbonSide.Left
        else -> RibbonSide.Right
    }

private fun BookPages?.isFresh(turns: PageTurnState?, leaves: Map<LeafKey, LeafFace>): Boolean {
    val baked = this ?: return false
    if (turns == null) return true
    val right = leaves[LeafKey(turns.page, right = true)]
    val left = leaves[LeafKey(turns.page - 1, right = false)]
    return (right == null || !right.exact || right.content == baked.pages.right) &&
        (left == null || !left.exact || left.content == baked.pages.left)
}

private fun Modifier.bleedStart(px: Int): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth + px
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-px, 0) }
}
