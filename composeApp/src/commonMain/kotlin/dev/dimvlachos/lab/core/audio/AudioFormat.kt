package dev.dimvlachos.lab.core.audio

/** Microphone audio as the blow detector hears it: mono, at this many samples a second. */
const val MicSampleRate = 16_000

/** Samples in each frame the detector takes, about 32 ms. */
const val MicFrameSize = 512

/** How long one frame lasts. */
const val MicFrameSeconds: Float = MicFrameSize / MicSampleRate.toFloat()
