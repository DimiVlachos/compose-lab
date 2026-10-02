@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.core.demo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.chat_script_2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class DemoStateTest {
    @Test
    fun aWipeGoesToTheDemosHandler() = runTest {
        val state = DemoState()
        val wipes = mutableListOf<Pair<List<Offset>, Duration>>()
        state.setWipeHandler { path, duration -> wipes += path to duration }

        state.wipe(listOf(Offset(0.1f, 0.2f), Offset(0.9f, 0.2f)), 1.seconds)

        assertEquals(listOf(listOf(Offset(0.1f, 0.2f), Offset(0.9f, 0.2f)) to 1.seconds), wipes)
    }

    @Test
    fun aWipeWithNoHandlerIsIgnored() = runTest {
        DemoState().wipe(listOf(Offset(0.5f, 0.5f)), 1.seconds)
    }

    @Test
    fun theStateKnowsWhetherItIsBeingRecorded() {
        assertEquals(false, DemoState().recording)
        assertEquals(true, DemoState(recording = true).recording)
    }

    @Test
    fun aDripGoesToTheDemosHandler() = runTest {
        val state = DemoState()
        val drips = mutableListOf<Pair<Offset, Float>>()
        state.setDripHandler { at, length -> drips += at to length }

        state.drip(Offset(0.1f, 0.2f), 0.3f)

        assertEquals(listOf(Offset(0.1f, 0.2f) to 0.3f), drips)
    }

    @Test
    fun aDripWithNoHandlerIsIgnored() = runTest {
        DemoState().drip(Offset(0.5f, 0.5f), 0.2f)
    }

    @Test
    fun aMessageAndItsTakingBackGoToTheDemosHandlers() = runTest {
        val state = DemoState()
        val calls = mutableListOf<String>()
        state.setSendHandler { text, typing -> calls += "send ${text.key} $typing" }
        state.setClearHandler { calls += "clear" }

        state.sendMessage(Res.string.chat_script_2, 1.seconds)
        state.clearMessages()

        assertEquals(listOf("send chat_script_2 1s", "clear"), calls)
    }

    @Test
    fun aMessageWithNoHandlerIsIgnored() = runTest {
        DemoState().sendMessage(Res.string.chat_script_2, 1.seconds)
        DemoState().clearMessages()
    }
}
