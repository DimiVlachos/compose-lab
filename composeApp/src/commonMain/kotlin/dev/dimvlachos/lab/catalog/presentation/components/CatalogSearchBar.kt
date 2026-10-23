package dev.dimvlachos.lab.catalog.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.searchmorph.MorphSearchBar
import dev.dimvlachos.lab.core.presentation.components.searchmorph.MorphSearchButton
import dev.dimvlachos.lab.core.presentation.components.searchmorph.SearchMorphKeys
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBar
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBarTitle
import dev.dimvlachos.lab.core.presentation.components.topbar.SoundButton
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_search
import dev.dimvlachos.lab.resources.catalog_search_clear
import dev.dimvlachos.lab.resources.catalog_title
import dev.dimvlachos.lab.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val CatalogSearchButtonTag = "catalogSearchButton"
internal const val CatalogSearchPillTag = "catalogSearchPill"
internal const val CatalogSearchBackTag = "catalogSearchBack"
private val CatalogSearchKeys = SearchMorphKeys("catalog_search", "catalog_search_icon")
// The bar's icon buttons are 48 dp; the round search button sits centred in a slot that size.
private val ButtonSlotModifier = Modifier.size(48.dp)
// The back arrow's 40 dp circle sits 4 dp in, so it centres where a bar's back button does.
private val BackInset = 4.dp
private val ClearSize = 32.dp
private val ClearIconSize = 20.dp
// The title and the theme button fade on the standard ease, quicker than the morph.
private const val ChromeFadeMs = 200

/**
 * The row of home's bar, laid out in the [LabTopBar]'s title slot with its padding taken away.
 * Closed: the lab's name, a round search button and the theme button. Open: the search button grown
 * into a pill holding the search field, with a back arrow ([onCloseSearch]) before it, the same
 * morph as the profile gallery's search. The field takes the focus once as it opens, when
 * [focusSearch] says so ([onSearchFocused] once it has); a clear button sits at its end while it
 * holds text.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun CatalogSearchBar(
    searchOpen: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    soundOn: Boolean,
    onToggleSound: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = rememberUpdatedState(searchOpen)
    val opening = remember { { open.value } }
    val chromeFadeIn = fadeIn(tween(ChromeFadeMs, easing = FastOutSlowInEasing))
    val chromeFadeOut = fadeOut(tween(ChromeFadeMs, easing = FastOutSlowInEasing))
    SharedTransitionLayout(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            // The closed row keeps its slots while the search is open, so the button's slot is
            // where it was when the pill shrinks back into it.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Slot(
                    !searchOpen,
                    Modifier.weight(1f),
                    chromeFadeIn,
                    chromeFadeOut,
                    Alignment.CenterStart,
                ) {
                    LabTopBarTitle(
                        stringResource(Res.string.catalog_title),
                        Modifier.padding(horizontal = LabTheme.spacing.small),
                    )
                }
                Slot(!searchOpen, ButtonSlotModifier, EnterTransition.None, ExitTransition.None) {
                    MorphSearchButton(
                        CatalogSearchKeys,
                        this@SharedTransitionLayout,
                        this,
                        opening,
                        onOpenSearch,
                        contentDescription = stringResource(Res.string.action_search),
                        modifier = Modifier.testTag(CatalogSearchButtonTag),
                    )
                }
                Slot(!searchOpen, ButtonSlotModifier, chromeFadeIn, chromeFadeOut) {
                    SoundButton(on = soundOn, onToggle = onToggleSound)
                }
                Slot(!searchOpen, ButtonSlotModifier, chromeFadeIn, chromeFadeOut) {
                    ThemeModeButton(mode = themeMode, onCycle = onCycleTheme)
                }
            }
            AnimatedVisibility(
                searchOpen,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                SearchPill(
                    text = text,
                    onTextChange = onTextChange,
                    focusSearch = focusSearch,
                    onSearchFocused = onSearchFocused,
                    opening = opening,
                    onClose = onCloseSearch,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchPill(
    text: String,
    onTextChange: (String) -> Unit,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
    opening: () -> Boolean,
    onClose: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    MorphSearchBar(
        CatalogSearchKeys,
        sharedTransitionScope,
        animatedVisibilityScope,
        opening,
        onClose,
        Modifier.fillMaxWidth().padding(start = BackInset, end = BackInset),
        backTag = CatalogSearchBackTag,
        pillTag = CatalogSearchPillTag,
        trailing = { chrome ->
            if (text.isNotEmpty()) {
                Box(
                    chrome
                        .size(ClearSize)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = { onTextChange("") }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.catalog_search_clear),
                        tint = LabTheme.colors.textMuted,
                        modifier = Modifier.size(ClearIconSize),
                    )
                }
            }
        },
    ) { chrome ->
        CatalogSearchField(
            value = text,
            onValueChange = onTextChange,
            autoFocus = focusSearch,
            onAutoFocused = onSearchFocused,
            modifier = chrome.weight(1f),
        )
    }
}

// A slot that keeps its place in the row while what it holds comes and goes; a plain Box scope,
// so the top-level AnimatedVisibility is the one called, not the Row's.
@Composable
private fun Slot(
    visible: Boolean,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    alignment: Alignment = Alignment.Center,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    Box(modifier, contentAlignment = alignment) {
        AnimatedVisibility(visible, enter = enter, exit = exit, content = content)
    }
}

@Composable
private fun CatalogSearchBarPreview(darkTheme: Boolean, open: Boolean) {
    LabTheme(darkTheme = darkTheme) {
        var text by remember { mutableStateOf("bath") }
        LabTopBar(titlePadding = 0.dp) {
            CatalogSearchBar(
                searchOpen = open,
                text = text,
                onTextChange = { text = it },
                focusSearch = false,
                onSearchFocused = {},
                themeMode = ThemeMode.System,
                onCycleTheme = {},
                soundOn = false,
                onToggleSound = {},
                onOpenSearch = {},
                onCloseSearch = {},
            )
        }
    }
}

@Preview
@Composable
private fun CatalogSearchBarClosedPreview() =
    CatalogSearchBarPreview(darkTheme = true, open = false)

@Preview
@Composable
private fun CatalogSearchBarOpenDarkPreview() =
    CatalogSearchBarPreview(darkTheme = true, open = true)

@Preview
@Composable
private fun CatalogSearchBarOpenLightPreview() =
    CatalogSearchBarPreview(darkTheme = false, open = true)
