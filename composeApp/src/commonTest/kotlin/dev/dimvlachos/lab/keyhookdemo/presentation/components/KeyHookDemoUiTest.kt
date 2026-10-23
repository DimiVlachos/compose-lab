package dev.dimvlachos.lab.keyhookdemo.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyRackState
import dev.dimvlachos.lab.keyhookdemo.Household
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class KeyHookDemoUiTest {
    @Test
    fun startsSignedInAsJordanWithHisShows() = runComposeUiTest {
        setContent { KeyHookDemo(DemoState()) }
        onNodeWithText("Welcome home, Jordan").assertExists()
        onNodeWithText("Harbor Lights").assertExists()
        onNodeWithTag("keys-0").assertContentDescriptionEquals("Sign out Jordan Miller")
        onNodeWithTag("keys-1").assertContentDescriptionEquals("Sign in as Ava Miller")
    }

    @Test
    fun theButtonsSignOutAndInAsTheKeysLand() = runComposeUiTest {
        val rack = KeyRackState(Household.people)
        setContent { KeyHookDemo(DemoState(), rack, reducedMotion = true) }
        onNodeWithTag("keys-0").performClick()
        waitForIdle()
        assertNull(rack.signedIn)
        onNodeWithText("Who’s home?").assertExists()
        onNodeWithText("Your shows appear here once you take your keys.").assertExists()
        onNodeWithTag("keys-3").performClick()
        waitForIdle()
        assertEquals("Grandpa Lou", rack.signedIn?.name)
        onNodeWithText("Welcome home, Lou").assertExists()
        onNodeWithText("Bass Lake Mornings").assertExists()
        onNodeWithTag("keys-3").assertContentDescriptionEquals("Sign out Grandpa Lou")
        onNodeWithTag("keys-0").assertContentDescriptionEquals("Sign in as Jordan Miller")
    }

    @Test
    fun withMotionTheSignInWaitsForTheKeysToLand() = runComposeUiTest {
        val rack = KeyRackState(Household.people)
        setContent { KeyHookDemo(DemoState(), rack, reducedMotion = false) }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithTag("keys-1").performClick()
        mainClock.advanceTimeBy(100)
        assertEquals("Jordan Miller", rack.signedIn?.name)
        mainClock.advanceTimeBy(2_000)
        assertEquals("Ava Miller", rack.signedIn?.name)
        onNodeWithText("Comet Club").assertExists()
    }

    @Test
    fun theFeedCrossfadesToTheNextPersonAndTheHeroKeepsItsHeight() = runComposeUiTest {
        val rack = KeyRackState(Household.people)
        setContent { KeyHookDemo(DemoState(), rack, reducedMotion = false) }
        waitForIdle()
        val hero = { onNodeWithTag("keyhook-hero").fetchSemanticsNode().size.height }
        val feed = { onNodeWithTag("keyhook-feed").fetchSemanticsNode().size.height }
        val h0 = hero()
        val f0 = feed()
        mainClock.autoAdvance = false
        onNodeWithTag("keys-1").performClick()
        var fadingMs = 0L
        repeat(160) {
            mainClock.advanceTimeBy(16)
            assertEquals(h0, hero())
            assertEquals(f0, feed())
            if (onAllNodesWithTag("keyhook-feed-out").fetchSemanticsNodes().isNotEmpty()) {
                fadingMs += 16
            }
        }
        // Nobody is signed in between (Ava's set lands after Jordan's), so one 400 ms crossfade.
        assertTrue(fadingMs in 320L..560L, "crossfading for $fadingMs ms")
        assertEquals("Ava Miller", rack.signedIn?.name)
        onNodeWithText("Comet Club").assertExists()
        assertTrue(onAllNodesWithTag("keyhook-feed-out").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun midwayTheFirstShowIsABlendOfBothPeoplesAndSignedOutTheFeedKeepsItsHeight() =
        runComposeUiTest {
            val rack = KeyRackState(Household.people)
            setContent { KeyHookDemo(DemoState(), rack, reducedMotion = false) }
            waitForIdle()
            val hero = { onNodeWithTag("keyhook-hero").fetchSemanticsNode().size.height }
            val feed = { onNodeWithTag("keyhook-feed").fetchSemanticsNode().size.height }
            val h0 = hero()
            val f0 = feed()
            val jordan = firstThumb()
            mainClock.autoAdvance = false
            onNodeWithTag("keys-1").performClick()
            var frames = 0
            while (onAllNodesWithTag("keyhook-feed-out").fetchSemanticsNodes().isEmpty()) {
                mainClock.advanceTimeBy(16)
                assertTrue(++frames < 200, "the crossfade never started")
            }
            // Halfway through the eased 400 ms, Ava's thumbnail is half over Jordan's, which stays
            // solid under it.
            mainClock.advanceTimeBy(200)
            val mid = firstThumb()
            mainClock.advanceTimeBy(600)
            val ava = firstThumb()
            for ((name, c) in
                listOf("red" to Color::red, "green" to Color::green, "blue" to Color::blue)) {
                val half = (c(jordan) + c(ava)) / 2f
                assertTrue(abs(c(mid) - half) < .03f, "$name ${c(jordan)} → ${c(mid)} → ${c(ava)}")
            }
            val apart =
                abs(jordan.red - ava.red) +
                    abs(jordan.green - ava.green) +
                    abs(jordan.blue - ava.blue)
            assertTrue(apart > .2f, "$jordan, $ava")
            assertEquals(1f, mid.alpha, .01f)
            // Signing out fades the shows into the empty note; nothing above or below moves.
            // As a screen reader or keyboard does: a tap there goes to whichever keys are under it,
            // and Ava's are still swinging from the card's hook.
            onNodeWithTag("keys-1").performSemanticsAction(SemanticsActions.OnClick)
            repeat(160) {
                mainClock.advanceTimeBy(16)
                assertEquals(h0, hero())
                assertEquals(f0, feed())
            }
            assertNull(rack.signedIn)
            onNodeWithText("Your shows appear here once you take your keys.").assertExists()
        }

    // The colour near the top left of the feed's first thumbnail, clear of its title and progress.
    private fun ComposeUiTest.firstThumb(): Color {
        val img = onNodeWithTag("keyhook-feed").captureToImage().toPixelMap()
        return img[img.width / 8, img.height / 6]
    }

    @Test
    fun theCrossfadeEasesAndKeepsTheOldShowsUnderTheNew() {
        assertEquals(400, FeedCrossfade.Ms)
        assertEquals(0f, FeedCrossfade.Easing.transform(0f))
        assertEquals(.5f, FeedCrossfade.Easing.transform(.5f), 1e-6f)
        assertEquals(1f, FeedCrossfade.Easing.transform(1f))
        assertTrue(FeedCrossfade.Easing.transform(.25f) < .25f)
        assertEquals(1f, FeedCrossfade.outgoing(.3f, keep = true))
        assertEquals(.7f, FeedCrossfade.outgoing(.3f, keep = false), 1e-6f)
    }
}
