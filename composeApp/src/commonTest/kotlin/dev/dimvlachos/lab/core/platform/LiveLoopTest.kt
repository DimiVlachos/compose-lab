package dev.dimvlachos.lab.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveLoopTest {
    private val voice = RecordingVoice()

    @Test
    fun aLoopAskedForBeforeItsClipLoadedStartsOnceItHas() {
        val loop = LiveLoop(voice, volume = 0.4f)
        loop.setVolume(0.7f)

        assertEquals(emptyList(), voice.calls)
        loop.ready = true

        assertEquals(listOf("start 0.7"), voice.calls)
        assertTrue(loop.sounding)
    }

    @Test
    fun aLoopAskedForOnSilentStartsWhenTheRingerComesBack() {
        val loop = LiveLoop(voice, volume = 1f)
        loop.audible = false
        loop.ready = true

        assertEquals(emptyList(), voice.calls)
        loop.audible = true

        assertEquals(listOf("start 1.0"), voice.calls)
    }

    @Test
    fun aSoundingLoopPausesOnSilentAndResumesAfter() {
        val loop = LiveLoop(voice, volume = 1f)
        loop.ready = true
        loop.audible = false
        loop.audible = false
        loop.audible = true

        assertEquals(listOf("start 1.0", "pause", "resume"), voice.calls)
    }

    @Test
    fun aVolumeSetWhilePausedReachesTheVoice() {
        val loop = LiveLoop(voice, volume = 1f)
        loop.ready = true
        loop.audible = false
        loop.setVolume(2f)

        assertEquals(listOf("start 1.0", "pause", "volume 1.0"), voice.calls)
    }

    @Test
    fun aStoppedLoopStaysStopped() {
        var forgotten: LiveLoop? = null
        val loop = LiveLoop(voice, volume = 1f, onStop = { forgotten = it })
        loop.ready = true
        loop.stop()
        loop.stop()
        loop.audible = false
        loop.audible = true
        loop.setVolume(0.5f)
        loop.restart()

        assertEquals(listOf("start 1.0", "stop"), voice.calls)
        assertEquals(loop, forgotten)
        assertFalse(loop.sounding)
    }

    @Test
    fun aLoopStoppedBeforeItStartedNeverTouchesTheVoice() {
        val loop = LiveLoop(voice, volume = 1f)
        loop.stop()
        loop.ready = true

        assertEquals(emptyList(), voice.calls)
    }

    @Test
    fun onlyASoundingLoopRestarts() {
        val loop = LiveLoop(voice, volume = 0.5f)
        loop.restart()
        loop.ready = true
        loop.restart()
        loop.audible = false
        loop.restart()

        assertEquals(listOf("start 0.5", "start 0.5", "pause"), voice.calls)
    }

    private class RecordingVoice : LoopVoice {
        val calls = mutableListOf<String>()

        override fun start(volume: Float) {
            calls += "start $volume"
        }

        override fun setVolume(volume: Float) {
            calls += "volume $volume"
        }

        override fun pause() {
            calls += "pause"
        }

        override fun resume() {
            calls += "resume"
        }

        override fun stop() {
            calls += "stop"
        }
    }
}
