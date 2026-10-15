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
import dev.dimvlachos.lab.resources.typewriter_key_face_figures
import dev.dimvlachos.lab.resources.typewriter_key_face_letters
import dev.dimvlachos.lab.resources.typewriter_key_figures
import dev.dimvlachos.lab.resources.typewriter_key_letters
import dev.dimvlachos.lab.resources.typewriter_key_new_line
import dev.dimvlachos.lab.resources.typewriter_key_shift
import dev.dimvlachos.lab.resources.typewriter_key_space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The typewriter's keys, row by row, [pitch] apart, each row [rowHeight] tall: the letters, or the
 * figures and stops, their legends set with [measurer]. A key takes a touch anywhere in its row's
 * height, so a row taller than the keys makes a bigger target of each. Each is a button for a
 * screen reader, named for what it types, and disabled while not [enabled].
 */
@Composable
internal fun TypewriterKeys(
    state: TypewriterState,
    pitch: Dp,
    rowHeight: Dp,
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
        for ((index, row) in rows.withIndex()) {
            // The rows further back stand a tier higher and look a little smaller from here.
            val depth = (rows.size - 1 - index).toFloat()
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
                        KeyCap(
                            state,
                            key,
                            motion,
                            pitch,
                            rowHeight,
                            enabled,
                            measurer,
                            shadow,
                            depth,
                        )
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
    rowHeight: Dp,
    enabled: Boolean,
    measurer: TextMeasurer,
    shadow: SoftShadow,
    depth: Float,
) {
    val colors = LabTheme.colors
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    val figures = state.keyboard.figures
    val shifted = state.keyboard.shifted
    val face =
        when (key.action) {
            KeyAction.Page ->
                stringResource(
                    if (figures) Res.string.typewriter_key_face_letters
                    else Res.string.typewriter_key_face_figures
                )
            else -> keyFace(key)
        }
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
        Modifier.size(pitch * key.width, maxOf(rowHeight, pitch * TypewriterDimens.KeyRowHeight))
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
                                // Not once the keys are disabled, as when a replay starts.
                                while (currentEnabled) {
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
                // them as it dips. Seen a little from above: its top is a flattened round, on a
                // chrome stem that goes down into a slot in the well. Drawn the same size in a
                // taller row, in its middle.
                val cell = pitch.toPx() * TypewriterDimens.KeyRowHeight
                val above = (size.height - cell) / 2f
                val scale = 1f - TypewriterDimens.TierShrink * depth
                val wide = key.action == KeyAction.Space
                val across = cell * TypewriterDimens.CapShare * scale
                val height = across * TypewriterDimens.CapSquash
                val width =
                    if (wide) size.width - cell * (1f - TypewriterDimens.CapShare) else across
                val capTop = above + cell * TypewriterDimens.CapRise
                val capLeft = (size.width - width) / 2f
                val ring = TypewriterDimens.RimWidth.toPx()
                val stem = across * TypewriterDimens.StemShare
                val slot = Offset(size.width / 2f, above + cell * TypewriterDimens.SlotAt)
                val travel = TypewriterDimens.KeyTravel.toPx()
                val chromeDark = lerp(colors.typeChrome, colors.typePlaten, 0.45f)
                val stemBrush =
                    Brush.horizontalGradient(
                        listOf(chromeDark, colors.typeChromeSheen, colors.typeChrome, chromeDark),
                        startX = -stem / 2f,
                        endX = stem / 2f,
                    )
                val chrome =
                    Brush.verticalGradient(
                        listOf(Color.White, colors.typeChromeSheen, colors.typeChrome, chromeDark),
                        startY = 0f,
                        endY = height,
                    )
                val glassColor =
                    if (lit) lerp(colors.typePlatenSheen, colors.typePaper, LitGlass)
                    else colors.typePlatenSheen
                // Celluloid over a dark face: lighter towards its rim, deep in the middle.
                val glass =
                    if (wide) {
                        Brush.verticalGradient(
                            listOf(colors.typeChromeSheen, colors.typeChrome, chromeDark),
                            startY = 0f,
                            endY = height,
                        )
                    } else {
                        Brush.radialGradient(
                            0f to colors.typePlaten,
                            0.7f to lerp(colors.typePlaten, glassColor, 0.5f),
                            1f to glassColor,
                            center = Offset(width / 2f, height * 0.45f),
                            radius = width * 0.55f,
                        )
                    }
                val legendAt =
                    Offset((width - label.size.width) / 2f, (height - label.size.height) / 2f)
                val stems = if (wide) listOf(width * 0.2f, width * 0.8f) else listOf(width / 2f)
                onDrawBehind {
                    val d = motion.dip.value
                    val top = capTop + travel * d
                    // The slot the stem goes down into, a dark notch in the well.
                    for (x in stems) {
                        drawOval(
                            Color.Black.copy(alpha = SlotAlpha),
                            Offset(capLeft + x - stem * 1.6f, slot.y - stem * 0.6f),
                            Size(stem * 3.2f, stem * 1.2f),
                        )
                    }
                    // The stem, from under the key down into its slot, shorter as it goes down.
                    for (x in stems) {
                        translate(capLeft + x, 0f) {
                            drawRect(
                                stemBrush,
                                Offset(-stem / 2f, top + height / 2f),
                                Size(stem, (slot.y - top - height / 2f).coerceAtLeast(0f)),
                            )
                        }
                    }
                    with(shadow) {
                        val drop = (TypewriterDimens.KeyLift.toPx() + 3.dp.toPx()) * (1f - d)
                        drawSoftShadow(
                            capLeft + width * 0.08f,
                            top + drop + height * 0.3f,
                            capLeft + width * 0.92f,
                            top + height + drop,
                            4.dp.toPx(),
                            KeyShadow - KeyShadowPressed * d,
                        )
                    }
                    translate(capLeft, top) {
                        val corner =
                            if (wide) CornerRadius(height / 2f)
                            else CornerRadius(width / 2f, height / 2f)
                        // The thin chrome ring, lit from above, and the face inside it.
                        drawRoundRect(chrome, size = Size(width, height), cornerRadius = corner)
                        drawRoundRect(
                            glass,
                            Offset(ring, ring),
                            Size(width - ring * 2f, height - ring * 2f),
                            CornerRadius(corner.x - ring, corner.y - ring),
                        )
                        // The studio light on the celluloid, a soft crescent at its top left,
                        // and a faint reflection along its foot.
                        drawOval(
                            Color.White.copy(alpha = GlassGlint),
                            Offset(width * 0.18f, height * 0.12f),
                            Size(width * 0.5f, height * 0.26f),
                        )
                        drawOval(
                            Color.White.copy(alpha = GlassGlint * 0.4f),
                            Offset(width * 0.25f, height * 0.74f),
                            Size(width * 0.5f, height * 0.12f),
                        )
                        // The legend, worn a little soft.
                        drawText(
                            label,
                            colors.typeKeyLegend.copy(alpha = LegendWear),
                            legendAt + Offset(0.4f, 0.3f),
                        )
                        drawText(label, colors.typeKeyLegend.copy(alpha = LegendInk), legendAt)
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

// What a key shows on its top; the page key's words are the caller's, from the strings.
private fun keyFace(key: TypeKey): String =
    when (val action = key.action) {
        is KeyAction.Type -> action.char.uppercaseChar().toString()
        KeyAction.Space -> ""
        KeyAction.Shift -> "⇧"
        KeyAction.Backspace -> "←"
        KeyAction.Return -> "↵"
        KeyAction.Page -> ""
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

// How lit Shift's glass is while it is down, how dark the slot its stem goes into, how worn and how
// dark its legend, how dark its shadow, up and pressed, and how bright the glint on its glass.
private const val LitGlass = 0.45f
private const val SlotAlpha = 0.6f
private const val LegendWear = 0.25f
private const val LegendInk = 0.88f
private const val KeyShadow = 0.45f
private const val KeyShadowPressed = 0.2f
private const val GlassGlint = 0.3f
