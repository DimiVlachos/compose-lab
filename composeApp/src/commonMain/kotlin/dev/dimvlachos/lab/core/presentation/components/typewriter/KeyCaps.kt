package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.magnet.SoftShadow
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.typewriter_key_delete
import dev.dimvlachos.lab.resources.typewriter_key_figures
import dev.dimvlachos.lab.resources.typewriter_key_letters
import dev.dimvlachos.lab.resources.typewriter_key_new_line
import dev.dimvlachos.lab.resources.typewriter_key_shift
import dev.dimvlachos.lab.resources.typewriter_key_space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The typewriter's keys, row by row, [pitch] apart: the letters, or the figures and stops, their
 * legends set with [measurer]. Each is a button for a screen reader, named for what it types, and
 * disabled while not [enabled].
 */
@Composable
internal fun TypewriterKeys(
    state: TypewriterState,
    pitch: Dp,
    enabled: Boolean,
    measurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    val rows = if (state.keyboard.figures) TypewriterLayout.Figures else TypewriterLayout.Letters
    // Each key's motion, by its id, for the one listener on the keyboard's presses to move.
    val motions = remember { mutableMapOf<String, KeyMotion>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) {
        state.keyboard.presses.collect { press ->
            val motion = motions[press.id] ?: return@collect
            scope.launch { motion.dip() }
            if (press.touched) scope.launch { motion.touch() }
        }
    }
    val shadow = remember { SoftShadow() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
        for (row in rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Keyed by what each key types, so turning the page never hands one key's dip or
                // fingertip to the key that takes its place.
                for (key in row) {
                    key(key.id) {
                        val motion = remember { KeyMotion() }
                        DisposableEffect(motion) {
                            motions[key.id] = motion
                            onDispose { if (motions[key.id] === motion) motions -= key.id }
                        }
                        KeyCap(state, key, motion, pitch, enabled, measurer, shadow)
                    }
                }
            }
        }
    }
}

/** How far a key is down, and how far the scripted fingertip on it is. */
private class KeyMotion {
    val dip = Animatable(0f)
    val touch = Animatable(0f)

    suspend fun dip() {
        dip.animateTo(1f, tween(TypewriterDimens.DipMs))
        dip.animateTo(0f, KeyUp)
    }

    suspend fun touch() {
        touch.snapTo(1f)
        delay(ScriptedTouch.TapHoldMs)
        touch.animateTo(0f, tween(ScriptedTouch.UpMs))
    }
}

// One key: a round glass top in a chrome rim on a lever, or a long bar for the space. It dips as it
// is pressed, by a finger, a scripted fingertip or a hardware key, and the fingertip shows on it.
@Composable
private fun KeyCap(
    state: TypewriterState,
    key: TypeKey,
    motion: KeyMotion,
    pitch: Dp,
    enabled: Boolean,
    measurer: TextMeasurer,
    shadow: SoftShadow,
) {
    val colors = LabTheme.colors
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    val figures = state.keyboard.figures
    val shifted = state.keyboard.shifted
    val face = keyFace(key, figures)
    val legend =
        with(LocalDensity.current) {
            val share =
                if (face.length > 1) TypewriterDimens.WordLegendShare
                else TypewriterDimens.LegendShare
            (pitch * share).toSp()
        }
    val label =
        remember(measurer, face, legend) {
            measurer.measure(
                face,
                TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = legend,
                ),
            )
        }
    val shift = key.action == KeyAction.Shift
    val lit = shift && shifted
    val name = keyName(key, figures, shifted)
    Spacer(
        Modifier.size(pitch * key.width, pitch * TypewriterDimens.KeyRowHeight)
            .semantics {
                contentDescription = name
                role = Role.Button
                if (shift) selected = shifted
                if (!enabled) disabled()
                onClick {
                    if (currentEnabled) state.press(key)
                    currentEnabled
                }
            }
            .pointerInput(state, key) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!currentEnabled) return@awaitEachGesture
                    down.consume()
                    scope.launch { motion.dip.animateTo(1f, tween(TypewriterDimens.DipMs)) }
                    // A typewriter strikes as the key goes down; only Delete goes on, slowly.
                    state.press(key)
                    val repeats =
                        if (key.action == KeyAction.Backspace) {
                            scope.launch {
                                delay(TypewriterDimens.RepeatAfterMs)
                                while (true) {
                                    state.press(key)
                                    delay(TypewriterDimens.RepeatEveryMs)
                                }
                            }
                        } else {
                            null
                        }
                    try {
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    } finally {
                        repeats?.cancel()
                        scope.launch { motion.dip.animateTo(0f, KeyUp) }
                    }
                }
            }
            .drawWithCache {
                // The key's shape and gradients, made once for its size; it is moved down under
                // them as it dips.
                val wide = key.action == KeyAction.Space
                val height = size.height * TypewriterDimens.CapShare
                val width =
                    if (wide) size.width - size.height * (1f - TypewriterDimens.CapShare)
                    else height
                val capTop = (size.height - height) / 2f - TypewriterDimens.KeyLift.toPx()
                val capLeft = (size.width - width) / 2f
                val radius = CornerRadius(height / 2f)
                val rim = TypewriterDimens.RimWidth.toPx()
                val stem = minOf(width, height) * TypewriterDimens.StemShare
                val travel = TypewriterDimens.KeyTravel.toPx()
                val lift = TypewriterDimens.KeyLift.toPx() + 2.dp.toPx()
                val chrome =
                    Brush.verticalGradient(
                        listOf(
                            colors.typeChromeSheen,
                            colors.typeChrome,
                            lerp(colors.typeChrome, colors.typePlaten, 0.35f),
                        ),
                        startY = 0f,
                        endY = height,
                    )
                val glassColor =
                    if (lit) lerp(colors.typePlatenSheen, colors.typePaper, LitGlass)
                    else colors.typePlatenSheen
                val glass =
                    Brush.radialGradient(
                        listOf(glassColor, colors.typePlaten),
                        center = Offset(width * 0.4f, height * 0.32f),
                        radius = maxOf(width, height) * 0.62f,
                    )
                val legendAt =
                    Offset((width - label.size.width) / 2f, (height - label.size.height) / 2f)
                onDrawBehind {
                    val d = motion.dip.value
                    val top = capTop + travel * d
                    // The lever under the key, shorter as the key goes down.
                    drawRect(
                        colors.typePlaten.copy(alpha = StemAlpha),
                        Offset(size.width / 2f - stem / 2f, top + height / 2f),
                        Size(stem, size.height - top - height / 2f),
                    )
                    with(shadow) {
                        val drop = lift * (1f - d) + 1.dp.toPx()
                        drawSoftShadow(
                            capLeft,
                            top + drop,
                            capLeft + width,
                            top + height + drop,
                            3.dp.toPx(),
                            KeyShadow - KeyShadowPressed * d,
                        )
                    }
                    translate(capLeft, top) {
                        // The chrome rim, lit from above, and the glass top inside it.
                        drawRoundRect(chrome, size = Size(width, height), cornerRadius = radius)
                        drawRoundRect(
                            glass,
                            Offset(rim, rim),
                            Size(width - rim * 2f, height - rim * 2f),
                            CornerRadius(height / 2f - rim),
                        )
                        // The glass catching the light, a crescent along its top.
                        drawRoundRect(
                            Color.White.copy(alpha = GlassGlint),
                            Offset(rim * 2.2f, rim * 1.6f),
                            Size(width - rim * 4.4f, height * 0.28f),
                            CornerRadius(height / 2f),
                        )
                        drawText(label, colors.typeKeyLegend, legendAt)
                        drawTouch(
                            colors.touch,
                            Offset(width / 2f, height / 2f),
                            motion.touch.value,
                        )
                    }
                }
            }
    )
}

// What a key shows on its top.
private fun keyFace(key: TypeKey, figures: Boolean): String =
    when (val action = key.action) {
        is KeyAction.Type -> action.char.uppercaseChar().toString()
        KeyAction.Space -> ""
        KeyAction.Shift -> "⇧"
        KeyAction.Backspace -> "←"
        KeyAction.Return -> "↵"
        KeyAction.Page -> if (figures) "ABC" else "123"
    }

// What a screen reader calls a key: a letter as it would type now, a capital after Shift.
@Composable
private fun keyName(key: TypeKey, figures: Boolean, shifted: Boolean): String =
    when (val action = key.action) {
        is KeyAction.Type -> (if (shifted) action.char.uppercaseChar() else action.char).toString()
        KeyAction.Space -> stringResource(Res.string.typewriter_key_space)
        KeyAction.Shift -> stringResource(Res.string.typewriter_key_shift)
        KeyAction.Backspace -> stringResource(Res.string.typewriter_key_delete)
        KeyAction.Return -> stringResource(Res.string.typewriter_key_new_line)
        KeyAction.Page ->
            stringResource(
                if (figures) Res.string.typewriter_key_letters
                else Res.string.typewriter_key_figures
            )
    }

private val KeyUp = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh)

// How lit Shift's glass is while it is down, how dark a key's lever and its shadow are, up and
// pressed, and how bright the glint on its glass.
private const val LitGlass = 0.35f
private const val StemAlpha = 0.5f
private const val KeyShadow = 0.45f
private const val KeyShadowPressed = 0.2f
private const val GlassGlint = 0.16f
