package com.worktime.app.ui.theme

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.AppMotion

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

private fun blendedColorScheme(darkFraction: Float): ColorScheme {
    val fraction = darkFraction.coerceIn(0f, 1f)
    val base = if (fraction < 0.5f) LightColors else DarkColors
    fun mix(light: Color, dark: Color): Color = lerp(light, dark, fraction)

    // One progress value drives every visible semantic role. This keeps the palette coherent
    // and avoids separate color animations finishing on different frames.
    return base.copy(
        primary = mix(LightColors.primary, DarkColors.primary),
        onPrimary = mix(LightColors.onPrimary, DarkColors.onPrimary),
        primaryContainer = mix(LightColors.primaryContainer, DarkColors.primaryContainer),
        onPrimaryContainer = mix(LightColors.onPrimaryContainer, DarkColors.onPrimaryContainer),
        inversePrimary = mix(LightColors.inversePrimary, DarkColors.inversePrimary),
        secondary = mix(LightColors.secondary, DarkColors.secondary),
        onSecondary = mix(LightColors.onSecondary, DarkColors.onSecondary),
        secondaryContainer = mix(LightColors.secondaryContainer, DarkColors.secondaryContainer),
        onSecondaryContainer = mix(LightColors.onSecondaryContainer, DarkColors.onSecondaryContainer),
        tertiary = mix(LightColors.tertiary, DarkColors.tertiary),
        onTertiary = mix(LightColors.onTertiary, DarkColors.onTertiary),
        tertiaryContainer = mix(LightColors.tertiaryContainer, DarkColors.tertiaryContainer),
        onTertiaryContainer = mix(LightColors.onTertiaryContainer, DarkColors.onTertiaryContainer),
        background = mix(LightColors.background, DarkColors.background),
        onBackground = mix(LightColors.onBackground, DarkColors.onBackground),
        surface = mix(LightColors.surface, DarkColors.surface),
        onSurface = mix(LightColors.onSurface, DarkColors.onSurface),
        surfaceVariant = mix(LightColors.surfaceVariant, DarkColors.surfaceVariant),
        onSurfaceVariant = mix(LightColors.onSurfaceVariant, DarkColors.onSurfaceVariant),
        surfaceTint = mix(LightColors.surfaceTint, DarkColors.surfaceTint),
        inverseSurface = mix(LightColors.inverseSurface, DarkColors.inverseSurface),
        inverseOnSurface = mix(LightColors.inverseOnSurface, DarkColors.inverseOnSurface),
        error = mix(LightColors.error, DarkColors.error),
        onError = mix(LightColors.onError, DarkColors.onError),
        errorContainer = mix(LightColors.errorContainer, DarkColors.errorContainer),
        onErrorContainer = mix(LightColors.onErrorContainer, DarkColors.onErrorContainer),
        outline = mix(LightColors.outline, DarkColors.outline),
        outlineVariant = mix(LightColors.outlineVariant, DarkColors.outlineVariant),
        scrim = mix(LightColors.scrim, DarkColors.scrim),
        surfaceBright = mix(LightColors.surfaceBright, DarkColors.surfaceBright),
        surfaceDim = mix(LightColors.surfaceDim, DarkColors.surfaceDim),
        surfaceContainer = mix(LightColors.surfaceContainer, DarkColors.surfaceContainer),
        surfaceContainerHigh = mix(LightColors.surfaceContainerHigh, DarkColors.surfaceContainerHigh),
        surfaceContainerHighest = mix(LightColors.surfaceContainerHighest, DarkColors.surfaceContainerHighest),
        surfaceContainerLow = mix(LightColors.surfaceContainerLow, DarkColors.surfaceContainerLow),
        surfaceContainerLowest = mix(LightColors.surfaceContainerLowest, DarkColors.surfaceContainerLowest),
        primaryFixed = mix(LightColors.primaryFixed, DarkColors.primaryFixed),
        primaryFixedDim = mix(LightColors.primaryFixedDim, DarkColors.primaryFixedDim),
        onPrimaryFixed = mix(LightColors.onPrimaryFixed, DarkColors.onPrimaryFixed),
        onPrimaryFixedVariant = mix(LightColors.onPrimaryFixedVariant, DarkColors.onPrimaryFixedVariant),
        secondaryFixed = mix(LightColors.secondaryFixed, DarkColors.secondaryFixed),
        secondaryFixedDim = mix(LightColors.secondaryFixedDim, DarkColors.secondaryFixedDim),
        onSecondaryFixed = mix(LightColors.onSecondaryFixed, DarkColors.onSecondaryFixed),
        onSecondaryFixedVariant = mix(LightColors.onSecondaryFixedVariant, DarkColors.onSecondaryFixedVariant),
        tertiaryFixed = mix(LightColors.tertiaryFixed, DarkColors.tertiaryFixed),
        tertiaryFixedDim = mix(LightColors.tertiaryFixedDim, DarkColors.tertiaryFixedDim),
        onTertiaryFixed = mix(LightColors.onTertiaryFixed, DarkColors.onTertiaryFixed),
        onTertiaryFixedVariant = mix(LightColors.onTertiaryFixedVariant, DarkColors.onTertiaryFixedVariant),
    )
}

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
    val darkFraction by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.StandardMillis,
            easing = AppMotion.StandardEasing,
        ),
        label = "app theme palette",
    )
    val colorScheme = blendedColorScheme(darkFraction)

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
