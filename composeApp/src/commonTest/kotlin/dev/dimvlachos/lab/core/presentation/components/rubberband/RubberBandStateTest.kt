package dev.dimvlachos.lab.core.presentation.components.rubberband

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RubberBandStateTest {
    private val opening =
        listOf(
            Who.Them to "Are we still on for Friday?",
            Who.Me to "Yes!! Can’t wait",
            Who.Them to "Want me to book somewhere? Ellie said she can come too",
            Who.Me to "I got it, one sec",
        )

    // A monospaced stand-in for the real text layout: 8 dp a letter.
    private val mono =
        object : GlyphPlaces {
            override fun inBubble(text: String, starts: IntArray, out: FloatArray) {
                for (i in starts.indices) {
                    out[i * 2] = 13f + i * 8f + 4f
                    out[i * 2 + 1] = 18f
                }
            }

            override fun inField(text: String, starts: IntArray, out: FloatArray): Float {
                for (i in starts.indices) out[i] = i * 8f + 4f
                return starts.size * 8f
            }
        }

    private val events = mutableListOf<BandEvent>()
    private val said = mutableListOf<BandSay>()
    private var restored: String? = null

    private fun chat(reduced: Boolean = false) =
        RubberBandState(opening).apply {
            reducedMotion = reduced
            glyphs = mono
            size(412f, 780f)
            eyeAt(Offset(27f, 740f))
            fieldAt(66f, 336f, 740f)
            threadAt(60f)
            onEvent = { e, _ -> events += e }
            onSay = { said += it }
            onRestore = { text, _ -> restored = text }
        }

    // Lays the tethered bubble out where the thread would put it: right-aligned above the composer.
    private fun RubberBandState.layOut() {
        val t = tether ?: return
        val w = 13f * 2 + t.row.text.length * 8f
        bubbleAt(t.row.id, Rect(398f - w, 640f, 398f, 677f))
        captionAt(t.row.id, Offset(398f - 120f, 683f))
    }

    private fun RubberBandState.run(ms: Int) {
        repeat(ms / 8) {
            layOut()
            advance(.008f)
        }
    }

    private fun RubberBandState.band() = bandOf(tether!!, Band())

    @Test
    fun startsWithTheThreadAndNothingTied() {
        val s = chat()
        assertEquals(4, s.rows.size)
        assertNull(s.tether)
        assertEquals(listOf(false, true, true, true), s.rows.map { it.gap })
        assertTrue(s.settled)
    }

    @Test
    fun blankTextSendsNothing() {
        val s = chat()
        assertFalse(s.send("   "))
        assertEquals(4, s.rows.size)
    }

    @Test
    fun sendAddsTheBubbleTrimmedAndTiesItAndTheWindowWaitsForItToLand() {
        val s = chat()
        assertTrue(s.send("  Running late, save me a seat "))
        val t = assertNotNull(s.tether)
        assertEquals("Running late, save me a seat", t.row.text)
        assertEquals(5, s.rows.size)
        assertEquals(Who.Me, s.rows.last().who)
        assertEquals(TetherMode.Rise, t.mode)
        assertEquals(0.0, t.elapsed)
        assertEquals(BandEvent.Send, events.last())
        assertEquals(BandSay.Sent, said.last())
        s.layOut()
        // It starts from the field: its left edge 4 dp before the text, its middle on the field's.
        val r = assertNotNull(s.bubbleRect(t))
        assertEquals(66f - 4f * s.k, r.left, .5f)
        assertEquals(740f, r.center.y, .5f)
        s.run(500)
        assertEquals(TetherMode.Live, t.mode)
        assertTrue(abs(t.off.x) + abs(t.off.y) < 1f)
        assertTrue(t.elapsed in 1.0..200.0, "${t.elapsed}")
    }

    @Test
    fun theBandRunsFromTheBubbleDownToTheEyeAndThePlaceholderFades() {
        val s = chat()
        s.send("Running late")
        s.run(800)
        val b = s.band()
        assertTrue(b.a.y < b.b.y && b.a.x > b.b.x + 40)
        assertTrue(hypot(b.b.x - s.eye.x, b.b.y - s.eye.y) < s.eyeR)
        assertTrue(s.ph < .05f)
        assertEquals(1f, s.captionAlpha(), .01f)
    }

    @Test
    fun theBandThinsAsTheWindowRunsOutAndNecksNearTheEnd() {
        val s = chat()
        s.send("Running late")
        s.run(600)
        val w0 = s.bandWidth(s.tether, s.band())
        s.run(4500)
        assertTrue(s.bandWidth(s.tether, s.band()) < w0 * .8f)
        assertEquals(0f, neckOf(s.life(s.tether)))
        assertEquals(.72f, neckOf(1f), 1e-4f)
    }

    @Test
    fun aShortPullHoldsPausesAndWobblesBackWithoutUnsending() {
        val s = chat()
        s.send("See you at 7")
        s.run(800)
        val t = s.tether!!
        val m = s.band().at(.5f)
        assertTrue(s.down(m.x, m.y))
        s.move(m.x, m.y + 30f * s.k)
        assertTrue(t.held)
        val e0 = t.elapsed
        s.run(400)
        assertEquals(e0, t.elapsed, 1e-6)
        assertTrue(s.band().stretch > 2f)
        s.up(m.x, m.y + 30f * s.k)
        assertEquals(TetherMode.Live, t.mode)
        assertFalse(t.held)
        assertEquals(BandEvent.Boing, events.last())
        val d0 = t.d.y
        var swung = false
        repeat(75) {
            s.run(8)
            if (sign(t.d.y) != sign(d0) && abs(t.d.y) > .3f) swung = true
        }
        assertTrue(swung, "the band wobbles back through rest")
    }

    @Test
    fun aTapOnTheBubbleLeavesItSent() {
        val s = chat()
        s.send("See you at 7")
        s.run(800)
        val r = s.bubbleRect(s.tether!!)!!
        for (f in listOf(.3f, .6f, .9f)) {
            val x = r.left + r.width * f
            assertTrue(s.down(x, r.center.y))
            s.up(x, r.center.y)
        }
        assertEquals(TetherMode.Live, s.tether!!.mode)
        assertEquals(5, s.rows.size)
    }

    @Test
    fun aPressAndAFarReleaseUnsendsAndTheLettersLandInTheField() {
        val s = chat()
        s.send("Running late")
        s.run(800)
        val t = s.tether!!
        val r = s.bubbleRect(t)!!
        s.down(r.left + r.width * .3f, r.center.y)
        s.up(r.left + r.width * .3f - 10f * s.k, r.center.y + 80f * s.k)
        assertEquals(TetherMode.Fly, t.mode)
        assertTrue(s.busy)
        assertEquals(BandEvent.Yank, events.last())
        s.run(360)
        assertTrue(t.popped)
        assertEquals("Running late".length, s.letters.size)
        val ls = s.letters.filter { it.ch != " " }
        assertTrue(ls.all { abs(it.y1 - 740f) < .5f })
        assertTrue(ls.zipWithNext().all { (a, b) -> b.x1 > a.x1 })
        // The first letter lands where the field sets it.
        assertEquals(66f + 4f, s.letters[0].x1, .01f)
        s.run(800)
        assertEquals("Running late", restored)
        assertNull(s.tether)
        assertFalse(s.busy)
        assertEquals(4, s.rows.size)
        assertEquals(1, s.unsent)
        assertEquals(BandSay.Unsent, said.last())
    }

    @Test
    fun aLongMessageLandsScrolledToItsEndItsStartFading() {
        val s = chat()
        val long = "Can you grab the folding chairs from the garage on your way over?"
        s.send(long)
        s.run(800)
        s.unsendNow()
        s.run(360)
        val last = s.letters.last()
        assertEquals(336f - 4f, last.x1, .01f)
        assertTrue(s.letters[0].clip)
    }

    @Test
    fun thePullIsFingerTravelSoTowardTheEyeCountsTheSame() {
        val s = chat()
        s.send("See you at 7:30")
        s.run(800)
        val t = s.tether!!
        val b = s.band()
        val m = b.at(.5f)
        val d = hypot(b.b.x - m.x, b.b.y - m.y)
        val ux = (b.b.x - m.x) / d
        val uy = (b.b.y - m.y) / d
        s.down(m.x, m.y)
        s.up(m.x + ux * 30f * s.k, m.y + uy * 30f * s.k)
        assertEquals(TetherMode.Live, t.mode)
        s.down(m.x, m.y)
        s.up(m.x + ux * 44f * s.k, m.y + uy * 44f * s.k)
        assertEquals(TetherMode.Fly, t.mode)
    }

    @Test
    fun aCancelledPullFarOutStillUnsends() {
        val s = chat()
        s.send("See you at 7")
        s.run(800)
        val m = s.band().at(.4f)
        s.down(m.x, m.y)
        s.move(m.x - 40f * s.k, m.y - 110f * s.k)
        s.cancel()
        assertEquals(TetherMode.Fly, s.tether!!.mode)
    }

    @Test
    fun aDragElsewhereLeavesTheBandAlone() {
        val s = chat()
        s.send("See you at 8")
        s.run(800)
        assertFalse(s.down(206f, 200f))
        assertEquals(TetherMode.Live, s.tether!!.mode)
    }

    @Test
    fun leftAloneTheBandSnapsSaysDeliveredAndCantBePulledBack() {
        val s = chat()
        s.send("See you at 8")
        s.run(500)
        val t = s.tether!!
        s.run(8400)
        assertNotNull(s.tether)
        var guard = 0
        while (s.tether != null && guard++ < 200) s.run(8)
        assertNull(s.tether)
        assertEquals(TetherMode.Done, t.mode)
        assertEquals(t.row.id, s.receiptId)
        assertEquals(BandEvent.Twang, events.last())
        assertEquals(BandSay.Delivered, said.last())
        assertEquals(1, s.breaks.size)
        s.run(400)
        assertTrue(s.ph > .95f)
        assertTrue(s.breaks.isEmpty())
        assertFalse(s.down(390f, 650f))
        assertEquals(5, s.rows.size)
    }

    @Test
    fun sendingAgainSnapsTheEarlierBandAtOnce() {
        val s = chat()
        s.send("Bringing the cake")
        s.run(600)
        val t4 = s.tether!!
        s.send("And candles")
        assertEquals(TetherMode.Done, t4.mode)
        assertEquals(t4.row.id, s.receiptId)
        assertEquals("And candles", s.tether!!.row.text)
        assertEquals(6, s.rows.size)
    }

    @Test
    fun theCaptionButtonUnsends() {
        val s = chat()
        s.send("And candles")
        s.run(600)
        s.unsendNow()
        assertEquals(TetherMode.Fly, s.tether!!.mode)
        s.run(1200)
        assertEquals("And candles", restored)
    }

    @Test
    fun aRowComingGlidesTheThreadFromWhereItWas() {
        val s = chat()
        s.threadMeasured(300f)
        s.send("Cool")
        s.threadMeasured(345f)
        assertEquals(45f, s.glideY(), .01f)
        s.run(120)
        assertTrue(s.glideY() in 1f..44f)
        s.run(200)
        assertEquals(0f, s.glideY())
        // A row's caption giving way to its receipt doesn't glide.
        s.threadMeasured(344f)
        assertEquals(0f, s.glideY())
    }

    @Test
    fun reducedMotionTiesAtOnceAndUnsendsAtOnce() {
        val s = chat(reduced = true)
        s.send("Here!")
        assertEquals(TetherMode.Live, s.tether!!.mode)
        s.layOut()
        s.advance(.01f)
        assertEquals(0f, s.ph)
        s.unsendNow()
        assertNull(s.tether)
        assertEquals("Here!", restored)
        assertEquals(4, s.rows.size)
    }

    @Test
    fun theTourRepliesAndResetPutsTheOpeningThreadBack() {
        val s = chat()
        s.send("Table’s booked")
        s.showTyping()
        assertEquals(Who.Typing, s.rows.last().who)
        s.reply("Perfect, see you there 🙌")
        assertEquals(Who.Them, s.rows.last().who)
        assertTrue(s.rows.none { it.who == Who.Typing })
        s.reset()
        assertEquals(opening.map { it.second }, s.rows.map { it.text })
        assertNull(s.tether)
        assertNull(s.receiptId)
    }

    @Test
    fun anEmojiIsOneLetter() {
        assertEquals(listOf(0, 1, 3), letterStarts("a🙌b").toList())
    }
}
