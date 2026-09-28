package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphDetail
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphDimens
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphLayers
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhoto
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphKey
import dev.dimvlachos.lab.core.presentation.components.imagemorph.rememberMorphGate
import dev.dimvlachos.lab.core.presentation.components.imagemorph.rememberMorphPainters
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarSource
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarTarget
import dev.dimvlachos.lab.core.presentation.components.profile.FabDialogTarget
import dev.dimvlachos.lab.core.presentation.components.profile.FabSize
import dev.dimvlachos.lab.core.presentation.components.profile.FabSource
import dev.dimvlachos.lab.core.presentation.components.profile.SearchSource
import dev.dimvlachos.lab.core.presentation.components.profile.SearchTarget
import dev.dimvlachos.lab.core.presentation.components.profile.matchesQuery
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.gallery_photo_count
import dev.dimvlachos.lab.resources.ic_history
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_santorini
import dev.dimvlachos.lab.resources.portrait
import dev.dimvlachos.lab.resources.search_no_results
import dev.dimvlachos.lab.resources.search_recent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val ProfileNameTag = "profileName"
internal const val GalleryScrollTag = "galleryScroll"
internal const val GalleryGridTag = "galleryGrid"
internal const val SearchGridTag = "searchGrid"
internal const val SearchPageTag = "searchPage"
internal const val RecentSearchesTag = "recentSearches"
private const val SearchContentFadeInMs = 200
// A fast screen fade on the standard ease, shorter than the bar's morph.
private const val SearchFadeMs = 200
// The search bar's row: 16 dp above and below its 48 dp pill, so the results start 16 dp under it.
private val SearchTopSpace = 80.dp
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
    name: String,
    portrait: DrawableResource,
    photos: List<MorphPhoto>,
    searchQuery: String,
    recentSearches: List<String> = emptyList(),
    scene: GalleryScene,
    onSceneChange: (GalleryScene) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val current = scene.normalized(photos.size)
    val latest = rememberUpdatedState(current)
    val report = rememberUpdatedState(onSceneChange)
    val applyChange: ((GalleryScene) -> GalleryScene) -> Unit = remember {
        { change -> report.value(change(latest.value)) }
    }
    val portraitBitmap = imageResource(portrait)
    val portraitPainter = remember(portraitBitmap) { BitmapPainter(portraitBitmap) }
    val painters = rememberMorphPainters(photos)
    val typed = remember { mutableStateOf("") }
    val noQuery = remember { mutableStateOf("") }
    val searchOpening = remember { { latest.value.search } }
    val collapseRange =
        with(LocalDensity.current) {
            (HeaderExpandedHeight - HeaderCollapsedHeight).toPx()
        }
    val collapse =
        remember(scrollState, collapseRange) {
            { headerCollapse(scrollState.value, collapseRange) }
        }
    SharedTransitionLayout(modifier.fillMaxSize()) {
        // Every tap that opens or closes something goes through the gate, so none cuts into a
        // morph in flight. The change is computed when it runs, from the scene at that moment.
        val gate = rememberMorphGate()
        val update: ((GalleryScene) -> GalleryScene) -> Unit =
            remember(gate) {
                { change -> gate { applyChange(change) } }
            }
        // The system back gesture closes the topmost layer, through the same gate as a tap. On the
        // bare screen it is off, so back is the app's again.
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = current != GalleryScene(),
            onBackCompleted = { update { it.back() } },
        )
        // Home and the search page cross-fade, as an app's Explore and Search screens do: home
        // fades out while the page fades in, and back. The search bar morphs above both, in the
        // shared overlay, so neither fade touches it. Read in layer blocks only.
        val homeAlpha =
            animateFloatAsState(
                if (current.search) 0f else 1f,
                tween(SearchFadeMs, easing = FastOutSlowInEasing),
                label = "homeAlpha",
            )
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = homeAlpha.value }
                    .testTag(GalleryScrollTag)
                    // In the viewport's coordinates (before the scroll), read at draw time.
                    .drawWithContent {
                        clipRect(top = gridClipTop(collapse()).toPx()) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    .verticalScroll(scrollState)
                    // Inside the scroll, so the last row scrolls clear of the navigation bar.
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(bottom = LabTheme.spacing.medium)
            ) {
                Spacer(Modifier.height(HeaderExpandedHeight + GridTopGap))
                GalleryGrid(
                    photos = photos,
                    painters = painters,
                    query = noQuery,
                    openPhoto = if (current.search) 0 else current.photo,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    onOpen = { index -> update { it.copy(photo = index) } },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = LabTheme.spacing.mediumLarge)
                            .testTag(GalleryGridTag),
                )
            }
            // The grid is cut off under the bar only in drawing, so this takes the taps there
            // too: the bar and the band below it never open a photo scrolled out of sight. Its
            // height follows the collapse in layout, like the cut, and the header's own buttons
            // sit above it.
            Box(
                Modifier.fillMaxWidth()
                    .layout { measurable, constraints ->
                        val height = gridClipTop(collapse()).roundToPx()
                        val placeable =
                            measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                        layout(placeable.width, height) { placeable.place(0, 0) }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() }
                    }
            )
            // Over the grid, which scrolls under it. Search opens over both, as its own page.
            CollapsingHeader(
                modifier = Modifier.graphicsLayer { alpha = homeAlpha.value },
                collapse = collapse,
                name = name,
                count = stringResource(Res.string.gallery_photo_count, photos.size),
                avatar = {
                    SourceSlot(!current.avatar, Modifier.fillMaxSize()) {
                        AvatarSource(
                            portraitPainter,
                            this@SharedTransitionLayout,
                            this,
                            { update { it.copy(avatar = true) } },
                        )
                    }
                },
                search = {
                    SourceSlot(!current.search, Modifier.fillMaxSize()) {
                        SearchSource(
                            this@SharedTransitionLayout,
                            this,
                            searchOpening,
                            { update { it.copy(search = true) } },
                        )
                    }
                },
            )
            AnimatedVisibility(
                current.search,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                SearchPage(
                    photos = photos,
                    recentSearches = recentSearches,
                    painters = painters,
                    query = searchQuery,
                    typed = typed,
                    openPhoto = current.photo,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    opening = searchOpening,
                    onOpen = { index -> update { it.copy(photo = index) } },
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
                        // The card it came from: the search page's copy while searching.
                        key = if (current.search) searchMorphKey(i + 1) else morphKey(i + 1),
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

// The search page's cards need keys of their own: the home grid stays composed under the page, and
// two cards sharing a key would be one shared element with a stray copy.
private fun searchMorphKey(index: Int): String = "search_" + morphKey(index)

// Search as its own page over home, which stays exactly as it was underneath. The page fades in
// with the bar's morph, showing the recent searches until the query types in.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchPage(
    photos: List<MorphPhoto>,
    recentSearches: List<String>,
    painters: List<Painter>,
    query: String,
    typed: MutableState<String>,
    openPhoto: Int,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
    onOpen: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val pageFade =
        with(animatedVisibilityScope) {
            Modifier.animateEnterExit(
                enter = fadeIn(tween(SearchFadeMs, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(SearchFadeMs, easing = FastOutSlowInEasing)),
            )
        }
    Box(Modifier.fillMaxSize()) {
        // Opaque at rest, cross-fading with home in 200 ms. It swallows taps; the back arrow
        // closes. The search bar clears the query once the page has gone, so the results stay
        // put while it fades.
        Box(
            Modifier.fillMaxSize()
                .then(pageFade)
                .background(LabTheme.colors.background)
                .clickable(interactionSource = null, indication = null, onClick = {})
                .testTag(SearchPageTag)
        )
        // Below the bar's row, so results scrolling up are cut off under it instead of passing
        // behind the back arrow, and taps on the row never reach them.
        Column(
            Modifier.fillMaxSize()
                .then(pageFade)
                .padding(top = SearchTopSpace)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = LabTheme.spacing.medium)
        ) {
            SearchContent(
                photos = photos,
                painters = painters,
                recentSearches = recentSearches,
                typed = typed,
                openPhoto = openPhoto,
                sharedTransitionScope = sharedTransitionScope,
                onOpen = onOpen,
            )
        }
        SearchTarget(
            query,
            typed,
            sharedTransitionScope,
            animatedVisibilityScope,
            opening,
            onClose = onClose,
        )
    }
}

private enum class SearchState {
    Recent,
    Results,
    NothingFound,
}

// What the page shows under the bar: the recent searches until something is typed, then the
// matching photos, or a line saying nothing matched. It changes state by a short cross-fade; while
// the state holds, the grid stays in place, so each typed letter glides the matches along.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchContent(
    photos: List<MorphPhoto>,
    painters: List<Painter>,
    recentSearches: List<String>,
    typed: MutableState<String>,
    openPhoto: Int,
    sharedTransitionScope: SharedTransitionScope,
    onOpen: (Int) -> Unit,
) {
    val query = typed.value
    val state =
        when {
            query.isBlank() -> SearchState.Recent
            photos.any { matchesQuery(it.title, query) } -> SearchState.Results
            else -> SearchState.NothingFound
        }
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(tween(SearchContentFadeInMs)),
                initialContentExit = fadeOut(tween(MorphDimens.ChromeFadeOutMs)),
                sizeTransform = null,
            )
        },
        label = "searchContent",
    ) { shown ->
        when (shown) {
            SearchState.Recent -> RecentSearches(recentSearches, onPick = { typed.value = it })
            SearchState.Results ->
                GalleryGrid(
                    photos = photos,
                    painters = painters,
                    query = typed,
                    openPhoto = openPhoto,
                    sharedTransitionScope = sharedTransitionScope,
                    onOpen = onOpen,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = LabTheme.spacing.mediumLarge)
                            .testTag(SearchGridTag),
                    morphKeyOf = ::searchMorphKey,
                )
            SearchState.NothingFound ->
                Text(
                    stringResource(Res.string.search_no_results, typed.value),
                    color = LabTheme.colors.textMuted,
                    style = LabTheme.typography.body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(LabTheme.spacing.large),
                )
        }
    }
}

@Composable
private fun RecentSearches(entries: List<String>, onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = LabTheme.spacing.mediumLarge)
            .testTag(RecentSearchesTag)
    ) {
        Text(
            stringResource(Res.string.search_recent),
            color = LabTheme.colors.textMuted,
            style = LabTheme.typography.body,
            modifier = Modifier.padding(bottom = LabTheme.spacing.small),
        )
        for (entry in entries) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(LabTheme.spacing.small))
                    .clickable(role = Role.Button) { onPick(entry) }
                    .padding(vertical = LabTheme.spacing.smallMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.medium),
            ) {
                Icon(
                    painterResource(Res.drawable.ic_history),
                    contentDescription = null,
                    tint = LabTheme.colors.textMuted,
                )
                Text(entry, color = LabTheme.colors.textPrimary, style = LabTheme.typography.body)
            }
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
