package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas

// Skia's points call takes the whole array: the batch past [count] lies off the screen, so it is
// drawn whole, in one call, rather than copied to its length or drawn a point at a time.
internal actual fun drawPointBatch(canvas: Canvas, points: FloatArray, count: Int, paint: Paint) {
    canvas.nativeCanvas.drawPoints(points, paint.asFrameworkPaint())
}
