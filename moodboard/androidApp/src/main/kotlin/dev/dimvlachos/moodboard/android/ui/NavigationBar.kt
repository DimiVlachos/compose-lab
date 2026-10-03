package dev.dimvlachos.moodboard.android.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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

@Composable
fun MoodboardNavigationBar(current: NavKey, onSelect: (NavKey) -> Unit) {
    NavigationBar {
        Tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { MoodboardIcon(tab.icon, null) },
                label = { Text(tab.label) },
            )
        }
    }
}
