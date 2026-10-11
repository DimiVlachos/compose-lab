package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.dimvlachos.lab.core.presentation.components.searchmorph.MorphSearchBar
import dev.dimvlachos.lab.core.presentation.components.searchmorph.MorphSearchButton
import dev.dimvlachos.lab.core.presentation.components.searchmorph.SearchBarBackTag
import dev.dimvlachos.lab.core.presentation.components.searchmorph.SearchBarPillTag
import dev.dimvlachos.lab.core.presentation.components.searchmorph.SearchMorphKeys
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_search
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource

internal const val SearchKey = "profile_search"
internal const val SearchIconKey = "profile_search_icon"
internal const val SearchSourceTag = "searchSource"
internal const val SearchPillTag = SearchBarPillTag
internal const val SearchBackTag = SearchBarBackTag
private val ProfileSearchKeys = SearchMorphKeys(SearchKey, SearchIconKey)
private const val SearchStartTimeoutMs = 250L
private const val LandedFraction = 0.999f
// A beat between the pill landing and the first letter, as a person would take before typing.
private const val SearchTypingPauseMs = 400L

// The profile's search button, in the header: the shared round button, under the gallery's keys.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchSource(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MorphSearchButton(
        ProfileSearchKeys,
        sharedTransitionScope,
        animatedVisibilityScope,
        opening,
        onOpen,
        modifier.testTag(SearchSourceTag),
    )
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
        val start = withFrameMillis { it } + SearchTypingPauseMs
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
    // The bar sits where the top bar was, as clear of the sides as the header; the page under it
    // shows what [typed] matches.
    MorphSearchBar(
        ProfileSearchKeys,
        sharedTransitionScope,
        animatedVisibilityScope,
        opening,
        onClose,
        Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(LabTheme.spacing.medium),
        fraction = fraction,
    ) { chrome ->
        TypedQuery(typed, chrome)
    }
}

// A scope of its own: typing recomposes only this, never the morphing container around it.
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
