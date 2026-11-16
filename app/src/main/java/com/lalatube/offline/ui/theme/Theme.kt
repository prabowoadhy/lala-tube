package com.lalatube.offline.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lalatube.offline.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = YouTubeRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E8),
    onPrimaryContainer = Color(0xFF41000B),
    secondary = MutedInk,
    onSecondary = Color.White,
    secondaryContainer = ChipLight,
    onSecondaryContainer = Ink,
    background = PageLight,
    onBackground = Ink,
    surface = PageLight,
    onSurface = Ink,
    surfaceVariant = ChipLight,
    onSurfaceVariant = MutedInk,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = ChipLight,
    surfaceContainerHigh = Color(0xFFEDEDED),
    surfaceContainerHighest = Color(0xFFE5E5E5),
    outline = Color(0xFFD0D0D0),
    outlineVariant = LineLight,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = YouTubeRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5F0017),
    onPrimaryContainer = Color(0xFFFFDADC),
    secondary = MutedDark,
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = ChipDark,
    onSecondaryContainer = Color.White,
    background = PageDark,
    onBackground = Color(0xFFF1F1F1),
    surface = PageDark,
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = ChipDark,
    onSurfaceVariant = MutedDark,
    surfaceContainerLowest = Color(0xFF090909),
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF292929),
    surfaceContainerHighest = Color(0xFF333333),
    outline = LineDark,
    outlineVariant = Color(0xFF2E2E2E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun LalaTubeTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = LalaTypography,
        content = content,
    )
}
