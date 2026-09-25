package dev.dimvlachos.lab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.presentation.screen.CatalogScreen
import dev.dimvlachos.lab.core.demo.RecordingLog
import dev.dimvlachos.lab.core.platform.platformLabel
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.demo.presentation.screen.DemoScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(initialDemoId: String?, record: Boolean, label: Boolean) {
    LabTheme {
        val initialDemo = remember { Catalog.find(initialDemoId) }
        var currentDemo by remember { mutableStateOf(initialDemo) }
        LaunchedEffect(Unit) {
            if (initialDemoId != null && initialDemo == null) {
                RecordingLog.unknownDemo(initialDemoId)
            }
        }
        val demo = currentDemo
        if (demo == null) {
            CatalogScreen(onOpen = { currentDemo = it })
        } else {
            DemoScreen(
                demo = demo,
                record = record && demo === initialDemo,
                label = if (label) stringResource(platformLabel) else null,
                onBack = if (record && demo === initialDemo) null else ({ currentDemo = null }),
            )
        }
    }
}
