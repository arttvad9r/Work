package com.worktime.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.parseDecimalMicros

@Composable
fun SettingsScreen(
    defaultHourlyRateMicros: Long,
    themeMode: ThemeMode,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onRateChange: (Long) -> Unit,
    onOpenRateHistory: () -> Unit,
    onExportData: () -> Unit,
    onExportCsv: () -> Unit,
    onImportData: () -> Unit,
) {
    var rateEditing by rememberSaveable { mutableStateOf(false) }
    var exportFormatOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationErrorMessage)
        }
    }

    // Surface sets LocalContentColor=onSurface so titles/icons follow the theme.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            AppTopBar(
                title = stringResource(R.string.settings),
                onBack = onDismiss,
            )
            SectionLabel(stringResource(R.string.section_calculation))
            RateRow(
                rateMicros = defaultHourlyRateMicros,
                editing = rateEditing,
                onEdit = { rateEditing = true },
                onDone = { rateEditing = false },
                onRateChange = onRateChange,
            )
            SettingsRow(
                label = stringResource(R.string.rate_history),
                onClick = onOpenRateHistory,
            )

            SectionDivider()

            SectionLabel(stringResource(R.string.section_appearance))
            ThemeSegmentedControl(
                selected = themeMode,
                onSelect = onThemeChange,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            SectionDivider()

            SectionLabel(stringResource(R.string.section_data))
            SettingsRow(
                label = stringResource(R.string.export_data),
                onClick = { exportFormatOpen = true },
            )
            SettingsRow(
                label = stringResource(R.string.import_data),
                onClick = onImportData,
            )

            Box(modifier = Modifier.navigationBarsPadding().height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
        }
    }

    if (exportFormatOpen) {
        ExportFormatDialog(
            onSelectJson = {
                exportFormatOpen = false
                onExportData()
            },
            onSelectCsv = {
                exportFormatOpen = false
                onExportCsv()
            },
            onDismiss = { exportFormatOpen = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun RateRow(
    rateMicros: Long,
    editing: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onRateChange: (Long) -> Unit,
) {
    var rateInput by rememberSaveable(editing) {
        mutableStateOf(formatDecimalMicros(rateMicros))
    }
    if (!editing) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onEdit),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_rate),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val parsed = runCatching { parseDecimalMicros(rateInput) }.getOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_rate),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        CompactMoneyField(
            text = rateInput,
            onTextChange = { text ->
                rateInput = text
                val value = runCatching { parseDecimalMicros(text) }.getOrNull()
                if (value != null && value <= MoneyLimits.MAX_COMPONENT_MICROS) {
                    onRateChange(value)
                }
            },
            isError = parsed == null || parsed > MoneyLimits.MAX_COMPONENT_MICROS,
            contentDescription = stringResource(R.string.settings_rate),
            autoFocus = true,
            onLostFocus = onDone,
        )
    }
}

@Composable
private fun ThemeSegmentedControl(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    activeBorderColor = Color.Transparent,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                ),
                icon = {},
                label = {
                    Text(
                        text = themeLabel(mode),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun themeLabel(themeMode: ThemeMode): String = when (themeMode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatDialog(
    onSelectJson: () -> Unit,
    onSelectCsv: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.export_format_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ExportFormatOption(
                title = stringResource(R.string.export_json_option),
                subtitle = stringResource(R.string.export_json_hint),
                onClick = onSelectJson,
            )
            ExportFormatOption(
                title = stringResource(R.string.export_csv_option),
                subtitle = stringResource(R.string.export_csv_hint),
                onClick = onSelectCsv,
            )
        }
    }
}

@Composable
private fun ExportFormatOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
