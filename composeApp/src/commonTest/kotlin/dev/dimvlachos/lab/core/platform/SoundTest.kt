package dev.dimvlachos.lab.core.platform

import dev.dimvlachos.lab.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
            isStaleSoundCopy(Sound.Strike1, soundCacheName(Sound.Strike2, byteArrayOf(1)), current),
        )
    }

    @Test
    fun everySoundDecodesToSamples() = runTest {
        for (sound in Sound.entries) {
            val clip = decodeWave(Res.readBytes(sound.path))

            assertEquals(1, clip.channels, sound.file)
            assertTrue(clip.sampleRate in 8000..48000, "${sound.file} at ${clip.sampleRate} Hz")
            assertTrue(clip.frames > 0, sound.file)
            assertTrue(clip.samples.all { it in -1f..1f }, sound.file)
        }
    }

    @Test
    fun aWaveFilesSamplesDecodePastOtherChunks() {
        val bytes =
            wave(
                channels = 2,
                bits = 16,
                extra = "LIST" to 3,
                samples = shortArrayOf(0, 16384, -32768, 32767),
            )

        val clip = decodeWave(bytes)

        assertEquals(22050, clip.sampleRate)
        assertEquals(2, clip.channels)
        assertEquals(2, clip.frames)
        assertEquals(listOf(0f, 0.5f, -1f, 32767 / 32768f), clip.samples.toList())
    }

    @Test
    fun onlySixteenBitPcmDecodes() {
        assertFailsWith<IllegalArgumentException> { decodeWave(wave(bits = 8)) }
        assertFailsWith<IllegalArgumentException> { decodeWave("RIFF....WAVX".encodeToByteArray()) }
    }

    /** A WAVE file at 22050 Hz, with an [extra] chunk of a given size before the data. */
    private fun wave(
        channels: Int = 1,
        bits: Int,
        extra: Pair<String, Int>? = null,
        samples: ShortArray = shortArrayOf(1, 2),
    ): ByteArray {
        val out = mutableListOf<Byte>()
        fun text(s: String) = s.encodeToByteArray().forEach { out += it }
        fun int16(v: Int) = repeat(2) { out += (v shr (8 * it)).toByte() }
        fun int32(v: Int) = repeat(4) { out += (v shr (8 * it)).toByte() }
        text("RIFF")
        int32(0)
        text("WAVE")
        text("fmt ")
        int32(16)
        int16(1)
        int16(channels)
        int32(22050)
        int32(22050 * channels * bits / 8)
        int16(channels * bits / 8)
        int16(bits)
        if (extra != null) {
            text(extra.first)
            int32(extra.second)
            repeat(extra.second + (extra.second and 1)) { out += 7 }
        }
        text("data")
        int32(samples.size * 2)
        samples.forEach { int16(it.toInt()) }
        return out.toByteArray()
    }

    private companion object {
        const val MaxClipBytes = 160 * 1024
    }
}
