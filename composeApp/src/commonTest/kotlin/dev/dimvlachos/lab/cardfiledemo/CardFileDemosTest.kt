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
    fun itIsATallDemo() {
        assertEquals("cardfile.contacts", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipTurnsThreeCardsFlicksToMayaPullsHerOutAndFilesHerBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        val names = controller.calls.map { it.second }
        assertEquals(
            listOf(
                "spinCards(3)",
                "spinCards(${CardFileDemos.FlickToMaya})",
                "tapControl(pull)",
                "tapControl(file)",
                "resetFeed()",
            ),
            names,
        )
    }

    @Test
    fun itStartsOnBenHollisAndTheFlickLandsOnMayaBrooks() {
        val deck = ContactDeck(DemoContacts)
        assertEquals(deck.indexOf("ben-hollis"), CardFileDemos.StartIndex)
        assertEquals(
            deck.indexOf("maya-brooks"),
            CardFileDemos.StartIndex + 3 + CardFileDemos.FlickToMaya,
        )
        assertTrue(CardFileDemos.FlickToMaya > 0)
    }

    @Test
    fun theClipFitsInFourteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 14.seconds, "${demo.script.nominalDuration}")
    }
}
