package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphDimens
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphLayers
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhoto
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhotoCard
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphKey
import dev.dimvlachos.lab.core.presentation.components.profile.matchesQuery
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.roundToInt

private const val CardAspect = 0.8f // width / height: the photos' own 4:5
private const val FilterFadeMs = 200

/**
 * The photo grid, filtered by [query]. A card that stops matching gives up its slot at once and
 * fades out where it stands, under the cards gliding into the freed slots; clearing the query fades
 * the returning ones in at their slots while the others glide back. Nothing waits for anything, so
 * no photo is ever missing from a slot mid-reshuffle. Every card keeps one parent and one place in
 * the composition, so its shared node and its glide survive the reshuffle.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun GalleryGrid(
    photos: List<MorphPhoto>,
    painters: List<Painter>,
    query: State<String>,
    openPhoto: Int,
    sharedTransitionScope: SharedTransitionScope,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    morphKeyOf: (Int) -> String = ::morphKey,
) {
    // Starting from the query's own matches, so a grid that appears mid-search fades nothing out.
    val filter =
        remember(photos.size) {
            List(photos.size) {
                MutableTransitionState(matchesQuery(photos[it].title, query.value))
            }
        }
    // Where each card last stood, so a card fading out stays there instead of jumping.
    val lastSlot = remember(photos.size) { IntArray(photos.size) { it } }
    val typed = query.value
    photos.forEachIndexed { i, photo -> filter[i].targetState = matchesQuery(photo.title, typed) }
    val glide = remember {
        BoundsTransform { _, _ -> tween(MorphDimens.OpenMs, easing = MorphDimens.MorphEasing) }
    }
    val gap = LabTheme.spacing.smallMedium
    Layout(
        modifier = modifier,
        content = {
            photos.forEachIndexed { i, photo ->
                Box(
                    with(sharedTransitionScope) {
                        Modifier.animateBounds(lookaheadScope = this, boundsTransform = glide)
                    }
                ) {
                    AnimatedVisibility(
                        visibleState = filter[i],
                        modifier = Modifier.fillMaxSize(),
                        enter = fadeIn(tween(FilterFadeMs)),
                        exit = fadeOut(tween(FilterFadeMs)),
                    ) {
                        // The photo morph's source: None both ways, the shared bounds carry it.
                        AnimatedVisibility(
                            openPhoto != i + 1,
                            modifier = Modifier.fillMaxSize(),
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            MorphPhotoCard(
                                painter = painters[i],
                                title = photo.title,
                                key = morphKeyOf(i + 1),
                                layers = MorphLayers.All,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = this,
                                onClick = { onOpen(i + 1) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val columns = MorphDimens.Columns
        val cellWidth = (constraints.maxWidth - gapPx * (columns - 1)) / columns
        val cellHeight = (cellWidth / CardAspect).roundToInt()
        val cell = Constraints.fixed(cellWidth, cellHeight)
        val placeables = measurables.map { it.measure(cell) }
        // As tall as the slots in use: the matches, and any card still fading out where it stood.
        var slots = 0
        var leavingEnd = 0
        filter.forEachIndexed { i, state ->
            if (state.targetState) slots++
            else if (state.currentState) leavingEnd = maxOf(leavingEnd, lastSlot[i] + 1)
        }
        val rows = (maxOf(slots, leavingEnd) + columns - 1) / columns
        val height =
            constraints.constrainHeight(rows * cellHeight + (rows - 1).coerceAtLeast(0) * gapPx)
        layout(constraints.maxWidth, height) {
            // Read here, in placement: a card that finishes fading re-places the grid without
            // recomposing it. Leaving cards are placed first and below, so the gliders pass over.
            fun slotX(slot: Int) = (slot % columns) * (cellWidth + gapPx)
            fun slotY(slot: Int) = (slot / columns) * (cellHeight + gapPx)
            placeables.forEachIndexed { i, placeable ->
                if (!filter[i].targetState && filter[i].currentState) {
                    placeable.place(slotX(lastSlot[i]), slotY(lastSlot[i]), zIndex = -1f)
                }
            }
            var slot = 0
            placeables.forEachIndexed { i, placeable ->
                if (filter[i].targetState) {
                    lastSlot[i] = slot
                    placeable.place(slotX(slot), slotY(slot))
                    slot++
                }
            }
        }
    }
}
