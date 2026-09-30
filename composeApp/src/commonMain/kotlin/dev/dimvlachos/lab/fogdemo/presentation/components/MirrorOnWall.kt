package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bathroom_wall
import kotlin.math.max
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

/**
 * A mirror on a bathroom wall: the photo fills the screen, and [glass] is laid exactly over the
 * mirror's glass in it, with its rounded corners, on any screen shape.
 */
@Composable
internal fun MirrorOnWall(
    modifier: Modifier = Modifier,
    glass: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screen = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val onScreen = wallGlassOn(screen)
        val scale = max(screen.width / WallPhotoSize.width, screen.height / WallPhotoSize.height)
        val corner = with(density) { (WallGlassCorner * WallPhotoSize.width * scale).toDp() }
        Image(
            painterResource(Res.drawable.bathroom_wall),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = { _, _, _ ->
                IntOffset(-cropX(screen).roundToInt(), -cropY(screen).roundToInt())
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.offset { IntOffset(onScreen.left.roundToInt(), onScreen.top.roundToInt()) }
                .size(
                    with(density) { onScreen.width.toDp() },
                    with(density) { onScreen.height.toDp() },
                )
                .clip(RoundedCornerShape(corner))
        ) {
            glass(Modifier.fillMaxSize())
        }
    }
}

/** Where the wall photo's mirror glass lands on [screen]. */
internal fun wallGlassOn(screen: Size): Rect =
    glassOnScreen(WallPhotoSize, screen, WallGlass, WallMirror)

/**
 * Where the photo's [glass] (fractions of the photo) lands on [screen], the photo covering it: its
 * sides cropped evenly, and, on a screen wider than the photo, its top and bottom cropped about the
 * middle of the mirror's [frame], so the whole mirror shows where it fits. Kept on the screen.
 */
internal fun glassOnScreen(photo: Size, screen: Size, glass: Rect, frame: Rect): Rect {
    val scale = max(screen.width / photo.width, screen.height / photo.height)
    val x = cropX(screen, photo, scale)
    val y = cropY(screen, photo, scale, frame)
    return Rect(
        (glass.left * photo.width * scale - x).coerceIn(0f, screen.width),
        (glass.top * photo.height * scale - y).coerceIn(0f, screen.height),
        (glass.right * photo.width * scale - x).coerceIn(0f, screen.width),
        (glass.bottom * photo.height * scale - y).coerceIn(0f, screen.height),
    )
}

// How far into the scaled photo the screen starts, across and down.
private fun cropX(screen: Size, photo: Size = WallPhotoSize, scale: Float = scaleFor(screen)) =
    (photo.width * scale - screen.width) / 2

private fun cropY(
    screen: Size,
    photo: Size = WallPhotoSize,
    scale: Float = scaleFor(screen),
    frame: Rect = WallMirror,
) =
    (frame.center.y * photo.height * scale - screen.height / 2).coerceIn(
        0f,
        photo.height * scale - screen.height,
    )

private fun scaleFor(screen: Size) =
    max(screen.width / WallPhotoSize.width, screen.height / WallPhotoSize.height)

// bathroom_wall.jpg (scripts/bathroom-wall.sh): its size; where its mirror's glass is, and the
// whole mirror with its frame and outer rail, as fractions of it; and the radius of the glass's
// corners, as a fraction of the photo's width.
private val WallPhotoSize = Size(1220f, 2639f)
private val WallGlass = Rect(144f / 1220f, 462f / 2639f, 1077f / 1220f, 1719f / 2639f)
private val WallMirror = Rect(20f / 1220f, 348f / 2639f, 1190f / 1220f, 1833f / 2639f)
private const val WallGlassCorner = 0.064f
