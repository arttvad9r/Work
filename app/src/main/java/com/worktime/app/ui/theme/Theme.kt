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
import androidx.compose.ui.graphics.luminance
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
    primary = Color(0xFFB8CCFF),
    onPrimary = Color(0xFF0A315E),
    primaryContainer = Color(0xFF2C4670),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFC2CBE0),
    onSecondary = Color(0xFF293143),
    secondaryContainer = Color(0xFF354054),
    onSecondaryContainer = Color(0xFFE0E7F5),
    tertiary = Color(0xFFB8CCFF),
    tertiaryContainer = Color(0xFF2C4670),
    onTertiaryContainer = Color(0xFFDCE6FF),
    background = Color(0xFF121419),
    onBackground = Color(0xFFF0F1F7),
    surface = Color(0xFF121419),
    onSurface = Color(0xFFF0F1F7),
    surfaceContainerLowest = Color(0xFF0E1015),
    surfaceContainerLow = Color(0xFF191C22),
    surfaceContainer = Color(0xFF20242B),
    surfaceContainerHigh = Color(0xFF2A2F37),
    surfaceContainerHighest = Color(0xFF343A44),
    surfaceVariant = Color(0xFF454B55),
    onSurfaceVariant = Color(0xFFD3D6DF),
    outline = Color(0xFFA2A7B1),
    outlineVariant = Color(0xFF434954),
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
        val lightSystemBars = colorScheme.background.luminance() > 0.5f
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightSystemBars
            controller.isAppearanceLightNavigationBars = lightSystemBars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WorkTimeTypography,
        shapes = WorkTimeShapes,
        content = content,
    )
}
