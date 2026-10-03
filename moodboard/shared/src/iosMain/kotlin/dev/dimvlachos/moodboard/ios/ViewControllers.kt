package dev.dimvlachos.moodboard.ios

import androidx.compose.ui.window.ComposeUIViewController
import dev.dimvlachos.moodboard.spike.SpikeDetail
import platform.UIKit.UIViewController

fun SpikeDetailViewController(): UIViewController = ComposeUIViewController { SpikeDetail() }
