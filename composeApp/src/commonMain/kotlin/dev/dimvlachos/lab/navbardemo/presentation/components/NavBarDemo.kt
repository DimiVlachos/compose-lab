package dev.dimvlachos.lab.navbardemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.navbar.AnimatedNavBar
import dev.dimvlachos.lab.core.presentation.components.navbar.NavBarLayers
import dev.dimvlachos.lab.core.presentation.components.navbar.NavItem
import dev.dimvlachos.lab.core.presentation.components.navbar.rememberNavBarScrollState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_home
import dev.dimvlachos.lab.resources.ic_home_filled
import dev.dimvlachos.lab.resources.ic_profile
import dev.dimvlachos.lab.resources.ic_profile_filled
import dev.dimvlachos.lab.resources.ic_saved
import dev.dimvlachos.lab.resources.ic_saved_filled
import dev.dimvlachos.lab.resources.ic_search
import dev.dimvlachos.lab.resources.nav_home
import dev.dimvlachos.lab.resources.nav_profile
import dev.dimvlachos.lab.resources.nav_saved
import dev.dimvlachos.lab.resources.nav_search
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NavBarDemo(state: DemoState, layers: NavBarLayers, scrollingContent: Boolean = false) {
    val homeLabel = stringResource(Res.string.nav_home)
    val searchLabel = stringResource(Res.string.nav_search)
    val savedLabel = stringResource(Res.string.nav_saved)
    val profileLabel = stringResource(Res.string.nav_profile)
    val navItems =
        remember(homeLabel, searchLabel, savedLabel, profileLabel) {
            listOf(
                NavItem(homeLabel, Res.drawable.ic_home, Res.drawable.ic_home_filled),
                NavItem(searchLabel, Res.drawable.ic_search, Res.drawable.ic_search),
                NavItem(savedLabel, Res.drawable.ic_saved, Res.drawable.ic_saved_filled),
                NavItem(profileLabel, Res.drawable.ic_profile, Res.drawable.ic_profile_filled),
            )
        }
    val scrollState = rememberNavBarScrollState()
    Box(Modifier.fillMaxSize()) {
        if (scrollingContent) {
            FeedList(state, scrollState)
        } else {
            PageLabel(navItems[state.selectedIndex].label)
        }
        AnimatedNavBar(
            items = navItems,
            selectedIndex = state.selectedIndex,
            onSelect = state::select,
            modifier = Modifier.align(Alignment.BottomCenter).padding(LabTheme.spacing.mediumLarge),
            layers = layers,
            scrollState = scrollState,
        )
    }
}
