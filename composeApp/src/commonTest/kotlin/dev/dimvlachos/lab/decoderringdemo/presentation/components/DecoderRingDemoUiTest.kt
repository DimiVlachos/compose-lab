package dev.dimvlachos.lab.decoderringdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingState
import dev.dimvlachos.lab.decoderringdemo.DecoderRingDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class DecoderRingDemoUiTest {
    private val spanish =
        "¿Alguien ha visto un gato gris con collar rojo? Se escapó anoche cerca del parque Elm. " +
            "Se llama Pimienta y es muy tímido. ¡Gracias, vecinos!"

    @Test
    fun theMessageStartsInSpanishAndAClickTranslatesIt() = runComposeUiTest {
        val ring = DecoderRingState()
        setContent { Phone { DecoderRingDemo(DemoState(), ring, reducedMotion = false) } }
        waitForIdle()
        onNodeWithTag("message").assertContentDescriptionEquals(spanish)
        onNodeWithTag("decoder-ring").assertContentDescriptionEquals("Translate to English")
        // Every character has both its slots, measured at the bubble's width, inside it.
        assertTrue(ring.slots.size >= spanish.length)
        val width = onNodeWithTag("message").fetchSemanticsNode().size.width / density.density
        assertTrue(ring.slots.all { it.pa.x + it.pa.w <= width - 26f + .5f || it.ca == ' ' })
        onNodeWithTag("decoder-ring").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertTrue(ring.english)
        onNodeWithTag("message")
            .assertContentDescriptionEquals(
                "Has anyone seen a gray cat with a red collar? He slipped out last night near Elm Park. " +
                    "His name is Pepper and he’s very shy. Thanks, neighbors!",
            )
        onNodeWithTag("decoder-ring").assertContentDescriptionEquals("Show original Spanish")
        onNodeWithTag("live").assertContentDescriptionEquals("Translated from Spanish.")
        // The bubble is as tall as the English's lines.
        assertEquals(ring.contentHeight(), ring.height, .01f)
    }

    @Test
    fun theScriptTwistsThroughTheGestureAndEndsInSpanish() = runComposeUiTest {
        val state = DemoState()
        val ring = DecoderRingState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            Phone { DecoderRingDemo(state, ring, reducedMotion = false) }
        }
        waitForIdle()
        scope.launch { state.tapControl(DecoderRingDemos.Twist) }
        mainClock.advanceTimeBy(1_400)
        assertTrue(ring.held, "the hand has the ring")
        assertTrue(ring.g > 0f && ring.g < 1f, "g ${ring.g}")
        mainClock.advanceTimeBy(2_600)
        waitForIdle()
        assertTrue(ring.english)
        scope.launch { state.tapControl(DecoderRingDemos.TwistBack) }
        mainClock.advanceTimeBy(4_000)
        waitForIdle()
        assertFalse(ring.english)
        assertEquals(0f, ring.g)
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertFalse(ring.held)
        assertEquals(0f, ring.g)
    }

    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 860.dp)) { content() }
    }
}
