package dev.dimvlachos.lab.core.camera

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MirrorCameraIosTest {
    @Test
    fun iosHasNoMirrorCameraYet() = runComposeUiTest {
        var access: CameraAccess? = null
        setContent { access = rememberCameraAccess(enabled = true) }
        waitForIdle()
        assertEquals(CameraAccess.Unavailable, access)
    }
}
