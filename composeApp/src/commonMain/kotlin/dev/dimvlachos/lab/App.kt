package dev.dimvlachos.lab

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.withFrameNanos
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.presentation.rememberCatalogFilterState
import dev.dimvlachos.lab.catalog.presentation.screen.FolderScreen
import dev.dimvlachos.lab.catalog.presentation.screen.HomeScreen
import dev.dimvlachos.lab.core.demo.RecordingLog
import dev.dimvlachos.lab.core.platform.ThemeModeStore
import dev.dimvlachos.lab.core.platform.platformLabel
import dev.dimvlachos.lab.core.platform.rememberThemeModeStore
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.LocalSystemBarRequests
import dev.dimvlachos.lab.core.presentation.ui.SystemAppearance
import dev.dimvlachos.lab.core.presentation.ui.SystemBarRequests
import dev.dimvlachos.lab.core.presentation.ui.ThemeController
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import dev.dimvlachos.lab.core.presentation.ui.useDarkTheme
import dev.dimvlachos.lab.demo.presentation.screen.DemoScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The lab: a catalog of every demo, each opening on its own screen. [initialDemoId] opens that demo
 * straight away (null, or an id the catalog does not know, starts on the catalog). With [record],
 * that demo plays as a clip for the screen recorder: framed for the crop, with no Back button.
 * [label] shows the platform's name over each demo, for side-by-side captures.
 */
@Composable
public fun App(initialDemoId: String?, record: Boolean, label: Boolean) {
    LabApp(initialDemoId = initialDemoId, record = record, label = label)
}

/**
 * [App], with the theme mode kept in [themeStore]: light or dark as the mode chosen says, or as the
 * system is, except while recording, which is always dark.
 */
@Composable
internal fun LabApp(
    initialDemoId: String?,
    record: Boolean,
    label: Boolean,
    themeStore: ThemeModeStore = rememberThemeModeStore(),
) {
    val theme = remember(themeStore) { ThemeController(themeStore) }
    val dark = useDarkTheme(theme.mode, isSystemInDarkTheme(), record)
    LabWindow(mode = theme.mode, dark = dark, record = record) {
        val initialDemo = remember { Catalog.find(initialDemoId) }
        val navigation = remember { AppNavigation(initialDemo, record) }
        // Kept here, above the screens, so a search is still there after a demo opened from it.
        val filter = rememberCatalogFilterState()
        LaunchedEffect(navigation.searchOpen) {
            if (!navigation.searchOpen) filter.text = ""
        }
        // The system back gesture steps back like the on-screen Back buttons; on the home screen
        // it closes the search, and then is left to the system, which closes the app. It is the
        // app's one back handler: one inside a screen would stay registered as the screen slid
        // off, and take the gesture from this one.
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
        val saveableStates = rememberSaveableStateHolder()
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
                    // Each catalog screen keeps its scroll while a demo is open over it.
                    saveableStates.SaveableStateProvider("folder:${shown.group.id}") {
                        FolderScreen(
                            group = shown.group,
                            onOpenDemo = navigation::openDemo,
                            onBack = navigation::back,
                        )
                    }
                AppScreen.Home ->
                    saveableStates.SaveableStateProvider("home") {
                        HomeScreen(
                            entries = Catalog.entries,
                            filter = filter,
                            searchOpen = navigation.searchOpen,
                            onOpenSearch = navigation::openSearch,
                            onCloseSearch = navigation::back,
                            onOpenDemo = navigation::openDemo,
                            onOpenGroup = navigation::openGroup,
                        )
                    }
            }
        }
    }
}

/**
 * The lab's theme, light or dark as [dark] says, around [content], with the system bars and the
 * window set to match and the screens' requests of the bars applied. While recording ([record]) the
 * system's pieces are left as they are.
 */
@Composable
private fun LabWindow(
    mode: ThemeMode,
    dark: Boolean,
    record: Boolean,
    content: @Composable () -> Unit,
) {
    val barRequests = remember { SystemBarRequests() }
    if (!record) SystemAppearance(mode, dark, barRequests)
    CompositionLocalProvider(LocalSystemBarRequests provides barRequests) {
        LabTheme(darkTheme = dark, content = content)
    }
}

// Frames a screen is given to be built and laid out before it starts to slide.
private const val SettleFrames = 2
