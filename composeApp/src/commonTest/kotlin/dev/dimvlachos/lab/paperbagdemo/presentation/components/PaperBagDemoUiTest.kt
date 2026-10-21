package dev.dimvlachos.lab.paperbagdemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoOverlay
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.LocalDemoOverlay
import dev.dimvlachos.lab.core.presentation.components.paperbag.PaperBagState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.paperbagdemo.GroceryCart
import dev.dimvlachos.lab.paperbagdemo.PaperBagDemos
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
    fun aFlightArcsOverTheScreensBarNotBehindIt() = runComposeUiTest {
        // As the demo screen lays it out: a bar, the stage clipped under it, and the overlay over
        // both.
        val overlay = DemoOverlay()
        setContent {
            LabTheme {
                Box(Modifier.size(360.dp, 804.dp).testTag("screen")) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(60.dp).background(Color.Red))
                        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                            CompositionLocalProvider(LocalDemoOverlay provides overlay) {
                                PaperBagDemo(DemoState(), cart, bag, reducedMotion = false)
                            }
                        }
                    }
                    overlay.Layer()
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        add("bread")
        // Halfway, the baguette is at the top of its arc, above the stage.
        mainClock.advanceTimeBy(280)
        val pixels = onNodeWithTag("screen").captureToImage().toPixelMap()
        var covered = 0
        with(density) {
            for (x in 0 until pixels.width step 2) for (y in 0 until 60.dp.toPx().toInt() step 2) {
                val c = pixels[x, y]
                if (!(c.red > .9f && c.green < .1f && c.blue < .1f)) covered++
            }
        }
        mainClock.autoAdvance = true
        assertTrue(covered > 20, "the baguette is drawn over the bar ($covered px)")
    }

    @Test
    fun theBagStandsInItsButtonAfterTheScreenSlidesIn() = runComposeUiTest {
        // As the app's push transition does: the screen starts a whole width off the edge and
        // slides in on its own layer, which moves what was drawn without drawing it again.
        var shift by mutableStateOf(360.dp)
        setContent {
            LabTheme {
                Box(Modifier.size(360.dp, 744.dp).clipToBounds().testTag("frame")) {
                    Box(Modifier.graphicsLayer { translationX = shift.toPx() }.fillMaxSize()) {
                        PaperBagDemo(DemoState(), cart, bag, reducedMotion = false)
                    }
                }
            }
        }
        waitForIdle()
        // Part way in, the screen's left side shows while the bag's button is still off the edge.
        shift = 200.dp
        waitForIdle()
        shift = 0.dp
        waitForIdle()
        val button = onNodeWithTag("bag").getBoundsInRoot()
        val pixels = onNodeWithTag("frame").captureToImage().toPixelMap()
        fun kraftAround(cx: Float, cy: Float): Boolean {
            for (dx in -8..8) for (dy in -8..8) {
                val c = pixels[(cx + dx).toInt(), (cy + dy).toInt()]
                if (c.red - c.blue > .2f && c.red > .4f) return true
            }
            return false
        }
        with(density) {
            val cx = button.left.toPx() + (button.right - button.left).toPx() / 2
            // The bag's body is a little below the button's centre.
            val cy = button.top.toPx() + (button.bottom - button.top).toPx() / 2 + 2.dp.toPx()
            assertTrue(kraftAround(cx, cy), "the bag is drawn in its button")
            assertTrue(!kraftAround(30.dp.toPx(), 30.dp.toPx()), "nothing is left in the corner")
        }
    }

    @Test
    fun aFingerThatMovesALittleStillTapsInTheOpenBag() = runComposeUiTest {
        show()
        add("bread")
        add("celery")
        waitForIdle()
        onNodeWithTag("bag").performClick()
        waitForIdle()
        // A real finger always moves a pixel or two between down and up; a click() never does.
        fun SemanticsNodeInteraction.fingerTap() = performTouchInput {
            down(center)
            moveBy(Offset(3f, 2f))
            moveBy(Offset(1f, 1f))
            up()
        }
        val celery = cart.entries.single { it.product.id == "celery" }
        onNodeWithTag("rm-${celery.key}").fingerTap()
        waitForIdle()
        assertEquals(listOf("bread"), cart.entries.map { it.product.id })
        onNodeWithTag("done").fingerTap()
        waitForIdle()
        assertTrue(!bag.isOpen, "Done closes the bag")
    }

    @Test
    fun checkingOutFoldsTheBagPlacesTheOrderAndSetsItBackEmpty() = runComposeUiTest {
        show()
        add("bread")
        add("flowers")
        waitForIdle()
        onNodeWithTag("bag").performClick()
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithTag("check-out").performTouchInput {
            down(center)
            moveBy(Offset(2f, 1f))
            up()
        }
        mainClock.advanceTimeBy(900)
        assertTrue(bag.folded, "folded shut")
        onNodeWithText("Order placed").assertExists()
        assertEquals("Order placed. Arriving today, 4 to 5 PM.", live())
        assertEquals(2, cart.count, "still in the bag while the order shows")
        mainClock.advanceTimeBy(1600)
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(0, cart.count)
        assertTrue(!bag.isOpen && !bag.checkingOut)
        bagSays("Bag, empty")
    }

    @Test
    fun theScriptsCheckOutReturnsOnlyOnceTheBagIsBackEmptyInTheCorner() = runComposeUiTest {
        val state = DemoState()
        var go by mutableStateOf(false)
        var returnedOpen = Float.NaN
        var returnedCount = -1
        setContent {
            LabTheme {
                Box(Modifier.size(360.dp, 744.dp)) {
                    PaperBagDemo(state, cart, bag, reducedMotion = false)
                }
            }
            LaunchedEffect(go) {
                if (!go) return@LaunchedEffect
                state.tapControl(PaperBagDemos.CheckOut)
                returnedOpen = bag.open
                returnedCount = cart.count
            }
        }
        add("bread")
        add("flowers")
        waitForIdle()
        onNodeWithTag("bag").performClick()
        waitForIdle()
        go = true
        waitUntil(timeoutMillis = 10_000) { returnedCount >= 0 }
        // A recording plays the script twice in a row, so its end must be its start.
        assertEquals(0, returnedCount, "emptied before the script goes on")
        assertTrue(returnedOpen < .01f, "closed in the corner, not $returnedOpen open")
        assertTrue(!bag.isOpen && !bag.checkingOut)
    }

    @Test
    fun closingMidCheckoutStillSetsTheBagBackEmpty() = runComposeUiTest {
        show()
        add("eggs")
        waitForIdle()
        onNodeWithTag("bag").performClick()
        waitForIdle()
        onNodeWithTag("check-out").performClick()
        onNodeWithTag("done").performClick()
        waitForIdle()
        assertEquals(0, cart.count)
        assertTrue(!bag.isOpen && !bag.checkingOut)
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
