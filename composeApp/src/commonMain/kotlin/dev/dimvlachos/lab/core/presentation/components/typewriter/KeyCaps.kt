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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
 * The typewriter's keys, row by row, [pitch] apart: the letters, or the figures and stops. Each is
 * a button for a screen reader, named for what it types.
 */
@Composable
internal fun TypewriterKeys(
    state: TypewriterState,
    pitch: Dp,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val rows = if (state.keyboard.figures) TypewriterLayout.Figures else TypewriterLayout.Letters
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
        for (row in rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Keyed by what each key types, so turning the page never hands one key's dip or
                // fingertip to the key that takes its place.
                for (key in row) key(key.id) { KeyCap(state, key, pitch, enabled) }
            }
        }
    }
}

// One key: a round glass top in a chrome rim on a lever, or a long bar for the space. It dips as it
// is pressed, by a finger, a scripted fingertip or a hardware key, and the fingertip shows on it.
@Composable
private fun KeyCap(state: TypewriterState, key: TypeKey, pitch: Dp, enabled: Boolean) {
    val colors = LabTheme.colors
    val dip = remember { Animatable(0f) }
    val touch = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    val shadow = remember { SoftShadow() }
    val measurer = rememberTextMeasurer()
    val name = keyName(key, state.keyboard.figures)
    val face = keyFace(key, state.keyboard.figures)
    val labelSize =
        with(LocalDensity.current) {
            (pitch * (if (face.length > 1) 0.26f else 0.36f)).toSp()
        }
    val label =
        remember(measurer, face, labelSize) {
            measurer.measure(
                face,
                TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = labelSize,
                ),
            )
        }
    val shift = key.action == KeyAction.Shift
    val shifted = shift && state.keyboard.shifted
    // A scripted tap shows the fingertip and dips the key; a hardware key only dips it.
    LaunchedEffect(state, key.id) {
        var seen = state.keyboard.taps[key.id] ?: 0
        snapshotFlow { state.keyboard.taps[key.id] ?: 0 }
            .collect { taps ->
                if (taps > seen) {
                    seen = taps
                    launch {
                        touch.snapTo(1f)
                        delay(ScriptedTouch.TapHoldMs)
                        touch.animateTo(0f, tween(ScriptedTouch.UpMs))
                    }
                    launch { blip(dip) }
                }
            }
    }
    LaunchedEffect(state, key.id) {
        var seen = state.keyboard.dips[key.id] ?: 0
        snapshotFlow { state.keyboard.dips[key.id] ?: 0 }
            .collect { dips ->
                if (dips > seen) {
                    seen = dips
                    launch { blip(dip) }
                }
            }
    }
    Spacer(
        Modifier.size(pitch * key.width, pitch * KeyRowHeight)
            .semantics {
                contentDescription = name
                role = Role.Button
                if (shift) selected = shifted
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
                    scope.launch { dip.animateTo(1f, tween(DipMs)) }
                    // A typewriter strikes as the key goes down; only Backspace goes on, slowly.
                    state.press(key)
                    val repeats =
                        if (key.action == KeyAction.Backspace) {
                            scope.launch {
                                delay(RepeatAfterMs)
                                while (true) {
                                    state.press(key)
                                    delay(RepeatEveryMs)
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
                        scope.launch { dip.animateTo(0f, KeyUp) }
                    }
                }
            }
            .drawBehind {
                val d = dip.value
                val travel = KeyTravel.toPx() * d
                val wide = key.action == KeyAction.Space
                val height = size.height * CapShare
                val width = if (wide) size.width - size.height * (1f - CapShare) else height
                val capTop = (size.height - height) / 2f - KeyLift.toPx() + travel
                val capLeft = (size.width - width) / 2f
                val radius = CornerRadius(height / 2f)
                // The lever under the key, shorter as the key goes down.
                val stem = width.coerceAtMost(height) * StemShare
                drawRect(
                    colors.platen.copy(alpha = StemAlpha),
                    Offset(size.width / 2f - stem / 2f, capTop + height / 2f),
                    Size(stem, size.height - capTop - height / 2f),
                )
                with(shadow) {
                    val drop = (KeyLift.toPx() + 2.dp.toPx()) * (1f - d) + 1.dp.toPx()
                    drawSoftShadow(
                        capLeft,
                        capTop + drop,
                        capLeft + width,
                        capTop + height + drop,
                        3.dp.toPx(),
                        0.45f - 0.2f * d,
                    )
                }
                // The chrome rim, lit from above, and the glass top inside it.
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(
                            colors.clipSheen,
                            colors.clipMetal,
                            lerp(colors.clipMetal, colors.platen, 0.35f),
                        ),
                        startY = capTop,
                        endY = capTop + height,
                    ),
                    Offset(capLeft, capTop),
                    Size(width, height),
                    radius,
                )
                val rim = RimWidth.toPx()
                val glass =
                    if (shifted) lerp(colors.platenSheen, colors.typePaper, 0.35f)
                    else colors.platenSheen
                drawRoundRect(
                    Brush.radialGradient(
                        listOf(glass, colors.platen),
                        center = Offset(capLeft + width * 0.4f, capTop + height * 0.32f),
                        radius = maxOf(width, height) * 0.62f,
                    ),
                    Offset(capLeft + rim, capTop + rim),
                    Size(width - rim * 2f, height - rim * 2f),
                    CornerRadius(height / 2f - rim),
                )
                // The glass catching the light, a crescent along its top.
                drawRoundRect(
                    Color.White.copy(alpha = GlassGlint),
                    Offset(capLeft + rim * 2.2f, capTop + rim * 1.6f),
                    Size(width - rim * 4.4f, height * 0.28f),
                    CornerRadius(height / 2f),
                )
                drawText(
                    label,
                    colors.paper,
                    Offset(
                        capLeft + (width - label.size.width) / 2f,
                        capTop + (height - label.size.height) / 2f,
                    ),
                )
                drawTouch(
                    colors.touch,
                    Offset(size.width / 2f, capTop + height / 2f),
                    touch.value,
                )
            }
    )
}

// A key going down and coming back up, as a tap or a hardware key presses it.
private suspend fun blip(dip: Animatable<Float, *>) {
    dip.animateTo(1f, tween(DipMs))
    dip.animateTo(0f, KeyUp)
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

// What a screen reader calls a key.
@Composable
private fun keyName(key: TypeKey, figures: Boolean): String =
    when (val action = key.action) {
        is KeyAction.Type -> action.char.toString()
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

private const val DipMs = 35
private val KeyUp = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh)
private const val RepeatAfterMs = 450L
private const val RepeatEveryMs = 140L

// A key's top is this share of its cell, stands this far up on its lever, and goes this far down.
private const val CapShare = 0.78f
private val KeyLift = 1.5.dp
private val KeyTravel = 3.dp
private val RimWidth = 2.dp
private const val StemShare = 0.18f
private const val StemAlpha = 0.5f
private const val GlassGlint = 0.16f
