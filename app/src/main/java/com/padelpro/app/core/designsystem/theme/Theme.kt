package com.padelpro.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun PadelProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PpDarkColorScheme,
        typography = PpTypography,
        shapes = PpShapes,
        content = content
    )
}
