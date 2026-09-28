package dev.dimvlachos.lab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
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
        val navigation = remember { AppNavigation(initialDemo, record) }
        // The system back gesture steps back like the on-screen Back buttons; on the home screen
        // it is left to the system, which closes the app.
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = navigation.canGoBack,
            onBackCompleted = navigation::back,
        )
        LaunchedEffect(Unit) {
            if (initialDemoId != null && initialDemo == null) {
                RecordingLog.unknownDemo(initialDemoId)
            }
        }
        val demo = navigation.demo
        if (demo != null) {
            DemoScreen(
                demo = demo,
                record = record && demo === initialDemo,
                label = if (label) stringResource(platformLabel) else null,
                onBack = if (navigation.canGoBack) navigation::back else null,
            )
        } else {
            CatalogScreen(onOpen = navigation::openDemo)
        }
    }
}
