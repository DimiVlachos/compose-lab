package dev.dimvlachos.lab.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoundGateTest {
    private class Recorder : SoundPlayer {
        val played = mutableListOf<Sound>()
        val volumes = mutableListOf<Float>()
        var stopped = false

        override fun play(sound: Sound, volume: Float, rate: Float) {
            played += sound
        }

        override fun loop(sound: Sound, volume: Float): SoundLoop {
            volumes += volume
            return object : SoundLoop {
                override fun setVolume(volume: Float) {
                    volumes += volume
                }

                override fun stop() {
                    stopped = true
                }
            }
        }
    }

    @Test
    fun whileOffNothingPlaysAndALoopKeepsQuiet() {
        val inner = Recorder()
        val gated = GatedSoundPlayer(inner, on = false)
        gated.play(Sound.Bell)
        assertTrue(inner.played.isEmpty())
        val loop = gated.loop(Sound.BurnLoop, .7f)
        assertEquals(listOf(0f), inner.volumes)
        loop.setVolume(.4f)
        assertEquals(0f, inner.volumes.last(), "a volume set while off waits for sound")
    }

    @Test
    fun turnedOnTheLoopComesBackAtItsVolumeAndOneShotsPlay() {
        val inner = Recorder()
        val gated = GatedSoundPlayer(inner, on = false)
        val loop = gated.loop(Sound.BurnLoop, .7f)
        loop.setVolume(.4f)
        gated.on = true
        assertEquals(.4f, inner.volumes.last())
        gated.play(Sound.Bell)
        assertEquals(listOf(Sound.Bell), inner.played)
        gated.on = false
        assertEquals(0f, inner.volumes.last(), "and quiet again the moment it is turned off")
        loop.stop()
        assertTrue(inner.stopped)
    }

    @Test
    fun theStoreKeepsWhatWasSavedAndStartsOff() {
        val store = InMemorySoundSettingStore()
        assertEquals(false, store.load())
        val setting = SoundSetting(store)
        setting.toggle()
        assertTrue(setting.on)
        assertTrue(store.load())
        assertEquals(true, SoundSetting(store).on, "a new run starts from what was saved")
    }
}
