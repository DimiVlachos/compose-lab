@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.planedemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.chat_script_1
import dev.dimvlachos.lab.resources.chat_script_2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PlaneDemosTest {
    @Test
    fun onThePhoneTheChatWaitsToBeTypedIn() {
        assertFalse(PlaneDemos.all.single().autoplay)
    }

    @Test
    fun theClipSendsBothLinesAndTakesThemBackBeforeItEnds() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        val script = PlaneDemos.all.single().script
        script.play(controller)
        assertEquals(listOf(Res.string.chat_script_1, Res.string.chat_script_2), controller.sends)
        val cleared = controller.calls.single { it.second == "clearMessages()" }.first
        val lastSend = controller.calls.last { it.second.startsWith("sendMessage") }.first
        assertTrue(cleared > lastSend)
        assertTrue(cleared.milliseconds + script.holdEnd <= script.nominalDuration)
    }
}
