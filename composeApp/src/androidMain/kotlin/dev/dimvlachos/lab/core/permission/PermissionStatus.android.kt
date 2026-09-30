package dev.dimvlachos.lab.core.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Where one runtime permission stands, and what can be done about it. */
internal sealed interface PermissionStatus {
    data object Granted : PermissionStatus

    /** The system will still ask; a refusal hands over a new [Askable]. */
    class Askable(val ask: () -> Unit) : PermissionStatus

    /** Refused for good: only [openSettings] can turn it on. */
    class Blocked(val openSettings: () -> Unit) : PermissionStatus
}

/**
 * [permission]'s status for this app, checked again every time the screen resumes (back from
 * settings, or from anywhere). It never asks by itself.
 */
@Composable
internal fun rememberPermissionStatus(permission: String): PermissionStatus {
    val context = LocalContext.current
    fun isGranted() = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(isGranted()) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    var refusals by rememberSaveable { mutableIntStateOf(0) }
    var rationaleBefore by rememberSaveable { mutableStateOf(false) }
    fun rationale() =
        context.findActivity()?.shouldShowRequestPermissionRationale(permission) == true
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            if (!ok) {
                refusals++
                // Only settings can help once the system will not ask again.
                blocked = refusedForGood(rationaleBefore, rationale(), refusals)
            }
        }
    LifecycleResumeEffect(Unit) {
        granted = isGranted()
        onPauseOrDispose {}
    }
    val blockedStatus = remember {
        PermissionStatus.Blocked {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            )
        }
    }
    // A new Askable after each refusal, so a demo can tell the answer came.
    val askableStatus =
        remember(refusals) {
            PermissionStatus.Askable {
                rationaleBefore = rationale()
                launcher.launch(permission)
            }
        }
    return when {
        granted -> PermissionStatus.Granted
        blocked -> blockedStatus
        else -> askableStatus
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
