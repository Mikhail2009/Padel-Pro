package com.padelpro.app.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class PpMotionSpec(val scale: Float) {
    val fast get() = (140 * scale).toInt()
    val base get() = (240 * scale).toInt()
    val slow get() = (420 * scale).toInt()
    // Пружина без «резинового» отскока: быстрая и собранная, как у системных контролов iOS.
    fun <T> pop() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
    fun <T> glide() = tween<T>(durationMillis = base)
    val enabled get() = scale > 0.01f
}

val LocalPpMotion = staticCompositionLocalOf { PpMotionSpec(1f) }

@Composable
fun rememberPpMotion(): PpMotionSpec {
    val ctx = LocalContext.current
    return remember(ctx) {
        val s = runCatching {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        PpMotionSpec(if (s.isNaN()) 1f else s.coerceIn(0f, 2f))
    }
}

private val PpScheme = darkColorScheme(
    primary = PpLime,
    onPrimary = PpOnAccent,
    primaryContainer = PpLimeDeep,
    onPrimaryContainer = PpOnAccent,
    secondary = PpGlassCyan,
    onSecondary = PpOnAccent,
    secondaryContainer = PpCourtGlass,
    onSecondaryContainer = PpTextHigh,
    tertiary = PpAmberLed,
    onTertiary = PpOnAccent,
    tertiaryContainer = Color(0x26F2B84B),
    onTertiaryContainer = PpAmberLed,
    background = PpCourtDeep,
    onBackground = PpTextHigh,
    surface = PpCourtNight,
    onSurface = PpTextHigh,
    surfaceVariant = PpCourtPanel,
    onSurfaceVariant = PpTextMid,
    surfaceContainer = PpCourtPanel,
    surfaceContainerHigh = PpCourtGlass,
    surfaceContainerHighest = PpCourtGlass,
    surfaceContainerLow = PpCourtNight,
    surfaceContainerLowest = PpCourtDeep,
    outline = PpCourtLine,
    outlineVariant = PpCourtLineStrong,
    error = PpDanger,
    onError = PpOnAccent,
    errorContainer = Color(0x33FF5A5F),
    onErrorContainer = PpDanger,
    scrim = PpScrimColor,
    inverseSurface = PpTextHigh,
    inverseOnSurface = PpCourtDeep,
    inversePrimary = PpLimeDeep
)

@Composable
fun PadelProTheme(content: @Composable () -> Unit) {
    val motion = rememberPpMotion()
    CompositionLocalProvider(LocalPpMotion provides motion) {
        MaterialTheme(colorScheme = PpScheme, typography = PpTypography, content = content)
    }
}