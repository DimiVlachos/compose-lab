package dev.dimvlachos.lab.core.platform

import dev.dimvlachos.lab.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SoundTest {
    @Test
    fun everySoundIsAShortWaveFileInTheResources() = runTest {
        for (sound in Sound.entries) {
            val bytes = Res.readBytes(sound.path)

            assertEquals("RIFF", bytes.decodeToString(0, 4), sound.file)
            assertEquals("WAVE", bytes.decodeToString(8, 12), sound.file)
            assertTrue(bytes.size < MaxClipBytes, "${sound.file} is ${bytes.size} bytes")
        }
    }

    @Test
    fun soundsHaveDistinctFiles() {
        assertEquals(Sound.entries.size, Sound.entries.map { it.file }.toSet().size)
    }

    @Test
    fun theSilentPlayerCanBeUsedLikeAnyOther() {
        SilentSoundPlayer.play(Sound.Bell, volume = 2f, rate = 0f)
        val loop = SilentSoundPlayer.loop(Sound.BurnLoop)
        loop.setVolume(0.5f)
        loop.stop()
    }

    private companion object {
        const val MaxClipBytes = 160 * 1024
    }
}
