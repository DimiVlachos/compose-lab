package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MagnetTableUiTest {
    private class Table(val state: MagnetState, val compositions: () -> Int)

    // The three test photos and tags of MagnetStateTest, on a 360 × 720 dp table.
    private fun ComposeUiTest.table(): Table {
        var state: MagnetState? = null
        var compositions = 0
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalMagnetCompositionProbe provides { compositions++ }) {
                    val table = rememberMagnetState(TestPhotos, TestTags)
                    state = table
                    MagnetTable(table, Modifier.size(360.dp, 720.dp).testTag("table"))
                }
            }
        }
        waitForIdle()
        return Table(state!!) { compositions }
    }

    private fun stateIs(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun ComposeUiTest.runAction(node: SemanticsNodeInteraction, label: String) {
        val action =
            node.fetchSemanticsNode().config[SemanticsActions.CustomActions].first {
                it.label == label
            }
        runOnUiThread { action.action() }
        waitForIdle()
    }

    @Test
    fun draggingAMagnetOverAMatchingPhotoSticksItAndShowsTheCount() = runComposeUiTest {
        val table = table()
        val from = table.state.magnetPosition("a")!!
        val photo = table.state.photoPosition("strong")!!
        onNodeWithTag("table").performTouchInput {
            val to = photo + Offset(0f, 90.dp.toPx())
            down(from)
            for (i in 1..10) moveTo(lerp(from, to, i / 10f))
            up()
        }
        mainClock.advanceTimeBy(2_000)
        assertEquals(setOf("strong"), table.state.results["a"])
        onNodeWithContentDescription("Alpha filter").assertIsOn().assert(stateIs("1 photo"))
        onNodeWithContentDescription("Strong").assert(stateIs("On the Alpha magnet"))
    }

    @Test
    fun nothingRecomposesWhileThePhotosMove() = runComposeUiTest {
        mainClock.autoAdvance = false
        val table = table()
        // Only the weak photo matches Gamma, under the threshold: it leans towards the magnet
        // but never sticks, so nothing composition reads changes.
        runOnUiThread { table.state.apply("c") }
        repeat(3) { mainClock.advanceTimeByFrame() }
        val before = table.compositions()
        val start = table.state.photoPosition("weak")!!
        repeat(60) { mainClock.advanceTimeByFrame() }
        val moved = (table.state.photoPosition("weak")!! - start).getDistance()
        assertTrue(moved > 5f, "the weak photo moved only $moved px")
        assertEquals(before, table.compositions(), "moving photos must not recompose")
    }

    @Test
    fun theScreenReadersActionsApplyAndRemoveAFilter() = runComposeUiTest {
        val table = table()
        val magnet = onNodeWithContentDescription("Alpha filter")
        magnet.assertIsOff().assert(stateIs("Off"))
        runAction(magnet, "Apply Alpha filter")
        mainClock.advanceTimeBy(2_000)
        magnet.assertIsOn().assert(stateIs("1 photo"))
        assertEquals(setOf("strong"), table.state.results["a"])
        runAction(magnet, "Remove Alpha filter")
        magnet.assertIsOff()
        assertTrue(table.state.results.isEmpty())
    }

    @Test
    fun aTapOnAMagnetFansItsPhotosOutAndATapAnywhereFoldsThem() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        val at = table.state.magnetPosition("a")!!
        onNodeWithTag("table").performTouchInput { click(at) }
        assertEquals("a", table.state.fannedOut)
        mainClock.advanceTimeBy(600)
        onNodeWithTag("table").performTouchInput { click(Offset(20f, 20f)) }
        assertNull(table.state.fannedOut)
        mainClock.advanceTimeBy(600)
        assertNull(table.state.fanShown)
        assertEquals(setOf("strong"), table.state.results["a"], "folding gives nothing up")
    }

    @Test
    fun flickedBackIntoTheStripAMagnetLetsGoOfItsPhotos() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        val at = table.state.magnetPosition("a")!!
        onNodeWithTag("table").performTouchInput {
            swipe(at, Offset(at.x, at.y + 300.dp.toPx()), durationMillis = 120)
        }
        mainClock.advanceTimeBy(3_000)
        assertTrue(table.state.results.isEmpty())
        assertEquals(table.state.slotPosition("a"), table.state.magnetPosition("a"))
    }

    @Test
    fun aPhotoPulledOffByHandLeavesTheResults() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        val magnet = table.state.magnetPosition("a")!!
        val card = table.state.photoPosition("strong")!!
        onNodeWithTag("table").performTouchInput {
            // Taken by its far side, clear of the magnet's own touch target.
            val away = card - magnet
            val grip = card + away / away.getDistance() * 20.dp.toPx()
            swipe(grip, grip + Offset(0f, -150.dp.toPx()), durationMillis = 300)
        }
        mainClock.advanceTimeBy(1_000)
        assertEquals(emptySet(), table.state.results["a"])
    }

    @Test
    fun aTapOnAStuckCardLeavesItOnItsMagnet() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        val magnet = table.state.magnetPosition("a")!!
        val card = table.state.photoPosition("strong")!!
        onNodeWithTag("table").performTouchInput {
            val away = card - magnet
            click(card + away / away.getDistance() * 20.dp.toPx())
        }
        mainClock.advanceTimeBy(1_000)
        assertEquals(setOf("strong"), table.state.results["a"])
    }

    @Test
    fun aTapOnAMagnetInTheStripChangesNothing() = runComposeUiTest {
        val table = table()
        val before = table.compositions()
        val slot = table.state.magnetPosition("b")!!
        onNodeWithTag("table").performTouchInput { click(slot) }
        mainClock.advanceTimeBy(1_000)
        assertTrue(table.state.results.isEmpty())
        assertEquals(
            before,
            table.compositions(),
            "a tap in the strip must not flicker the results",
        )
    }

    @Test
    fun aNewCountIsAnnouncedFromANodeThatStaysStill() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        onNodeWithContentDescription("Alpha filter")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Alpha filter, 1 photo"),
                )
            )
        val before =
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
                .fetchSemanticsNode()
                .boundsInRoot
        runOnUiThread { table.state.remove("a") }
        mainClock.advanceTimeBy(1_000)
        val after =
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
                .fetchSemanticsNode()
                .boundsInRoot
        assertEquals(before, after)
    }

    @Test
    fun aRefusedApplyTellsTheScreenReaderItFailed() = runComposeUiTest {
        val table = table()
        runOnUiThread {
            table.state.apply("a")
            table.state.apply("b")
        }
        waitForIdle()
        val click =
            onNodeWithContentDescription("Gamma filter")
                .fetchSemanticsNode()
                .config[SemanticsActions.OnClick]
        var handled = true
        runOnUiThread { handled = click.action!!.invoke() }
        assertTrue(!handled, "a third magnet is refused, and the click must say so")
    }

    @Test
    fun eachMagnetWearsItsTagsColour() = runComposeUiTest {
        var state: MagnetState? = null
        val tags = listOf(MagnetTag("a", "Alpha", Color.Blue), MagnetTag("b", "Beta"))
        setContent {
            LabTheme {
                val table = rememberMagnetState(TestPhotos, tags)
                state = table
                MagnetTable(table, Modifier.size(360.dp, 720.dp).testTag("table"))
            }
        }
        waitForIdle()
        val pixels = onNodeWithTag("table").captureToImage().toPixelMap()
        // A horseshoe: its left leg in the tag's colour, its right leg a deeper shade of it, steel
        // tips at the foot of each, and nothing but the strip between its legs.
        fun at(tag: String, dx: Float, dy: Float): Color {
            val at = state!!.magnetPosition(tag)!! + Offset(dx, dy) * density.density
            return pixels[at.x.toInt(), at.y.toInt()]
        }
        val leg =
            MagnetDimens.HorseshoeWidth.value / 2f - MagnetDimens.HorseshoeThickness.value / 2f
        assertEquals(Color.Blue, at("a", -leg, 4f))
        val right = at("a", leg, 4f)
        assertTrue(right.blue < Color.Blue.blue && right.red < 0.05f, "the right leg is $right")
        assertTrue(at("a", 0f, 8f) != Color.Blue, "the middle of the U is hollow")
        // A tag without a colour of its own wears the theme's red.
        assertEquals(AppColors().magnetRed, at("b", -leg, 4f))
    }

    private class Ticks : HapticFeedback {
        val types = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            types += hapticFeedbackType
        }
    }

    // A table of one photo that matches Alpha [strength] well, with its haptics heard.
    private fun ComposeUiTest.tableOfOne(strength: Float, ticks: Ticks): MagnetState {
        var state: MagnetState? = null
        val photos = listOf(MagnetPhoto("p", TestPhotos[0].image, "P", mapOf("a" to strength)))
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalHapticFeedback provides ticks) {
                    val table = rememberMagnetState(photos, TestTags)
                    state = table
                    MagnetTable(table, Modifier.size(360.dp, 720.dp))
                }
            }
        }
        waitForIdle()
        return state!!
    }

    @Test
    fun aStrongMatchSnapsWithAClickAndAWeakerOneWithATick() = runComposeUiTest {
        val ticks = Ticks()
        val table = tableOfOne(0.95f, ticks)
        runOnUiThread { table.apply("a") }
        mainClock.advanceTimeBy(2_000)
        assertEquals(listOf(HapticFeedbackType.VirtualKey), ticks.types)
    }

    @Test
    fun aMatchJustOverTheThresholdSnapsWithALightTick() = runComposeUiTest {
        val ticks = Ticks()
        val table = tableOfOne(0.6f, ticks)
        runOnUiThread { table.apply("a") }
        mainClock.advanceTimeBy(2_000)
        assertEquals(listOf(HapticFeedbackType.SegmentFrequentTick), ticks.types)
    }

    @Test
    fun aMagnetLandingBackInItsSlotEndsWithASoftTick() = runComposeUiTest {
        val ticks = Ticks()
        val table = tableOfOne(0f, ticks)
        runOnUiThread { table.apply("a") }
        mainClock.advanceTimeBy(500)
        runOnUiThread { table.remove("a") }
        mainClock.advanceTimeBy(2_000)
        assertEquals(listOf(HapticFeedbackType.GestureEnd), ticks.types)
    }

    @Test
    fun aTapOnAPhotoInTheGridOpensItAndATapClosesIt() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        runOnUiThread { table.state.fanOut("a") }
        mainClock.advanceTimeBy(1_000)
        val card = table.state.photoCentre(0) * density.density
        onNodeWithTag("table").performTouchInput { click(card) }
        assertEquals("strong", table.state.opened)
        mainClock.advanceTimeBy(1_000)
        onNodeWithContentDescription("Strong, A strong match").assertExists()
        onNodeWithTag("table").performTouchInput { click(Offset(20f, 20f)) }
        assertNull(table.state.opened)
        mainClock.advanceTimeBy(1_000)
        assertEquals("a", table.state.fannedOut, "a tap closes the photo, not the grid")
    }

    @Test
    fun atLargeTextTheStripGrowsSoItsLabelsFitUnderTheMagnets() = runComposeUiTest {
        val strips = mutableListOf<Float>()
        for (fontScale in listOf(1f, 2f)) {
            var state: MagnetState? = null
            setContent {
                LabTheme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, fontScale)
                    ) {
                        val table = rememberMagnetState(TestPhotos, TestTags)
                        state = table
                        MagnetTable(table, Modifier.size(360.dp, 720.dp))
                    }
                }
            }
            waitForIdle()
            strips += state!!.stripHeight
        }
        assertEquals(MagnetDimens.StripHeight.value, strips[0])
        assertTrue(strips[1] > strips[0], "at twice the text, the strip is ${strips[1]} dp")
    }
}
