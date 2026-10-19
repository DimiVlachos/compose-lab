package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.nativeCanvas

// Skia's points call takes the whole array: the batch past [count] lies just off the screen, and
// the array is never more than a few times its count, so it is drawn whole, in one call, rather
// than copied to its length each frame or drawn a point at a time.
internal actual fun drawPointBatch(canvas: Canvas, points: FloatArray, count: Int, paint: Paint) {
    canvas.nativeCanvas.drawPoints(points, paint.asFrameworkPaint())
}

// The row's sampled drawing is done with once its pixels are read: its memory goes back at once.
internal actual fun releaseBitmap(bitmap: ImageBitmap) {
    bitmap.asSkiaBitmap().close()
}
