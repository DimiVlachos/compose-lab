package dev.dimvlachos.lab

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import dev.dimvlachos.lab.core.presentation.ui.LocalHostViewController
import platform.UIKit.UIViewController

/**
 * The lab's [App] in a view controller for the iOS app, with logging sent to the system log.
 * [demoId], [record] and [label] are [App]'s `initialDemoId`, `record` and `label`.
 */
public fun MainViewController(demoId: String?, record: Boolean, label: Boolean): UIViewController {
    Logger.setLogWriters(platformLogWriter(), CommonWriter())
    return ComposeUIViewController {
        // The theme sets the window's interface style through this controller.
        CompositionLocalProvider(LocalHostViewController provides LocalUIViewController.current) {
            App(initialDemoId = demoId, record = record, label = label)
        }
    }
}
