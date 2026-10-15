@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.typewriterdemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.typewriter.PullToSend
import dev.dimvlachos.lab.core.presentation.components.typewriter.TypewriterDimens
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.typewriterchat_script_1
import dev.dimvlachos.lab.resources.typewriterchat_script_1b
import dev.dimvlachos.lab.resources.typewriterchat_script_2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class TypewriterDemosTest {
    private val demo = TypewriterDemos.all.single()

    @Test
    fun onThePhoneTheTypewriterWaitsToBeTypedOn() {
        assertEquals("typewriter.composer", demo.id)
        assertFalse(demo.autoplay)
    }

    @Test
    fun theClipTypesTwoMessagesPushesTheLeverPullsThemOutAndTakesThemBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                Res.string.typewriterchat_script_1,
                Res.string.typewriterchat_script_1b,
                Res.string.typewriterchat_script_2,
            ),
            controller.sends,
        )
        // The first message's line break is the lever, pushed by the fingertip between its halves.
        val lever = controller.calls.single { it.second.startsWith("pushTypewriterLever") }.first
        val halves =
            controller.calls.filter { it.second.startsWith("sendMessage") }.map { it.first }
        assertTrue(lever > halves[0] && lever < halves[1])
        assertEquals(
            listOf(TypewriterDemos.ShortPull, TypewriterDemos.FullPull, TypewriterDemos.FullPull),
            controller.typewriterSheetPulls,
        )
        val cleared = controller.calls.single { it.second == "clearMessages()" }.first
        val lastPull = controller.calls.last { it.second.startsWith("pullTypewriterSheet") }.first
        assertTrue(cleared > lastPull + 4_000, "the reply must be in before it is cleared")
        assertEquals(0, controller.messagesOut)
    }

    @Test
    fun theShortPullSlidesBackAndTheFullOneSends() {
        // The smallest unit gives the most units for the short pull, the biggest the fewest for
        // the full one.
        val short =
            PullToSend.shown(
                TypewriterDemos.ShortPull / TypewriterDimens.ShortestUnit,
                inked = true,
            )
        val full =
            PullToSend.shown(TypewriterDemos.FullPull / TypewriterDimens.BiggestUnit, inked = true)
        assertTrue(short < TypewriterDimens.SendPull, "the short pull sends at $short")
        assertTrue(full > TypewriterDimens.SendPull, "the full pull slides back at $full")
    }

    @Test
    fun theClipFitsInTwentySixSeconds() {
        assertTrue(demo.script.nominalDuration <= 26.seconds, "${demo.script.nominalDuration}")
    }
}
