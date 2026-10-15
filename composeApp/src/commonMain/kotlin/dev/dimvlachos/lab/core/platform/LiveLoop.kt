package dev.dimvlachos.lab.core.platform

/** What a [LiveLoop] drives: a platform voice playing one clip over and over. */
internal interface LoopVoice {
    /** Starts the clip from the top, at [volume]. */
    fun start(volume: Float)

    fun setVolume(volume: Float)

    fun pause()

    fun resume()

    fun stop()
}

/**
 * A [SoundLoop] that is live from the moment it is asked for, whether or not it can sound yet. It
 * remembers its volume, starts its [voice] once the clip is [ready] and the phone is [audible],
 * pauses it while the phone is kept quiet and resumes it after. [onStop] lets the player forget it.
 */
internal class LiveLoop(
    private val voice: LoopVoice,
    volume: Float,
    private val onStop: (LiveLoop) -> Unit = {},
) : SoundLoop {
    private var volume = volume.coerceIn(0f, 1f)
    private var started = false
    private var stopped = false

    /** Whether the voice is playing, as far as the loop knows. */
    var sounding = false
        private set

    /** Whether the clip has loaded. */
    var ready = false
        set(value) {
            field = value
            update()
        }

    /** Whether the phone may make sounds: not set to silent or vibrate. */
    var audible = true
        set(value) {
            field = value
            update()
        }

    private fun update() {
        if (stopped) return
        val wanted = ready && audible
        when {
            wanted && !started -> {
                voice.start(volume)
                started = true
                sounding = true
            }
            wanted && !sounding -> {
                voice.resume()
                sounding = true
            }
            !wanted && sounding -> {
                voice.pause()
                sounding = false
            }
        }
    }

    /** Starts the clip over if the loop is sounding, after the platform has stopped its voice. */
    fun restart() {
        if (!stopped && sounding) voice.start(volume)
    }

    override fun setVolume(volume: Float) {
        if (stopped) return
        this.volume = volume.coerceIn(0f, 1f)
        if (started) voice.setVolume(this.volume)
    }

    override fun stop() {
        if (stopped) return
        stopped = true
        sounding = false
        if (started) voice.stop()
        onStop(this)
    }
}
