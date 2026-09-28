package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphDetail
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphDimens
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphLayers
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhoto
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphKey
import dev.dimvlachos.lab.core.presentation.components.imagemorph.rememberMorphPainters
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarSize
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarSource
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarTarget
import dev.dimvlachos.lab.core.presentation.components.profile.FabDialogTarget
import dev.dimvlachos.lab.core.presentation.components.profile.FabSize
import dev.dimvlachos.lab.core.presentation.components.profile.FabSource
import dev.dimvlachos.lab.core.presentation.components.profile.SearchSource
import dev.dimvlachos.lab.core.presentation.components.profile.SearchTarget
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.gallery_photo_count
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_santorini
import dev.dimvlachos.lab.resources.portrait
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

internal const val ProfileNameTag = "profileName"
internal const val GalleryScrollTag = "galleryScroll"
internal const val GalleryGridTag = "galleryGrid"
private val SearchButtonSlot = 40.dp
private val FabEdgeInset = 36.dp

/**
 * A profile screen where every element that opens does so as a shared-element morph: a photo card
 * grows into its detail, the avatar into a large circle whose pencil FAB grows into a "Change
 * profile photo" dialog, and the search button into a bar whose typed query filters the grid.
 * [scene] says what is open; any value is accepted and shown as the nearest legal scene. The screen
 * itself is always composed under the targets, and every source keeps its slot's size while away.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileGallery(
    title: String,
    name: String,
    portrait: DrawableResource,
    photos: List<MorphPhoto>,
    searchQuery: String,
    scene: GalleryScene,
    onSceneChange: (GalleryScene) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = scene.normalized(photos.size)
    val latest = rememberUpdatedState(current)
    val report = rememberUpdatedState(onSceneChange)
    val update: ((GalleryScene) -> GalleryScene) -> Unit = remember {
        { change -> report.value(change(latest.value)) }
    }
    val portraitBitmap = imageResource(portrait)
    val portraitPainter = remember(portraitBitmap) { BitmapPainter(portraitBitmap) }
    val painters = rememberMorphPainters(photos)
    val typed = remember { mutableStateOf("") }
    // Leaving search clears the filter at once, so the cards glide back while the bar closes.
    LaunchedEffect(current.search) { if (!current.search) typed.value = "" }
    val searchOpening = remember { { latest.value.search } }
    SharedTransitionLayout(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(LabTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Under the search bar's back arrow, so it leaves as the bar arrives.
                    AnimatedVisibility(
                        !current.search,
                        modifier = Modifier.weight(1f),
                        enter =
                            fadeIn(tween(MorphDimens.CloseMs, easing = MorphDimens.MorphEasing)),
                        exit = fadeOut(tween(MorphDimens.ChromeFadeOutMs)),
                    ) {
                        Text(
                            title,
                            color = LabTheme.colors.textPrimary,
                            style = LabTheme.typography.title,
                        )
                    }
                    SourceSlot(!current.search, Modifier.size(SearchButtonSlot)) {
                        SearchSource(
                            this@SharedTransitionLayout,
                            this,
                            searchOpening,
                            { update { it.copy(search = true) } },
                        )
                    }
                }
                // The header and the grid scroll together under the bar.
                Column(
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .testTag(GalleryScrollTag)
                        .verticalScroll(rememberScrollState())
                        // Inside the scroll, so the last row scrolls clear of the navigation bar.
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        )
                        .padding(bottom = LabTheme.spacing.medium)
                ) {
                    // Folds away with the search bar's open and back with its close, on the same
                    // curve and duration, so the grid rises and settles with the bar.
                    AnimatedVisibility(
                        !current.search,
                        enter =
                            fadeIn(tween(MorphDimens.CloseMs, easing = MorphDimens.MorphEasing)) +
                                expandVertically(
                                    tween(MorphDimens.CloseMs, easing = MorphDimens.MorphEasing)
                                ),
                        exit =
                            fadeOut(tween(MorphDimens.OpenMs, easing = MorphDimens.MorphEasing)) +
                                shrinkVertically(
                                    tween(MorphDimens.OpenMs, easing = MorphDimens.MorphEasing)
                                ),
                    ) {
                        ProfileHeader(
                            name = name,
                            count = stringResource(Res.string.gallery_photo_count, photos.size),
                            painter = portraitPainter,
                            avatarOpen = current.avatar,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            onAvatar = { update { it.copy(avatar = true) } },
                        )
                    }
                    GalleryGrid(
                        photos = photos,
                        painters = painters,
                        query = typed,
                        openPhoto = current.photo,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        onOpen = { index -> update { it.copy(photo = index) } },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = LabTheme.spacing.mediumLarge)
                                .testTag(GalleryGridTag),
                    )
                }
            }
            AnimatedVisibility(
                current.search,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                SearchTarget(
                    searchQuery,
                    typed,
                    this@SharedTransitionLayout,
                    this,
                    searchOpening,
                    onClose = { update { it.copy(search = false) } },
                )
            }
            AnimatedVisibility(
                current.avatar,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                AvatarTarget(
                    portraitPainter,
                    this@SharedTransitionLayout,
                    this,
                    onClose = { update { it.copy(avatar = false, dialog = false) } },
                ) { chrome ->
                    EditPhoto(
                        dialogOpen = current.dialog,
                        chrome = chrome,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        onOpen = { update { it.copy(dialog = true) } },
                        onClose = { update { it.copy(dialog = false) } },
                    )
                }
            }
            photos.forEachIndexed { i, photo ->
                AnimatedVisibility(
                    current.photo == i + 1,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                ) {
                    MorphDetail(
                        painter = painters[i],
                        title = photo.title,
                        caption = photo.caption,
                        key = morphKey(i + 1),
                        layers = MorphLayers.All,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        onClose = { update { it.copy(photo = 0) } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileHeader(
    name: String,
    count: String,
    painter: Painter,
    avatarOpen: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    onAvatar: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(
                start = LabTheme.spacing.mediumLarge,
                end = LabTheme.spacing.mediumLarge,
                bottom = LabTheme.spacing.mediumLarge,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.mediumLarge),
    ) {
        SourceSlot(!avatarOpen, Modifier.size(AvatarSize)) {
            AvatarSource(painter, sharedTransitionScope, this, onAvatar)
        }
        Column {
            Text(
                name,
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.subtitle,
                modifier = Modifier.testTag(ProfileNameTag),
            )
            Text(count, color = LabTheme.colors.textMuted, style = LabTheme.typography.body)
        }
    }
}

// The pencil FAB and the dialog it grows into, over the zoomed avatar. The FAB's slot carries the
// avatar's chrome, so it fades in as the circle lands and leaves first when it closes; that alpha
// is
// 1 whenever the FAB itself morphs.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BoxScope.EditPhoto(
    dialogOpen: Boolean,
    chrome: Modifier,
    sharedTransitionScope: SharedTransitionScope,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    // The dialog first and the FAB over it, so a FAB that has just landed is never covered by the
    // dialog it came from while that one finishes leaving.
    AnimatedVisibility(
        dialogOpen,
        modifier = Modifier.fillMaxSize(),
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        FabDialogTarget(sharedTransitionScope, this, onClose)
    }
    SourceSlot(
        !dialogOpen,
        Modifier.align(Alignment.BottomEnd)
            // Clear of the system navigation bar, then its own inset from the corner.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = FabEdgeInset, bottom = FabEdgeInset)
            .then(chrome)
            .size(FabSize),
    ) {
        FabSource(sharedTransitionScope, this, onOpen)
    }
}

// A fixed-size slot, so the screen keeps its layout while its source is away in the target; and a
// plain Box scope, so the top-level AnimatedVisibility is the one called, not a Row's or Column's.
@Composable
private fun SourceSlot(
    visible: Boolean,
    modifier: Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    Box(modifier) {
        AnimatedVisibility(
            visible,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            content = content,
        )
    }
}

@Preview
@Composable
private fun ProfileGalleryPreview() {
    LabTheme {
        var scene by remember { mutableStateOf(GalleryScene()) }
        ProfileGallery(
            title = "Profile",
            name = "Alex Morgan",
            portrait = Res.drawable.portrait,
            photos =
                listOf(
                    MorphPhoto(Res.drawable.photo_corfu, "Corfu", "Sunset over the west coast"),
                    MorphPhoto(Res.drawable.photo_paxos, "Paxos", "A harbour town at golden hour"),
                    MorphPhoto(Res.drawable.photo_santorini, "Santorini", "Oia lit up at dusk"),
                    MorphPhoto(Res.drawable.photo_milos, "Milos", "A boat under the white cliffs"),
                    MorphPhoto(Res.drawable.photo_naxos, "Naxos", "The Portara at sunset"),
                    MorphPhoto(Res.drawable.photo_hydra, "Hydra", "Sailing into the harbour"),
                ),
            searchQuery = "xos",
            scene = scene,
            onSceneChange = { scene = it },
        )
    }
}
