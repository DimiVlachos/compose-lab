package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer = SilentSoundPlayer
