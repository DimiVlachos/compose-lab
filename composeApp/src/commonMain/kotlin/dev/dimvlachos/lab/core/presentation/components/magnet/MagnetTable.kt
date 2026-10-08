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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.magnet_apply
import dev.dimvlachos.lab.resources.magnet_close
import dev.dimvlachos.lab.resources.magnet_count
import dev.dimvlachos.lab.resources.magnet_filter
import dev.dimvlachos.lab.resources.magnet_off
import dev.dimvlachos.lab.resources.magnet_on_both
import dev.dimvlachos.lab.resources.magnet_on_one
import dev.dimvlachos.lab.resources.magnet_open
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
 * is announced. Tap a photo in the grid to open it, as a screen reader's Open does; with it open,
 * it alone is there for a screen reader. System back closes an open photo, then folds the grid,
 * then is the screen's again. Nothing recomposes while the cards move, the filings turn, the
 * magnets swing on their nails or the grid fans out: only a photo sticking or coming off, a magnet
 * going out or back, or the grid or a photo opening, does. The table fills the space it is given.
 */
@Composable
public fun MagnetTable(state: MagnetState, modifier: Modifier = Modifier) {
    ReportComposition()
    val haptics = LocalHapticFeedback.current
    DisposableEffect(state, haptics) {
        // A strong match snaps with a key's click, as the pull cord's does; one that only just
        // matches, with a light tick, so a hand feels how well each matched. A magnet put away
        // lands in its slot with a soft tick that closes the gesture.
        val stick: (Float) -> Unit = { strength ->
            haptics.performHapticFeedback(
                if (strength >= MagnetDimens.StrongMatch) HapticFeedbackType.VirtualKey
                else HapticFeedbackType.SegmentFrequentTick
            )
        }
        val slot: () -> Unit = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) }
        // A third magnet, refused, buzzes no: two at a time is the most.
        val refuse: () -> Unit = { haptics.performHapticFeedback(HapticFeedbackType.Reject) }
        state.onStick = stick
        state.onSlot = slot
        state.onRefuse = refuse
        onDispose {
            if (state.onStick === stick) state.onStick = null
            if (state.onSlot === slot) state.onSlot = null
            if (state.onRefuse === refuse) state.onRefuse = null
        }
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
    // The system back gesture closes the topmost layer, as a tap does: an open photo first, then
    // the fanned-out grid. With neither out it is off, so back is the screen's again.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.opened != null || state.fannedOut != null,
        onBackCompleted = { if (state.opened != null) state.close() else state.fanOut(null) },
    )
    val colors = LabTheme.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = LabTheme.typography.label.copy(color = colors.textPrimary)
    val badgeStyle = LabTheme.typography.label.copy(color = colors.onBadge)
    val labels =
        remember(state.tags, measurer, labelStyle) {
            state.tags.map { measurer.measure(it.label, labelStyle) }
        }
    // The strip is tall enough for a magnet in the middle of it and its label under it, at any
    // size of text: half of it holds the magnet's lower half, the gap, the label and a little room.
    val labelHeight = labels.maxOfOrNull { it.size.height } ?: 0
    val strip =
        with(density) {
            maxOf(
                MagnetDimens.StripHeight.value,
                2f *
                    ((MagnetDimens.HorseshoeLegEnd +
                            MagnetDimens.HorseshoeTip +
                            MagnetDimens.LabelGap +
                            MagnetDimens.StripPad)
                        .value + labelHeight.toDp().value),
            )
        }
    val size = remember(state) { TableSize() }
    SideEffect { state.layOut(size.width, size.height, density.density, strip) }
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
    val titleStyle = LabTheme.typography.label.copy(color = colors.textPrimary)
    // Each title fits its cell in the grid, on one line, cut short if it must be; the grid leaves
    // room under its cards for the tallest, at any size of text.
    val cellWidth = with(density) { (MagnetDimens.CardWidth * MagnetDimens.FanScale).roundToPx() }
    val titles =
        remember(state.photos, measurer, titleStyle, cellWidth) {
            state.photos.map {
                measurer.measure(
                    it.title,
                    titleStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = cellWidth),
                )
            }
        }
    val caption =
        with(density) {
            maxOf(
                MagnetDimens.FanCaption.value,
                (titles.maxOfOrNull { it.size.height } ?: 0).toDp().value +
                    MagnetDimens.FanCaptionGap.value * 2f,
            )
        }
    SideEffect { state.fanCaption = caption }
    // Over the fanned-out grid: the magnet's name and its count, "Sunset · 3 photos". It stays
    // drawn as the grid folds after its magnet has been let go.
    val fanned = state.tags.indexOfFirst { it.id == state.fannedOut }
    val header =
        remember(fanned, counts, measurer, badgeStyle) {
            if (fanned < 0) null
            else measurer.measure("${state.tags[fanned].label} · ${counts[fanned]}", badgeStyle)
        }
    val lastHeader = remember(state) { HeaderHolder() }
    if (header != null) lastHeader.value = header
    // The opened photo's title and caption, measured as it opens, and kept as it closes.
    val openStyle = LabTheme.typography.subtitle.copy(color = colors.textPrimary)
    val captionStyle = LabTheme.typography.body.copy(color = colors.textMuted)
    val openPhoto = state.photos.firstOrNull { it.id == state.opened }
    val lastOpen = remember(state) { OpenWords() }
    // As wide as the opened photo, and wrapped onto more lines if they need them.
    val wordsWidth =
        with(density) { (size.width - (MagnetDimens.OpenMargin * 2).roundToPx()).coerceAtLeast(1) }
    if (openPhoto != null) {
        val words = Constraints(maxWidth = wordsWidth)
        lastOpen.title =
            remember(openPhoto, measurer, openStyle, wordsWidth) {
                measurer.measure(openPhoto.title, openStyle, constraints = words)
            }
        lastOpen.caption =
            remember(openPhoto, measurer, captionStyle, wordsWidth) {
                if (openPhoto.caption.isEmpty()) null
                else measurer.measure(openPhoto.caption, captionStyle, constraints = words)
            }
    }
    val photos = state.photos.map { imageResource(it.image) }
    val thumbs =
        remember(photos, density.density) {
            with(density) {
                // As big as the cards grow in the grid, so they stay sharp there too.
                val inset = MagnetDimens.CardBorder * 2
                val width = ((MagnetDimens.CardWidth - inset) * MagnetDimens.FanScale).roundToPx()
                val height = ((MagnetDimens.CardHeight - inset) * MagnetDimens.FanScale).roundToPx()
                photos.map { thumbnail(it, width, height) }
            }
        }
    val painter = remember(state) { MagnetPainter() }
    val openPainter = remember(state) { OpenPainter() }
    val filings = remember(state) { FilingsPainter() }
    val slop = LocalViewConfiguration.current.touchSlop
    Box(
        modifier
            .onSizeChanged {
                size.width = it.width
                size.height = it.height
                state.layOut(it.width, it.height, density.density, strip)
            }
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
                    drawFan(state, thumbs, titles, lastHeader.value, colors)
                }
                with(openPainter) {
                    drawOpen(state, photos, lastOpen.title, lastOpen.caption, colors)
                }
            }
        )
        // With a photo open it alone is there for a screen reader, as it alone is to be seen.
        if (openPhoto != null) {
            OpenNode(state, openPhoto)
            return@Box
        }
        state.photos.forEachIndexed { index, photo ->
            val on = state.tags.filter { results[it.id]?.contains(photo.id) == true }
            val where =
                when (on.size) {
                    0 -> null
                    1 -> stringResource(Res.string.magnet_on_one, on[0].label)
                    else -> stringResource(Res.string.magnet_on_both, on[0].label, on[1].label)
                }
            PhotoNode(state, index, photo, where, inGrid = state.fannedOut != null)
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

// The table's size as last laid out, in px, so a change of strip lays it out again.
private class TableSize {
    var width by mutableIntStateOf(0)
    var height by mutableIntStateOf(0)
}

private class HeaderHolder {
    var value: TextLayoutResult? = null
}

private class LastAnnounced(var counts: List<String?>) {
    var text = ""
}

// A photo for a screen reader, over its card: placed in layout, so it follows the card without
// recomposing.
@Composable
private fun PhotoNode(
    state: MagnetState,
    index: Int,
    photo: MagnetPhoto,
    where: String?,
    inGrid: Boolean,
) {
    ReportComposition()
    val open = stringResource(Res.string.magnet_open)
    Spacer(
        Modifier.offset { state.photoTopLeft(index) }
            .size(MagnetDimens.CardWidth, MagnetDimens.CardHeight)
            .semantics {
                contentDescription = photo.title
                if (where != null) stateDescription = where
                // In the grid, a photo opens, as a tap on it does.
                if (inGrid && state.inFan(index)) {
                    onClick(label = open) {
                        state.open(photo.id)
                        state.opened == photo.id
                    }
                }
            }
    )
}

// The opened photo for a screen reader: its title and caption over the whole table, closed back
// into the grid as a tap does.
@Composable
private fun OpenNode(state: MagnetState, photo: MagnetPhoto) {
    val close = stringResource(Res.string.magnet_close)
    val description =
        if (photo.caption.isEmpty()) photo.title else "${photo.title}, ${photo.caption}"
    Spacer(
        Modifier.fillMaxSize().semantics {
            // A pane of its own, so a screen reader says it has opened.
            paneTitle = photo.title
            contentDescription = description
            onClick(label = close) {
                state.close()
                true
            }
        }
    )
}

private class OpenWords {
    var title: TextLayoutResult? = null
    var caption: TextLayoutResult? = null
}

// A magnet for a screen reader: a switch that applies its filter or removes it, saying how many
// photos it holds. It moves with its magnet, so the count's changes are announced from a node that
// stays still instead.
@Composable
private fun MagnetNode(state: MagnetState, tag: MagnetTag, count: String?) {
    ReportComposition()
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
        // Fanned out, a tap on a photo opens it; with a photo open, a tap closes it back into
        // the grid; anywhere else, a tap folds the grid back.
        if (state.fannedOut != null) {
            down.consume()
            waitForUpOrCancellation()?.consume() ?: return@awaitEachGesture
            val photo = state.fanPhotoAt(down.position)
            when {
                state.opened != null -> state.close()
                photo != null -> state.open(photo)
                else -> state.fanOut(null)
            }
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
