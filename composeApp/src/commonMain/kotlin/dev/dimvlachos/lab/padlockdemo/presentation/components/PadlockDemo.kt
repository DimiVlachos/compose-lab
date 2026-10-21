package dev.dimvlachos.lab.padlockdemo.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.padlock.Need
import dev.dimvlachos.lab.core.presentation.components.padlock.Padlock
import dev.dimvlachos.lab.core.presentation.components.padlock.PadlockSounds
import dev.dimvlachos.lab.core.presentation.components.padlock.PadlockState
import dev.dimvlachos.lab.core.presentation.components.padlock.PasswordCheck
import dev.dimvlachos.lab.core.presentation.components.padlock.StrengthLevel
import dev.dimvlachos.lab.core.presentation.components.padlock.checkPassword
import dev.dimvlachos.lab.core.presentation.components.padlock.needs
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PadlockScreenColors
import dev.dimvlachos.lab.padlockdemo.PadlockDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.padlock_continue
import dev.dimvlachos.lab.resources.padlock_email
import dev.dimvlachos.lab.resources.padlock_email_value
import dev.dimvlachos.lab.resources.padlock_heading
import dev.dimvlachos.lab.resources.padlock_intro
import dev.dimvlachos.lab.resources.padlock_level_empty
import dev.dimvlachos.lab.resources.padlock_level_fair
import dev.dimvlachos.lab.resources.padlock_level_good
import dev.dimvlachos.lab.resources.padlock_level_strong
import dev.dimvlachos.lab.resources.padlock_level_weak
import dev.dimvlachos.lab.resources.padlock_need_capital
import dev.dimvlachos.lab.resources.padlock_need_characters
import dev.dimvlachos.lab.resources.padlock_need_number
import dev.dimvlachos.lab.resources.padlock_need_symbol
import dev.dimvlachos.lab.resources.padlock_password
import dev.dimvlachos.lab.resources.padlock_rule_length
import dev.dimvlachos.lab.resources.padlock_rule_mix
import dev.dimvlachos.lab.resources.padlock_rule_number
import dev.dimvlachos.lab.resources.padlock_show
import dev.dimvlachos.lab.resources.padlock_step
import dev.dimvlachos.lab.resources.padlock_tip_common
import dev.dimvlachos.lab.resources.padlock_tip_empty
import dev.dimvlachos.lab.resources.padlock_tip_one
import dev.dimvlachos.lab.resources.padlock_tip_strong
import dev.dimvlachos.lab.resources.padlock_tip_two
import dev.dimvlachos.lab.resources.padlock_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's sign-up step: a brass padlock over the password field is its strength meter and
 * its show/hide switch. The script's taps, typing and erasing go through the same field and lock a
 * finger and the keyboard drive.
 */
@Composable
internal fun PadlockDemo(
    state: DemoState,
    field: TextFieldState = rememberTextFieldState(),
    lock: PadlockState = remember { PadlockState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(PadlockSounds)
    SideEffect { lock.reducedMotion = reducedMotion }
    // The script's tap on the field shows it focused without raising the keyboard over the clip.
    var scriptFocus by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf(checkPassword("")) }

    LaunchedEffect(field, lock) {
        snapshotFlow { field.text.toString() }
            .collect { text ->
                check = checkPassword(text)
                lock.onText(check.level)
            }
    }

    DisposableEffect(state, field, lock) {
        state.setControlTapHandler { control ->
            val typed = PadlockDemos.typed(control)
            val erased = PadlockDemos.erased(control)
            when {
                control == PadlockDemos.Focus -> scriptFocus = true
                control == PadlockDemos.Peek -> lock.showPassword(!lock.peek)
                typed != null ->
                    for (ch in typed) {
                        field.edit { append(ch) }
                        delay(PadlockDemos.KeyEvery)
                    }
                erased != null ->
                    repeat(erased) {
                        field.edit { if (length > 0) replace(length - 1, length, "") }
                        delay(PadlockDemos.EraseEvery)
                    }
            }
        }
        // Empties the field and hides it again, so the clip loops.
        state.setFeedResetHandler {
            field.edit { replace(0, length, "") }
            if (lock.peek) lock.showPassword(false)
            scriptFocus = false
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    val colors = if (LabTheme.isDark) PadlockScreenColors.Dark else PadlockScreenColors.Light
    Column(
        Modifier.fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(
                demoInsets(state).union(WindowInsets.ime.only(WindowInsetsSides.Bottom)),
            ),
    ) {
        AppBar(colors)
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp)) {
            Text(
                stringResource(Res.string.padlock_heading),
                Modifier.semantics { heading() },
                color = colors.ink,
                fontSize = 23.sp,
                lineHeight = 26.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.01).em,
            )
            Text(
                stringResource(Res.string.padlock_intro),
                Modifier.padding(top = 5.dp),
                color = colors.muted,
                fontSize = 13.5.sp,
                lineHeight = BodyLine,
            )
        }
        Padlock(
            state = lock,
            onToggle = { lock.showPassword(it) },
            label = stringResource(Res.string.padlock_show),
            sounds = sounds,
            modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 120.dp),
        )
        Meter(check, colors)
        Form(field, lock.peek, scriptFocus, check, colors)
        val enabled = check.level >= StrengthLevel.Good
        val alpha by animateFloatAsState(if (enabled) 1f else .28f, tween(250), label = "continue")
        Box(
            Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 22.dp)
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.ink)
                .clickable(enabled = enabled) {}
                .testTag("continue"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.padlock_continue),
                color = colors.background,
                fontSize = 16.sp,
                fontWeight = FontWeight(650),
            )
        }
    }
}

/** The step's bar: back, the flow's title, and which step this is. */
@Composable
private fun AppBar(colors: PadlockScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The sign-up flow's own back, drawn as the prototype's: the demo screen's bar goes back.
        Canvas(Modifier.size(44.dp).clearAndSetSemantics {}) {
            val s = 22.dp.toPx() / 24f
            val o = Offset((size.width - 22.dp.toPx()) / 2f, (size.height - 22.dp.toPx()) / 2f)
            val w = 2.1f * s
            drawLine(
                colors.ink,
                o + Offset(14.5f * s, 5.5f * s),
                o + Offset(8f * s, 12f * s),
                w,
                StrokeCap.Round,
            )
            drawLine(
                colors.ink,
                o + Offset(8f * s, 12f * s),
                o + Offset(14.5f * s, 18.5f * s),
                w,
                StrokeCap.Round,
            )
        }
        Text(
            stringResource(Res.string.padlock_title),
            Modifier.weight(1f),
            color = colors.ink,
            fontSize = 15.5.sp,
            lineHeight = BodyLine,
            fontWeight = FontWeight(650),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(Res.string.padlock_step),
            Modifier.clip(CircleShape)
                .background(colors.chip)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = colors.ink,
            fontSize = 12.sp,
            lineHeight = BodyLine,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** The level under the lock, read out as it changes, and the tip that says what's missing. */
@Composable
private fun Meter(check: PasswordCheck, colors: PadlockScreenColors) {
    val level =
        when (check.level) {
            StrengthLevel.Empty -> Res.string.padlock_level_empty
            StrengthLevel.Weak -> Res.string.padlock_level_weak
            StrengthLevel.Fair -> Res.string.padlock_level_fair
            StrengthLevel.Good -> Res.string.padlock_level_good
            StrengthLevel.Strong -> Res.string.padlock_level_strong
        }
    Column(
        Modifier.fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 10.dp)
            .heightIn(min = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(level),
            Modifier.testTag("level").semantics { liveRegion = LiveRegionMode.Polite },
            color = if (check.level == StrengthLevel.Strong) colors.ok else colors.ink,
            fontSize = 15.sp,
            lineHeight = BodyLine,
            fontWeight = FontWeight.Bold,
            letterSpacing = .01.em,
        )
        Text(
            tip(check),
            Modifier.testTag("tip"),
            color = colors.muted,
            fontSize = 12.5.sp,
            lineHeight = BodyLine,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun tip(check: PasswordCheck): String =
    when {
        check.level == StrengthLevel.Empty -> stringResource(Res.string.padlock_tip_empty)
        check.level == StrengthLevel.Strong -> stringResource(Res.string.padlock_tip_strong)
        check.common -> stringResource(Res.string.padlock_tip_common)
        else -> {
            val parts = needs(check).map { need(it) }
            when (parts.size) {
                0 -> stringResource(Res.string.padlock_tip_strong)
                1 -> stringResource(Res.string.padlock_tip_one, parts[0])
                else -> stringResource(Res.string.padlock_tip_two, parts[0], parts[1])
            }
        }
    }

@Composable
private fun need(need: Need): String =
    when (need) {
        is Need.Characters ->
            pluralStringResource(Res.plurals.padlock_need_characters, need.count, need.count)
        Need.Number -> stringResource(Res.string.padlock_need_number)
        Need.Capital -> stringResource(Res.string.padlock_need_capital)
        Need.Symbol -> stringResource(Res.string.padlock_need_symbol)
    }

/** The email, already filled in; the password field, shown or hidden by the lock; and its rules. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Form(
    field: TextFieldState,
    peek: Boolean,
    scriptFocus: Boolean,
    check: PasswordCheck,
    colors: PadlockScreenColors,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val highlight = focused || scriptFocus
    val edge by
        animateColorAsState(
            if (highlight) colors.accent else colors.line,
            tween(200),
            label = "edge",
        )
    val ring by
        animateColorAsState(
            if (highlight) colors.accent.copy(alpha = .18f) else Color.Transparent,
            tween(200),
            label = "ring",
        )
    Column(
        Modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FieldCard(colors.card, colors.line, Color.Transparent) {
            Label(stringResource(Res.string.padlock_email), colors)
            Text(
                stringResource(Res.string.padlock_email_value),
                color = colors.ink,
                fontSize = 15.5.sp,
                lineHeight = BodyLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FieldCard(colors.card, edge, ring) {
            Label(stringResource(Res.string.padlock_password), colors)
            BasicSecureTextField(
                state = field,
                modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp).testTag("password"),
                textObfuscationMode =
                    if (peek) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                textStyle =
                    TextStyle(
                        color = colors.ink,
                        fontSize = 16.sp,
                        lineHeight = BodyLine,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = if (peek) .02.em else .14.em,
                    ),
                cursorBrush = SolidColor(colors.accent),
                interactionSource = interaction,
            )
        }
        FlowRow(
            Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Rule(stringResource(Res.string.padlock_rule_length), check.longEnough, colors)
            Rule(stringResource(Res.string.padlock_rule_number), check.hasNumber, colors)
            Rule(stringResource(Res.string.padlock_rule_mix), check.hasMix, colors)
        }
    }
}

/** A field's card: its hairline [edge], and a soft [ring] round it while it has focus. */
@Composable
private fun FieldCard(fill: Color, edge: Color, ring: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.border(3.dp, ring, RoundedCornerShape(17.dp))) {
        Column(
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(fill)
                .border(1.dp, edge, shape)
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun Label(text: String, colors: PadlockScreenColors) {
    Text(
        text,
        color = colors.muted,
        fontSize = 11.5.sp,
        lineHeight = BodyLine,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = .02.em,
    )
}

/** One rule under the field: a ring, filled green with a gap once [met]. */
@Composable
private fun Rule(text: String, met: Boolean, colors: PadlockScreenColors) {
    val ink by animateColorAsState(if (met) colors.ok else colors.muted, tween(200), label = "rule")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(12.dp)) {
            val stroke = 1.5.dp.toPx()
            val r = size.minDimension / 2f
            if (met) {
                // The CSS dot: a filled ring with a 2 dp gap of the page inside its edge.
                drawCircle(ink, r)
                drawCircle(colors.background, r - stroke - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            } else {
                drawCircle(
                    ink.copy(alpha = ink.alpha * .55f),
                    r - stroke / 2f,
                    style = Stroke(stroke),
                )
            }
        }
        Text(
            text,
            color = ink,
            fontSize = 12.sp,
            lineHeight = BodyLine,
            modifier = Modifier.widthIn(max = 220.dp),
        )
    }
}

/** The prototype's body line height, which every line on the screen inherits. */
private val BodyLine = 1.55.em
