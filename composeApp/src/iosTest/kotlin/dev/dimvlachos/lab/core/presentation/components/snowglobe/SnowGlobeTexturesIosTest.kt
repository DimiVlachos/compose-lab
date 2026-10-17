package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Skia copies a changeable bitmap into a new image every time it is drawn. The globe draws its
 * scene, base and sprites every frame, the bokeh once per near flake, so each must be frozen once
 * painted, or an iPhone copies megabytes a frame.
 */
class SnowGlobeTexturesIosTest {
    private fun frozen(image: ImageBitmap, what: String) =
        assertTrue(image.asSkiaBitmap().isImmutable, "$what is still changeable")

    @Test
    fun theSpritesAreFrozen() {
        frozen(cloudSprite.bitmap, "the cloud")
        frozen(glowSprite.bitmap, "the glow")
        frozen(bokehSprite.bitmap, "the bokeh")
    }

    @Test
    fun theScenesAreFrozen() {
        SantoriniScenes.shared.scenes.forEach { (w, image) -> frozen(image, "the $w scene") }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theBaseIsFrozen() = runComposeUiTest {
        var measurer: TextMeasurer? = null
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        val art = paintBase(100f, 1f, dark = false, checkNotNull(measurer), Density(1f))
        frozen(art.back, "the base's back")
        frozen(art.front, "the base's front")
    }
}
