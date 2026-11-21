package com.padelpro.app.core.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Держит экран горизонтально, пока composable в композиции; затем возвращает прежнюю ориентацию. */
@Composable
fun LockLandscape() {
    val ctx = LocalContext.current
    DisposableEffect(ctx) {
        val activity = ctx.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (previous != null) activity.requestedOrientation = previous
        }
    }
}