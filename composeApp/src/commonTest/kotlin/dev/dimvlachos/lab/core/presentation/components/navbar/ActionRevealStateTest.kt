package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_add
import dev.dimvlachos.lab.resources.ic_edit
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ActionRevealStateTest {
    private val editAction = NavAction(Res.drawable.ic_edit, "Edit profile")
    private val addAction = NavAction(Res.drawable.ic_add, "New post")

    @Test
    fun interruptingAHideWithAShowSettlesFullyRevealed() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ActionRevealState(editAction)
        var target by mutableStateOf<NavAction?>(editAction)
        setContent { LaunchedEffect(target) { state.animateTo(target) } }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = null }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(60)

        runOnUiThread { target = editAction }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(3_000)

        assertEquals(1f, state.revealValue, 0.01f)
        assertEquals(1f, state.scaleXValue, 0.01f)
        assertEquals(1f, state.scaleYValue, 0.01f)
    }

    @Test
    fun interruptingAShowWithAHideSettlesFullyHidden() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ActionRevealState(null)
        var target by mutableStateOf<NavAction?>(null)
        setContent { LaunchedEffect(target) { state.animateTo(target) } }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = editAction }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(60)

        runOnUiThread { target = null }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(3_000)

        assertEquals(0f, state.revealValue, 0.01f)
    }

    @Test
    fun swappingMidShowStillRestoresFullReveal() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = ActionRevealState(null)
        var target by mutableStateOf<NavAction?>(null)
        setContent { LaunchedEffect(target) { state.animateTo(target) } }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = editAction }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(60)

        runOnUiThread { target = addAction }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(3_000)

        assertEquals(1f, state.revealValue, 0.01f)
        assertEquals(1f, state.scaleXValue, 0.01f)
        assertEquals(1f, state.scaleYValue, 0.01f)
    }
}
