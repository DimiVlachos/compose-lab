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
import dev.dimvlachos.lab.core.presentation.components.navbar.NavAction
import dev.dimvlachos.lab.core.presentation.components.navbar.NavBarLayers
import dev.dimvlachos.lab.core.presentation.components.navbar.NavItem
import dev.dimvlachos.lab.core.presentation.components.navbar.rememberNavBarScrollState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_edit_profile
import dev.dimvlachos.lab.resources.action_new_post
import dev.dimvlachos.lab.resources.ic_add
import dev.dimvlachos.lab.resources.ic_edit
import dev.dimvlachos.lab.resources.ic_home
import dev.dimvlachos.lab.resources.ic_home_filled
import dev.dimvlachos.lab.resources.ic_profile
import dev.dimvlachos.lab.resources.ic_profile_filled
import dev.dimvlachos.lab.resources.ic_saved
import dev.dimvlachos.lab.resources.ic_saved_filled
import dev.dimvlachos.lab.resources.nav_home
import dev.dimvlachos.lab.resources.nav_profile
import dev.dimvlachos.lab.resources.nav_saved
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NavBarDemo(state: DemoState) {
    val homeLabel = stringResource(Res.string.nav_home)
    val savedLabel = stringResource(Res.string.nav_saved)
    val profileLabel = stringResource(Res.string.nav_profile)
    val newPostLabel = stringResource(Res.string.action_new_post)
    val editProfileLabel = stringResource(Res.string.action_edit_profile)
    val navItems =
        remember(homeLabel, savedLabel, profileLabel, newPostLabel, editProfileLabel) {
            listOf(
                NavItem(
                    homeLabel,
                    Res.drawable.ic_home,
                    Res.drawable.ic_home_filled,
                    NavAction(Res.drawable.ic_add, newPostLabel),
                ),
                NavItem(savedLabel, Res.drawable.ic_saved, Res.drawable.ic_saved_filled),
                NavItem(
                    profileLabel,
                    Res.drawable.ic_profile,
                    Res.drawable.ic_profile_filled,
                    NavAction(Res.drawable.ic_edit, editProfileLabel),
                ),
            )
        }
    val scrollState = rememberNavBarScrollState()
    Box(Modifier.fillMaxSize()) {
        FeedList(state, scrollState)
        AnimatedNavBar(
            items = navItems,
            selectedIndex = state.selectedIndex,
            onSelect = state::select,
            onActionClick = {},
            modifier = Modifier.align(Alignment.BottomCenter).padding(LabTheme.spacing.mediumLarge),
            layers = NavBarLayers.All,
            scrollState = scrollState,
        )
    }
}
