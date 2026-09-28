package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_santorini
import org.jetbrains.compose.resources.imageResource

/**
 * A container transform: a grid of photo cards where the card at [expandedIndex] (1-based, 0 for
 * the grid) has grown into a full-screen detail. Grid and detail overlap for the whole 400 ms
 * morph, never swap, and the transition between them is None on both sides: a slide or a fade on a
 * morph edge detaches the shared photo from its slot.
 *
 * Only the pieces a scripted demo with bundled images needs are here. A navigation hold (the signal
 * that keeps placeholders from flashing while a pop stalls the main thread), a video frame hand-off
 * and a pinned-header overlay clip all belong to the apps this was ported from; a hold would wrap
 * the layout and be called before the index flips. Going straight from one detail to another is not
 * a matched morph (the keys differ) and swaps; return to the grid first.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageMorph(
    photos: List<MorphPhoto>,
    expandedIndex: Int,
    onExpand: (Int) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    layers: MorphLayers = MorphLayers.All,
) {
    val painters = rememberMorphPainters(photos)
    val target = if (expandedIndex in 1..photos.size) expandedIndex else 0
    val latestExpand = rememberUpdatedState(onExpand)
    val latestCollapse = rememberUpdatedState(onCollapse)
    SharedTransitionLayout(modifier) {
        // Opens and closes go through the gate, so neither cuts into a morph in flight.
        val gate = rememberMorphGate()
        val expand: (Int) -> Unit = remember(gate) { { i -> gate { latestExpand.value(i) } } }
        val collapse: () -> Unit = remember(gate) { { gate { latestCollapse.value() } } }
        AnimatedContent(
            targetState = target,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                    // The detail stays on top in both directions, so on a close the grid is
                    // revealed under the fading backdrop rather than placed over it.
                    targetContentZIndex = if (targetState == 0) 0f else 1f,
                    // The stage already clips; the default size transform adds a clip layer.
                    sizeTransform = null,
                )
            },
            label = "imageMorph",
        ) { index ->
            if (index == 0) {
                MorphGrid(
                    photos = photos,
                    painters = painters,
                    layers = layers,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                    onExpand = expand,
                )
            } else {
                val photo = photos[index - 1]
                MorphDetail(
                    painter = painters[index - 1],
                    title = photo.title,
                    caption = photo.caption,
                    key = morphKey(index),
                    layers = layers,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                    onClose = collapse,
                )
            }
        }
    }
}

// Decoded above the transition, so both ends of a morph draw the same painter and the detail's
// first frame is never an empty placeholder (a new Image measures 0x0 on its first layout).
// Remembered per bitmap: painterResource builds a fresh BitmapPainter on every composition, which
// would hand each card a new painter whenever the scene changes and recompose them all.
@Composable
internal fun rememberMorphPainters(photos: List<MorphPhoto>): List<Painter> = photos.map { photo ->
    val bitmap = imageResource(photo.image)
    remember(bitmap) { BitmapPainter(bitmap) }
}

// Plain rows, not a lazy grid: every card stays composed, so the match for a returning detail is
// always there and never depends on what a lazy layout happened to keep.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MorphGrid(
    photos: List<MorphPhoto>,
    painters: List<Painter>,
    layers: MorphLayers,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onExpand: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(LabTheme.spacing.mediumLarge),
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        photos.indices.chunked(MorphDimens.Columns).forEach { row ->
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
            ) {
                row.forEach { i ->
                    MorphPhotoCard(
                        painter = painters[i],
                        title = photos[i].title,
                        key = morphKey(i + 1),
                        layers = layers,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onExpand(i + 1) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ImageMorphPreview() {
    LabTheme {
        var expanded by remember { mutableIntStateOf(0) }
        ImageMorph(
            photos =
                listOf(
                    MorphPhoto(Res.drawable.photo_corfu, "Corfu", "Sunset over the west coast"),
                    MorphPhoto(Res.drawable.photo_paxos, "Paxos", "A harbour town at golden hour"),
                    MorphPhoto(Res.drawable.photo_santorini, "Santorini", "Oia lit up at dusk"),
                    MorphPhoto(Res.drawable.photo_milos, "Milos", "A boat under the white cliffs"),
                    MorphPhoto(Res.drawable.photo_naxos, "Naxos", "The Portara at sunset"),
                    MorphPhoto(Res.drawable.photo_hydra, "Hydra", "Sailing into the harbour"),
                ),
            expandedIndex = expanded,
            onExpand = { expanded = it },
            onCollapse = { expanded = 0 },
            modifier = Modifier.fillMaxSize().background(LabTheme.colors.background),
        )
    }
}
