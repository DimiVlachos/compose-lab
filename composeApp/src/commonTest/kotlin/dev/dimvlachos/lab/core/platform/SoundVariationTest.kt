package dev.dimvlachos.lab.core.platform

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class SoundVariationTest {
    private class Recorder : SoundPlayer {
        val plays = mutableListOf<Pair<Float, Float>>()

        override fun play(sound: Sound, volume: Float, rate: Float) {
            plays += volume to rate
        }

        override fun loop(sound: Sound, volume: Float): SoundLoop = error("not used")
    }

    @Test
    fun eachPlayVariesALittleAroundWhatWasAskedFor() {
        val player = Recorder()
        val random = Random(7)
        repeat(200) { player.playVaried(Sound.Bell, .8f, 1.2f, random) }
        assertTrue(player.plays.all { (v, r) -> v in .72f..0.8f && r in 1.152f..1.248f })
        assertTrue(
            player.plays.map { it.second }.distinct().size > 100,
            "the rate changes from play to play",
        )
    }

    @Test
    fun theVariedRateStaysInTheRangePlayAccepts() {
        val player = Recorder()
        val random = Random(1)
        repeat(100) {
            player.playVaried(Sound.Bell, 1f, .5f, random)
            player.playVaried(Sound.Bell, 1f, 2f, random)
        }
        assertTrue(player.plays.all { (_, r) -> r in .5f..2f })
    }
}
