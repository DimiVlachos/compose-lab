package dev.dimvlachos.moodboard.android.boards

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Edits apply as they are made, so Back is the only way out; a Done beside it did the
            // same thing.
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.screen_edit_board),
                        Modifier.semantics { heading() },
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MoodboardIcon(R.drawable.ic_arrow_back, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        BoardEditorContent(state = state, onAction = viewModel::onAction, contentPadding = padding)
    }
}
