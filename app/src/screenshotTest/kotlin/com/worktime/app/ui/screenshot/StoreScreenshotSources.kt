package com.worktime.app.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.worktime.app.R
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppRowDivider
import com.worktime.app.ui.components.AppSectionSurface
import com.worktime.app.ui.dayeditor.DayEditorSheet
import com.worktime.app.ui.settings.PrivacyScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.LocalDate

/**
 * Reproducible real-UI source states used only to compose the RuStore gallery.
 * These previews intentionally use the production components and strings rather
 * than drawing marketing-only facsimiles of app screens.
 */

@PreviewTest
@Preview(name = "Store day editor populated", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun StoreDayEditorPopulatedScreenshot() {
    val date = LocalDate.of(2025, 2, 14)
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        DayEditorSheet(
            date = date,
            existing = WorkEntry(
                date = date,
                workedMinutes = 8 * 60,
                hourlyRateMicros = 500_000_000L,
                bonusMicros = 1_200_000_000L,
                penaltyMicros = 250_000_000L,
            ),
            defaultHourlyRateMicros = 500_000_000L,
            operationErrorMessage = null,
            onDismiss = {},
            onSave = {},
            onDelete = {},
        )
    }
}

@PreviewTest
@Preview(name = "Store privacy dark", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun StorePrivacyDarkScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.DARK) {
        PrivacyScreen(onDismiss = {})
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@PreviewTest
@Preview(name = "Store export format", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun StoreExportFormatScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Mirrors SettingsScreen's production export-format sheet using the
            // same shared primitives and the same localized resources.
            AppModalBottomSheet(
                onDismissRequest = {},
                title = stringResource(R.string.export_format_title),
            ) {
                AppSectionSurface {
                    AppNavigationRow(
                        label = stringResource(R.string.export_json_option),
                        subtitle = stringResource(R.string.export_json_hint),
                        onClick = {},
                    )
                    AppRowDivider()
                    AppNavigationRow(
                        label = stringResource(R.string.export_csv_option),
                        subtitle = stringResource(R.string.export_csv_hint),
                        onClick = {},
                    )
                }
            }
        }
    }
}
