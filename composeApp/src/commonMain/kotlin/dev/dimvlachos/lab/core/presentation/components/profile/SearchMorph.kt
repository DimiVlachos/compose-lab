package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphDimens
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ReportMorphComposition
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ShapedMorphNode
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphBoundsTransform
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphChromeAlpha
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
    candidates: List<String>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
    onClose: () -> Unit,
) {
    ReportMorphComposition(MorphEnd.Detail)
    val fraction = remember { mutableFloatStateOf(1f) }
    val typed = remember { mutableStateOf("") }
    // Typing starts once the pill has landed. It waits for the morph to start first, because
    // isTransitionActive is still false on this end's first composition.
    LaunchedEffect(Unit) {
        withTimeoutOrNull(SearchStartTimeoutMs) {
            snapshotFlow { sharedTransitionScope.isTransitionActive }.first { it }
        }
        snapshotFlow { sharedTransitionScope.isTransitionActive }.first { !it }
        val start = withFrameMillis { it }
        while (typed.value != query) {
            withFrameMillis { now ->
                typed.value = searchQueryAt(now - start, query, SearchPerCharMs)
            }
        }
    }
    val chromeFade =
        with(animatedVisibilityScope) {
            Modifier.animateEnterExit(
                enter = fadeIn(tween(MorphDimens.ChromeFadeInMs, easing = MorphDimens.MorphEasing)),
                exit = fadeOut(tween(MorphDimens.ChromeFadeOutMs, easing = LinearEasing)),
            )
        }
    val chromeLayer = Modifier.graphicsLayer {
        alpha = morphChromeAlpha(fraction.floatValue)
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
    Column(Modifier.fillMaxWidth().padding(LabTheme.spacing.medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.then(chromeFade)
                    .then(chromeLayer)
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
                    Modifier.morphWidthFraction(fraction)
                        .padding(horizontal = LabTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
                ) {
                    Magnifier(sharedTransitionScope, animatedVisibilityScope, opening)
                    TypedQuery(typed, Modifier.then(chromeFade).then(chromeLayer))
                }
            }
        }
        SearchResults(typed, candidates, Modifier.then(chromeFade).then(chromeLayer))
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

@Composable
private fun SearchResults(typed: State<String>, candidates: List<String>, modifier: Modifier) {
    Column(modifier.padding(top = LabTheme.spacing.medium)) {
        for (result in searchResults(typed.value, candidates)) {
            Text(
                result,
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.body,
                modifier = Modifier.padding(vertical = LabTheme.spacing.smallMedium),
            )
        }
    }
}
