package dev.dimvlachos.lab.agenticdemo.presentation.components

import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.R
import dev.dimvlachos.lab.agenticdemo.presentation.ConciergeState
import dev.dimvlachos.lab.agenticdemo.presentation.FailureReason
import dev.dimvlachos.lab.agenticdemo.presentation.TranscriptItem
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

@Composable
fun ConciergeScreen(
    state: ConciergeState,
    surfaces: List<A2uiSurfaceModel>,
    onSend: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val followBottom = rememberFollowBottom(listState)
    val send: (String) -> Unit = {
        followBottom.follow()
        onSend(it)
    }
    val surfacesById = surfaces.associateBy { it.id }
    Column(
        Modifier.fillMaxSize()
            .background(LabTheme.colors.background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.union(WindowInsets.ime).only(WindowInsetsSides.Bottom)
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(LabTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
        ) {
            if (state.transcript.isEmpty()) {
                item(key = "intro") { Intro(enabled = state.hasApiKey, onPick = send) }
            }
            items(state.transcript, key = { it.key }) { item ->
                TranscriptRow(item, surfacesById)
            }
        }
        if (state.thinking) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        InputBar(enabled = state.hasApiKey && !state.thinking, onSend = send)
    }
}

@Composable
private fun TranscriptRow(item: TranscriptItem, surfaces: Map<String, A2uiSurfaceModel>) {
    when (item) {
        is TranscriptItem.User -> Bubble(item.text, fromUser = true)
        is TranscriptItem.Agent -> Bubble(item.text, fromUser = false)
        is TranscriptItem.Action ->
            Note(stringResource(R.string.concierge_action, item.name), accent = true)
        is TranscriptItem.Correction ->
            Note(stringResource(R.string.concierge_correction, item.errors), accent = false)
        is TranscriptItem.Failure ->
            Note(
                stringResource(
                    when (item.reason) {
                        FailureReason.Network -> R.string.concierge_failure_network
                        FailureReason.Refused -> R.string.concierge_failure_refused
                    }
                ),
                accent = false,
            )
        is TranscriptItem.Surface -> {
            val surface = surfaces[item.surfaceId]
            if (surface == null) {
                Note(stringResource(R.string.concierge_surface_closed), accent = false)
            } else {
                AgentSurface(surface)
            }
        }
    }
}

@Composable
private fun AgentSurface(surface: A2uiSurfaceModel) {
    A2uiSurface(
        surfaceModel = surface,
        modifier = Modifier.fillMaxWidth(),
        loadingContent = {
            Box(
                Modifier.fillMaxWidth()
                    .heightIn(min = LabTheme.spacing.huge * 2)
                    .background(LabTheme.colors.surface, LabTheme.shapes.Large),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.concierge_thinking),
                    color = LabTheme.colors.textMuted,
                    style = LabTheme.typography.label,
                )
            }
        },
        errorContent = { exception ->
            Note(
                stringResource(R.string.concierge_surface_error, exception.message.orEmpty()),
                accent = false,
            )
        },
        // The loading card grows into the agent's UI instead of swapping under the reader's eye.
        transitionSpec = {
            (fadeIn(tween(400)) + expandVertically(tween(400))) togetherWith
                fadeOut(tween(200)) using
                SizeTransform(clip = false)
        },
    )
}

@Composable
private fun Bubble(text: String, fromUser: Boolean) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text,
            color = if (fromUser) LabTheme.colors.onAccent else LabTheme.colors.textPrimary,
            style = LabTheme.typography.body,
            modifier =
                Modifier.widthIn(max = 320.dp)
                    .background(
                        if (fromUser) LabTheme.colors.accent else LabTheme.colors.surface,
                        LabTheme.shapes.Large,
                    )
                    .padding(
                        horizontal = LabTheme.spacing.medium,
                        vertical = LabTheme.spacing.smallMedium,
                    ),
        )
    }
}

@Composable
private fun Note(text: String, accent: Boolean) {
    Text(
        text,
        color = if (accent) LabTheme.colors.accent else LabTheme.colors.textMuted,
        style = LabTheme.typography.caption,
        modifier = Modifier.fillMaxWidth().padding(horizontal = LabTheme.spacing.small),
    )
}

@Composable
private fun Intro(enabled: Boolean, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.small)) {
        Text(
            stringResource(
                if (enabled) R.string.concierge_intro else R.string.concierge_missing_key
            ),
            color = LabTheme.colors.textMuted,
            style = LabTheme.typography.body,
        )
        if (enabled) {
            listOf(
                    R.string.concierge_suggestion_athens,
                    R.string.concierge_suggestion_date,
                    R.string.concierge_suggestion_cheap,
                )
                .forEach { id ->
                    val suggestion = stringResource(id)
                    SuggestionChip(onClick = { onPick(suggestion) }, label = { Text(suggestion) })
                }
        }
    }
}

@Composable
private fun InputBar(enabled: Boolean, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = {
        onSend(text)
        text = ""
    }
    Row(
        Modifier.fillMaxWidth().padding(LabTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            enabled = enabled,
            placeholder = { Text(stringResource(R.string.concierge_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(onClick = send, enabled = enabled && text.isNotBlank()) {
            Text(stringResource(R.string.concierge_send))
        }
    }
}

@Preview
@Composable
private fun ConciergeScreenPreview() {
    LabTheme {
        ConciergeTheme {
            ConciergeScreen(
                state =
                    ConciergeState(
                        transcript =
                            listOf(
                                TranscriptItem.User("0", "Dinner for 4 in Athens"),
                                TranscriptItem.Agent("1", "Here are three places near Psyrri."),
                                TranscriptItem.Action("2", "choose"),
                            )
                    ),
                surfaces = emptyList(),
                onSend = {},
            )
        }
    }
}
