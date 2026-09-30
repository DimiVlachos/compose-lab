package dev.dimvlachos.lab.core.audio

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MicrophoneIosTest {
    @Test
    fun iosHasNoMicrophoneYet() = runComposeUiTest {
        var access: MicAccess? = null
        setContent { access = rememberMicAccess(enabled = true) }
        waitForIdle()
        assertEquals(MicAccess.Unavailable, access)
    }
}
