package dev.dimvlachos.lab.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi

/**
 * A screen that can be made again from what it saved, as on a rotation: compose it with
 * [setContent], then [saveAndRestore] it. The test library's own StateRestorationTester can't do
 * this on iOS yet, so this keeps what is saved as it is, rather than as a platform would encode it.
 */
@OptIn(ExperimentalTestApi::class)
internal class Recreation(private val test: ComposeUiTest) {
    private var registry by mutableStateOf(SaveableStateRegistry(null) { true })
    private var shown by mutableStateOf(true)

    fun setContent(content: @Composable () -> Unit) {
        test.setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                if (shown) content()
            }
        }
    }

    /** Saves what the screen saves, throws the screen away, and makes it again from that. */
    fun saveAndRestore() {
        var saved: Map<String, List<Any?>> = emptyMap()
        test.runOnIdle {
            saved = registry.performSave()
            shown = false
        }
        test.runOnIdle {
            registry = SaveableStateRegistry(saved) { true }
            shown = true
        }
        test.waitForIdle()
    }
}
