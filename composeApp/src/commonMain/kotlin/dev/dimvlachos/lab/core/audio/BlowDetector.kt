package dev.dimvlachos.lab.core.audio

import kotlin.math.log10
import kotlin.math.sqrt

// How far above the room a frame must be to count, in dB, and how much further a firm blow goes.
private const val MarginDb = 15f
private const val StrengthRangeDb = 25f

// Below this a frame is too quiet to be a blow, however quiet the room.
private const val MinLevelDb = -50f

// Breath is hiss; a voice or music is tones.
private const val MinFlatness = 0.35f

// About 100 ms to start and 130 ms to stop, so a breath does not flicker; past about 4 s, a steady
// "blow" is the room, a fan or a vacuum, and becomes the new background.
private const val OnsetFrames = 3
private const val ReleaseFrames = 4
private const val MaxBlowFrames = 125

// How quickly the background follows the room, per frame.
private const val BackgroundFollow = 0.05f

// A blow too faint to see is still a blow: the fog always moves.
private const val MinStrength = 0.3f

/**
 * Hears a blow on the microphone: sound well above the room's own level, noise-like rather than
 * tonal, for long enough to be a breath. Feed it [MicFrameSize]-sample frames in order; each
 * returns the blow's strength, from 0, no blow, to 1, a firm one.
 */
class BlowDetector(private val sampleRate: Int = MicSampleRate) {
    private var background = Float.NaN
    private var candidateFrames = 0
    private var blowFrames = 0
    private var quietFrames = 0
    private var strength = 0f

    fun process(frame: FloatArray): Float {
        val level = loudnessDb(frame)
        if (background.isNaN()) background = level
        val margin = level - background
        val blowing =
            level >= MinLevelDb &&
                margin >= MarginDb &&
                spectralFlatness(powerSpectrum(frame), sampleRate, 100f, 4_000f) >= MinFlatness
        if (strength > 0f) {
            if (blowing) {
                quietFrames = 0
                blowFrames++
                strength = strengthFor(margin)
                if (blowFrames > MaxBlowFrames) {
                    background = level
                    stop()
                }
            } else if (++quietFrames >= ReleaseFrames) {
                stop()
            }
        } else if (blowing) {
            if (++candidateFrames >= OnsetFrames) {
                blowFrames = candidateFrames
                strength = strengthFor(margin)
            }
        } else {
            candidateFrames = 0
            background += (level - background) * BackgroundFollow
        }
        return strength
    }

    private fun stop() {
        strength = 0f
        candidateFrames = 0
        blowFrames = 0
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
