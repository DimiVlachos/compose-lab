package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertNotNull

class GlassEffectIosTest {
    @Test
    fun skiaCompilesTheGlass() {
        assertNotNull(glassRenderEffect(Offset(200f, 200f), 180f))
    }
}
