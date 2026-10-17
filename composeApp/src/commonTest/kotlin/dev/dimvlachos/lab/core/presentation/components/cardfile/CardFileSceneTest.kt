package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardFileSceneTest {
    private val deg = (PI / 180).toFloat()

    // A, two contacts, B, three contacts: the B divider is card 3.
    private val cards: List<PaperCard> =
        listOf(
            PaperCard.Divider('A', 0),
            contact("Abby Carter", 31),
            contact("Andre Wallace", 48),
            PaperCard.Divider('B', 1),
            contact("Ben Hollis", 65),
            contact("Bianca Reyes", 82),
            contact("Bo Lane", 99),
        )

    private fun contact(name: String, seed: Int) =
        PaperCard.Contact(name, "Friend", listOf("(802) 555-0100", "Main St", "Note"), seed)

    @Test
    fun cardsStandLeanBackAndFallAsThePrototypes() {
        assertEquals(-6f * deg, phiOf(0f), 1e-5f)
        assertEquals((-6f - 4.6f * 3) * deg, phiOf(3f), 1e-5f)
        assertEquals(72f * deg, phiOf(-1f), 1e-5f)
        assertEquals(33f * deg, phiOf(-.5f), 1e-5f)
        // Further down the pile in front, each lies 3.4° further over.
        assertEquals((72f + 3.4f * 2) * deg, phiOf(-3f), 1e-5f)
    }

    @Test
    fun thePileStopsLeaningAtTheCardsTheWheelShows() {
        // The seventh card in front and the fifteenth behind lie as far over as any: deeper ones
        // rest exactly beneath them, so the pile looks as the prototype's did at rest.
        val front = (72f + 3.4f * 6) * deg
        val back = (-6f - 4.6f * 15) * deg
        assertEquals(front, phiOf(-7f), 1e-5f)
        assertEquals(front, phiOf(-8f), 1e-5f)
        assertEquals(front, phiOf(-30f), 1e-5f)
        assertEquals(back, phiOf(15f), 1e-5f)
        assertEquals(back, phiOf(16f), 1e-5f)
        assertEquals(back, phiOf(40f), 1e-5f)
    }

    @Test
    fun noCardPopsInOrOutAsTheWheelSettles() {
        // Settling creeps onto a card from either side and stops on it. A card that is drawn on
        // one side of that and not the other must lie where a card drawn on both sides lies, so
        // nothing shows up or goes as the wheel comes to rest.
        fun slots(p: Float) = wheelSlots(p, 40, -1).associateBy { it.i }
        for (n in listOf(12, 20)) {
            val rest = slots(n.toFloat())
            for (near in listOf(n - 1e-3f, n + 1e-3f)) {
                val moving = slots(near)
                val both = rest.keys intersect moving.keys
                for (frame in listOf(rest, moving)) {
                    for ((i, slot) in frame) {
                        if (i in both) continue
                        assertTrue(
                            both.any { abs(frame.getValue(it).phi - slot.phi) < .01f },
                            "card $i near $near shows on its own",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun aSettledWheelShowsThePrototypesCards() {
        // At rest, seven fallen cards in front and fifteen behind.
        assertEquals((13..35).toList(), wheelSlots(20f, 40, -1).map { it.i })
        assertFalse(wheelSlots(20f, 40, 21).any { it.i == 21 })
    }

    @Test
    fun theStageLaysOutAsThePrototypeDoes() {
        val s = CardFileScene(cards)
        s.layout(390f, 700f, 2f)
        // avail = 700 - 24 - 104 - 16 = 556; k = min(390 × .41, 556 / 3, 190).
        assertEquals(159.9f, s.k, .01f)
        assertEquals(195f, s.cx, .01f)
        assertEquals(24f + 1.45f * 159.9f + (556f - 3f * 159.9f) * .52f, s.cy, .01f)
        assertEquals(.17f * 159.9f, s.pitch, .01f)
        assertEquals(351f, s.target.w, .01f)
        assertEquals((CH + TH) * 351f / CW, s.target.h, .01f)
        assertEquals(s.cy - .05f * s.k, s.target.cy, .01f)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun painted(
        position: Float,
        pulled: Int = -1,
        q: Float = 0f,
        check: (CardFileScene) -> Unit,
    ) = runComposeUiTest {
        var measurer: TextMeasurer? = null
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        val s = CardFileScene(cards)
        s.ink =
            CardInk(
                checkNotNull(measurer),
                mapOf("TYPED" to FontFamily.Monospace, "HAND" to FontFamily.Cursive),
                makeGrain(),
            )
        s.layout(390f, 700f, 1f)
        val g = Canvas2D.create(390, 700)
        s.paint(g, position, pulled, q, reduced = false)
        check(s)
    }

    @Test
    fun aTapOnTheFrontCardFindsIt() =
        painted(4f) { s ->
            // Half way up the front card's body.
            val y = s.proj(R0 + CH * .5f, phiOf(0f)).y
            assertEquals(4, s.cardAt(s.cx, y))
            assertEquals(-1, s.cardAt(5f, 5f), "nothing at the corner of the room")
        }

    @Test
    fun aDividersTabPeeksOverTheCardsInFront() =
        painted(1f) { s ->
            // The B divider stands two behind Abby; its tab is the second step along.
            val phi = phiOf(2f)
            val top = s.proj(R0 + CH + TH, phi)
            val body = s.proj(R0 + CH, phi)
            val x = s.cx + (tabX(1) + .15f - CW / 2) * s.k * top.s
            val y = (top.y + body.y) / 2
            assertEquals(3, s.cardAt(x, y))
        }

    @Test
    fun theKnobsAreFoundBesideTheCards() =
        painted(4f) { s ->
            val (kx, ky) = s.p3(CW / 2 + .2f, 0f, 0f)
            assertTrue(s.knobAt(kx, ky), "the right knob")
            assertTrue(s.knobAt(2 * s.cx - kx, ky), "the left knob")
            assertFalse(s.knobAt(s.cx, s.cy), "the middle of the rod")
        }

    @Test
    fun aPulledCardIsFoundWhereItHangs() =
        painted(4f, pulled = 4, q = 1f) { s ->
            assertTrue(s.inPulled(s.target.cx, s.target.cy + s.target.h * .1f))
            assertFalse(s.inPulled(s.target.cx, s.target.cy + s.target.h))
        }

    @Test
    fun theTexturesAreKeptWithinReach() =
        painted(4f) { s ->
            assertTrue(
                s.cachedFaces in 1..cards.size,
                "faces baked for the cards drawn: ${s.cachedFaces}",
            )
        }
}
