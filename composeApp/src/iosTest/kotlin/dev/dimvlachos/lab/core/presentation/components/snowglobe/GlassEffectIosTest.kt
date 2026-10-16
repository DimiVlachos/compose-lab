package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.test.Test
import kotlin.test.assertNotNull

class GlassEffectIosTest {
    @Test
    fun skiaCompilesTheGlass() {
        assertNotNull(compileGlassShader())
    }
}
