package dev.dimvlachos.lab.magnetdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetDimens
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetPhoto
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetState
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The island table on a phone the size of the S23's, at its density.
private const val Density = 3.75f
private const val Width = 412f
private const val Height = 780f

/**
 * A magnet is a search: wherever on the table it is put down, it finds every photo that matches its
 * tag, and only those. Two apart are two searches; snapped together, one for both.
 */
class IslandSearchTest {
    private val photos = IslandTags.photos.map { MagnetPhoto(it.id, it.image, it.id, it.strengths) }
    private val tags = IslandTags.tags.map { MagnetTag(it.id, it.id) }

    private fun matches(tag: String): Set<String> =
        IslandTags.photos
            .filter { (it.strengths[tag] ?: 0f) >= MagnetDimens.StickThreshold }
            .map { it.id }
            .toSet()

    private fun table() =
        MagnetState(photos, tags).apply {
            layOut((Width * Density).toInt(), (Height * Density).toInt(), Density)
        }

    private fun MagnetState.run(seconds: Float) {
        repeat((seconds * 60).toInt()) { advance(1f / 60f) }
    }

    // Carries [tag]'s magnet from where it is to [to], in dp, over half a second, and lets go.
    private fun MagnetState.carry(tag: String, to: Offset) {
        val from = magnetPosition(tag)!!
        place(tag, from)
        for (k in 1..30) {
            drag(tag, from + (to * Density - from) * (k / 30f))
            advance(1f / 60f)
        }
        release(tag, Offset.Zero)
    }

    // The middle, the corners and the edges of the table.
    private val spots =
        listOf(
            Offset(206f, 340f),
            Offset(60f, 60f),
            Offset(352f, 60f),
            Offset(60f, 620f),
            Offset(352f, 620f),
            Offset(206f, 60f),
            Offset(30f, 340f),
        )

    @Test
    fun aMagnetPutDownAnywhereFindsEveryPhotoOfItsTagAndOnlyThose() {
        for (tag in IslandTags.tags.map { it.id }) {
            for (spot in spots) {
                val table = table()
                table.carry(tag, spot)
                table.run(4f)
                assertEquals(matches(tag), table.results[tag], "$tag put down at $spot")
            }
        }
    }

    @Test
    fun twoMagnetsApartFindEitherTagsPhotosEachOnlyOnItsOwn() {
        val ids = IslandTags.tags.map { it.id }
        for (a in ids) for (b in ids) {
            if (a == b) continue
            val table = table()
            table.carry(a, Offset(90f, 150f))
            table.run(1f)
            table.carry(b, Offset(320f, 560f))
            table.run(4f)
            val onA = table.results[a].orEmpty()
            val onB = table.results[b].orEmpty()
            assertEquals(matches(a) + matches(b), onA + onB, "$a and $b apart")
            assertTrue((onA intersect onB).isEmpty(), "$a and $b share ${onA intersect onB}")
            assertTrue(onA.all { it in matches(a) } && onB.all { it in matches(b) }, "$a $b")
        }
    }

    @Test
    fun twoMagnetsSnappedTogetherFindOnlyThePhotosOfBoth() {
        val ids = IslandTags.tags.map { it.id }
        for (a in ids) for (b in ids) {
            if (a == b) continue
            val table = table()
            table.carry(a, Offset(206f, 340f))
            table.run(1f)
            table.carry(b, Offset(250f, 360f))
            table.run(4f)
            val both = matches(a) intersect matches(b)
            assertEquals(both, table.results[a].orEmpty(), "$a with $b")
            assertEquals(both, table.results[b].orEmpty(), "$b with $a")
        }
    }
}
