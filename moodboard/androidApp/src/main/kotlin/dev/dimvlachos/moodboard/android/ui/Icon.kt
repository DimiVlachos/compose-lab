package dev.dimvlachos.moodboard.android.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

@Composable
fun MoodboardIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(painterResource(id), contentDescription, modifier, tint)
}
