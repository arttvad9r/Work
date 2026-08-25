package com.worktime.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.worktime.app.domain.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF3568B5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3EBFA),
    onPrimaryContainer = Color(0xFF16335F),
    secondary = Color(0xFF555A63),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EFFB),
    onSecondaryContainer = Color(0xFF1D2B45),
    tertiary = Color(0xFF3568B5),
    tertiaryContainer = Color(0xFFE3EBFA),
    onTertiaryContainer = Color(0xFF16335F),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF1D1F23),
    surface = Color(0xFFF8F9FC),
    onSurface = Color(0xFF1D1F23),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2F3F8),
    surfaceContainerHigh = Color(0xFFECEDF3),
    surfaceContainerHighest = Color(0xFFE6E8EF),
    surfaceVariant = Color(0xFFE1E4EC),
    onSurfaceVariant = Color(0xFF555A63),
    outline = Color(0xFF969AA3),
    outlineVariant = Color(0xFFE4E7EE),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFABC7FF),
    onPrimary = Color(0xFF0A305F),
    primaryContainer = Color(0xFF21395F),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFBAC8E0),
    onSecondary = Color(0xFF243044),
    secondaryContainer = Color(0xFF2C3A50),
    onSecondaryContainer = Color(0xFFD6E4FC),
    tertiary = Color(0xFFABC7FF),
    tertiaryContainer = Color(0xFF21395F),
    onTertiaryContainer = Color(0xFFD7E3FF),
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
    outlineVariant = Color(0xFF3A3E46),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Default Material typography with tabular numerals so money/time values never jitter. */
private val WorkTimeTypography: Typography = run {
    val base = Typography()
    base.copy(
        displayLarge = base.displayLarge.tabular(),
        displayMedium = base.displayMedium.tabular(),
        displaySmall = base.displaySmall.tabular(),
        headlineLarge = base.headlineLarge.tabular(),
        headlineMedium = base.headlineMedium.tabular(),
        headlineSmall = base.headlineSmall.tabular(),
        titleLarge = base.titleLarge.tabular(),
        titleMedium = base.titleMedium.tabular(),
        titleSmall = base.titleSmall.tabular(),
        bodyLarge = base.bodyLarge.tabular(),
        bodyMedium = base.bodyMedium.tabular(),
        bodySmall = base.bodySmall.tabular(),
        labelLarge = base.labelLarge.tabular(),
        labelMedium = base.labelMedium.tabular(),
        labelSmall = base.labelSmall.tabular(),
    )
}

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
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors

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
        typography = WorkTimeTypography,
        shapes = WorkTimeShapes,
        content = content,
    )
}
