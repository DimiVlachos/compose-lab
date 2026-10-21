package dev.dimvlachos.lab.padlockdemo.presentation.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.padlock.PadlockState
import dev.dimvlachos.lab.padlockdemo.PadlockDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class PadlockDemoUiTest {
    @Test
    fun aWeakPasswordReadsWeakAndKeepsContinueOff() = runComposeUiTest {
        val field = TextFieldState()
        setContent { PadlockDemo(DemoState(), field) }
        field.edit { append("maple") }
        waitForIdle()
        onNodeWithTag("level").assertTextEquals("Weak")
        onNodeWithTag("tip").assertTextEquals("Add 7 more characters and a number")
        onNodeWithTag("continue").assertIsNotEnabled()
    }

    @Test
    fun aStrongPasswordLocksTheLockAndEnablesContinue() = runComposeUiTest {
        val field = TextFieldState()
        val lock = PadlockState()
        setContent { PadlockDemo(DemoState(), field, lock) }
        field.edit { append("maple42-River!") }
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        onNodeWithTag("level").assertTextEquals("Strong")
        onNodeWithTag("tip").assertTextEquals("Locked tight. Nice one.")
        onNodeWithTag("continue").assertIsEnabled()
        assertTrue(lock.locked, "lift ${lock.lift}, swing ${lock.swing}")
    }

    @Test
    fun tappingTheLockShowsAndHidesThePassword() = runComposeUiTest {
        val lock = PadlockState()
        setContent { PadlockDemo(DemoState(), lock = lock) }
        onNodeWithTag("padlock").assertIsOff()
        onNodeWithTag("padlock").performClick()
        onNodeWithTag("padlock").assertIsOn()
        assertTrue(lock.peek)
        onNodeWithTag("padlock").performClick()
        assertFalse(lock.peek)
    }

    @Test
    fun theScriptTypesErasesAndClearsThroughTheField() = runComposeUiTest {
        val state = DemoState()
        val field = TextFieldState()
        val lock = PadlockState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            PadlockDemo(state, field, lock)
        }
        waitForIdle()
        scope.launch { state.tapControl(PadlockDemos.type("maple42-River!")) }
        mainClock.advanceTimeBy(3_000)
        waitForIdle()
        assertEquals("maple42-River!", field.text.toString())
        scope.launch { state.tapControl(PadlockDemos.erase(3)) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals("maple42-Riv", field.text.toString())
        scope.launch { state.tapControl(PadlockDemos.Peek) }
        waitForIdle()
        assertTrue(lock.peek)
        scope.launch { state.resetFeed() }
        waitForIdle()
        assertEquals("", field.text.toString())
        assertFalse(lock.peek)
        assertTrue(lock.settled)
    }
}
