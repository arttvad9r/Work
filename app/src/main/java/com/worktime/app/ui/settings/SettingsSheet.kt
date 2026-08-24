package com.worktime.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.parseDecimalMicros

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    defaultHourlyRateMicros: Long,
    themeMode: ThemeMode,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Long, ThemeMode) -> Unit,
    onPreviewTheme: (ThemeMode) -> Unit,
    onChangeRateForPeriod: () -> Unit,
    onOpenYearSummary: () -> Unit,
    onExportData: () -> Unit,
    onExportCsv: () -> Unit,
    onImportData: () -> Unit,
) {
    var rate by rememberSaveable(defaultHourlyRateMicros) {
        mutableStateOf(formatDecimalMicros(defaultHourlyRateMicros))
    }
    var selectedTheme by rememberSaveable(themeMode) { mutableStateOf(themeMode) }
    var exportFormatOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationErrorMessage)
        }
    }

    val parsedRate = runCatching { parseDecimalMicros(rate) }.getOrNull()
    val rateHasError = parsedRate == null || parsedRate > MoneyLimits.MAX_COMPONENT_MICROS
    val canSave = !rateHasError
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rateLabel = stringResource(R.string.default_hourly_rate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlainDragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                SettingsSection(title = stringResource(R.string.calculation)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rateLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        CompactMoneyField(
                            text = rate,
                            onTextChange = { rate = it },
                            isError = rateHasError,
                            contentDescription = rateLabel,
                        )
                    }
                    SettingsRow(
                        label = stringResource(R.string.change_rate_for_period),
                        onClick = onChangeRateForPeriod,
                    )
                }

                SettingsSection(title = stringResource(R.string.statistics)) {
                    SettingsRow(
                        label = stringResource(R.string.year_summary),
                        onClick = onOpenYearSummary,
                    )
                }

                SettingsSection(title = stringResource(R.string.appearance)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedTheme == mode,
                                onClick = {
                                    selectedTheme = mode
                                    onPreviewTheme(mode)
                                },
                                label = {
                                    Text(
                                        text = themeLabel(mode),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.weight(
                                    if (mode == ThemeMode.SYSTEM) 1.2f else 1f,
                                ),
                            )
                        }
                    }
                }

                SettingsSection(title = stringResource(R.string.data_and_operations)) {
                    SettingsRow(
                        label = stringResource(R.string.export_data),
                        onClick = { exportFormatOpen = true },
                    )
                    SettingsRow(
                        label = stringResource(R.string.import_data),
                        onClick = onImportData,
                    )
                }

                Button(
                    onClick = {
                        val safeRate = parsedRate ?: return@Button
                        onSave(safeRate, selectedTheme)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                ) {
                    Text(stringResource(R.string.save_settings))
                }
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
private fun ExportFormatDialog(
    onSelectJson: () -> Unit,
    onSelectCsv: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_format_title)) },
        text = {
            Column {
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
        },
        confirmButton = {},
    )
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
            .padding(vertical = 12.dp, horizontal = 4.dp),
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

@Composable
private fun themeLabel(themeMode: ThemeMode): String = when (themeMode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            // Surface stacks children like a Box; group rows in a Column so
            // sections with several rows lay out vertically instead of overlapping.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }
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
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
