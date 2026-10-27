package dev.dimvlachos.lab.core.presentation.components.decoderring

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecoderRingStateTest {
    private val es =
        "¿Alguien ha visto un gato gris con collar rojo? Se escapó anoche cerca del parque Elm. " +
            "Se llama Pimienta y es muy tímido. ¡Gracias, vecinos!"
    private val en =
        "Has anyone seen a gray cat with a red collar? He slipped out last night near Elm Park. " +
            "His name is Pepper and he’s very shy. Thanks, neighbors!"

    // A stand-in for the font: narrow i, l and spaces, wide m and w, the rest 8.
    private fun measure(s: String) =
        s.sumOf {
            when (it) {
                ' ' -> 4.0
                'i', 'l', '.', ',', '¡', '!', '’' -> 3.5
                'm', 'w', 'M', 'W' -> 12.0
                else -> 8.0
            }
        }.toFloat()

    private val radius = 31f

    private fun state(width: Float = 260f): Pair<DecoderRingState, List<DecoderEvent>> {
        val events = mutableListOf<DecoderEvent>()
        val s = DecoderRingState()
        s.onEvent = { events += it }
        s.layout(wrapDecoderText(es, width, ::measure), wrapDecoderText(en, width, ::measure))
        s.reset()
        return s to events
    }

    private fun at(deg: Float): Pair<Float, Float> {
        val r = radius * .8f
        val a = deg * PI.toFloat() / 180f
        return cos(a) * r to sin(a) * r
    }

    private fun DecoderRingState.down(deg: Float) = at(deg).let { (x, y) -> down(x, y) }

    private fun DecoderRingState.move(deg: Float) = at(deg).let { (x, y) -> move(x, y, radius) }

    private fun DecoderRingState.up(deg: Float, cancel: Boolean = false) =
        at(deg).let { (x, y) -> up(x, y, radius, cancel) }

    private fun DecoderRingState.settle(seconds: Float = 3f) {
        var t = 0f
        while (t < seconds && (target != null || held || awake)) {
            step(1 / 60f)
            t += 1 / 60f
        }
    }

    @Test
    fun theWrapKeepsEveryCharacterInsideTheWidth() {
        for (w in listOf(180f, 260f, 400f)) {
            for (text in listOf(es, en)) {
                val l = wrapDecoderText(text, w, ::measure)
                assertEquals(text.length, l.chars.size)
                assertEquals(text, l.chars.map { it.ch }.joinToString(""))
                assertTrue(l.chars.all { it.ch == ' ' || it.x + it.w <= w + .5f }, "fits $w")
                assertEquals(l.lines - 1, l.chars.maxOf { it.line })
            }
        }
    }

    @Test
    fun charactersSitWhereTheirWordsPrefixesEnd() {
        val l = wrapDecoderText("ami go", 100f, ::measure)
        assertEquals(listOf(0f, 8f, 20f, 23.5f, 27.5f, 35.5f), l.chars.map { it.x })
        assertEquals(listOf(8f, 12f, 3.5f, 4f, 8f, 8f), l.chars.map { it.w })
        // Too wide for the line, the second word goes down.
        val two = wrapDecoderText("ami go", 30f, ::measure)
        assertEquals(listOf(0, 0, 0, 0, 1, 1), two.chars.map { it.line })
        assertEquals(0f, two.chars[4].x)
    }

    @Test
    fun theAccentsAndTheAlphabetReadAsThePrototypeReadsThem() {
        assertEquals('o', baseOf('ó'))
        assertEquals('I', baseOf('Í'))
        assertEquals(14, idx('ó'))
        assertEquals(('¿'.code * 7) % 26, idx('¿'))
        assertFalse(isLetter('¡'))
    }

    @Test
    fun everySlotHasBothEndsAndAShortReelFromItsOwnCharacterToTheTarget() {
        val (s, _) = state()
        assertEquals(maxOf(es.length, en.length), s.slots.size)
        s.slots.forEachIndexed { i, slot ->
            assertTrue(slot.d in ReelMin..ReelMax, "reel $i is ${slot.d}")
            assertEquals(slot.ca, slot.reel(0, slot.ca))
            assertEquals(slot.cb, slot.reel(slot.d, slot.ca))
        }
        // The first character's reel starts from the ¿ itself, never another letter.
        assertEquals('¿', s.slots[0].reel(0, s.slots[0].ca))
    }

    @Test
    fun aSparseTwistDrivesTheWaveAboutSeventyPercentThrough() {
        val (s, events) = state()
        s.down(-120f)
        for (a in listOf(-75f, -25f, 20f, 65f, 110f)) s.move(a)
        assertTrue(s.g > .6f && s.g < .8f, "g ${s.g}")
        assertEquals(1f, s.slotQ(0))
        assertTrue(s.slotQ(s.slots.size - 1) < .2f)
        assertEquals(230 * PI.toFloat() / 180f, s.ringA, .05f)
        assertTrue(DecoderEvent.Tick in events)
        s.up(110f)
        s.settle()
        assertTrue(s.english)
        assertEquals(1f, s.g)
        assertNull(s.target)
        assertTrue(DecoderEvent.Done in events)
        val lines = wrapDecoderText(en, 260f, ::measure).lines
        assertEquals(s.heightOf(lines), s.height, .01f)
    }

    @Test
    fun aShortTwistSpringsBackAndATapToggles() {
        val (s, events) = state()
        s.down(-90f)
        s.move(-60f)
        s.move(-40f)
        assertTrue(s.g > 0f && s.g < DecoderRingState.Commit)
        s.up(-40f)
        s.settle()
        assertEquals(0f, s.g)
        assertFalse(s.english)
        assertTrue(DecoderEvent.Back !in events, "springing back to where it was says nothing")
        s.down(90f)
        s.up(90f)
        s.settle()
        assertTrue(s.english)
        // A short twist back from English springs back to English.
        s.down(0f)
        s.move(-30f)
        s.move(-55f)
        s.up(-55f)
        s.settle()
        assertTrue(s.english)
        assertEquals(1f, s.g)
        s.toggle()
        s.settle()
        assertFalse(s.english)
        assertEquals(DecoderEvent.Back, events.last { it == DecoderEvent.Back || it == DecoderEvent.Done })
    }

    @Test
    fun aCounterClockwiseTwistCancelledPastTheCommitPointStillTranslates() {
        val (s, _) = state()
        s.down(60f)
        for (a in listOf(20f, -30f, -80f)) s.move(a)
        s.up(-80f, cancel = true)
        s.settle()
        assertTrue(s.english)
    }

    @Test
    fun aTapWhileItAnimatesReversesIt() {
        val (s, _) = state()
        s.toggle()
        while (s.g < .32f) s.step(1 / 60f)
        s.down(90f)
        s.up(90f)
        assertEquals(0, s.target)
        s.settle()
        assertFalse(s.english)
        assertEquals(0f, s.g)
    }

    @Test
    fun grabbingItMidAnimationCarriesOnFromWhereItWas() {
        val (s, _) = state()
        s.toggle()
        while (s.g < .38f) s.step(1 / 60f)
        val g0 = s.g
        s.down(0f)
        s.move(10f)
        assertEquals(g0 + 10f / 330f, s.g, .01f)
    }

    @Test
    fun lettingAHeldTwistGoFinishesToTheNearerEnd() {
        val (s, _) = state()
        val id = s.down(-90f)
        for (a in listOf(-40f, 10f, 60f)) s.move(a)
        assertTrue(s.holds(id))
        s.release()
        assertFalse(s.held)
        s.settle()
        assertTrue(s.english)
    }

    @Test
    fun theBubbleNeverClipsALetterThatHasNotLeftItsLine() {
        // Narrow enough that the Spanish takes more lines than the English.
        val width =
            (100..400).map { it.toFloat() }.first {
                wrapDecoderText(es, it, ::measure).lines != wrapDecoderText(en, it, ::measure).lines
            }
        val a = wrapDecoderText(es, width, ::measure)
        val b = wrapDecoderText(en, width, ::measure)
        for (forward in listOf(true, false)) {
            val s = DecoderRingState()
            s.layout(a, b)
            s.reset()
            if (!forward) {
                s.toggle()
                s.settle()
            }
            s.toggle()
            var t = 0f
            while (s.target != null && t < 3f) {
                s.step(1 / 60f)
                assertTrue(s.height >= s.contentHeight() - .01f)
                t += 1 / 60f
            }
        }
    }

    @Test
    fun reducedMotionLandsAtOnce() {
        val (s, events) = state()
        s.reducedMotion = true
        s.toggle()
        assertTrue(s.english)
        assertEquals(1f, s.g)
        assertNull(s.target)
        assertTrue(DecoderEvent.Done in events)
        s.step(1 / 60f)
        assertEquals(s.contentHeight(), s.height)
    }

    @Test
    fun theDialTurnsOnAsTheWaveFinishesByItself() {
        val (s, _) = state()
        s.toggle()
        s.settle()
        assertEquals(DecoderRingState.Turn, s.ringA, .01f)
        assertTrue(abs(s.ringA) > 0f)
    }
}
