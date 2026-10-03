package dev.dimvlachos.lab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.catalog.CatalogEntry
import dev.dimvlachos.lab.core.demo.Demo

/**
 * Where the app is: home, a folder of versions, or a demo. The on-screen Back button and the system
 * back gesture both step back through [back]: a demo to its folder, if it came from one, then home.
 * A demo opened directly by id goes back home too, except while it is being recorded: the recorder
 * owns that screen.
 */
internal sealed interface AppScreen {
    /** How many screens deep it is: a deeper one pushes in over the one it opens from. */
    val depth: Int

    data object Home : AppScreen {
        override val depth = 0
    }

    data class Folder(val group: CatalogEntry.Group) : AppScreen {
        override val depth = 1
    }

    /** A demo, opened from [group]'s folder, or from home when there is none. */
    data class Open(val demo: Demo, val group: CatalogEntry.Group?) : AppScreen {
        override val depth = if (group != null) 2 else 1
    }

    /** Whether opening this from [screen] pushes it in over it; if not, it is back to it. */
    fun pushesOver(screen: AppScreen): Boolean = depth > screen.depth
}

internal class AppNavigation(initialDemo: Demo?, record: Boolean) {
    var demo by mutableStateOf(initialDemo)
        private set

    var group by mutableStateOf<CatalogEntry.Group?>(null)
        private set

    private val recordedDemo = if (record) initialDemo else null

    /** False on the home screen, where back leaves the app, and on the recorded demo. */
    val canGoBack: Boolean
        get() = demo.let { if (it != null) it !== recordedDemo else group != null }

    /** The screen the app is on. */
    val screen: AppScreen
        get() =
            demo?.let { AppScreen.Open(it, group) }
                ?: group?.let(AppScreen::Folder)
                ?: AppScreen.Home

    /** The screen back leads to, if it leads anywhere: what a back swipe uncovers as it goes. */
    val backScreen: AppScreen?
        get() =
            when {
                !canGoBack -> null
                demo != null -> group?.let(AppScreen::Folder) ?: AppScreen.Home
                else -> AppScreen.Home
            }

    fun openGroup(group: CatalogEntry.Group) {
        this.group = group
    }

    fun openDemo(demo: Demo) {
        this.demo = demo
    }

    fun back() {
        when {
            !canGoBack -> Unit
            demo != null -> demo = null
            else -> group = null
        }
    }
}
