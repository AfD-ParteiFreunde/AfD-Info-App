package de.afd.parteiapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AfDBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9E2F8),
    onPrimaryContainer = Color(0xFF00344F),
    secondary = AfDNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBE2F3),
    onSecondaryContainer = AfDNavy,
    tertiary = AfDRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDADB),
    onTertiaryContainer = Color(0xFF410007),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = AfDRed,
    surfaceTint = AfDBlue,
    surfaceBright = Color(0xFFF6FBFF),
    surfaceDim = Color(0xFFC6DBEC),
    surfaceContainerLowest = Color(0xFFFBFDFF),
    surfaceContainerLow = Color(0xFFF0F8FF),
    surfaceContainer = Color(0xFFE4F1FC),
    surfaceContainerHigh = Color(0xFFD9EAF8),
    surfaceContainerHighest = Color(0xFFCFE3F4),
    inverseSurface = Color(0xFF10293D),
    inverseOnSurface = Color(0xFFE6F0F8),
    inversePrimary = AfDBlueBright,
)

private val DarkColors = darkColorScheme(
    primary = AfDBlueBright,
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF0B4C6E),
    onPrimaryContainer = Color(0xFFCDEBFB),
    secondary = Color(0xFF9FC3DE),
    onSecondary = AfDNavy,
    secondaryContainer = Color(0xFF1B3A55),
    onSecondaryContainer = Color(0xFFD8E6F2),
    tertiary = AfDRedSoft,
    onTertiary = Color(0xFF340006),
    tertiaryContainer = Color(0xFF5C1018),
    onTertiaryContainer = Color(0xFFFFDADB),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = AfDRedSoft,
    surfaceTint = AfDBlueBright,
    surfaceBright = Color(0xFF1A3D5A),
    surfaceDim = Color(0xFF03101B),
    surfaceContainerLowest = Color(0xFF04121C),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF0E2A41),
    surfaceContainerHigh = Color(0xFF133149),
    surfaceContainerHighest = Color(0xFF183A55),
    inverseSurface = Color(0xFFDCEDFB),
    inverseOnSurface = Color(0xFF0D2437),
    inversePrimary = AfDBlue,
)

@Composable
fun AfDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AfDTypography,
        content = content,
    )
}
