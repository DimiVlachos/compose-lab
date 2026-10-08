package dev.dimvlachos.lab.magnetdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetPhoto
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetState
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetTag
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The island table on a phone the size of the S23's, at its density.
private const val Density = 3.75f
private const val Width = 412f
private const val Height = 780f

class IslandTableSettlesTest {
    private val photos = IslandTags.photos.map { MagnetPhoto(it.id, it.image, it.id, it.strengths) }
    private val tags = IslandTags.tags.map { MagnetTag(it.id, it.id) }

    private fun MagnetState.run(seconds: Float) {
        repeat((seconds * 60).toInt()) { advance(1f / 60f) }
    }

    // Carries [tag]'s magnet from where it is to [to], in px, over [frames] frames, and lets go.
    private fun MagnetState.carry(tag: String, to: Offset, frames: Int) {
        val from = magnetPosition(tag)!!
        place(tag, from)
        for (k in 1..frames) {
            drag(tag, from + (to - from) * (k.toFloat() / frames))
            advance(1f / 60f)
        }
        release(tag, Offset.Zero)
    }

    @Test
    fun afterAnyFewMovesOfOneOrTwoMagnetsEverythingComesToRest() {
        // Random ones, and the ones that once left a photo kicking for ever: one slid home under a
        // cluster lying over its spot, was shoved out again, and slipped back, again and again.
        for (seed in listOf(16, 32, 44, 59) + (0 until 40)) {
            val random = Random(seed)
            val table = MagnetState(photos, tags)
            table.layOut((Width * Density).toInt(), (Height * Density).toInt(), Density)
            val chosen = tags.shuffled(random).take(1 + random.nextInt(2)).map { it.id }
            for (tag in chosen) {
                val to = Offset(random.nextFloat() * Width, random.nextFloat() * 600f) * Density
                table.carry(tag, to, frames = 20 + random.nextInt(60))
                table.run(0.5f)
            }
            val last = chosen.last()
            val nudge = Offset(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f) * 400f
            table.carry(last, table.magnetPosition(last)!! + nudge, frames = 30)
            table.run(10f)
            assertFalse(table.awake, "seed $seed: the table never comes to rest")
            val before = photos.map { table.photoPosition(it.id)!! }
            table.run(2f)
            photos.forEachIndexed { i, photo ->
                val moved = (table.photoPosition(photo.id)!! - before[i]).getDistance() / Density
                assertTrue(moved < 0.5f, "seed $seed: ${photo.id} moved $moved dp at rest")
            }
        }
    }
}
