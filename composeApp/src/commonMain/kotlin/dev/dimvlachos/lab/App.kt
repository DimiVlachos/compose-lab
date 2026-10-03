package dev.dimvlachos.lab

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(initialDemoId: String?, record: Boolean, label: Boolean) {
    LabTheme {
        val initialDemo = remember { Catalog.find(initialDemoId) }
        val navigation = remember { AppNavigation(initialDemo, record) }
        // The system back gesture steps back like the on-screen Back buttons; on the home screen
        // it is left to the system, which closes the app.
        val backGesture = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = backGesture,
            isBackEnabled = navigation.canGoBack,
            onBackCompleted = navigation::back,
        )
        LaunchedEffect(Unit) {
            if (initialDemoId != null && initialDemo == null) {
                RecordingLog.unknownDemo(initialDemoId)
            }
        }
        val screen = navigation.screen
        val screens = remember { SeekableTransitionState<AppScreen>(screen) }
        val transition = rememberTransition(screens)
        val gesture = backGesture.transitionState
        val behind = navigation.backScreen
        if (gesture is NavigationEventTransitionState.InProgress && behind != null) {
            // While a back swipe is under way the screen slides off under the finger, the one
            // behind it coming back as far as the swipe has gone.
            val progress = gesture.latestEvent.progress
            LaunchedEffect(progress) {
                screens.seekTo(progress, behind)
            }
        } else {
            LaunchedEffect(screen) {
                if (screens.currentState != screen) {
                    // A screen not yet on its way in is set out first, still, before it moves:
                    // building it takes a frame or two, and the slide would lose them, leaping
                    // ahead on its first frame. One a swipe has brought in part of the way
                    // carries on from there.
                    if (screens.targetState != screen) {
                        screens.seekTo(0f, screen)
                        repeat(SettleFrames) { withFrameNanos {} }
                    }
                    screens.animateTo(screen)
                } else if (screens.targetState != screen) {
                    // A swipe called off: the screen slides back from as far as it had gone, and
                    // then settles where it was.
                    val total = transition.totalDurationNanos / 1_000_000
                    animate(
                        screens.fraction,
                        0f,
                        animationSpec = tween((screens.fraction * total).toInt()),
                    ) { value, _ ->
                        launch { if (value > 0f) screens.seekTo(value) else screens.snapTo(screen) }
                    }
                }
            }
        }
        val motion = LabTheme.motion
        transition.AnimatedContent(transitionSpec = { appTransition(motion) }) { shown ->
            when (shown) {
                is AppScreen.Open ->
                    DemoScreen(
                        demo = shown.demo,
                        record = record && shown.demo === initialDemo,
                        label = if (label) stringResource(platformLabel) else null,
                        // Decided by the screen itself, not where the app is: sliding off as it is
                        // left, it keeps the Back it was left by.
                        onBack =
                            if (record && shown.demo === initialDemo) null else navigation::back,
                    )
                is AppScreen.Folder ->
                    CatalogScreen(
                        title = stringResource(shown.group.title),
                        entries = shown.group.demos.map { CatalogEntry.Single(it) },
                        onOpenDemo = navigation::openDemo,
                        onOpenGroup = navigation::openGroup,
                        onBack = navigation::back,
                    )
                AppScreen.Home ->
                    CatalogScreen(
                        title = stringResource(Res.string.catalog_title),
                        entries = Catalog.entries,
                        onOpenDemo = navigation::openDemo,
                        onOpenGroup = navigation::openGroup,
                    )
            }
        }
    }
}

// Frames a screen is given to be built and laid out before it starts to slide.
private const val SettleFrames = 2
