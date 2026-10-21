package dev.dimvlachos.lab.paperbagdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.paperbag.PaperBagState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.paperbagdemo.GroceryCart
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PaperBagDemoUiTest {
    private val cart = GroceryCart()
    private val bag = PaperBagState(Random(3))

    private fun ComposeUiTest.show(reducedMotion: Boolean = false) {
        setContent {
            LabTheme {
                Box(Modifier.size(360.dp, 744.dp)) {
                    PaperBagDemo(DemoState(), cart, bag, reducedMotion)
                }
            }
        }
    }

    private fun ComposeUiTest.add(id: String) = onNodeWithTag("add-$id").performClick()

    private fun ComposeUiTest.bagSays(text: String) =
        onNodeWithTag("bag").assertContentDescriptionEquals(text)

    private fun ComposeUiTest.live(): String? =
        onNodeWithTag("live")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.firstOrNull()

    @Test
    fun anAddFliesToTheBagAndCountsOnItsRowAtOnce() = runComposeUiTest {
        show()
        bagSays("Bag, empty")
        mainClock.autoAdvance = false
        add("bread")
        mainClock.advanceTimeBy(200)
        assertEquals(0, cart.count, "still in the air")
        onNodeWithText(" · 1 in bag").assertExists()
        mainClock.advanceTimeBy(700)
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(1, cart.count)
        bagSays("Bag, 1 item")
        assertEquals("Sourdough baguette added. 1 in bag.", live())
    }

    @Test
    fun theInBagCountShowsBesideALongUnitLine() = runComposeUiTest {
        show(reducedMotion = true)
        add("bread")
        add("milk")
        add("milk")
        waitForIdle()
        // The baguette's and the milk's unit lines are the long ones; their counts are their own.
        onNodeWithText(" · 1 in bag").assertExists()
        onNodeWithText(" · 2 in bag").assertExists()
        bagSays("Bag, 3 items")
    }

    @Test
    fun theOpenBagListsWhatIsInItAndMinusLiftsOneOut() = runComposeUiTest {
        show()
        add("bread")
        add("celery")
        waitForIdle()
        onNodeWithTag("bag").performClick()
        waitForIdle()
        assertTrue(bag.isOpen)
        onNodeWithText("Your bag · 2 items").assertExists()
        val celery = cart.entries.single { it.product.id == "celery" }
        onNodeWithTag("rm-${celery.key}").performClick()
        waitForIdle()
        assertEquals(listOf("bread"), cart.entries.map { it.product.id })
        bagSays("Bag, 1 item")
        assertEquals("Celery hearts removed. 1 in bag.", live())
        onNodeWithTag("done").performClick()
        waitForIdle()
        assertTrue(!bag.isOpen && bag.open == 0f)
    }

    @Test
    fun nothingIsAddedWhileTheBagIsOpen() = runComposeUiTest {
        show(reducedMotion = true)
        add("bread")
        onNodeWithTag("bag").performClick()
        waitForIdle()
        // Hidden from a screen reader under the sheet, and the sheet takes the tap.
        onNodeWithTag("add-eggs", useUnmergedTree = true).performClick()
        waitForIdle()
        assertEquals(1, cart.count)
        assertEquals(0, cart.incoming.size)
    }

    @Test
    fun aFullBagRefusesTheTenthThing() = runComposeUiTest {
        show(reducedMotion = true)
        repeat(9) { add("apples") }
        waitForIdle()
        add("eggs")
        waitForIdle()
        assertEquals(9, cart.count)
        assertEquals("The bag is full", live())
        onAllNodesWithText(" · 9 in bag").fetchSemanticsNodes().single()
    }

    @Test
    fun reducedMotionDropsThingsStraightIn() = runComposeUiTest {
        show(reducedMotion = true)
        mainClock.autoAdvance = false
        add("flowers")
        mainClock.advanceTimeBy(16)
        assertEquals(1, cart.count)
        mainClock.autoAdvance = true
    }
}
