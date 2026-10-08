package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.magnet_apply
import dev.dimvlachos.lab.resources.magnet_count
import dev.dimvlachos.lab.resources.magnet_filter
import dev.dimvlachos.lab.resources.magnet_off
import dev.dimvlachos.lab.resources.magnet_on_both
import dev.dimvlachos.lab.resources.magnet_on_one
import dev.dimvlachos.lab.resources.magnet_remove
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A steel table strewn with photo cards and dusted with iron filings, with a magnet for each tag
 * waiting in a strip along its foot. Drag a magnet over the table: the filings rise along its
 * field, matching photos are pulled towards it, harder the better they match, and strong matches
 * snap and stick, with a tick of haptics. What sticks is the filter's result, counted on a badge
 * over the magnet. A second magnet combines filters: a photo matching both hangs between them, one
 * matching one clings to its own. Flick a magnet back into the strip and its photos fall off and
 * slide home; pull a photo off by hand and it leaves the results. Tap a magnet to fan its photos
 * out into a grid, and tap anywhere to fold them back.
 *
 * Every photo is a node for a screen reader, with its title and the magnet it is on; every magnet
 * is a switch, "Sunset filter, 3 photos", that applies or removes its filter, and a change of count
 * is announced. Nothing recomposes while the cards move, the filings turn or the grid fans out:
 * only a photo sticking or coming off, or a magnet going out or back, does. The table fills the
 * space it is given.
 */
@Composable
public fun MagnetTable(state: MagnetState, modifier: Modifier = Modifier) {
    ReportComposition()
    val haptics = LocalHapticFeedback.current
    DisposableEffect(state, haptics) {
        // A key's click on Android and a light impact on iOS, as the pull cord's click.
        val hook: () -> Unit = { haptics.performHapticFeedback(HapticFeedbackType.VirtualKey) }
        state.onStick = hook
        onDispose { if (state.onStick === hook) state.onStick = null }
    }
    // The table steps on a frame at a time while anything moves or is held, and the loop sleeps
    // once it is all still.
    LaunchedEffect(state) {
        while (true) {
            snapshotFlow { state.awake }.first { it }
            var last = withFrameNanos { it }
            while (state.awake) {
                withFrameNanos { now ->
                    state.advance((now - last) / 1_000_000_000f)
                    last = now
                }
            }
        }
    }
    val colors = LabTheme.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = LabTheme.typography.label.copy(color = colors.textPrimary)
    val badgeStyle = LabTheme.typography.label.copy(color = colors.onBadge)
    val labels =
        remember(state.tags, measurer, labelStyle) {
            state.tags.map { measurer.measure(it.label, labelStyle) }
        }
    val results = state.results
    val counts =
        state.tags.map { tag ->
            results[tag.id]?.let {
                pluralStringResource(Res.plurals.magnet_count, it.size, it.size)
            }
        }
    val badges =
        remember(counts, measurer, badgeStyle) {
            counts.map { count -> count?.let { measurer.measure(it, badgeStyle) } }
        }
    val photos = state.photos.map { imageResource(it.image) }
    val thumbs =
        remember(photos, density) {
            with(density) {
                // As big as the cards grow in the grid, so they stay sharp there too.
                val inset = MagnetDimens.CardBorder * 2
                val width = ((MagnetDimens.CardWidth - inset) * MagnetDimens.FanScale).roundToPx()
                val height = ((MagnetDimens.CardHeight - inset) * MagnetDimens.FanScale).roundToPx()
                photos.map { thumbnail(it, width, height) }
            }
        }
    val painter = remember { MagnetPainter() }
    val filings = remember { FilingsPainter() }
    val slop = LocalViewConfiguration.current.touchSlop
    Box(
        modifier
            .onSizeChanged { state.layOut(it.width, it.height, density.density) }
            .pointerInput(state, slop) { tableGestures(state, slop) }
    ) {
        // The steel and the strip, drawn again only when the table changes size.
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val steel = Brush.verticalGradient(listOf(colors.tableSteel, colors.tableSteelDeep))
                onDrawBehind { with(painter) { drawTable(state, colors, steel) } }
            }
        )
        // The filings, in a layer of their own, drawn again only when a magnet moves.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawBehind {
                with(filings) { drawFilings(state, colors.filing) }
            }
        )
        // The cards and the magnets, every frame they move.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawBehind {
                with(painter) {
                    drawCards(state, thumbs, colors)
                    drawMagnets(state, labels, badges, colors)
                    drawFan(state, thumbs, colors)
                }
            }
        )
        state.photos.forEachIndexed { index, photo ->
            val on = state.tags.filter { results[it.id]?.contains(photo.id) == true }
            val where =
                when (on.size) {
                    0 -> null
                    1 -> stringResource(Res.string.magnet_on_one, on[0].label)
                    else -> stringResource(Res.string.magnet_on_both, on[0].label, on[1].label)
                }
            PhotoNode(state, index, photo.title, where)
        }
        for ((i, tag) in state.tags.withIndex()) MagnetNode(state, tag, counts[i])
        Announcer(state, counts)
    }
}

// Says, politely, which magnet's count just changed, "Sunset filter, 3 photos", from a node that
// never moves: on a magnet's own node, every step of its slide would be announced again.
@Composable
private fun Announcer(state: MagnetState, counts: List<String?>) {
    val names = state.tags.map { stringResource(Res.string.magnet_filter, it.label) }
    val off = stringResource(Res.string.magnet_off)
    val last = remember(state) { LastAnnounced(counts) }
    val changed = counts.indices.firstOrNull { counts[it] != last.counts.getOrNull(it) }
    val text = if (changed == null) last.text else "${names[changed]}, ${counts[changed] ?: off}"
    SideEffect {
        last.counts = counts
        last.text = text
    }
    Spacer(
        Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            if (text.isNotEmpty()) contentDescription = text
        }
    )
}

private class LastAnnounced(var counts: List<String?>) {
    var text = ""
}

// A photo for a screen reader, over its card: placed in layout, so it follows the card without
// recomposing.
@Composable
private fun PhotoNode(state: MagnetState, index: Int, title: String, where: String?) {
    Spacer(
        Modifier.offset { state.photoTopLeft(index) }
            .size(MagnetDimens.CardWidth, MagnetDimens.CardHeight)
            .semantics {
                contentDescription = title
                if (where != null) stateDescription = where
            }
    )
}

// A magnet for a screen reader: a switch that applies its filter or removes it, saying how many
// photos it holds. It moves with its magnet, so the count's changes are announced from a node that
// stays still instead.
@Composable
private fun MagnetNode(state: MagnetState, tag: MagnetTag, count: String?) {
    val name = stringResource(Res.string.magnet_filter, tag.label)
    val off = stringResource(Res.string.magnet_off)
    val apply = stringResource(Res.string.magnet_apply, tag.label)
    val remove = stringResource(Res.string.magnet_remove, tag.label)
    val on = count != null
    Spacer(
        Modifier.offset { state.magnetTopLeft(tag.id) }
            .size(MagnetDimens.GrabRadius * 2)
            .semantics {
                contentDescription = name
                role = Role.Switch
                toggleableState = ToggleableState(on)
                stateDescription = count ?: off
                // A third magnet out is refused, and the click says so.
                onClick {
                    if (on) {
                        state.remove(tag.id)
                        true
                    } else {
                        state.apply(tag.id)
                    }
                }
                customActions =
                    listOf(
                        if (on) {
                            CustomAccessibilityAction(remove) {
                                state.remove(tag.id)
                                true
                            }
                        } else {
                            CustomAccessibilityAction(apply) { state.apply(tag.id) }
                        }
                    )
            }
    )
}

private suspend fun PointerInputScope.tableGestures(state: MagnetState, slop: Float) {
    awaitEachGesture {
        val down = awaitFirstDown()
        // Fanned out, a touch anywhere folds the grid back once it lifts.
        if (state.fannedOut != null) {
            down.consume()
            waitForUpOrCancellation()?.consume()
            state.fanOut(null)
            return@awaitEachGesture
        }
        val tag = state.magnetAt(down.position)
        if (tag != null) {
            carryMagnet(state, tag, down, slop)
            return@awaitEachGesture
        }
        if (!state.photoUnder(down.position)) return@awaitEachGesture
        down.consume()
        // Taken off its magnet only once the finger moves: a tap, or a tap that just missed a
        // magnet, leaves the card where it is.
        if (awaitSlop(down, slop) == null) return@awaitEachGesture
        val centre = state.grabPhoto(down.position) ?: return@awaitEachGesture
        // Carried by where it was taken, not by its middle.
        val grip = centre - down.position
        try {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                change.consume()
                state.dragPhoto(change.position + grip)
                if (!change.pressed) break
            }
        } finally {
            state.dropPhoto()
        }
    }
}

// Takes a magnet where the finger came down and carries it until it lifts. It doesn't move until
// the finger has gone past the touch slop, so a tap leaves it where it lies; a tap on a magnet
// down on the table fans out what is on it.
private suspend fun AwaitPointerEventScope.carryMagnet(
    state: MagnetState,
    tag: String,
    down: PointerInputChange,
    slop: Float,
) {
    val wasOut = state.isOut(tag)
    val grip = (state.magnetPosition(tag) ?: down.position) - down.position
    // A magnet in the strip is taken out only once the finger moves: a tap there changes nothing.
    if (!wasOut) {
        down.consume()
        val moving = awaitSlop(down, slop) ?: return
        if (!state.place(tag, down.position + grip)) return
        state.drag(tag, moving.position + grip)
    } else {
        if (!state.place(tag, down.position + grip)) return
        down.consume()
    }
    val tracker = VelocityTracker()
    tracker.addPosition(down.uptimeMillis, down.position)
    var moved = !wasOut
    var lifted = false
    try {
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            change.consume()
            tracker.addPosition(change.uptimeMillis, change.position)
            if (!moved && (change.position - down.position).getDistance() > slop) moved = true
            if (moved) state.drag(tag, change.position + grip)
            if (!change.pressed) {
                lifted = true
                break
            }
        }
    } finally {
        val velocity = tracker.calculateVelocity()
        state.release(tag, if (moved) Offset(velocity.x, velocity.y) else Offset.Zero)
    }
    if (lifted && !moved && wasOut) state.fanOut(tag)
}

// Waits for the finger that came [down] to move past the touch [slop], and returns that move;
// null if it lifts, or the gesture ends, first.
private suspend fun AwaitPointerEventScope.awaitSlop(
    down: PointerInputChange,
    slop: Float,
): PointerInputChange? {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return null
        change.consume()
        if (!change.pressed) return null
        if ((change.position - down.position).getDistance() > slop) return change
    }
}
