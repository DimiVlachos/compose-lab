package dev.dimvlachos.lab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogEntry
import dev.dimvlachos.lab.catalog.presentation.screen.CatalogScreen
import dev.dimvlachos.lab.core.demo.RecordingLog
import dev.dimvlachos.lab.core.platform.platformLabel
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.demo.presentation.screen.DemoScreen
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.catalog_title
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
        val group = navigation.group
        when {
            demo != null ->
                DemoScreen(
                    demo = demo,
                    record = record && demo === initialDemo,
                    label = if (label) stringResource(platformLabel) else null,
                    onBack = if (navigation.canGoBack) navigation::back else null,
                )
            group != null ->
                CatalogScreen(
                    title = stringResource(group.title),
                    entries = group.demos.map { CatalogEntry.Single(it) },
                    onOpenDemo = navigation::openDemo,
                    onOpenGroup = navigation::openGroup,
                    onBack = navigation::back,
                )
            else ->
                CatalogScreen(
                    title = stringResource(Res.string.catalog_title),
                    entries = Catalog.entries,
                    onOpenDemo = navigation::openDemo,
                    onOpenGroup = navigation::openGroup,
                )
        }
    }
}
