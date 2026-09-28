package dev.dimvlachos.lab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.demo.Demo

/**
 * Where the app is: home, or a demo. The on-screen Back button and the system back gesture both
 * step back through [back]. A demo opened directly by id goes back home too, except while it is
 * being recorded: the recorder owns that screen.
 */
internal class AppNavigation(initialDemo: Demo?, record: Boolean) {
    var demo by mutableStateOf(initialDemo)
        private set

    private val recordedDemo = if (record) initialDemo else null

    /** False on the home screen, where back leaves the app, and on the recorded demo. */
    val canGoBack: Boolean
        get() = demo.let { it != null && it !== recordedDemo }

    fun openDemo(demo: Demo) {
        this.demo = demo
    }

    fun back() {
        if (canGoBack) demo = null
    }
}
