package dev.dimvlachos.lab.paperbagdemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.components.paperbag.BagInk
import dev.dimvlachos.lab.core.presentation.components.paperbag.BagItem
import dev.dimvlachos.lab.core.presentation.components.paperbag.PaperBagState
import dev.dimvlachos.lab.core.presentation.components.paperbag.drawBag
import dev.dimvlachos.lab.core.presentation.components.paperbag.drawItem
import dev.dimvlachos.lab.core.presentation.components.paperbag.drawThumb
import dev.dimvlachos.lab.core.presentation.components.paperbag.ease
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PaperBagColors
import dev.dimvlachos.lab.paperbagdemo.BagEntry
import dev.dimvlachos.lab.paperbagdemo.Groceries
import dev.dimvlachos.lab.paperbagdemo.GroceryCart
import dev.dimvlachos.lab.paperbagdemo.PaperBagDemos
import dev.dimvlachos.lab.paperbagdemo.Product
import dev.dimvlachos.lab.paperbagdemo.money
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.caveat_semibold
import dev.dimvlachos.lab.resources.paperbag_add
import dev.dimvlachos.lab.resources.paperbag_add_label
import dev.dimvlachos.lab.resources.paperbag_added
import dev.dimvlachos.lab.resources.paperbag_bag_empty
import dev.dimvlachos.lab.resources.paperbag_bag_items
import dev.dimvlachos.lab.resources.paperbag_check_out
import dev.dimvlachos.lab.resources.paperbag_check_out_note
import dev.dimvlachos.lab.resources.paperbag_check_out_total
import dev.dimvlachos.lab.resources.paperbag_chip_bakery
import dev.dimvlachos.lab.resources.paperbag_chip_dairy
import dev.dimvlachos.lab.resources.paperbag_chip_flowers
import dev.dimvlachos.lab.resources.paperbag_chip_produce
import dev.dimvlachos.lab.resources.paperbag_delivery
import dev.dimvlachos.lab.resources.paperbag_done
import dev.dimvlachos.lab.resources.paperbag_full
import dev.dimvlachos.lab.resources.paperbag_hello
import dev.dimvlachos.lab.resources.paperbag_hello_line
import dev.dimvlachos.lab.resources.paperbag_in_bag
import dev.dimvlachos.lab.resources.paperbag_nothing
import dev.dimvlachos.lab.resources.paperbag_price_unit
import dev.dimvlachos.lab.resources.paperbag_remove
import dev.dimvlachos.lab.resources.paperbag_removed
import dev.dimvlachos.lab.resources.paperbag_store
import dev.dimvlachos.lab.resources.paperbag_your_bag
import dev.dimvlachos.lab.resources.paperbag_your_bag_items
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** What the screen last read out: something added or removed, the bag full, or checkout. */
private sealed interface Said {
    val id: Int

    class Added(val product: Product, val count: Int, override val id: Int) : Said

    class Removed(val product: Product, val count: Int, override val id: Int) : Said

    class Full(override val id: Int) : Said

    class CheckOut(override val id: Int) : Said
}

/** A product on its way from its row to the bag: where it set off, and when on the bag's clock. */
private class Flight(val entry: BagEntry, val t0: Double, val from: Offset)

/** The scripted fingertip: where it is, in px, how far shown, and whether it is pressed. */
@Stable
private class Finger {
    var at by mutableStateOf(Offset.Zero)
    var shown by mutableStateOf(0f)
    var down by mutableStateOf(false)
}

/**
 * Fernwood Market's aisle, laid out as the HTML prototype lays it out, with a kraft paper bag for
 * the cart icon in its top bar. Add a product and it hops into the bag on an arc; the bag squashes,
 * bulges as it fills and has its count rewritten in grease pencil. Tap the bag and it grows out of
 * the corner and tips toward you over the list of what is in it, where − lifts a thing back out.
 *
 * The bag and everything flying are drawn in one canvas over the screen, unclipped, so what pokes
 * out of the bag shows whole in the bar.
 */
@Composable
internal fun PaperBagDemo(
    state: DemoState,
    cart: GroceryCart = remember { GroceryCart() },
    bag: PaperBagState = remember { PaperBagState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    bag.reducedMotion = reducedMotion
    val colors = if (LabTheme.isDark) PaperBagColors.Dark else PaperBagColors.Light
    val dark = LabTheme.isDark
    val touch = LabTheme.colors.touch
    val sounds = rememberSoundPlayer(setOf(Sound.BagDrop, Sound.BagLift, Sound.BagTip))
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    val measurer = rememberTextMeasurer()
    val hand = FontFamily(Font(Res.font.caveat_semibold, FontWeight.SemiBold))
    val ink = remember(measurer, hand) { BagInk(measurer, hand) }

    // Where the controls are on the screen, in px from its top left, for flights and the script.
    val bounds = remember { HashMap<String, Rect>() }
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    fun Modifier.place(id: String) = onGloballyPositioned { c ->
        val origin = root?.takeIf { it.isAttached }?.boundsInRoot()?.topLeft ?: Offset.Zero
        bounds[id] = c.boundsInRoot().translate(-origin)
    }

    val flights = remember { mutableStateListOf<Flight>() }
    val finger = remember { Finger() }
    var said by remember { mutableStateOf<Said?>(null) }
    var saidCount by remember { mutableStateOf(0) }
    fun say(make: (Int) -> Said) {
        said = make(++saidCount)
    }
    val inputMode = LocalInputModeManager.current
    val doneFocus = remember { FocusRequester() }
    val bagFocus = remember { FocusRequester() }

    fun land(key: Int) {
        val entry = cart.landed(key) ?: return
        bag.landed(cart.count)
        sounds.play(Sound.BagDrop, volume = .6f + min(.4f, cart.count * .06f))
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        say { Said.Added(entry.product, cart.count, it) }
    }

    fun add(product: Product) {
        if (bag.isOpen || bag.open > .05f) return
        val entry = cart.started(product)
        if (entry == null) {
            bag.refuse()
            say { Said.Full(it) }
            return
        }
        if (reducedMotion) {
            land(entry.key)
            return
        }
        val from = bounds["thumb-${product.id}"]?.center ?: Offset.Zero
        flights += Flight(entry, bag.now, from + Offset(0f, 18f * density))
    }

    fun finishLift(key: Int) {
        val entry = cart.removed(key) ?: return
        bag.lifted(key, cart.count)
        say { Said.Removed(entry.product, cart.count, it) }
    }

    fun remove(entry: BagEntry) {
        if (!cart.lifting(entry.key)) return
        if (reducedMotion) {
            finishLift(entry.key)
            return
        }
        bag.lift(entry.key)
        sounds.play(Sound.BagLift)
    }

    fun openBag() {
        if (bag.isOpen) return
        bag.openTo(true)
        sounds.play(Sound.BagTip)
        // Focus follows only a keyboard: moved after a tap it would ring the button, as the
        // prototype's bag stayed ringed after it was set back.
        if (inputMode.inputMode == InputMode.Keyboard) {
            scope.launch {
                delay(50)
                runCatching { doneFocus.requestFocus() }
            }
        }
    }

    fun closeBag() {
        if (!bag.isOpen) return
        bag.openTo(false)
        sounds.play(Sound.BagTip, volume = .8f)
        if (inputMode.inputMode == InputMode.Keyboard) runCatching { bagFocus.requestFocus() }
    }

    // The bag's clock: springs, flights landing, lifts finishing. It rests while nothing moves,
    // and starts again when the bag is set moving or something flies.
    LaunchedEffect(bag, cart) {
        var last = withFrameNanos { it }
        while (true) {
            if (!bag.moving && flights.isEmpty() && cart.lifting.isEmpty()) {
                val at = bag.wakes
                snapshotFlow {
                    bag.wakes != at || flights.isNotEmpty() || cart.lifting.isNotEmpty()
                }
                    .first { it }
                last = withFrameNanos { it }
            }
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceIn(0f, .15f)
                last = now
                bag.tick(dt)
                for (f in flights.toList()) {
                    if (bag.now - f.t0 >= FlightMs) {
                        flights.remove(f)
                        land(f.entry.key)
                    }
                }
                for (key in cart.lifting.toList()) {
                    if (bag.rise(key) >= 1.3f) finishLift(key)
                }
            }
        }
    }

    // The scripted fingertip, as the prototype's demo moves it: it travels to a control on a
    // little arc, presses it, and fades out once the script has paused.
    var fingerJob by remember { mutableStateOf<Job?>(null) }
    suspend fun fingerTap(id: String, action: () -> Unit) {
        val target = bounds[id]?.center ?: return
        fingerJob?.cancel()
        val size = root?.size
        if (finger.shown < .01f && size != null) {
            finger.at = Offset(size.width * .5f, size.height * .7f)
        }
        val from = finger.at
        val travel = 520
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { now ->
                p = ((now - start) / 1e6f / travel).coerceIn(0f, 1f)
                val e = ease(p)
                finger.shown = min(1f, finger.shown + .12f)
                finger.at =
                    Offset(
                        from.x + (target.x - from.x) * e,
                        from.y + (target.y - from.y) * e - sin(p * PI.toFloat()) * 18f * density,
                    )
            }
        }
        finger.shown = 1f
        delay(100)
        finger.down = true
        delay(ScriptedTouch.TapHoldMs)
        finger.down = false
        action()
        fingerJob = scope.launch {
            delay(1400)
            val at = withFrameNanos { it }
            var q = 0f
            while (q < 1f) {
                withFrameNanos { now ->
                    q = ((now - at) / 1e6f / ScriptedTouch.UpMs).coerceIn(0f, 1f)
                    finger.shown = 1f - q
                }
            }
        }
    }

    // Lifts everything out of the bag, one by one, wherever the bag is.
    suspend fun emptyBag() {
        flights.clear()
        cart.incoming.clear()
        val keys = cart.entries.map { it.key }
        if (reducedMotion) {
            keys.forEach(::finishLift)
            return
        }
        for (key in keys) {
            remove(cart.entries.first { it.key == key })
            delay(140)
        }
    }

    DisposableEffect(state) {
        state.setControlTapHandler { control ->
            when {
                control.startsWith(PaperBagDemos.AddPrefix) -> {
                    val product = Groceries.byId(control.removePrefix(PaperBagDemos.AddPrefix))
                    fingerTap("add-${product.id}") { add(product) }
                }
                control == PaperBagDemos.Bag -> fingerTap("bag") { openBag() }
                control.startsWith(PaperBagDemos.RemovePrefix) -> {
                    val id = control.removePrefix(PaperBagDemos.RemovePrefix)
                    val entry = cart.entries.firstOrNull { it.product.id == id }
                    if (entry != null) fingerTap("rm-${entry.key}") { remove(entry) }
                }
                control == PaperBagDemos.Done -> fingerTap("done") { closeBag() }
            }
        }
        state.setFeedResetHandler { emptyBag() }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = bag.isOpen,
        onBackCompleted = { closeBag() },
    )

    val sheetShown by remember {
        derivedStateOf {
            bag.frame
            bag.isOpen || bag.open > .02f
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(colors.background)
            .onGloballyPositioned { root = it }
            .onPreviewKeyEvent {
                if (it.key == Key.Escape && it.type == KeyEventType.KeyUp && bag.isOpen) {
                    closeBag()
                    true
                } else false
            },
    ) {
        val screenHeight = maxHeight
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(demoInsets(state))
                .then(
                    if (maxWidth > 600.dp) Modifier.padding(horizontal = (maxWidth - 560.dp) / 2)
                    else Modifier,
                ),
        ) {
            AppBar(colors) {
                val description =
                    if (cart.count == 0) stringResource(Res.string.paperbag_bag_empty)
                    else
                        pluralStringResource(Res.plurals.paperbag_bag_items, cart.count, cart.count)
                PressableBox(
                    onClick = { if (bag.isOpen) closeBag() else openBag() },
                    shape = RoundedCornerShape(14.dp),
                    ring = colors.accent,
                    modifier =
                        Modifier.size(52.dp, 48.dp)
                            .place("bag")
                            .focusRequester(bagFocus)
                            .testTag("bag")
                            .semantics { contentDescription = description },
                ) {}
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Aisle(
                    colors,
                    cart,
                    ink,
                    { id -> Modifier.place(id) },
                    ::add,
                    sheetShown && bag.isOpen,
                )
                if (sheetShown) {
                    Sheet(
                        colors = colors,
                        cart = cart,
                        bag = bag,
                        screenHeight = screenHeight,
                        place = { id -> Modifier.place(id) },
                        doneFocus = doneFocus,
                        onDone = ::closeBag,
                        onRemove = ::remove,
                        onCheckOut = { say { Said.CheckOut(it) } },
                    )
                }
            }
        }

        // The bag, wherever it is between the corner and the sheet, and what is flying to it.
        Canvas(Modifier.fillMaxSize()) {
            bag.frame
            val g = Canvas2D(drawContext.canvas, size.width.toInt(), size.height.toInt())
            g.setTransform(density, 0f, 0f, density, 0f, 0f)
            val icon = bounds["bag"]
            // Where the bag is and how big: in the corner, in the sheet, or on its way.
            var bagX = (icon?.center?.x ?: 0f) / density
            var bagY = (icon?.center?.y ?: 0f) / density + IconDrop
            var bagS = IconSize
            val items = cart.entries.map { BagItem(it.product.art, it.slot, bag.rise(it.key)) }
            val pose = bag.pose(items)
            if (icon != null) {
                val e = ease(bag.open)
                val well = bounds["well"]
                if (bag.open > 0f && well != null) {
                    val ws = (well.height / density * .54f).coerceIn(84f, 140f)
                    val wx = well.center.x / density - ws * .06f
                    val wy = well.bottom / density - 12f
                    bagX += (wx - bagX) * e
                    bagY += (wy - bagY) * e
                    bagS += (ws - bagS) * e
                }
                drawBag(g, bagX, bagY, bagS, pose, ink, dark, density)
            }
            val h = size.height / density
            for (f in flights) {
                val p = ((bag.now - f.t0) / FlightMs).toFloat().coerceIn(0f, 1f)
                val e = p * p * (3 - 2 * p)
                val x0 = f.from.x / density
                val y0 = f.from.y / density
                // Aimed at the bag where it is now, so a thing still flying when the bag is
                // opened follows it into the sheet: the prototype's went on to the empty corner.
                val x = x0 + (bagX - x0) * e
                val y =
                    y0 + (bagY - bagS * .7f - y0) * e - sin(p * PI.toFloat()) * min(160f, h * .22f)
                val s = 40f + (bagS * .9f - 40f) * e
                g.save()
                g.translate(x, y)
                g.rotate((1 - p) * .9f * (if (f.entry.product.id == "bread") 1f else .4f) - .2f)
                g.globalAlpha = if (p > .9f) 1 - (p - .9f) / .1f * .6f else 1f
                drawItem(g, f.entry.product.art, s, ink)
                g.restore()
            }
            if (finger.shown > 0f) {
                val k = if (finger.down) .82f else 1f
                scale(k, k, pivot = finger.at) { drawTouch(touch, finger.at, finger.shown) }
            }
        }

        // Read out as things go in and out.
        val announcement =
            when (val s = said) {
                is Said.Added ->
                    stringResource(
                        Res.string.paperbag_added,
                        stringResource(s.product.name),
                        s.count,
                    )
                is Said.Removed ->
                    stringResource(
                        Res.string.paperbag_removed,
                        stringResource(s.product.name),
                        s.count,
                    )
                is Said.Full -> stringResource(Res.string.paperbag_full)
                is Said.CheckOut -> stringResource(Res.string.paperbag_check_out_note)
                null -> null
            }
        Box(
            Modifier.size(1.dp).testTag("live").semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement != null) contentDescription = announcement
            },
        )
    }
}

@Composable
private fun AppBar(colors: PaperBagColors, action: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(70.dp)
            .drawWithContent {
                drawContent()
                drawRect(
                    colors.line,
                    Offset(0f, size.height - 1.dp.toPx()),
                    size.copy(height = 1.dp.toPx()),
                )
            }
            .padding(start = 16.dp, end = 8.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(Res.string.paperbag_store),
                style = TextStyle(colors.ink, 17.sp, FontWeight.Bold, letterSpacing = (-.17).sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                stringResource(Res.string.paperbag_delivery),
                style = TextStyle(colors.muted, 12.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        action()
    }
}

@Composable
private fun Aisle(
    colors: PaperBagColors,
    cart: GroceryCart,
    ink: BagInk,
    place: (String) -> Modifier,
    onAdd: (Product) -> Unit,
    covered: Boolean,
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
            // Under the open bag it is hidden from a screen reader, as the prototype's was.
            .then(if (covered) Modifier.clearAndSetSemantics {} else Modifier),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            BasicText(
                stringResource(Res.string.paperbag_hello),
                style = TextStyle(colors.ink, 22.sp, FontWeight.Bold, letterSpacing = (-.33).sp),
            )
            BasicText(
                stringResource(Res.string.paperbag_hello_line),
                style = TextStyle(colors.muted, 13.5.sp),
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(0.dp)),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val chips =
                listOf(
                    Res.string.paperbag_chip_produce,
                    Res.string.paperbag_chip_bakery,
                    Res.string.paperbag_chip_dairy,
                    Res.string.paperbag_chip_flowers,
                )
            chips.forEachIndexed { i, chip ->
                BasicText(
                    stringResource(chip),
                    style = TextStyle(if (i == 0) colors.background else colors.ink, 13.sp),
                    maxLines = 1,
                    softWrap = false,
                    modifier =
                        Modifier.clip(CircleShape)
                            .background(if (i == 0) colors.ink else colors.chip)
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (product in Groceries.all) {
                ProductRow(colors, product, cart.quantity(product), ink, place, { onAdd(product) })
            }
        }
    }
}

@Composable
private fun ProductRow(
    colors: PaperBagColors,
    product: Product,
    quantity: Int,
    ink: BagInk,
    place: (String) -> Modifier,
    onAdd: () -> Unit,
) {
    val name = stringResource(product.name)
    val price = money(product.priceCents)
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(
            Modifier.size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.tile)
                .then(place("thumb-${product.id}")),
        ) {
            val g = Canvas2D(drawContext.canvas, size.width.toInt(), size.height.toInt())
            g.setTransform(density, 0f, 0f, density, 0f, 0f)
            drawThumb(g, product.art, 56f, ink)
        }
        Column(Modifier.weight(1f)) {
            BasicText(
                name,
                style = TextStyle(colors.ink, 15.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The count stands apart from the unit line, which gives way first: in the prototype
            // it was the end of that line and cut off with it.
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    stringResource(
                        Res.string.paperbag_price_unit,
                        price,
                        stringResource(product.unit),
                    ),
                    style = TextStyle(colors.muted, 12.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (quantity > 0) {
                    BasicText(
                        " · " + stringResource(Res.string.paperbag_in_bag, quantity),
                        style = TextStyle(colors.accent, 12.5.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        val label = stringResource(Res.string.paperbag_add_label, name, price)
        PressableBox(
            onClick = onAdd,
            shape = CircleShape,
            ring = colors.accent,
            modifier =
                Modifier.heightIn(min = 40.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .then(place("add-${product.id}"))
                    .testTag("add-${product.id}")
                    .semantics { contentDescription = label },
        ) {
            BasicText(
                stringResource(Res.string.paperbag_add),
                style = TextStyle(colors.accentInk, 14.sp, FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun Sheet(
    colors: PaperBagColors,
    cart: GroceryCart,
    bag: PaperBagState,
    screenHeight: Dp,
    place: (String) -> Modifier,
    doneFocus: FocusRequester,
    onDone: () -> Unit,
    onRemove: (BagEntry) -> Unit,
    onCheckOut: () -> Unit,
) {
    val title =
        if (cart.count == 0) stringResource(Res.string.paperbag_your_bag)
        else pluralStringResource(Res.plurals.paperbag_your_bag_items, cart.count, cart.count)
    val paneTitle = stringResource(Res.string.paperbag_your_bag)
    Column(
        Modifier.fillMaxSize()
            .graphicsLayer {
                bag.frame
                alpha = (bag.open * 1.6f).coerceIn(0f, 1f)
            }
            .background(colors.background)
            // While it is open it takes every touch; closing, it lets them through, as the
            // prototype's did.
            .then(
                if (bag.isOpen) Modifier.pointerInput(Unit) { awaitPointerEventScopeConsumeAll() }
                else Modifier,
            )
            .semantics { this.paneTitle = paneTitle },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(title, style = TextStyle(colors.ink, 17.sp, FontWeight.Bold))
            PressableBox(
                onClick = onDone,
                shape = CircleShape,
                ring = colors.accent,
                modifier =
                    Modifier.clip(CircleShape)
                        .background(colors.chip)
                        .then(place("done"))
                        .focusRequester(doneFocus)
                        .testTag("done"),
            ) {
                BasicText(
                    stringResource(Res.string.paperbag_done),
                    style = TextStyle(colors.ink, 13.5.sp, FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(
            Modifier.fillMaxWidth()
                .height((screenHeight * .3f).coerceIn(150.dp, 240.dp))
                .then(place("well")),
        )
        Column(
            Modifier.fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            if (cart.entries.isEmpty()) {
                BasicText(
                    stringResource(Res.string.paperbag_nothing),
                    style =
                        TextStyle(
                            colors.muted,
                            14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 18.dp),
                )
            }
            for (entry in cart.entries) {
                val name = stringResource(entry.product.name)
                val removeLabel = stringResource(Res.string.paperbag_remove, name)
                Row(
                    Modifier.fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                colors.line,
                                Offset(0f, size.height - 1.dp.toPx()),
                                size.copy(height = 1.dp.toPx()),
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicText(
                        name,
                        style = TextStyle(colors.ink, 14.5.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    BasicText(
                        money(entry.product.priceCents),
                        style = TextStyle(colors.muted, 13.sp),
                    )
                    PressableBox(
                        onClick = { onRemove(entry) },
                        shape = CircleShape,
                        ring = colors.accent,
                        modifier =
                            Modifier.size(36.dp)
                                .border(1.5.dp, colors.line, CircleShape)
                                .then(place("rm-${entry.key}"))
                                .testTag("rm-${entry.key}")
                                .semantics { contentDescription = removeLabel },
                    ) {
                        BasicText(
                            "−",
                            style = TextStyle(colors.ink, 18.sp),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawRect(colors.line, Offset.Zero, size.copy(height = 1.dp.toPx()))
                }
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
        ) {
            val enabled = cart.count > 0
            val label =
                if (enabled)
                    stringResource(Res.string.paperbag_check_out_total, money(cart.totalCents))
                else stringResource(Res.string.paperbag_check_out)
            PressableBox(
                onClick = { if (enabled) onCheckOut() },
                shape = RoundedCornerShape(14.dp),
                ring = colors.accent,
                modifier =
                    Modifier.fillMaxWidth()
                        .graphicsLayer { alpha = if (enabled) 1f else .45f }
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent)
                        .semantics { if (!enabled) disabled() },
            ) {
                BasicText(
                    label,
                    style = TextStyle(colors.accentInk, 15.sp, FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center).padding(13.dp),
                )
            }
        }
    }
}

/**
 * A button that shrinks a little while pressed, as the prototype's do, and rings only while it has
 * the keyboard's focus: a tap never leaves a ring behind, as it did on the prototype's bag.
 */
@Composable
private fun PressableBox(
    onClick: () -> Unit,
    shape: Shape,
    ring: Color,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val inputMode = LocalInputModeManager.current
    var focused by remember { mutableStateOf(false) }
    Box(
        // The ring goes round the outside, so it comes before the caller's clip.
        Modifier.onFocusChanged { focused = it.isFocused }
            .drawWithContent {
                drawContent()
                if (focused && inputMode.inputMode == InputMode.Keyboard) {
                    val o = 2.dp.toPx() + 1.dp.toPx()
                    val outline =
                        shape.createOutline(
                            size.copy(width = size.width + 2 * o, height = size.height + 2 * o),
                            layoutDirection,
                            this,
                        )
                    translate(-o, -o) {
                        drawOutline(outline, ring, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            .then(modifier)
            .graphicsLayer {
                val k = if (pressed) .93f else 1f
                scaleX = k
                scaleY = k
            }
            .clickable(
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope
    .awaitPointerEventScopeConsumeAll() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

// How long a product takes to hop into the bag, in milliseconds.
private const val FlightMs = 560.0

// The bag in the corner: its height, and how far below the button's centre its bottom stands. The
// prototype drew it 13 px down in the icon but tipped it open from 16, so it jumped as it opened.
private const val IconSize = 34f
private const val IconDrop = 13f
