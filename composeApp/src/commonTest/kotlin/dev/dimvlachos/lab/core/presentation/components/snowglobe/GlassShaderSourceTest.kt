package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.test.Test
import kotlin.test.assertTrue

class GlassShaderSourceTest {
    @Test
    fun itDeclaresWhatBothPlatformsBind() {
        for (name in
            listOf(
                "uniform shader content;",
                "uniform float2 center;",
                "uniform float radius;",
                "half4 main(float2 p)",
            )) {
            assertTrue(name in GlassShaderSource, "missing $name")
        }
    }
}
