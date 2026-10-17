package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
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

    // A 40-card wheel with a divider every fourth card, its tab in slot i / 4 % 5.
    private fun tabEvery4(i: Int): Int? = if (i % 4 == 0) i / 4 % 5 else null

    @Test
    fun noCardPopsInOrOutAsTheWheelSettles() {
        // Settling creeps onto a card from either side and stops on it. A card drawn on one side
        // of that and not the other must lie where a card drawn on both lies, and a divider where
        // a divider with its tab in the same place lies, so nothing shows up or goes.
        fun slots(p: Float) = wheelSlots(p, 40, -1, ::tabEvery4).associateBy { it.i }
        for (n in listOf(12, 20, 27)) {
            val rest = slots(n.toFloat())
            for (near in listOf(n - 1e-3f, n + 1e-3f)) {
                val moving = slots(near)
                val both = rest.keys intersect moving.keys
                for (frame in listOf(rest, moving)) {
                    for ((i, slot) in frame) {
                        if (i in both) continue
                        val tab = tabEvery4(i)
                        assertTrue(
                            both.any {
                                abs(frame.getValue(it).phi - slot.phi) < .01f &&
                                    (tab == null || tabEvery4(it) == tab)
                            },
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
        assertEquals((13..35).toSet(), wheelSlots(20f, 40, -1).map { it.i }.toSet())
        assertFalse(wheelSlots(20f, 40, 21).any { it.i == 21 })
    }

    @Test
    fun aBuriedDividersTabStillShows() {
        // Deeper than the pile's ends a card lies hidden, all but a divider's tab: the nearest
        // divider with its tab in each place is still drawn there, and no deeper one.
        fun tabs(i: Int): Int? =
            when (i) {
                4,
                9 -> 0
                6 -> 1
                38,
                39 -> 2
                else -> null
            }
        val buried = wheelSlots(20f, 40, -1, ::tabs).filter { it.buried }.map { it.i }.toSet()
        assertEquals(setOf(9, 6, 38), buried)
    }

    @Test
    fun aWheelPastItsDeckOnlyAsksAfterItsCards() {
        // A deck can shrink under the wheel, leaving it past either end for a moment.
        val tabs = List(10) { it % 5 }
        for (p in listOf(20f, -30f)) {
            val asked = wheelSlots(p, 10, -1) { tabs[it] }.map { it.i }
            assertTrue(asked.all { it in 0 until 10 }, "at $p")
        }
    }

    @Test
    fun cardsLyingTogetherPaintTheNearerLast() {
        // Card 35 is the last shown behind; the divider at 38 lies beneath it, so is painted first.
        val order = wheelSlots(20f, 40, -1) { if (it == 38) 2 else null }.map { it.i }
        assertTrue(38 in order && order.indexOf(38) < order.indexOf(35))
        // In front, the buried divider at 9 lies beneath card 13.
        val front = wheelSlots(20f, 40, -1) { if (it == 9) 0 else null }.map { it.i }
        assertTrue(9 in front && front.indexOf(9) < front.indexOf(13))
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
        check: (CardFileScene, Canvas2D) -> Unit,
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
        check(s, g)
    }

    @Test
    fun aTapOnTheFrontCardFindsIt() =
        painted(4f) { s, _ ->
            // Half way up the front card's body.
            val y = s.proj(R0 + CH * .5f, phiOf(0f)).y
            assertEquals(4, s.cardAt(s.cx, y))
            assertEquals(-1, s.cardAt(5f, 5f), "nothing at the corner of the room")
        }

    @Test
    fun aDividersTabPeeksOverTheCardsInFront() =
        painted(1f) { s, _ ->
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
        painted(4f) { s, _ ->
            val (kx, ky) = s.p3(CW / 2 + .2f, 0f, 0f)
            assertTrue(s.knobAt(kx, ky), "the right knob")
            assertTrue(s.knobAt(2 * s.cx - kx, ky), "the left knob")
            assertFalse(s.knobAt(s.cx, s.cy), "the middle of the rod")
        }

    @Test
    fun aPulledCardIsFoundWhereItHangs() =
        painted(4f, pulled = 4, q = 1f) { s, _ ->
            assertTrue(s.inPulled(s.target.cx, s.target.cy + s.target.h * .1f))
            assertFalse(s.inPulled(s.target.cx, s.target.cy + s.target.h))
        }

    @Test
    fun aPulledCardsHolesShowWhatIsBehindIt() =
        painted(4f, pulled = 4, q = 1f) { s, g ->
            // The left of the two notches at the card's foot, the card hanging face on.
            val t = s.target
            val x = t.cx - t.w / 2 + t.w * .34f
            val y = t.cy + t.h / 2 - t.h * .085f / (CH + TH)
            val c = g.bitmap.toPixelMap()[x.toInt(), y.toInt()]
            val paper = listOf(233, 223, 201)
            val rgb = listOf(c.red, c.green, c.blue).map { (it * 255).toInt() }
            val off = rgb.zip(paper).sumOf { (a, b) -> abs(a - b) }
            assertTrue(off > 90, "the hole shows paper, $rgb")
        }

    @Test
    fun aCardsNotchesShowWhatIsBehindItHoweverItIsShaded() {
        // The front card's left notch, shaded by the lamp on the wheel, and the same card unshaded
        // as a pulled card still in its rail: what lies behind is the same, so the notch must be,
        // to the level. The dark wood behind it shows a shade over the hole as only a level or two.
        fun notch(s: CardFileScene): Pair<Int, Int> {
            val p = s.proj(R0 + .085f, phiOf(0f))
            return (s.cx + (CW * .34f - CW / 2) * s.k * p.s).toInt() to p.y.toInt()
        }
        fun rgb(m: PixelMap, x: Int, y: Int) =
            (-2..2 step 2).flatMap { dy ->
                val c = m[x, y + dy]
                listOf(c.red, c.green, c.blue).map { (it * 255).toInt() }
            }
        var shaded = emptyList<Int>()
        var plain = emptyList<Int>()
        painted(4f) { s, g ->
            val (x, y) = notch(s)
            shaded = rgb(g.bitmap.toPixelMap(), x, y)
        }
        painted(4f, pulled = 4, q = 0f) { s, g ->
            val (x, y) = notch(s)
            plain = rgb(g.bitmap.toPixelMap(), x, y)
        }
        assertTrue(
            shaded.zip(plain).all { (a, b) -> abs(a - b) <= 1 },
            "the notch shows $shaded, behind the card $plain",
        )
    }

    @Test
    fun theTexturesAreKeptWithinReach() =
        painted(4f) { s, _ ->
            assertTrue(
                s.cachedFaces in 1..cards.size,
                "faces baked for the cards drawn: ${s.cachedFaces}",
            )
        }
}
