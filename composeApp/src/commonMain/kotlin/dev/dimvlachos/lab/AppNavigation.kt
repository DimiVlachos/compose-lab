package dev.dimvlachos.lab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogSection
import dev.dimvlachos.lab.core.demo.Demo

/**
 * Where the app is: home, a section, or a demo inside one. The on-screen Back buttons and the
 * system back gesture both step back through [back]. A demo opened directly by id still has its
 * section to go back to, except while it is being recorded: the recorder owns that screen.
 */
internal class AppNavigation(initialDemo: Demo?, record: Boolean) {
    var section by mutableStateOf(initialDemo?.let(Catalog::sectionOf))
        private set

    var demo by mutableStateOf(initialDemo)
        private set

    private val recordedDemo = if (record) initialDemo else null

    /** False on the home screen, where back leaves the app, and on the recorded demo. */
    val canGoBack: Boolean
        get() = demo?.let { it !== recordedDemo } ?: (section != null)

    fun openSection(section: CatalogSection) {
        this.section = section
    }

    fun openDemo(demo: Demo) {
        this.demo = demo
    }

    fun back() {
        if (!canGoBack) return
        if (demo != null) demo = null else section = null
    }
}
