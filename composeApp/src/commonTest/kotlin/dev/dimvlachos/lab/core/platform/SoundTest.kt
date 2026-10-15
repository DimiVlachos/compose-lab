package dev.dimvlachos.lab.core.platform

import dev.dimvlachos.lab.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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

    @Test
    fun aClipsCacheNameFollowsItsBytes() {
        val clip = byteArrayOf(1, 2, 3)

        val name = soundCacheName(Sound.Bell, clip)

        assertEquals(name, soundCacheName(Sound.Bell, byteArrayOf(1, 2, 3)))
        assertNotEquals(name, soundCacheName(Sound.Bell, byteArrayOf(1, 2, 4)))
        assertNotEquals(name, soundCacheName(Sound.Bell, byteArrayOf(1, 2, 3, 0)))
        assertTrue(name.startsWith("bell-") && name.endsWith(".wav"), name)
    }

    @Test
    fun onlyAClipsOwnOlderCopiesAreStale() {
        val current = soundCacheName(Sound.Strike1, byteArrayOf(1))
        val older = soundCacheName(Sound.Strike1, byteArrayOf(2))

        assertTrue(isStaleSoundCopy(Sound.Strike1, older, current))
        assertTrue(isStaleSoundCopy(Sound.Strike1, "strike_1.wav", current))
        assertFalse(isStaleSoundCopy(Sound.Strike1, current, current))
        assertFalse(isStaleSoundCopy(Sound.Strike1, "strike_1123.part", current))
        assertFalse(
            isStaleSoundCopy(Sound.Strike1, soundCacheName(Sound.Strike2, byteArrayOf(1)), current)
        )
    }

    private companion object {
        const val MaxClipBytes = 160 * 1024
    }
}
