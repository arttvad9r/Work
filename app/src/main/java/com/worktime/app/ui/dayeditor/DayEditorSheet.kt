package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.components.AppDestructiveAction
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppFieldValueSlot
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.components.AppPrimaryButton
import com.worktime.app.ui.components.CompactInputChrome
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.parseDecimalMicros
import com.worktime.app.ui.format.sanitizeMoneyInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DayEditorSheet(
    date: LocalDate,
    existing: WorkEntry?,
    defaultHourlyRateMicros: Long,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    key(date.toEpochDay(), existing) {
        DayEditorSheetContent(
            date = date,
            existing = existing,
            defaultHourlyRateMicros = defaultHourlyRateMicros,
            operationErrorMessage = operationErrorMessage,
            onDismiss = onDismiss,
            onSave = onSave,
            onDelete = onDelete,
        )
    }
}

internal enum class NumericField(val labelTextRes: Int) {
    Duration(R.string.worked),
    Rate(R.string.at_rate),
    Bonus(R.string.bonus),
    Penalty(R.string.penalty),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun DayEditorSheetContent(
    date: LocalDate,
    existing: WorkEntry?,
    defaultHourlyRateMicros: Long,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    val durationState = rememberTextFieldState(
        initialText = formatDurationEditorInput(existing?.workedMinutes ?: 0),
    )
    val rateState = rememberTextFieldState(
        initialText = formatMoneyEditorInput(existing?.hourlyRateMicros ?: defaultHourlyRateMicros),
    )
    val bonusState = rememberTextFieldState(
        initialText = formatMoneyEditorInput(existing?.bonusMicros ?: 0L),
    )
    val penaltyState = rememberTextFieldState(
        initialText = formatMoneyEditorInput(existing?.penaltyMicros ?: 0L),
    )
    val durationInputTransformation = remember {
        InputTransformation.byValue { _, proposed -> sanitizeDurationInput(proposed.toString()) }
    }
    val moneyInputTransformation = remember {
        InputTransformation.byValue { _, proposed -> sanitizeMoneyInput(proposed.toString()) }
    }
    val numericKeyboardOptions = remember {
        KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        )
    }

    // One focusable editor moves between the fixed value slots. Keeping the same
    // node preserves the platform IME session when the user switches numeric fields.
    val editorState = rememberTextFieldState(initialText = durationState.text.toString())
    var activeField by remember { mutableStateOf(NumericField.Duration) }
    var editorHasFocus by remember { mutableStateOf(false) }
    var focusEditorOnActivate by remember { mutableStateOf(false) }

    var bonusVisible by rememberSaveable { mutableStateOf((existing?.bonusMicros ?: 0L) > 0L) }
    var penaltyVisible by rememberSaveable { mutableStateOf((existing?.penaltyMicros ?: 0L) > 0L) }

    val editorFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun backingState(field: NumericField): TextFieldState = when (field) {
        NumericField.Duration -> durationState
        NumericField.Rate -> rateState
        NumericField.Bonus -> bonusState
        NumericField.Penalty -> penaltyState
    }

    val activateField: (NumericField) -> Unit = { target ->
        if (target != activeField) {
            backingState(activeField).setTextAndPlaceCursorAtEnd(editorState.text.toString())
            activeField = target
            editorState.setTextAndPlaceCursorAtEnd(backingState(target).text.toString())
        }

        if (!editorHasFocus) {
            focusEditorOnActivate = true
        }
    }

    LaunchedEffect(focusEditorOnActivate, activeField) {
        if (focusEditorOnActivate) {
            editorFocusRequester.requestFocus()
            keyboardController?.show()
            focusEditorOnActivate = false
        }
    }

    LaunchedEffect(editorState, activeField) {
        snapshotFlow { editorState.text.toString() }.collect { editorText ->
            val state = backingState(activeField)
            if (state.text.toString() != editorText) {
                state.setTextAndPlaceCursorAtEnd(editorText)
            }
        }
    }

    LaunchedEffect(editorHasFocus) {
        if (!editorHasFocus) {
            when (activeField) {
                NumericField.Bonus ->
                    if (bonusState.text.isBlank()) bonusVisible = false
                NumericField.Penalty ->
                    if (penaltyState.text.isBlank()) penaltyVisible = false
                else -> Unit
            }
        }
    }

    val showBonus = {
        bonusVisible = true
        activateField(NumericField.Bonus)
    }
    val showPenalty = {
        penaltyVisible = true
        activateField(NumericField.Penalty)
    }

    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationErrorMessage)
        }
    }

    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    fun valueFor(field: NumericField): String =
        if (activeField == field) editorState.text.toString() else backingState(field).text.toString()

    val duration = valueFor(NumericField.Duration)
    val rate = valueFor(NumericField.Rate)
    val bonus = valueFor(NumericField.Bonus)
    val penalty = valueFor(NumericField.Penalty)

    val parsedDuration = parseDurationInput(duration)
    val parsedHours = parsedDuration?.hours
    val parsedMinutes = parsedDuration?.minutes
    val hoursValid = parsedHours != null && parsedHours in 0..24
    val minutesValid = parsedMinutes != null && parsedMinutes in 0..59
    val durationValid = hoursValid && minutesValid && !(parsedHours == 24 && parsedMinutes != 0)
    val workedMinutes = if (durationValid) parsedHours * 60 + parsedMinutes else null

    val parsedRate = parseMoneyOrNull(rate)
    val parsedBonus = parseMoneyOrNull(bonus)
    val parsedPenalty = parseMoneyOrNull(penalty)
    val rateWithinLimit = parsedRate != null && parsedRate <= MoneyLimits.MAX_COMPONENT_MICROS
    val bonusWithinLimit = parsedBonus != null && parsedBonus <= MoneyLimits.MAX_COMPONENT_MICROS
    val penaltyWithinLimit = parsedPenalty != null && parsedPenalty <= MoneyLimits.MAX_COMPONENT_MICROS
    val positiveRateRequired = (workedMinutes ?: 0) > 0
    val rateValid = rateWithinLimit && (!positiveRateRequired || parsedRate > 0L)
    val hasEffectiveData = workedMinutes != null && (
        workedMinutes > 0 || (parsedBonus ?: 0L) > 0L || (parsedPenalty ?: 0L) > 0L
    )

    val draft = if (
        durationValid && rateValid && bonusWithinLimit && penaltyWithinLimit && hasEffectiveData
    ) {
        runCatching {
            WorkEntry(
                date = date,
                workedMinutes = workedMinutes,
                hourlyRateMicros = parsedRate,
                bonusMicros = parsedBonus,
                penaltyMicros = parsedPenalty,
                note = existing?.note.orEmpty(),
            )
        }.getOrNull()
    } else {
        null
    }

    val durationHasError = !hoursValid || !minutesValid || !durationValid
    val rateHasError =
        parsedRate == null ||
            parsedRate > MoneyLimits.MAX_COMPONENT_MICROS ||
            (positiveRateRequired && parsedRate == 0L)
    val bonusHasError = parsedBonus == null || parsedBonus > MoneyLimits.MAX_COMPONENT_MICROS
    val penaltyHasError = parsedPenalty == null || parsedPenalty > MoneyLimits.MAX_COMPONENT_MICROS
    val totalMicros = draft?.let { runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull() }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = date.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM", LocalLocale.current.platformLocale),
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
            ) {
                NumericEditorSection(
                    durationState = durationState,
                    rateState = rateState,
                    bonusState = bonusState,
                    penaltyState = penaltyState,
                    editorState = editorState,
                    activeField = activeField,
                    bonusVisible = bonusVisible,
                    penaltyVisible = penaltyVisible,
                    durationInputTransformation = durationInputTransformation,
                    moneyInputTransformation = moneyInputTransformation,
                    numericKeyboardOptions = numericKeyboardOptions,
                    durationHasError = durationHasError,
                    rateHasError = rateHasError,
                    bonusHasError = bonusHasError,
                    penaltyHasError = penaltyHasError,
                    onActivateField = activateField,
                    onShowBonus = showBonus,
                    onShowPenalty = showPenalty,
                    onNext = {
                        when (activeField) {
                            NumericField.Duration -> activateField(NumericField.Rate)
                            NumericField.Rate -> when {
                                bonusVisible -> activateField(NumericField.Bonus)
                                penaltyVisible -> activateField(NumericField.Penalty)
                            }
                            NumericField.Bonus -> if (penaltyVisible) {
                                activateField(NumericField.Penalty)
                            }
                            NumericField.Penalty -> Unit
                        }
                    },
                    editorFocusRequester = editorFocusRequester,
                    onEditorFocusChanged = { editorHasFocus = it },
                )

                CalculationSummary(
                    draft = draft,
                    totalMicros = totalMicros,
                )

                AppPrimaryButton(
                    text = stringResource(R.string.save),
                    onClick = { draft?.let(onSave) },
                    enabled = draft != null && totalMicros != null,
                )
                if (existing != null) {
                    AppDestructiveAction(
                        text = stringResource(R.string.delete_entry),
                        onClick = { confirmDelete = true },
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(AppDimens.screenHorizontalPadding),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_entry)) },
            text = { Text(stringResource(R.string.delete_entry_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(date)
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun NumericEditorSection(
    durationState: TextFieldState,
    rateState: TextFieldState,
    bonusState: TextFieldState,
    penaltyState: TextFieldState,
    editorState: TextFieldState,
    activeField: NumericField,
    bonusVisible: Boolean,
    penaltyVisible: Boolean,
    durationInputTransformation: InputTransformation,
    moneyInputTransformation: InputTransformation,
    numericKeyboardOptions: KeyboardOptions,
    durationHasError: Boolean,
    rateHasError: Boolean,
    bonusHasError: Boolean,
    penaltyHasError: Boolean,
    onActivateField: (NumericField) -> Unit,
    onShowBonus: () -> Unit,
    onShowPenalty: () -> Unit,
    onNext: () -> Unit,
    editorFocusRequester: FocusRequester,
    onEditorFocusChanged: (Boolean) -> Unit,
) {
    val rowHeight = AppDimens.rowMinHeight
    val rateY = rowHeight
    val adjustmentTop = rateY + rowHeight
    val expandedAdjustments = bonusVisible || penaltyVisible
    val penaltyTop = adjustmentTop + rowHeight
    val sectionHeight = penaltyTop + rowHeight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sectionHeight),
    ) {
        if (activeField != NumericField.Duration) {
            EditorValueRow(
                label = stringResource(NumericField.Duration.labelTextRes),
                valueText = durationState.text.toString().ifBlank { null },
                placeholderText = stringResource(R.string.duration_placeholder),
                isError = durationHasError,
                onClick = { onActivateField(NumericField.Duration) },
                modifier = Modifier
                    .offset(y = 0.dp)
                    .testTag("day-editor-row-duration")
                    .fillMaxWidth()
                    .height(rowHeight),
            )
        }
        if (activeField != NumericField.Rate) {
            EditorValueRow(
                label = stringResource(NumericField.Rate.labelTextRes),
                valueText = rateState.text.toString().ifBlank { null },
                placeholderText = "—",
                isError = rateHasError,
                onClick = { onActivateField(NumericField.Rate) },
                modifier = Modifier
                    .offset(y = rateY)
                    .testTag("day-editor-row-rate")
                    .fillMaxWidth()
                    .height(rowHeight),
            )
        }

        if (!expandedAdjustments) {
            AdjustmentAddRow(
                label = stringResource(R.string.bonus),
                onClick = onShowBonus,
                modifier = Modifier
                    .offset(y = adjustmentTop)
                    .testTag("day-editor-row-bonus")
                    .fillMaxWidth()
                    .height(rowHeight),
            )
            AdjustmentAddRow(
                label = stringResource(R.string.penalty),
                onClick = onShowPenalty,
                modifier = Modifier
                    .offset(y = penaltyTop)
                    .testTag("day-editor-row-penalty")
                    .fillMaxWidth()
                    .height(rowHeight),
            )
        } else {
            if (bonusVisible) {
                if (activeField != NumericField.Bonus) {
                    EditorValueRow(
                        label = stringResource(NumericField.Bonus.labelTextRes),
                        valueText = bonusState.text.toString().ifBlank { null },
                        placeholderText = null,
                        isError = bonusHasError,
                        onClick = { onActivateField(NumericField.Bonus) },
                        modifier = Modifier
                            .offset(y = adjustmentTop)
                            .testTag("day-editor-row-bonus")
                            .fillMaxWidth()
                            .height(rowHeight),
                    )
                }
            } else {
                AdjustmentAddRow(
                    label = stringResource(R.string.bonus),
                    onClick = onShowBonus,
                    modifier = Modifier
                        .offset(y = adjustmentTop)
                        .testTag("day-editor-row-bonus")
                        .fillMaxWidth()
                        .height(rowHeight),
                )
            }

            if (penaltyVisible) {
                if (activeField != NumericField.Penalty) {
                    EditorValueRow(
                        label = stringResource(NumericField.Penalty.labelTextRes),
                        valueText = penaltyState.text.toString().ifBlank { null },
                        placeholderText = null,
                        isError = penaltyHasError,
                        onClick = { onActivateField(NumericField.Penalty) },
                        modifier = Modifier
                            .offset(y = penaltyTop)
                            .testTag("day-editor-row-penalty")
                            .fillMaxWidth()
                            .height(rowHeight),
                    )
                }
            } else {
                AdjustmentAddRow(
                    label = stringResource(R.string.penalty),
                    onClick = onShowPenalty,
                    modifier = Modifier
                        .offset(y = penaltyTop)
                        .testTag("day-editor-row-penalty")
                        .fillMaxWidth()
                        .height(rowHeight),
                )
            }
        }

        PersistentNumericEditor(
            state = editorState,
            activeField = activeField,
            durationInputTransformation = durationInputTransformation,
            moneyInputTransformation = moneyInputTransformation,
            durationHasError = durationHasError,
            rateHasError = rateHasError,
            bonusHasError = bonusHasError,
            penaltyHasError = penaltyHasError,
            keyboardOptions = numericKeyboardOptions,
            onNext = onNext,
            editorFocusRequester = editorFocusRequester,
            onEditorFocusChanged = onEditorFocusChanged,
            modifier = Modifier
                .offset(
                    y = when (activeField) {
                        NumericField.Duration -> 0.dp
                        NumericField.Rate -> rateY
                        NumericField.Bonus -> adjustmentTop
                        NumericField.Penalty -> penaltyTop
                    },
                )
                .testTag("day-editor-active-field")
                .fillMaxWidth()
                .height(rowHeight),
        )
    }
}

@Composable
private fun EditorValueRow(
    label: String,
    valueText: String?,
    placeholderText: String?,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        AppFieldValueSlot {
            Text(
                text = valueText ?: placeholderText.orEmpty(),
                modifier = Modifier.padding(end = AppDimens.rowGap),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    valueText != null -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AdjustmentAddRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PersistentNumericEditor(
    state: TextFieldState,
    activeField: NumericField,
    durationInputTransformation: InputTransformation,
    moneyInputTransformation: InputTransformation,
    durationHasError: Boolean,
    rateHasError: Boolean,
    bonusHasError: Boolean,
    penaltyHasError: Boolean,
    keyboardOptions: KeyboardOptions,
    onNext: () -> Unit,
    editorFocusRequester: FocusRequester,
    onEditorFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDuration = activeField == NumericField.Duration
    val label = if (activeField == NumericField.Rate) {
        stringResource(R.string.hourly_rate)
    } else {
        stringResource(activeField.labelTextRes)
    }
    val isError = when (activeField) {
        NumericField.Duration -> durationHasError
        NumericField.Rate -> rateHasError
        NumericField.Bonus -> bonusHasError
        NumericField.Penalty -> penaltyHasError
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        CompactInputChrome(isError = isError) {
            BasicTextField(
                state = state,
                inputTransformation = if (isDuration) durationInputTransformation else moneyInputTransformation,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = { onNext() },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(editorFocusRequester)
                    .onFocusChanged { onEditorFocusChanged(it.isFocused) },
            )
            if (isDuration && state.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.duration_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CalculationSummary(
    draft: WorkEntry?,
    totalMicros: Long?,
) {
    if (draft == null || totalMicros == null) return
    val locale = LocalLocale.current.platformLocale
    val entryPay = SalaryCalculator.entryPay(draft)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CalculationRow(
            label = stringResource(
                R.string.calc_expression,
                formatDurationCompact(draft.workedMinutes),
                formatDecimalMicros(draft.hourlyRateMicros),
            ),
            value = stringResource(
                R.string.amount_with_currency,
                formatAmountMicros(entryPay.basePayMicros, locale),
            ),
        )
        if (draft.bonusMicros > 0L) {
            CalculationRow(
                label = stringResource(R.string.calculation_bonus),
                value = "+" + stringResource(
                    R.string.amount_with_currency,
                    formatAmountMicros(draft.bonusMicros, locale),
                ),
            )
        }
        if (draft.penaltyMicros > 0L) {
            CalculationRow(
                label = stringResource(R.string.calculation_penalty),
                value = "−" + stringResource(
                    R.string.amount_with_currency,
                    formatAmountMicros(draft.penaltyMicros, locale),
                ),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CalculationRow(
            label = stringResource(R.string.calculation_total),
            value = stringResource(
                R.string.amount_with_currency,
                formatAmountMicros(totalMicros, locale),
            ),
            emphasized = true,
        )
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    val textStyle = if (emphasized) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val fontWeight = if (emphasized) FontWeight.SemiBold else null
    val labelColor = if (emphasized) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = textStyle,
            fontWeight = fontWeight,
            color = labelColor,
            maxLines = 1,
        )
        Text(
            text = value,
            style = textStyle,
            fontWeight = fontWeight,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private data class DurationInput(val hours: Int, val minutes: Int)

private fun formatDurationEditorInput(workedMinutes: Int): String =
    if (workedMinutes == 0) "" else formatDurationCompact(workedMinutes)

private fun formatMoneyEditorInput(micros: Long): String =
    if (micros == 0L) "" else formatDecimalMicros(micros)

internal fun sanitizeDurationInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == ':' }
    val firstColon = filtered.indexOf(':')
    if (firstColon >= 0) {
        val rawHours = filtered.take(firstColon).filter(Char::isDigit)
        val hours = normalizeLeadingZeroes(rawHours).take(2)
        val minutes = filtered.drop(firstColon + 1).filter(Char::isDigit).take(2)
        return "$hours:$minutes"
    }

    val rawDigits = filtered.filter(Char::isDigit).take(4)
    val digits = if (rawDigits.length > 1) normalizeLeadingZeroes(rawDigits) else rawDigits
    return when (digits.length) {
        0, 1 -> digits
        2 -> {
            if (digits.toInt() <= 24) digits else "${digits.take(1)}:${digits.drop(1)}"
        }
        3 -> {
            val twoDigitHours = digits.take(2).toInt()
            if (twoDigitHours <= 24) {
                "${digits.take(2)}:${digits.drop(2)}"
            } else {
                "${digits.take(1)}:${digits.drop(1)}"
            }
        }
        else -> "${digits.take(2)}:${digits.drop(2)}"
    }
}

private fun normalizeLeadingZeroes(value: String): String {
    if (value.isEmpty()) return value
    return value.trimStart('0').ifEmpty { "0" }
}

private fun parseDurationInput(text: String): DurationInput? {
    if (text.isBlank()) return DurationInput(hours = 0, minutes = 0)
    val parts = text.split(':')
    if (parts.size > 2) return null
    val hours = parts[0].ifBlank { "0" }.toIntOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.ifBlank { "0" }?.toIntOrNull() ?: 0
    return DurationInput(hours = hours, minutes = minutes)
}

private fun parseMoneyOrNull(text: String): Long? = runCatching { parseDecimalMicros(text) }.getOrNull()
