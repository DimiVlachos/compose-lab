package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.up
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
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

    @Test
    fun theWheelSaysWhoIsAtTheFrontAndTheArrowsFlip() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        setContent {
            CardFile(
                cards,
                { Text(it.key) },
                { Text("out ${it.key}") },
                ::describe,
                Modifier.fillMaxSize(),
                state,
            )
        }
        onNodeWithTag("cardfile")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Person a0"),
                )
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
        setContent {
            CardFile(
                cards,
                { Text(it.key) },
                { Text("out ${it.key}") },
                ::describe,
                Modifier.fillMaxSize(),
                state,
            )
        }
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithTag("pulled").assertExists()
        onNodeWithTag("pulled").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithTag("pulled").assertDoesNotExist()
    }

    @Test
    fun aPushDownFilesThePulledCard() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        setContent {
            CardFile(
                cards,
                { Text(it.key) },
                { Text("out ${it.key}") },
                ::describe,
                Modifier.fillMaxSize(),
                state,
            )
        }
        onNodeWithTag("card-a0").performClick()
        waitForIdle()
        onNodeWithTag("pulled").performTouchInput { swipeDown() }
        waitForIdle()
        onNodeWithTag("pulled").assertDoesNotExist()
    }

    @Test
    fun enterOnADividerSpinsToItsFirstEntry() = runComposeUiTest {
        val state = CardFileState(initial = 4)
        setContent {
            CardFile(
                cards,
                { Text(it.key) },
                { Text("out ${it.key}") },
                ::describe,
                Modifier.fillMaxSize(),
                state,
            )
        }
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(5, state.front)
        onNodeWithTag("pulled").assertDoesNotExist()
    }

    @Test
    fun aRaisedTabSpinsToItsLetter() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        setContent {
            CardFile(
                cards,
                { Text(it.key) },
                { Text("out ${it.key}") },
                ::describe,
                Modifier.fillMaxSize(),
                state,
            )
        }
        onNodeWithTag("tab-B").performClick()
        waitForIdle()
        assertEquals(5, state.front)
    }

    @Test
    fun spinningRecomposesOnlyAsTheWindowMoves() = runComposeUiTest {
        var compositions = 0
        val state = CardFileState(initial = 1)
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalCardFileCompositionProbe provides { compositions++ }) {
                CardFile(
                    cards,
                    { Text(it.key) },
                    { Text("out ${it.key}") },
                    ::describe,
                    Modifier.fillMaxSize(),
                    state,
                )
            }
        }
        mainClock.advanceTimeBy(100)
        val before = compositions
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) }
        mainClock.advanceTimeBy(1_000)
        assertTrue(
            compositions - before <= 2,
            "${compositions - before} recompositions for one card",
        )
    }

    @Test
    fun aKnobTurnsTheWheelSlowly() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        setContent {
            CardFile(
                cards,
                { Text(it.key) },
                { Text("out ${it.key}") },
                ::describe,
                Modifier.fillMaxSize(),
                state,
            )
        }
        onNodeWithTag("knob-right").performTouchInput {
            swipeDown(endY = centerY + 4 * 56.dp.toPx())
        }
        waitForIdle()
        assertEquals(2, state.front, "four cards of drag on a knob turns one card")
    }

    private fun androidx.compose.ui.test.ComposeUiTest.cardFile(
        state: CardFileState,
        list: List<FileCard> = cards,
        face: @Composable (FileCard.Entry) -> Unit = { Text(it.key) },
    ) = setContent {
        CardFile(list, face, { Text("out ${it.key}") }, ::describe, Modifier.fillMaxSize(), state)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.cardFileOf(
        state: CardFileState,
        list: () -> List<FileCard>,
    ) = setContent {
        CardFile(
            list(),
            { Text(it.key) },
            { Text("out ${it.key}") },
            ::describe,
            Modifier.fillMaxSize(),
            state,
        )
    }

    private fun SemanticsNode.subtree(): List<SemanticsNode> =
        listOf(this) + children.flatMap { it.subtree() }

    private fun androidx.compose.ui.test.ComposeUiTest.actionLabels(): List<String> =
        onNodeWithTag("cardfile")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
            .map { it.label }

    @Test
    fun theWheelIsTheOnlyFocusableNodeAndCarriesTheDescriptionAndActions() = runComposeUiTest {
        cardFile(CardFileState(initial = 1))
        // The tree accessibility services see: a cleared subtree's own nodes are left out of it.
        val root = onNodeWithTag("cardfile").fetchSemanticsNode()
        val nodes = root.subtree()
        val focusable = nodes.filter { SemanticsProperties.Focused in it.config }
        assertEquals(listOf(root.id), focusable.map { it.id }, "one focus stop for the whole file")
        assertTrue(
            nodes.none { SemanticsActions.OnClick in it.config },
            "no card or tab is its own button",
        )
        assertTrue(
            nodes.none { SemanticsProperties.Text in it.config },
            "card faces aren't read separately",
        )
        assertTrue(
            nodes.any { it.config.getOrNull(SemanticsProperties.TestTag) == "card-a0" },
            "the cards keep their test tags",
        )
        assertEquals(
            listOf("Person a0"),
            root.config.getOrNull(SemanticsProperties.ContentDescription),
        )
        assertTrue(SemanticsActions.CustomActions in root.config)
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
    fun aFaceKeepsItsRememberedStateAsTheWheelTurns() = runComposeUiTest {
        // Long enough that the window of composed cards slides as the wheel turns.
        val deck = listOf(FileCard.Divider('C')) + (0 until 19).map { FileCard.Entry("c$it", 'C') }
        val state = CardFileState(initial = 1)
        val mismatches = mutableListOf<String>()
        cardFile(
            state,
            list = deck,
            face = { card ->
                val remembered = remember { card.key }
                SideEffect {
                    if (remembered != card.key) mismatches += "$remembered shown as ${card.key}"
                }
            },
        )
        onNodeWithTag("cardfile").requestFocus()
        repeat(8) {
            onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) }
            waitForIdle()
        }
        assertEquals(9, state.front)
        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun tappingACardBehindTheFrontSpinsToIt() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        // The next card's top peeks over the front card's top edge.
        val front = onNodeWithTag("card-a0").fetchSemanticsNode().boundsInRoot
        onRoot().performTouchInput { click(Offset(front.center.x, front.top - 4f)) }
        waitForIdle()
        assertEquals(2, state.front)
        assertEquals(null, state.pulled)
    }

    @Test
    fun aDragStartingOnACardSpinsTheWheel() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onNodeWithTag("card-a0", useUnmergedTree = true).performTouchInput {
            swipeDown(durationMillis = 1_000)
        }
        waitForIdle()
        assertTrue(state.front > 1, "front is ${state.front}")
    }

    @Test
    fun tappingTheScrimFilesTheCard() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onNodeWithTag("card-a0", useUnmergedTree = true).performClick()
        waitForIdle()
        onNodeWithTag("scrim").performTouchInput { click(Offset(10f, 10f)) }
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
                )
            )
    }

    @Test
    fun aDeckThatLosesThePulledEntryFilesItInsteadOfCrashing() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        var list by mutableStateOf(cards)
        setContent {
            CardFile(
                list,
                { Text(it.key) },
                { Text("out ${it.key}") },
                ::describe,
                Modifier.fillMaxSize(),
                state,
            )
        }
        onNodeWithTag("card-a0", useUnmergedTree = true).performClick()
        waitForIdle()
        list = listOf(FileCard.Divider('A'), FileCard.Divider('B')) + cards.drop(5)
        waitForIdle()
        onNodeWithTag("pulled").assertDoesNotExist()
        assertEquals(null, state.pulled)
    }

    @Test
    fun aTapWhereCardsOverlapGoesToTheNearestCard() = runComposeUiTest {
        // Every card in the window covers the front card's middle; composed in index order, the
        // cards behind would come last and win the tap if depth didn't order them.
        val state = CardFileState(initial = 1)
        cardFile(state)
        val front = onNodeWithTag("card-a0").fetchSemanticsNode().boundsInRoot
        onRoot().performTouchInput { click(front.center) }
        waitForIdle()
        assertEquals(1, state.pulled, "the front card is pulled, not a card behind it spun to")
        assertEquals(1, state.front)
    }

    @Test
    fun aDeckShrinkingUnderTheFrontCardEndsOnItsLastCard() = runComposeUiTest {
        val big = (0 until 30).map { FileCard.Entry("e$it", 'E') }
        val state = CardFileState(initial = 20)
        var list: List<FileCard> by mutableStateOf(big)
        cardFileOf(state) { list }
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
                )
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
    fun theScrimIsHiddenFromAccessibilityAndAPulledCardOffersNoSpin() = runComposeUiTest {
        val state = CardFileState(initial = 1)
        cardFile(state)
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        val scrim = onNodeWithTag("scrim").fetchSemanticsNode().config
        assertTrue(SemanticsActions.OnClick !in scrim, "the scrim is no unlabelled button")
        assertEquals(emptyList(), actionLabels(), "no spinning while a card is out")
    }

    @Test
    fun aTapOnASpinningWheelOnlyStopsIt() = runComposeUiTest {
        val deck = listOf(FileCard.Divider('C')) + (0 until 19).map { FileCard.Entry("c$it", 'C') }
        val state = CardFileState(initial = 1)
        cardFile(state, list = deck)
        val front = onNodeWithTag("card-c0").fetchSemanticsNode().boundsInRoot
        mainClock.autoAdvance = false
        onNodeWithTag("cardfile").requestFocus()
        // A long spin, still turning when the finger lands.
        repeat(6) { onNodeWithTag("cardfile").performKeyInput { pressKey(Key.DirectionDown) } }
        mainClock.advanceTimeBy(64)
        assertTrue(state.isSpinning)
        onRoot().performTouchInput { down(front.center) }
        mainClock.advanceTimeBy(32)
        val caught = state.position
        mainClock.advanceTimeBy(200)
        assertEquals(caught, state.position, 1e-3f, "the finger holds the wheel")
        onRoot().performTouchInput { up() }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(null, state.pulled, "the tap only stopped the wheel")
        assertEquals(state.front.toFloat(), state.position, 1e-3f, "it rests on a whole card")
        assertTrue(state.front < 7, "it stopped short of where it was spun to, on ${state.front}")
    }
}
