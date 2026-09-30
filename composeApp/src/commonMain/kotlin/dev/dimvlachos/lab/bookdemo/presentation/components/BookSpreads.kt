package dev.dimvlachos.lab.bookdemo.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint

// Placeholder spreads until the photos land: a wash of two hues and a sun, per spread.
private const val Width = 1152
private const val Height = 576
private val Hues = listOf(200f to 30f, 150f to 50f, 280f to 330f, 20f to 210f)

@Composable
internal fun rememberBookSpreads(): List<ImageBitmap> = remember {
    Hues.map { (from, to) ->
        val image = ImageBitmap(Width, Height)
        val canvas = Canvas(image)
        val wash =
            Paint().apply {
                shader =
                    LinearGradientShader(
                        Offset.Zero,
                        Offset(Width.toFloat(), Height.toFloat()),
                        listOf(Color.hsv(from, 0.45f, 0.9f), Color.hsv(to, 0.55f, 0.8f)),
                    )
            }
        canvas.drawRect(Rect(0f, 0f, Width.toFloat(), Height.toFloat()), wash)
        canvas.drawCircle(
            Offset(Width * 0.7f, Height * 0.35f),
            Height * 0.12f,
            Paint().apply { color = Color.hsv(50f, 0.3f, 1f) },
        )
        image
    }
}
