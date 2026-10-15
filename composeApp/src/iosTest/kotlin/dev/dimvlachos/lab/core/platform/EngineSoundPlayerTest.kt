package dev.dimvlachos.lab.core.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class EngineSoundPlayerTest {
    @Test
    fun everyClipPlaysAndLoopsWithoutThrowing() = runTest {
        val player = EngineSoundPlayer()
        player.load(Sound.entries.toSet())

        for (sound in Sound.entries) {
            player.play(sound, volume = 0f, rate = 0.5f)
            player.play(sound, volume = 0f, rate = 2f)
        }
        val loop = player.loop(Sound.BurnLoop, volume = 0f)
        loop.setVolume(0f)
        loop.stop()
        loop.stop()
        player.release()
        player.play(Sound.Bell, volume = 0f)
        player.loop(Sound.BurnLoop, volume = 0f).stop()
        player.release()
    }

    @Test
    fun aLoopAskedForBeforeItsClipLoadedSoundsOnceItHas() = runTest {
        val player = EngineSoundPlayer()
        val loop = player.loop(Sound.BurnLoop, volume = 0f) as LiveLoop

        assertFalse(loop.sounding)
        player.load(setOf(Sound.BurnLoop))

        assertTrue(loop.sounding)
        player.release()
        assertFalse(loop.sounding)
    }
}
