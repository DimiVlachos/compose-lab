package dev.dimvlachos.moodboard.android.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.nav.Boards
import dev.dimvlachos.moodboard.android.nav.Gallery
import dev.dimvlachos.moodboard.android.nav.Search

private data class Tab(val route: NavKey, val label: String, val icon: Int)

private val Tabs =
    listOf(
        Tab(Gallery, "Gallery", R.drawable.ic_gallery),
        Tab(Boards, "Boards", R.drawable.ic_boards),
        Tab(Search, "Search", R.drawable.ic_search),
    )

// M3 Expressive's short bar: 64dp with a pill indicator, instead of the baseline 80dp one.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoodboardNavigationBar(current: NavKey, onSelect: (NavKey) -> Unit) {
    ShortNavigationBar {
        Tabs.forEach { tab ->
            ShortNavigationBarItem(
                selected = current == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { MoodboardIcon(tab.icon, null) },
                label = { Text(tab.label) },
            )
        }
    }
}
