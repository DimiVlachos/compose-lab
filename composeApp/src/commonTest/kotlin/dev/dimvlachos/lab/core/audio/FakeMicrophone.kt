package dev.dimvlachos.lab.core.audio

import kotlinx.coroutines.flow.Flow

internal class FakeMicrophone(override val frames: Flow<FloatArray>) : Microphone
