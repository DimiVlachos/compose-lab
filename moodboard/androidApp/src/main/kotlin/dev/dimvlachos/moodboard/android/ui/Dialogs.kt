package dev.dimvlachos.moodboard.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.dimvlachos.moodboard.R

@Composable
fun DeletePhotoDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.dialogEntrance(),
        title = { Text("Delete photo?") },
        text = { Text("“$title” will be removed from the gallery and every board.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** New board and rename share one dialog: a name field and a confirm button. */
@Composable
fun BoardNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Opens ready to type, the current name selected so a rename can simply overwrite it.
    var name by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(initialName, TextRange(0, initialName.length)))
        }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val confirm = { if (name.text.isNotBlank()) onConfirm(name.text.trim()) }
    // The window no longer resizes for the keyboard, which snapped the dialog down and then up
    // as it opened; the content pads itself by the keyboard's insets instead, so the centred
    // dialog glides up with the keyboard frame by frame.
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.imePadding().dialogEntrance(),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = name.text.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Android's counterpart to the iOS action sheet: the two ways to delete a board. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBoardSheet(
    boardName: String,
    onDelete: (deletePhotos: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                "Delete “$boardName”?",
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp).semantics {
                        heading()
                    },
            )
            Text(
                "Its photos stay in the gallery unless you delete them too.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            // Rows on the sheet's own color, not ListItem's surface: no band behind each one.
            val rows = ListItemDefaults.colors(containerColor = Color.Transparent)
            val danger =
                ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                )
            ListItem(
                headlineContent = { Text("Delete board only") },
                leadingContent = { MoodboardIcon(R.drawable.ic_boards, null) },
                colors = rows,
                modifier = Modifier.clickable { onDelete(false) },
            )
            ListItem(
                headlineContent = { Text("Delete board and its photos") },
                leadingContent = { MoodboardIcon(R.drawable.ic_delete, null) },
                colors = danger,
                modifier = Modifier.clickable { onDelete(true) },
            )
            ListItem(
                headlineContent = { Text("Cancel") },
                leadingContent = { MoodboardIcon(R.drawable.ic_close, null) },
                colors = rows,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

/**
 * Dialogs ease in from a little smaller on a soft spring, over the platform's fade, instead of only
 * fading in at full size. Read in the layer block, so the animation never recomposes.
 */
@Composable
private fun Modifier.dialogEntrance(): Modifier {
    val scale = remember { Animatable(EntranceScale) }
    LaunchedEffect(Unit) { scale.animateTo(1f, EntranceSpring) }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

private const val EntranceScale = 0.92f
private val EntranceSpring =
    spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
