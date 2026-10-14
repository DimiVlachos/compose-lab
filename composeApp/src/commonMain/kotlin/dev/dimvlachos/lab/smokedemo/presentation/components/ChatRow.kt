package dev.dimvlachos.lab.smokedemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.smoke_archived_row
import dev.dimvlachos.lab.resources.smoke_unread
import dev.dimvlachos.lab.smokedemo.Avatar
import dev.dimvlachos.lab.smokedemo.Chat
import dev.dimvlachos.lab.smokedemo.CycladesChats
import org.jetbrains.compose.resources.stringResource

/**
 * A chat in the list, on its own card: the avatar, who it is with and when, and the last message,
 * with a dot when it is unread, bold and in the accent. A hairline over it, from the text on, when
 * [divider]. A screen reader reads it as one: the name, the message, the time, and whether unread.
 */
@Composable
internal fun ChatRow(chat: Chat, divider: Boolean, modifier: Modifier = Modifier) {
    val colors = LabTheme.colors
    val name = stringResource(chat.name)
    val preview = stringResource(chat.preview)
    val time = chat.day?.let { stringResource(it) } ?: chat.time
    val unread = stringResource(Res.string.smoke_unread)
    val line = colors.chipBorder
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.surface)
            .drawBehind {
                if (divider) {
                    val start = DividerStart.toPx()
                    drawRect(line, Offset(start, 0f), Size(size.width - start, 1.dp.toPx()))
                }
            }
            .clearAndSetSemantics {
                contentDescription =
                    listOfNotNull(name, preview, time, unread.takeIf { chat.unread })
                        .joinToString(", ")
            }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(chat.initials, avatarColour(chat.avatar, colors))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    name,
                    style = NameStyle,
                    fontWeight = if (chat.unread) FontWeight.Bold else FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    time,
                    style = TimeStyle,
                    color = if (chat.unread) colors.accent else colors.textMuted,
                    maxLines = 1,
                )
            }
            Text(
                preview,
                style = PreviewStyle,
                color = if (chat.unread) colors.textPrimary else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.size(9.dp)
                .background(if (chat.unread) colors.accent else Color.Transparent, CircleShape)
        )
    }
}

/**
 * The row at the top of the inbox that opens the archived chats, its [label] saying how many there
 * are, on a card of its own.
 */
@Composable
internal fun ArchivedRow(label: String, modifier: Modifier = Modifier) {
    val colors = LabTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val muted = colors.textMuted
        Box(
            Modifier.size(AvatarSize).background(colors.bar, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ArchiveGlyph(muted)
        }
        Text(
            label,
            style = NameStyle,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Chevron(muted, pointsRight = true)
    }
}

// The avatar: the chat's initials, white on its island colour.
@Composable
private fun Avatar(initials: String, colour: Color) {
    Box(
        Modifier.size(AvatarSize).background(colour, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = InitialsStyle,
            color = LabTheme.colors.onAccent,
        )
    }
}

// An archive box: a lid over a box with a slot for a handle.
@Composable
private fun ArchiveGlyph(colour: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val stroke = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round)
        val r = CornerRadius(2.dp.toPx())
        drawRoundRect(
            colour,
            Offset(w * 0.1f, w * 0.18f),
            Size(w * 0.8f, w * 0.2f),
            r,
            style = stroke,
        )
        drawRoundRect(
            colour,
            Offset(w * 0.17f, w * 0.38f),
            Size(w * 0.66f, w * 0.46f),
            r,
            style = stroke,
        )
        drawLine(
            colour,
            Offset(w * 0.4f, w * 0.55f),
            Offset(w * 0.6f, w * 0.55f),
            stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

/** A chevron, pointing right to open or left to go back. */
@Composable
internal fun Chevron(colour: Color, pointsRight: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp)) {
        val w = size.width
        val stroke = 1.8.dp.toPx()
        val tip = if (pointsRight) w * 0.68f else w * 0.32f
        val back = if (pointsRight) w * 0.36f else w * 0.64f
        drawLine(colour, Offset(back, w * 0.2f), Offset(tip, w * 0.5f), stroke, StrokeCap.Round)
        drawLine(colour, Offset(tip, w * 0.5f), Offset(back, w * 0.8f), stroke, StrokeCap.Round)
    }
}

/** A section's heading over its group of chats: small capitals, muted. */
@Composable
internal fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = HeadingStyle,
        color = LabTheme.colors.textMuted,
        modifier = modifier.padding(start = 6.dp, top = 16.dp, bottom = 7.dp),
    )
}

internal fun avatarColour(avatar: Avatar, colors: AppColors): Color =
    when (avatar) {
        Avatar.Green -> colors.chatGreen
        Avatar.Blue -> colors.chatBlue
        Avatar.Amber -> colors.chatAmber
        Avatar.Red -> colors.magnetRed
        Avatar.Teal -> colors.chatTeal
        Avatar.Violet -> colors.chatViolet
        Avatar.Pink -> colors.tagVillage
    }

private val AvatarSize = 44.dp

// Where the hairline between chats starts: past the avatar, under the text.
private val DividerStart = 70.dp

private val NameStyle = LabTheme.typography.body.copy(fontSize = 15.5.sp, lineHeight = 20.sp)
private val TimeStyle = LabTheme.typography.label.copy(fontSize = 12.5.sp)
private val PreviewStyle = LabTheme.typography.body.copy(fontSize = 13.5.sp, lineHeight = 18.sp)
private val InitialsStyle =
    LabTheme.typography.body.copy(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
private val HeadingStyle =
    LabTheme.typography.label.copy(
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    )

@Composable
private fun ChatRowPreview(dark: Boolean) {
    LabTheme(darkTheme = dark) {
        Column(Modifier.background(LabTheme.colors.background).padding(16.dp)) {
            ArchivedRow(stringResource(Res.string.smoke_archived_row, 2))
            Spacer(Modifier.size(12.dp))
            CycladesChats.all.take(3).forEachIndexed { i, chat -> ChatRow(chat, divider = i > 0) }
        }
    }
}

@Preview @Composable private fun ChatRowDarkPreview() = ChatRowPreview(dark = true)

@Preview @Composable private fun ChatRowLightPreview() = ChatRowPreview(dark = false)
