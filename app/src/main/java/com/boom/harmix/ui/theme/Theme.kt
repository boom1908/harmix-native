package com.boom.harmix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZenWaveColorScheme = darkColorScheme(
    primary = ZenCyan,
    onPrimary = DeepMidnight,
    secondary = SkyBlue,
    onSecondary = DeepMidnight,
    tertiary = SoftLavender,
    onTertiary = DeepMidnight,
    background = DeepMidnight,
    onBackground = MistWhite,
    surface = SurfaceDark,
    onSurface = MistWhite,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = CoolGray,
    outline = GlassBorder,
)

@Composable
fun HarmixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZenWaveColorScheme,
        typography = Typography,
        content = content
    )
}
