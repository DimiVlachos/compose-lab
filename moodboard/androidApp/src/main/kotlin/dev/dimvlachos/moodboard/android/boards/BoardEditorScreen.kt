package dev.dimvlachos.moodboard.android.boards

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.boards.BoardEditorContent
import dev.dimvlachos.moodboard.boards.BoardEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardEditorScreen(boardId: String, onBack: () -> Unit) {
    val viewModel = viewModel { BoardEditorViewModel(boardId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit board") },
                navigationIcon = {
                    IconButton(onClick = onBack) { MoodboardIcon(R.drawable.ic_arrow_back, "Back") }
                },
                actions = { TextButton(onClick = onBack) { Text("Done") } },
            )
        }
    ) { padding ->
        BoardEditorContent(state = state, onAction = viewModel::onAction, contentPadding = padding)
    }
}
