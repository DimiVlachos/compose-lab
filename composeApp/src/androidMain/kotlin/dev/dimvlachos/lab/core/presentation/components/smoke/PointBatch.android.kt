package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas

// The platform canvas takes a count, so a batch's array can be longer than what is in it.
internal actual fun drawPointBatch(canvas: Canvas, points: FloatArray, count: Int, paint: Paint) {
    canvas.nativeCanvas.drawPoints(points, 0, count * 2, paint.asFrameworkPaint())
}
