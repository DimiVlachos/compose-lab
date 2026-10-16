@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.cardfiledemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class CardFileDemosTest {
    private val demo = CardFileDemos.all.single()

    @Test
    fun itIsATallDemoThatPlaysByItself() {
        assertEquals("cardfile.contacts", demo.id)
        assertTrue(demo.tall)
        assertTrue(demo.autoplay)
    }

    @Test
    fun theClipFlipsFlicksToMayaPullsHerOutAndFilesHerBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        val names = controller.calls.map { it.second }
        assertEquals(
            listOf(
                "spinCards(1)",
                "spinCards(1)",
                "spinCards(1)",
                "spinCards(${CardFileDemos.FlickToMaya})",
                "tapControl(pull)",
                "tapControl(file)",
                "resetFeed()",
            ),
            names,
        )
    }

    @Test
    fun theFlickLandsOnMayaBrooks() {
        val deck = ContactDeck(DemoContacts)
        assertEquals(
            deck.indexOf("brooks-maya"),
            CardFileDemos.StartIndex + 3 + CardFileDemos.FlickToMaya,
        )
        assertTrue(CardFileDemos.FlickToMaya > 0)
    }

    @Test
    fun theClipFitsInFourteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 14.seconds, "${demo.script.nominalDuration}")
    }
}
