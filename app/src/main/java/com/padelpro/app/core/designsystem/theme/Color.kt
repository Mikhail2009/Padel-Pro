package com.padelpro.app.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val PpBackground = Color(0xFF090B0A)
val PpSurface = Color(0xFF121613)
val PpCard = Color(0xFF181E19)
val PpPrimary = Color(0xFFB8F34A)
val PpPrimaryDim = Color(0xFF8FCB38)
val PpOnPrimary = Color(0xFF090B0A)
val PpTextPrimary = Color(0xFFF5F7F2)
val PpTextSecondary = Color(0xFF8C968C)
val PpOutline = Color(0xFF29312A)
val PpError = Color(0xFFFF6B5E)

val PpPrimaryTint = PpPrimary.copy(alpha = 0.14f)
val PpErrorTint = PpError.copy(alpha = 0.12f)

val PpDarkColorScheme = darkColorScheme(
    primary = PpPrimary,
    onPrimary = PpOnPrimary,
    primaryContainer = PpPrimaryDim,
    onPrimaryContainer = PpOnPrimary,
    secondary = PpPrimaryDim,
    onSecondary = PpOnPrimary,
    secondaryContainer = PpCard,
    onSecondaryContainer = PpTextPrimary,
    tertiary = PpPrimary,
    onTertiary = PpOnPrimary,
    background = PpBackground,
    onBackground = PpTextPrimary,
    surface = PpSurface,
    onSurface = PpTextPrimary,
    surfaceVariant = PpCard,
    onSurfaceVariant = PpTextSecondary,
    outline = PpOutline,
    outlineVariant = PpOutline,
    error = PpError,
    onError = PpBackground,
    errorContainer = PpErrorTint,
    onErrorContainer = PpError
)
