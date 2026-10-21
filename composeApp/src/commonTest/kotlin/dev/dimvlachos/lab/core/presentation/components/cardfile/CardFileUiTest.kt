package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.up
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CardFileUiTest {
    private val cards: List<FileCard> =
        listOf(FileCard.Divider('A')) +
            (0 until 3).map { FileCard.Entry("a$it", 'A') } +
            listOf(FileCard.Divider('B')) +
            (0 until 3).map { FileCard.Entry("b$it", 'B') }

    private fun describe(card: FileCard) =
        when (card) {
            is FileCard.Divider -> "Letter ${card.letter}"
            is FileCard.Entry -> "Person ${card.key}"
        }

    private fun paper(card: FileCard.Entry) =
        PaperCard.Contact("Person ${card.key}", "Friend", listOf("(802) 555-0100"), 31)

    private fun ComposeUiTest.cardFile(state: CardFileState, list: List<FileCard> = cards) =
        setContent {
            CardFile(list, ::paper, ::describe, Modifier.fillMaxSize(), state)
        }

    /** The scene as the card file lays it out on this test's stage, for where things are drawn. */
    private fun ComposeUiTest.scene(list: List<FileCard> = cards): CardFileScene {
        val node = onNodeWithTag("cardfile").fetchSemanticsNode()
        val d = density.density
        return CardFileScene(paperCards(list, ::paper)).apply {
            layout(node.size.width / d, node.size.height / d, d)
        }
    }

    private fun ComposeUiTest.px(x: Float, y: Float) = Offset(x, y) * density.density

    /** Half way up the front card's body. */
    private fun ComposeUiTest.frontCard(): Offset {
        val s = scene()
        return px(s.cx, s.proj(R0 + CH * .5f, phiOf(0f)).y)
    }

    @Test
    fun theWheelSaysWhoIsAtTheFrontAndTheArrowsFlip() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onNodeWithTag("cardfile")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Person a0"),
                ),
            )
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) }
        waitForIdle()
        assertEquals(2, state.front)
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionUp) }
        waitForIdle()
        assertEquals(1, state.front)
    }

    @Test
    fun enterPullsTheCardOutAndEscapeFilesIt() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithTag("pulled").assertExists()
        onNodeWithTag("pulled").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithTag("pulled").assertDoesNotExist()
    }

    @Test
    fun tappingTheFrontCardPullsItOut() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        val at = frontCard()
        onRoot().performTouchInput { click(at) }
        waitForIdle()
        assertEquals(1, state.pulled)
        assertEquals(1f, state.pullProgress, 1e-3f)
    }

    @Test
    fun aPushDownFilesThePulledCard() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onRoot().performTouchInput { click(frontCard()) }
        waitForIdle()
        val s = scene()
        val from = px(s.target.cx, s.target.cy + s.target.h * .1f)
        // Slowly, 40% of the way back to the rail: past the 28% that files it.
        val to = from + px(0f, s.h * .42f * .4f)
        onRoot().performTouchInput { swipe(from, to, durationMillis = 1_500) }
        waitForIdle()
        onNodeWithTag("pulled").assertDoesNotExist()
        assertEquals(null, state.pulled)
    }

    @Test
    fun aShortPushLetsTheCardBackOut() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onRoot().performTouchInput { click(frontCard()) }
        waitForIdle()
        val s = scene()
        val from = px(s.target.cx, s.target.cy + s.target.h * .1f)
        val to = from + px(0f, s.h * .42f * .15f)
        onRoot().performTouchInput { swipe(from, to, durationMillis = 1_500) }
        waitForIdle()
        assertEquals(1, state.pulled)
        assertEquals(1f, state.pullProgress, 1e-3f)
    }

    @Test
    fun enterOnADividerSpinsToItsFirstEntry() = runComposeUiTest {
        val state = CardFileState(initial = 4)
        cardFile(state)
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(5, state.front)
        onNodeWithTag("pulled").assertDoesNotExist()
    }

    @Test
    fun aRaisedTabSpinsToItsLetter() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        // The B divider stands three behind a0, its tab the second step along.
        val s = scene()
        val phi = phiOf(3f)
        val top = s.proj(R0 + CH + TH, phi)
        val body = s.proj(R0 + CH, phi)
        val at = px(s.cx + (tabX(1) + .15f - CW / 2) * s.k * top.s, (top.y + body.y) / 2)
        onRoot().performTouchInput { click(at) }
        waitForIdle()
        assertEquals(5, state.front)
        assertEquals(null, state.pulled)
    }

    @Test
    fun spinningRecomposesOnlyAsTheFrontCardChanges() = runComposeUiTest {
        var compositions = 0
        val state = CardFileState(initial = 1)
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalCardFileCompositionProbe provides { compositions++ }) {
                CardFile(cards, ::paper, ::describe, Modifier.fillMaxSize(), state)
            }
        }
        mainClock.advanceTimeBy(100)
        val before = compositions
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) }
        mainClock.advanceTimeBy(1_500)
        assertEquals(2, state.front)
        assertTrue(
            compositions - before <= 2,
            "${compositions - before} recompositions for one card",
        )
    }

    @Test
    fun aKnobTurnsTheWheelSlowly() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        val s = scene()
        val knob = s.p3(CW / 2 + .2f, 0f, 0f)
        val from = px(knob.x, knob.y)
        // A knob takes 2.6 times the drag the cards do to turn one card.
        val to = from + px(0f, s.pitch * 2.6f)
        onRoot().performTouchInput { swipe(from, to, durationMillis = 2_000) }
        waitForIdle()
        assertEquals(2, state.front)
    }

    @Test
    fun aDragOnTheCardsTurnsACardAPitch() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        val s = scene()
        val from = frontCard()
        onRoot().performTouchInput {
            swipe(from, from + px(0f, s.pitch * 3f), durationMillis = 2_000)
        }
        waitForIdle()
        assertEquals(4, state.front)
    }

    private fun SemanticsNode.subtree(): List<SemanticsNode> =
        listOf(this) + children.flatMap { it.subtree() }

    private fun ComposeUiTest.actionLabels(): List<String> =
        onNodeWithTag("cardfile")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
            .map { it.label }

    @Test
    fun theWheelIsTheOnlyFocusableNodeAndCarriesTheDescriptionAndActions() = runComposeUiTest {
        cardFile(CardFileState(initial = 1))
        val root = onRoot().fetchSemanticsNode()
        val nodes = root.subtree()
        val focusable = nodes.filter { SemanticsProperties.Focused in it.config }
        val wheel = onNodeWithTag("cardfile").fetchSemanticsNode()
        assertEquals(listOf(wheel.id), focusable.map { it.id }, "one focus stop for the whole file")
        assertTrue(
            nodes.none { SemanticsActions.OnClick in it.config },
            "nothing is its own button",
        )
        assertEquals(
            listOf("Person a0"),
            wheel.config.getOrNull(SemanticsProperties.ContentDescription),
        )
        assertTrue(SemanticsActions.CustomActions in wheel.config)
    }

    @Test
    fun pullOutIsOfferedOnlyForAContactAndTheEndsDropTheirArrow() = runComposeUiTest {
        val state = CardFileState(initial = 0)
        cardFile(state)
        fun flip(key: Key, times: Int) =
            repeat(times) {
                onNodeWithTag("cardfile").performKeyInput { pressKey(key) }
                waitForIdle()
            }
        onNodeWithTag("cardfile").requestFocus()
        assertEquals(listOf("Next card"), actionLabels(), "a divider at the first card")
        flip(Key.DirectionDown, 1)
        assertEquals(listOf("Previous card", "Next card", "Pull out"), actionLabels(), "a contact")
        flip(Key.DirectionDown, 3)
        assertEquals(4, state.front)
        assertEquals(
            listOf("Previous card", "Next card"),
            actionLabels(),
            "a divider can't be pulled out",
        )
        flip(Key.DirectionDown, 3)
        assertEquals(7, state.front)
        assertEquals(
            listOf("Previous card", "Pull out"),
            actionLabels(),
            "a contact at the last card",
        )
    }

    @Test
    fun anEmptyDeckOffersNoActions() = runComposeUiTest {
        cardFile(CardFileState(), list = emptyList())
        assertEquals(emptyList(), actionLabels())
    }

    @Test
    fun tappingACardBehindTheFrontSpinsToIt() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        // The next card's top peeks over the front card's top edge.
        val s = scene()
        val front = s.proj(R0 + CH, phiOf(0f)).y
        val behind = s.proj(R0 + CH, phiOf(1f)).y
        onRoot().performTouchInput { click(px(s.cx, (front + behind) / 2)) }
        waitForIdle()
        assertEquals(2, state.front)
        assertEquals(null, state.pulled)
    }

    @Test
    fun tappingOutsideThePulledCardFilesIt() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onRoot().performTouchInput { click(frontCard()) }
        waitForIdle()
        onRoot().performTouchInput { click(Offset(10f, 10f)) }
        waitForIdle()
        onNodeWithTag("pulled").assertDoesNotExist()
        assertEquals(null, state.pulled)
    }

    @Test
    fun focusGoesToThePulledCardAndBackToTheWheel() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithTag("pulled").assertIsFocused()
        onNodeWithTag("pulled").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithTag("cardfile").assertIsFocused()
    }

    @Test
    fun escapeOnTheWheelFilesACardStillRisingAndNumPadEnterPulls() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        mainClock.autoAdvance = false
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.NumPadEnter) }
        mainClock.advanceTimeBy(48)
        assertEquals(1, state.pulled, "NumPadEnter pulls the card")
        onNodeWithTag("cardfile").assertIsFocused()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Escape) }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(null, state.pulled)
    }

    @Test
    fun theFirstFrameAlreadyShowsTheStartingCard() = runComposeUiTest {
        mainClock.autoAdvance = false
        cardFile(CardFileState(initial = 1))
        onNodeWithTag("cardfile")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Person a0"),
                ),
            )
    }

    @Test
    fun aDeckThatLosesThePulledEntryFilesItInsteadOfCrashing() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        var list by mutableStateOf(cards)
        setContent { CardFile(list, ::paper, ::describe, Modifier.fillMaxSize(), state) }
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(1, state.pulled)
        list = listOf(FileCard.Divider('A'), FileCard.Divider('B')) + cards.drop(5)
        waitForIdle()
        onNodeWithTag("pulled").assertDoesNotExist()
        assertEquals(null, state.pulled)
    }

    @Test
    fun aTapWhereCardsOverlapGoesToTheNearestCard() = runComposeUiTest {
        // Every card behind covers the front card's middle; the nearest is drawn last and wins.
        val state = CardFileState(initial = 1)
        cardFile(state)
        onRoot().performTouchInput { click(frontCard()) }
        waitForIdle()
        assertEquals(1, state.pulled, "the front card is pulled, not a card behind it spun to")
        assertEquals(1, state.front)
    }

    @Test
    fun aDeckShrinkingUnderTheFrontCardEndsOnItsLastCard() = runComposeUiTest {
        val big = (0 until 30).map { FileCard.Entry("e$it", 'E') }
        val state = CardFileState(initial = 20)
        var list: List<FileCard> by mutableStateOf(big)
        setContent { CardFile(list, ::paper, ::describe, Modifier.fillMaxSize(), state) }
        waitForIdle()
        assertEquals(20, state.front)
        list = big.take(10)
        waitForIdle()
        assertEquals(9, state.front)
        onNodeWithTag("cardfile")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Person e9"),
                ),
            )
    }

    @Test
    fun twoQuickDownPressesMoveTwoCards() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        mainClock.autoAdvance = false
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) }
        mainClock.advanceTimeBy(16)
        assertTrue(state.position < 1.5f, "still nearer the first card at ${state.position}")
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(3, state.front)
    }

    @Test
    fun aPulledCardOffersNoSpinAndNothingElseBecomesAButton() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        val nodes = onRoot().fetchSemanticsNode().subtree()
        assertTrue(nodes.none { SemanticsActions.OnClick in it.config }, "no unlabelled button")
        assertEquals(emptyList(), actionLabels(), "no spinning while a card is out")
    }

    @Test
    fun aTapOnASpinningWheelStopsItWithoutPullingACard() = runComposeUiTest {
        val deck = listOf(FileCard.Divider('C')) + (0 until 19).map { FileCard.Entry("c$it", 'C') }
        val state = CardFileState(initial = 1)
        cardFile(state, list = deck)
        val at = frontCard()
        mainClock.autoAdvance = false
        onNodeWithTag("cardfile").requestFocus()
        // A long spin, still turning when the finger lands.
        repeat(6) { onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) } }
        mainClock.advanceTimeBy(64)
        assertTrue(state.isSpinning)
        onRoot().performTouchInput { down(at) }
        mainClock.advanceTimeBy(32)
        val caught = state.position
        mainClock.advanceTimeBy(200)
        assertEquals(caught, state.position, 1e-3f, "the finger holds the wheel")
        onRoot().performTouchInput {
            moveTo(at)
            up()
        }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(null, state.pulled, "the tap only stopped the wheel")
        assertEquals(state.front.toFloat(), state.position, 1e-3f, "it rests on a whole card")
        assertTrue(state.front < 7, "it stopped short of where it was spun to, on ${state.front}")
    }

    @Test
    fun withReducedMotionFocusStillGoesToThePulledCard() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        state.reducedMotion = true
        cardFile(state)
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(1, state.pulled)
        onNodeWithTag("pulled").assertIsFocused()
    }
}
