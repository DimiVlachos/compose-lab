package dev.dimvlachos.lab.core.presentation.components.frenchpress

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrenchPressStateTest {
    private val paragraphs = List(6) { p -> List(42) { w -> "w$p-$w" } }
    private val points = listOf("The council voted.", "It starts in April.", "Shops fear it.")
    private val events = mutableListOf<FrenchPressEvent>()

    // The prototype's screen at 412 by 800: words six to a 24 dp line under the rule at 220.
    private fun press(reduced: Boolean = false) =
        FrenchPressState(paragraphs, points).apply {
            reducedMotion = reduced
            layout(412f, 800f)
            words.indices.forEach { setWord(it, 30f + (it % 6) * 60f, 10f + (it / 6) * 24f) }
            setWordHeight(20f)
            placeBody(20f, 220f, article = 65f)
            letters.indices.forEach { setLetter(it, 20f + (it % 30) * 10f, 40f + (it / 30) * 20f) }
            placeSummary(34f, 220f)
            onEvent = { e, _ -> events += e }
        }

    private var capped = true
    private var inGlass = true
    private var underMesh = true

    // Runs [ms] of frames, watching what the prototype's selftest watches on every frame.
    private fun FrenchPressState.run(
        ms: Int,
        until: (FrenchPressState) -> Boolean = { false },
    ): Boolean {
        val out = FloatArray(3)
        var t = 0
        while (t < ms) {
            if (until(this)) return true
            advance(1 / 60f)
            t += 16
            val my = meshY(p)
            for (g in grains) {
                grainPos(g, out)
                if (abs(out[0]) > cw / 2 + .5f || out[1] < rimY || out[1] > gb + .5f)
                    inGlass = false
                if (g.e >= 1f && out[1] < my - .5f) underMesh = false
            }
            if (
                mode == PressMode.Forward &&
                    arrived < total &&
                    p > .3f + .7f * FrenchPressState.RESIST + 1e-3f
            )
                capped = false
        }
        return until(this)
    }

    private val FrenchPressState.isIdle
        get() = asBegun

    private val FrenchPressState.isBrewed
        get() =
            mode == PressMode.Brewed &&
                parts.isEmpty() &&
                abs(p - 1) < 1e-6f &&
                tilt == 0f &&
                shown.all { it } &&
                cardOpen == 1f

    private fun FrenchPressState.tapAt(x: Float, y: Float) {
        pressDown(x, y)
        pressUp(x, y)
    }

    @Test
    fun itStartsAsTheFullArticleWithThePressInTheCorner() {
        val s = press()
        assertTrue(s.isIdle)
        assertTrue(s.knobX > 0 && s.knobX < 412 && s.knobScreenY > 400 && s.knobScreenY < 800)
        assertEquals(1.25f * s.k, s.kp, 1e-5f)
    }

    @Test
    fun aTapPressesTheWholeStoryIntoASummaryAndBack() {
        val s = press()
        s.tapAt(s.knobX, s.knobScreenY + 40)
        assertEquals(PressMode.Forward, s.mode)
        assertTrue(s.committed)
        s.run(300)
        assertTrue(
            s.parts.any { it.word && it.into } && s.gone.any { it } && s.gone.any { !it },
            "words leave as they fly",
        )
        s.tapAt(s.knobX, s.knobScreenY)
        assertEquals(PressMode.Forward, s.mode, "a tap while busy is ignored")
        assertTrue(s.run(9000) { it.isBrewed }, "it ends as a summary")
        assertTrue(
            s.total > 60 && s.grains.size == s.total,
            "every word on screen went in: ${s.total}",
        )
        assertTrue(s.gone.all { it })
        val out = FloatArray(3)
        assertTrue(
            s.grains.all {
                s.grainPos(it, out)
                out[1] >= s.meshBot - .5f && out[1] <= s.gb
            },
            "packed under the mesh",
        )
        assertEquals(PressSaid.Ready, s.said)
        assertTrue(capped, "held back before every word was in")
        assertTrue(
            FrenchPressEvent.Tink in events &&
                FrenchPressEvent.Pour in events &&
                FrenchPressEvent.Glug in events,
        )
        s.showFull()
        assertEquals(PressMode.Reverse, s.mode)
        assertTrue(s.run(9000) { it.isIdle }, "it ends as the full article")
        assertEquals(PressSaid.Full, s.said)
        assertTrue(inGlass && underMesh)
    }

    @Test
    fun aFlickWithNoMovesPressesAndATapOnThePressGivesItBack() {
        val s = press()
        s.pressDown(s.knobX, s.knobScreenY)
        s.pressUp(s.knobX, s.knobScreenY + s.travel * .8f)
        assertTrue(s.mode == PressMode.Forward && s.committed)
        assertTrue(s.run(9000) { it.isBrewed })
        s.tapAt(s.knobX, s.knobScreenY)
        assertEquals(PressMode.Reverse, s.mode)
        assertTrue(s.run(9000) { it.isIdle })
    }

    @Test
    fun aShortDragFollowsOnceTheWordsAreInAndCancels() {
        val s = press()
        val x = s.knobX
        val y = s.knobScreenY
        s.pressDown(x, y)
        s.pressMove(x, y + s.travel * .25f)
        assertTrue(s.mode == PressMode.Forward && !s.committed)
        s.pressMove(x, y + s.travel * .42f)
        s.run(3000) { it.arrived == it.total }
        assertTrue(s.run(1500) { abs(it.p - .42f) < .01f }, "follows the finger: ${s.p}")
        s.pressUp(x, y + s.travel * .42f)
        assertEquals(PressMode.Cancel, s.mode)
        assertTrue(s.run(9000) { it.isIdle } && s.maxP < .45f)
        assertEquals(PressSaid.Kept, s.said)
    }

    @Test
    fun pastHalfwayTheCardOpensAndLettingGoShortClosesIt() {
        val s = press()
        val x = s.knobX
        val y = s.knobScreenY
        s.pressDown(x, y)
        s.pressMove(x, y + s.travel * .3f)
        s.run(3000) { it.arrived == it.total && it.total > 0 }
        s.pressMove(x, y + s.travel * .75f)
        assertTrue(s.run(1500) { it.cardOpen > .2f } && s.mode == PressMode.Forward && !s.committed)
        s.pressMove(x, y + s.travel * .3f)
        s.pressUp(x, y + s.travel * .3f)
        assertEquals(PressMode.Cancel, s.mode)
        assertTrue(s.run(9000) { it.isIdle })
    }

    @Test
    fun aDragPastHalfwayFinishesAndPullingUpReverses() {
        val s = press()
        val x = s.knobX
        val y = s.knobScreenY
        s.pressDown(x, y)
        s.pressMove(x, y + s.travel * .3f)
        s.run(200)
        s.pressMove(x, y + s.travel * .75f)
        s.run(60)
        s.pressUp(x, y + s.travel * .75f)
        assertTrue(s.committed && s.run(9000) { it.isBrewed })
        val ky = s.knobScreenY
        s.pressDown(s.knobX, ky)
        s.pressUp(s.knobX, ky - 50 * s.k)
        assertEquals(PressMode.Reverse, s.mode)
        assertTrue(s.run(9000) { it.isIdle })
    }

    @Test
    fun aCancelledTouchFinishesFromItsLastPoint() {
        val s = press()
        s.pressDown(s.knobX, s.knobScreenY)
        s.pressMove(s.knobX, s.knobScreenY + s.travel * .9f)
        s.pressCancel()
        assertTrue(s.committed && s.run(9000) { it.isBrewed })
    }

    @Test
    fun aSecondFingerDoesNotTakeOverTheDrag() {
        val s = press()
        val x = s.knobX
        val y = s.knobScreenY
        s.pressDown(x, y)
        s.pressMove(x, y + s.travel * .25f)
        s.run(100)
        s.pressDown(x - 20, y + 30)
        assertTrue(s.heldByFinger)
        s.pressUp(x, y + s.travel * .25f)
        assertEquals(PressMode.Cancel, s.mode)
        assertTrue(s.run(9000) { it.isIdle })
    }

    @Test
    fun takingOverTheScriptMidPushNeverStrandsThePress() {
        val s = press()
        val x = s.knobX
        val y = s.knobScreenY
        s.pressDown(x, y, scripted = true)
        s.pressMove(x, y + s.travel * .3f)
        s.run(300)
        s.takeOver()
        assertEquals(PressMode.Cancel, s.mode)
        assertTrue(s.run(9000) { it.isIdle })
        // A real finger can press again at once.
        s.tapAt(s.knobX, s.knobScreenY)
        assertEquals(PressMode.Forward, s.mode)
    }

    @Test
    fun withReducedMotionItJumpsToTheSummaryAndBack() {
        val s = press(reduced = true)
        s.tapAt(s.knobX, s.knobScreenY)
        assertTrue(s.isBrewed)
        s.showFull()
        assertTrue(s.isIdle)
        assertFalse(s.parts.isNotEmpty())
    }

    @Test
    fun wordsInTheFadeAboveTheKnobStayPut() {
        val s = press()
        s.tap()
        s.run(2000)
        val kept = s.words.indices.filter { s.wordY(it) + 10 > s.clearY - 20 }
        assertTrue(kept.isNotEmpty() && kept.all { s.gone[it] })
        assertEquals(s.words.size - kept.size, s.total)
    }
}
