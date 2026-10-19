package dev.dimvlachos.lab.core.presentation.components.snowglobe

// The JVM only runs the tests, with no GPU: Skia would run the glass shader a pixel at a time in
// its interpreter, about 100 ms a frame, so the globe draws its plain glass, as where shaders
// can't compile.
internal actual fun compileGlassShader(): GlassShader? = null
