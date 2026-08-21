package com.worktime.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.parseDecimalMicros
import com.worktime.app.ui.format.sanitizeMoneyInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    defaultHourlyRateMicros: Long,
    themeMode: ThemeMode,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Long, ThemeMode) -> Unit,
    onPreviewTheme: (ThemeMode) -> Unit,
) {
    var rate by rememberSaveable(defaultHourlyRateMicros) {
        mutableStateOf(formatDecimalMicros(defaultHourlyRateMicros))
    }
    var selectedTheme by rememberSaveable(themeMode) { mutableStateOf(themeMode) }

    val parsedRate = runCatching { parseDecimalMicros(rate) }.getOrNull()
    val rateValid = parsedRate != null && parsedRate <= MoneyLimits.MAX_COMPONENT_MICROS
    val canSave = rateValid
    val rateError = when {
        parsedRate == null -> stringResource(R.string.invalid_money_value)
        parsedRate > MoneyLimits.MAX_COMPONENT_MICROS -> stringResource(
            R.string.money_value_too_large,
            formatDecimalMicros(MoneyLimits.MAX_COMPONENT_MICROS),
        )
        else -> null
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rateLabel = stringResource(R.string.default_hourly_rate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rateLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = rate,
                            onValueChange = { rate = sanitizeMoneyInput(it) },
                            isError = rateError != null,
                            modifier = Modifier
                                .width(120.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && rate == "0") {
                                        rate = ""
                                    } else if (!focusState.isFocused && rate.isBlank()) {
                                        rate = "0"
                                    }
                                }
                                .semantics { contentDescription = rateLabel },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                textAlign = TextAlign.Center,
                                lineHeight = MaterialTheme.typography.titleMedium.fontSize,
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }

                    if (rateError != null) {
                        Text(
                            text = rateError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
            }

            if (operationErrorMessage != null) {
                Text(
                    text = operationErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
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
    }
}

@Composable
private fun themeLabel(themeMode: ThemeMode): String = when (themeMode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}
