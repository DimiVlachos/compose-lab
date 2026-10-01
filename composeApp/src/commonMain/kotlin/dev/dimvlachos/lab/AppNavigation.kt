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
internal class AppNavigation(initialDemo: Demo?, record: Boolean) {
    var demo by mutableStateOf(initialDemo)
        private set

    var group by mutableStateOf<CatalogEntry.Group?>(null)
        private set

    private val recordedDemo = if (record) initialDemo else null

    /** False on the home screen, where back leaves the app, and on the recorded demo. */
    val canGoBack: Boolean
        get() = demo.let { if (it != null) it !== recordedDemo else group != null }

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
