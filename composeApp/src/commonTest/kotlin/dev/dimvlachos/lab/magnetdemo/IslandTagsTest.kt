package dev.dimvlachos.lab.magnetdemo

import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetDimens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IslandTagsTest {
    private val threshold = MagnetDimens.StickThreshold
    private val ids = IslandTags.tags.map { it.id }

    private fun strong(tag: String) =
        IslandTags.photos.filter { (it.strengths[tag] ?: 0f) >= threshold }.map { it.id }.toSet()

    private fun weak(tag: String) =
        IslandTags.photos.filter { (it.strengths[tag] ?: 0f) in 0.01f..(threshold - 0.01f) }

    @Test
    fun allTwelveIslandsHaveStrengthsFromNothingToAll() {
        assertEquals(12, IslandTags.photos.size)
        assertEquals(12, IslandTags.photos.map { it.id }.toSet().size)
        for (photo in IslandTags.photos) {
            assertTrue(photo.strengths.keys.all { it in ids }, "${photo.id} has an unknown tag")
            assertTrue(photo.strengths.values.all { it in 0f..1f }, photo.id)
        }
    }

    @Test
    fun eachTagPullsThreeToSixPhotosStronglyAndACoupleWeakly() {
        for (tag in ids) {
            assertTrue(strong(tag).size in 3..6, "$tag pulls ${strong(tag)}")
            assertTrue(weak(tag).size >= 2, "$tag leans ${weak(tag).map { it.id }}")
        }
    }

    @Test
    fun sunsetAndSeaAndBoatsAndVillageOverlapToShowAnd() {
        assertTrue((strong(IslandTags.Sunset) intersect strong(IslandTags.Sea)).isNotEmpty())
        assertTrue((strong(IslandTags.Boats) intersect strong(IslandTags.Village)).isNotEmpty())
    }
}
