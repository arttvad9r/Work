package com.worktime.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.worktime.app.domain.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF315DA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF102A56),
    secondary = Color(0xFF526582),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6ECF7),
    onSecondaryContainer = Color(0xFF26364D),
    tertiary = Color(0xFF6B5B95),
    tertiaryContainer = Color(0xFFEDE5FF),
    onTertiaryContainer = Color(0xFF392C62),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFF7F8FC),
    onSurface = Color(0xFF1A1C20),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F2F7),
    surfaceContainerHigh = Color(0xFFEBEDF3),
    surfaceContainerHighest = Color(0xFFE5E7EE),
    surfaceVariant = Color(0xFFE1E4EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7D0),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFABC7FF),
    onPrimary = Color(0xFF002F66),
    primaryContainer = Color(0xFF17467F),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFBAC8E0),
    onSecondary = Color(0xFF243044),
    secondaryContainer = Color(0xFF354157),
    onSecondaryContainer = Color(0xFFD6E4FC),
    tertiary = Color(0xFFD1BCFF),
    tertiaryContainer = Color(0xFF504377),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191B20),
    surfaceContainer = Color(0xFF1D1F24),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32343A),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C7D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    error = Color(0xFFFFB4AB),
)

private val WorkTimeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun WorkTimeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = WorkTimeShapes,
        content = content,
    )
}
