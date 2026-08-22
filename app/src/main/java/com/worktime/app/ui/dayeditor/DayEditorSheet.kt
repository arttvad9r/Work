package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.components.PlainDragHandle
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

private enum class NumericField {
    Duration,
    Rate,
    Bonus,
    Penalty,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayEditorSheetContent(
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

    // Keep one Android/Compose input session for every numeric logical field. ADB
    // traces on the target device showed that transferring focus between separate
    // TextFields caused client-side IME hide -> empty EditorInfo -> restartInput ->
    // show. The same editable node now moves between all visible numeric slots.
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

        // If the persistent editor already owns focus, do not request it again: this
        // is what preserves the existing platform input session during field switches.
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlainDragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

                Text(
                    text = date.format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", LocalLocale.current.platformLocale),
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

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

                Button(
                    onClick = { draft?.let(onSave) },
                    enabled = draft != null && totalMicros != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
                if (existing != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(R.string.delete_entry))
                    }
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
    val fieldHeight = 64.dp
    val buttonHeight = 48.dp
    val gap = 8.dp
    val adjustmentTop = fieldHeight + gap
    val expandedAdjustments = bonusVisible || penaltyVisible
    val bonusSlotHeight = if (bonusVisible) fieldHeight else buttonHeight
    val penaltySlotHeight = if (penaltyVisible) fieldHeight else buttonHeight
    val sectionHeight = if (!expandedAdjustments) {
        adjustmentTop + buttonHeight
    } else {
        adjustmentTop + bonusSlotHeight + gap + penaltySlotHeight
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(sectionHeight),
    ) {
        val halfWidth = (maxWidth - gap) / 2f
        val rateX = halfWidth + gap

        if (activeField != NumericField.Duration) {
            PassiveDurationField(
                state = durationState,
                label = stringResource(R.string.worked),
                isError = durationHasError,
                onClick = { onActivateField(NumericField.Duration) },
                modifier = Modifier
                    .width(halfWidth)
                    .height(fieldHeight),
            )
        }
        if (activeField != NumericField.Rate) {
            PassiveMoneyField(
                state = rateState,
                label = stringResource(R.string.hourly_rate),
                isError = rateHasError,
                onClick = { onActivateField(NumericField.Rate) },
                modifier = Modifier
                    .offset(x = rateX)
                    .width(halfWidth)
                    .height(fieldHeight),
            )
        }

        if (!expandedAdjustments) {
            AdjustmentButton(
                text = stringResource(R.string.add_bonus),
                onClick = onShowBonus,
                modifier = Modifier
                    .offset(y = adjustmentTop)
                    .width(halfWidth)
                    .height(buttonHeight),
            )
            AdjustmentButton(
                text = stringResource(R.string.add_penalty),
                onClick = onShowPenalty,
                modifier = Modifier
                    .offset(x = rateX, y = adjustmentTop)
                    .width(halfWidth)
                    .height(buttonHeight),
            )
        } else {
            if (bonusVisible) {
                if (activeField != NumericField.Bonus) {
                    PassiveMoneyField(
                        state = bonusState,
                        label = stringResource(R.string.bonus),
                        isError = bonusHasError,
                        onClick = { onActivateField(NumericField.Bonus) },
                        modifier = Modifier
                            .offset(y = adjustmentTop)
                            .fillMaxWidth()
                            .height(fieldHeight),
                    )
                }
            } else {
                AdjustmentButton(
                    text = stringResource(R.string.add_bonus),
                    onClick = onShowBonus,
                    modifier = Modifier
                        .offset(y = adjustmentTop)
                        .fillMaxWidth()
                        .height(buttonHeight),
                )
            }

            val penaltyTop = adjustmentTop + bonusSlotHeight + gap
            if (penaltyVisible) {
                if (activeField != NumericField.Penalty) {
                    PassiveMoneyField(
                        state = penaltyState,
                        label = stringResource(R.string.penalty),
                        isError = penaltyHasError,
                        onClick = { onActivateField(NumericField.Penalty) },
                        modifier = Modifier
                            .offset(y = penaltyTop)
                            .fillMaxWidth()
                            .height(fieldHeight),
                    )
                }
            } else {
                AdjustmentButton(
                    text = stringResource(R.string.add_penalty),
                    onClick = onShowPenalty,
                    modifier = Modifier
                        .offset(y = penaltyTop)
                        .fillMaxWidth()
                        .height(buttonHeight),
                )
            }
        }

        val editorX: Dp
        val editorY: Dp
        val editorWidth: Dp
        when (activeField) {
            NumericField.Duration -> {
                editorX = 0.dp
                editorY = 0.dp
                editorWidth = halfWidth
            }
            NumericField.Rate -> {
                editorX = rateX
                editorY = 0.dp
                editorWidth = halfWidth
            }
            NumericField.Bonus -> {
                editorX = 0.dp
                editorY = adjustmentTop
                editorWidth = maxWidth
            }
            NumericField.Penalty -> {
                editorX = 0.dp
                editorY = adjustmentTop + bonusSlotHeight + gap
                editorWidth = maxWidth
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
            modifier = Modifier
                .offset(x = editorX, y = editorY)
                .width(editorWidth)
                .height(fieldHeight)
                .focusRequester(editorFocusRequester)
                .onFocusChanged { onEditorFocusChanged(it.isFocused) },
        )
    }
}

@Composable
private fun AdjustmentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.focusProperties { canFocus = false },
    ) {
        Text(text, maxLines = 1)
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
    modifier: Modifier = Modifier,
) {
    val isDuration = activeField == NumericField.Duration
    val label = when (activeField) {
        NumericField.Duration -> stringResource(R.string.worked)
        NumericField.Rate -> stringResource(R.string.hourly_rate)
        NumericField.Bonus -> stringResource(R.string.bonus)
        NumericField.Penalty -> stringResource(R.string.penalty)
    }
    val isError = when (activeField) {
        NumericField.Duration -> durationHasError
        NumericField.Rate -> rateHasError
        NumericField.Bonus -> bonusHasError
        NumericField.Penalty -> penaltyHasError
    }

    OutlinedTextField(
        state = state,
        inputTransformation = if (isDuration) durationInputTransformation else moneyInputTransformation,
        label = { Text(label, maxLines = 1) },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        placeholder = if (isDuration) {
            {
                Text(
                    text = stringResource(R.string.duration_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        } else {
            null
        },
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
        ),
        isError = isError,
        modifier = modifier,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = { onNext() },
    )
}

@Composable
private fun PassiveDurationField(
    state: TextFieldState,
    label: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        DurationField(
            state = state,
            inputTransformation = null,
            label = label,
            isError = isError,
            keyboardOptions = KeyboardOptions.Default,
            readOnly = true,
            modifier = Modifier
                .matchParentSize()
                .focusProperties { canFocus = false },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}

@Composable
private fun PassiveMoneyField(
    state: TextFieldState,
    label: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        MoneyField(
            state = state,
            inputTransformation = null,
            label = label,
            isError = isError,
            keyboardOptions = KeyboardOptions.Default,
            readOnly = true,
            modifier = Modifier
                .matchParentSize()
                .focusProperties { canFocus = false },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}

@Composable
private fun CalculationSummary(
    draft: WorkEntry?,
    totalMicros: Long?,
) {
    val locale = LocalLocale.current.platformLocale
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.calculation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (draft != null && totalMicros != null) {
                if (draft.bonusMicros > 0L || draft.penaltyMicros > 0L) {
                    val entryPay = SalaryCalculator.entryPay(draft)
                    CalculationRow(
                        label = stringResource(R.string.calculation_base),
                        value = formatAmountMicros(entryPay.basePayMicros, locale),
                    )
                    if (draft.bonusMicros > 0L) {
                        CalculationRow(
                            label = "+ ${stringResource(R.string.calculation_bonus)}",
                            value = formatAmountMicros(draft.bonusMicros, locale),
                        )
                    }
                    if (draft.penaltyMicros > 0L) {
                        CalculationRow(
                            label = "− ${stringResource(R.string.calculation_penalty)}",
                            value = formatAmountMicros(draft.penaltyMicros, locale),
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f),
                    )
                }
                CalculationRow(
                    label = stringResource(R.string.calculation_total),
                    value = formatAmountMicros(totalMicros, locale),
                    emphasized = true,
                )
            } else {
                Text(
                    text = stringResource(R.string.total_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = textStyle, fontWeight = if (emphasized) FontWeight.Bold else null)
        Text(text = value, style = textStyle, fontWeight = if (emphasized) FontWeight.Bold else null)
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

@Composable
private fun DurationField(
    state: TextFieldState,
    inputTransformation: InputTransformation?,
    label: String,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        state = state,
        inputTransformation = inputTransformation,
        label = { Text(label, maxLines = 1) },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        placeholder = {
            Text(
                text = stringResource(R.string.duration_placeholder),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        },
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
        ),
        isError = isError,
        modifier = modifier,
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
private fun MoneyField(
    state: TextFieldState,
    inputTransformation: InputTransformation?,
    label: String,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        state = state,
        inputTransformation = inputTransformation,
        label = { Text(label, maxLines = 1) },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
        ),
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
    )
}
