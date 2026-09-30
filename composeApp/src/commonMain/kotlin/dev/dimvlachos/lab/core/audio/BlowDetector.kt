package dev.dimvlachos.lab.core.audio

import kotlin.math.log10
import kotlin.math.sqrt

// How far above the room a frame must be to count, in dB, and how much further a firm blow goes.
private const val MarginDb = 15f
private const val StrengthRangeDb = 25f

// Below this a frame is too quiet to be a blow, however quiet the room. Measured on a phone: blows
// sit around -12 dBFS, while the frames of talking that pass the other checks sit around -25.
private const val MinLevelDb = -22f

// A voice, a hum or music repeats at its pitch; a blow's turbulent air does not.
private const val MaxPeriodicity = 0.5f

// About 250 ms to start and 130 ms to stop, so a breath does not flicker; past about 4 s, a steady
// "blow" is the room, a fan or a vacuum, and becomes the new background. Measured on a phone:
// talking and room noise pass every other check only in runs of up to 3 frames, a blow runs 30 and
// more.
private const val OnsetFrames = 8
private const val ReleaseFrames = 4
private const val MaxBlowFrames = 125

// How quickly the background follows the room, per frame.
private const val BackgroundFollow = 0.05f

// A blow too faint to see is still a blow: the fog always moves.
private const val MinStrength = 0.3f

/**
 * Hears a blow on the microphone: loud sound well above the room's own level that does not repeat
 * at a pitch, as a voice or music does, for long enough to be a breath. Feed it
 * [MicFrameSize]-sample frames in order; each returns the blow's strength, from 0, no blow, to 1, a
 * firm one.
 */
class BlowDetector(private val sampleRate: Int = MicSampleRate) {
    private var background = Float.NaN
    private var candidateFrames = 0
    private var quietFrames = 0
    private var loudFrames = 0
    private var strength = 0f

    fun process(frame: FloatArray): Float {
        // Digital silence is a recorder starting up, not the room: it would pin the room's level at
        // the floor, where the room itself would then count as loud and never be learned.
        if (frame.all { it == 0f }) return strength
        val level = loudnessDb(frame)
        if (background.isNaN()) background = level
        val margin = level - background
        val loud = margin >= MarginDb
        // Loud for long enough, whatever the other checks said frame by frame, is the room: a fan
        // or
        // a vacuum that now and then fails a check must not start a fresh blow each time.
        loudFrames = if (loud) loudFrames + 1 else 0
        if (loudFrames > MaxBlowFrames) {
            background = level
            loudFrames = 0
            stop()
            return strength
        }
        val blowing =
            loud && level >= MinLevelDb && periodicity(frame, sampleRate) <= MaxPeriodicity
        if (strength > 0f) {
            if (blowing) {
                quietFrames = 0
                strength = strengthFor(margin)
            } else if (++quietFrames >= ReleaseFrames) {
                stop()
            }
        } else if (blowing) {
            if (++candidateFrames >= OnsetFrames) strength = strengthFor(margin)
        } else {
            candidateFrames = 0
            // Only quiet frames are the room: a loud sound that is no blow must not raise it.
            if (!loud) background += (level - background) * BackgroundFollow
        }
        return strength
    }

    private fun stop() {
        strength = 0f
        candidateFrames = 0
        quietFrames = 0
    }

    private fun strengthFor(margin: Float) =
        ((margin - MarginDb) / StrengthRangeDb).coerceIn(MinStrength, 1f)
}

private fun loudnessDb(frame: FloatArray): Float {
    var sum = 0.0
    for (sample in frame) sum += sample * sample
    val rms = sqrt(sum / frame.size)
    return (20 * log10(rms.coerceAtLeast(1e-9))).toFloat().coerceAtLeast(-90f)
}
