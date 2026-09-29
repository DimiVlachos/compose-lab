package dev.dimvlachos.lab.agenticdemo.presentation.components

import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.lab.BuildConfig
import dev.dimvlachos.lab.agenticdemo.catalog.ConciergeCatalog
import dev.dimvlachos.lab.agenticdemo.data.ClaudeConciergeAgent
import dev.dimvlachos.lab.agenticdemo.data.ConciergeAgent
import dev.dimvlachos.lab.agenticdemo.data.OpenAiConciergeAgent
import dev.dimvlachos.lab.agenticdemo.data.ReplayConciergeAgent
import dev.dimvlachos.lab.agenticdemo.data.SystemPrompt
import dev.dimvlachos.lab.agenticdemo.presentation.ConciergeViewModel

@Composable
fun ConciergeDemo() {
    val assets = LocalContext.current.applicationContext.assets
    // `--ez replay true` plays the scripted conversation instead of calling a model.
    val replay = LocalActivity.current?.intent?.getBooleanExtra("replay", false) == true
    // The lab has no navigation library, so a ViewModel would otherwise belong to the Activity and
    // outlive the demo: a reply would keep streaming (and billing) after Back, and reopening would
    // show the old conversation. This store lives exactly as long as the demo is on screen.
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val viewModel =
        viewModel(viewModelStoreOwner = owner) {
            val systemPrompt = {
                SystemPrompt.build(
                    assets,
                    ConciergeCatalog.ID,
                    ConciergeCatalog.catalog.toJsonSchemaString(),
                )
            }
            ConciergeViewModel(
                agent = if (replay) ReplayConciergeAgent(assets) else agentFor(systemPrompt),
                catalog = ConciergeCatalog.catalog,
            )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val surfaces by viewModel.surfaces.collectAsStateWithLifecycle()
    ConciergeTheme { ConciergeScreen(state, surfaces, onSend = viewModel::onSend) }
}

// Whichever provider has a key in local.properties; OpenAI wins when both do.
private fun agentFor(systemPrompt: () -> String): ConciergeAgent? =
    when {
        BuildConfig.OPENAI_API_KEY.isNotBlank() ->
            OpenAiConciergeAgent(BuildConfig.OPENAI_API_KEY, BuildConfig.OPENAI_MODEL, systemPrompt)
        BuildConfig.ANTHROPIC_API_KEY.isNotBlank() ->
            ClaudeConciergeAgent(BuildConfig.ANTHROPIC_API_KEY, systemPrompt)
        else -> null
    }
