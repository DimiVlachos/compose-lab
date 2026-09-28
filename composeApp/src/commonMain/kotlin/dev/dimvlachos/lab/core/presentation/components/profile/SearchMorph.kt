package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ReportMorphComposition
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ShapedMorphNode
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphBoundsTransform
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphChrome
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphWidthFraction
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import dev.dimvlachos.lab.resources.action_search
import dev.dimvlachos.lab.resources.ic_arrow_back
import dev.dimvlachos.lab.resources.ic_search
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val SearchKey = "profile_search"
internal const val SearchIconKey = "profile_search_icon"
internal const val SearchSourceTag = "searchSource"
internal const val SearchPillTag = "searchPill"
internal const val SearchBackTag = "searchBack"
private val SearchButtonSize = 40.dp
private val SearchPillHeight = 48.dp
private val SearchIconSize = 20.dp
private val PillShape = RoundedCornerShape(50)
private const val SearchStartTimeoutMs = 250L
private const val LandedFraction = 0.999f

// The magnifier is its own shared element, above the container, travelling from the button's
// centre to the pill's leading slot while the container grows around it.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Magnifier(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
) {
    with(sharedTransitionScope) {
        Icon(
            painter = painterResource(Res.drawable.ic_search),
            contentDescription = null,
            tint = LabTheme.colors.textPrimary,
            modifier =
                Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(SearchIconKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = remember(opening) { morphBoundsTransform(opening) },
                        zIndexInOverlay = 2f,
                    )
                    .size(SearchIconSize),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchSource(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReportMorphComposition(MorphEnd.Card)
    ShapedMorphNode(
        sharedTransitionScope,
        animatedVisibilityScope,
        SearchKey,
        PillShape,
        modifier
            .size(SearchButtonSize)
            .testTag(SearchSourceTag)
            .clickable(role = Role.Button, onClick = onOpen),
        color = LabTheme.colors.bar,
    ) {
        Box(Modifier.align(Alignment.Center)) {
            Magnifier(sharedTransitionScope, animatedVisibilityScope, opening)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchTarget(
    query: String,
    typed: MutableState<String>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
    onClose: () -> Unit,
) {
    ReportMorphComposition(MorphEnd.Detail)
    val fraction = remember { mutableFloatStateOf(1f) }
    // Typing starts once the pill has landed: its width reaches its final value. Not when
    // isTransitionActive turns false, which waits for every animation of this end, including the
    // one-second chrome fade. It waits for the morph to start first, because on this end's first
    // frames the fraction still holds its initial 1.
    LaunchedEffect(Unit) {
        withTimeoutOrNull(SearchStartTimeoutMs) {
            snapshotFlow { fraction.floatValue }.first { it < LandedFraction }
        }
        snapshotFlow { fraction.floatValue }.first { it >= LandedFraction }
        val start = withFrameMillis { it }
        // Types only over its own letters: once something else sets the query (a recent search
        // picked while the bar lands, or later), the scripted typing stops.
        var written = ""
        while (typed.value == written && written != query) {
            withFrameMillis { now ->
                if (typed.value == written) {
                    written = searchQueryAt(now - start, query, SearchPerCharMs)
                    typed.value = written
                }
            }
        }
    }
    // Cleared once the bar has gone, not when it opens: an open that clears it composes its first
    // frames with the last query's results, which then flash away.
    DisposableEffect(Unit) { onDispose { typed.value = "" } }
    val chrome = Modifier.morphChrome(animatedVisibilityScope, fraction)
    // The bar sits where the top bar was; the page under it shows what [typed] matches.
    Row(
        Modifier.fillMaxWidth().padding(LabTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            chrome
                .size(SearchButtonSize)
                .clip(CircleShape)
                .testTag(SearchBackTag)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_arrow_back),
                stringResource(Res.string.action_back),
                tint = LabTheme.colors.textPrimary,
            )
        }
        ShapedMorphNode(
            sharedTransitionScope,
            animatedVisibilityScope,
            SearchKey,
            PillShape,
            Modifier.weight(1f).height(SearchPillHeight).testTag(SearchPillTag),
            overlayZIndex = 1f,
            color = LabTheme.colors.bar,
        ) {
            Row(
                // The whole pill, so the fraction is the pill's own (a Row wrapping its
                // content would reach its final width long before the pill does) and the
                // row centres in its height.
                Modifier.fillMaxSize()
                    .morphWidthFraction(fraction)
                    .padding(horizontal = LabTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
            ) {
                Magnifier(sharedTransitionScope, animatedVisibilityScope, opening)
                TypedQuery(typed, chrome)
            }
        }
    }
}

// Separate scopes: typing recomposes only these two, never the morphing container around them.
@Composable
private fun TypedQuery(typed: State<String>, modifier: Modifier) {
    val text = typed.value
    Text(
        text.ifEmpty { stringResource(Res.string.action_search) },
        color = if (text.isEmpty()) LabTheme.colors.textMuted else LabTheme.colors.textPrimary,
        style = LabTheme.typography.body,
        maxLines = 1,
        modifier = modifier,
    )
}
