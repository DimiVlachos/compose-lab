package dev.dimvlachos.moodboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.dimvlachos.moodboard.resources.Res
import dev.dimvlachos.moodboard.resources.instrument_serif_regular
import org.jetbrains.compose.resources.Font

/** Android: the brand scheme. iOS: neutral system grays tinted with the brand blue. */
@Composable expect fun platformColorScheme(dark: Boolean): ColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoodboardTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = platformColorScheme(isSystemInDarkTheme()),
        typography = moodboardTypography(),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

/**
 * The brand's display face, Instrument Serif, on the big titles (screen titles, a photo's name);
 * everything else keeps the platform's own font, so controls and text still feel native. The serif
 * runs small for its size, hence the larger sizes.
 */
@Composable
private fun moodboardTypography(): Typography {
    val serif = FontFamily(Font(Res.font.instrument_serif_regular))
    val base = Typography()
    fun TextStyle.display(size: Int) =
        copy(
            fontFamily = serif,
            fontWeight = FontWeight.Normal,
            fontSize = size.sp,
            lineHeight = (size + 6).sp,
        )
    return base.copy(
        displayLarge = base.displayLarge.display(60),
        displayMedium = base.displayMedium.display(50),
        displaySmall = base.displaySmall.display(42),
        headlineLarge = base.headlineLarge.display(40),
        headlineMedium = base.headlineMedium.display(36),
    )
}
