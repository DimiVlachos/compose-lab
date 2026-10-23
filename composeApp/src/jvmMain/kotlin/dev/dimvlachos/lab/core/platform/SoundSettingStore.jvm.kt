package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// The JVM target only runs the tests: the switch is kept as long as the test runs.
@Composable
internal actual fun rememberSoundSettingStore(): SoundSettingStore = remember {
    InMemorySoundSettingStore()
}
