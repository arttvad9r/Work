package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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

private enum class PrimaryNumericField {
    Duration,
    Rate,
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

    // Keep one platform input session for the two primary logical fields. The same
    // editable node moves between slots; switching Duration <-> Rate never transfers
    // Compose focus to another TextField.
    val primaryEditorState = rememberTextFieldState(initialText = durationState.text.toString())
    var activePrimaryField by remember { mutableStateOf(PrimaryNumericField.Duration) }
    var primaryEditorHasFocus by remember { mutableStateOf(false) }
    var focusPrimaryOnActivate by remember { mutableStateOf(false) }

    var bonusVisible by rememberSaveable { mutableStateOf((existing?.bonusMicros ?: 0L) > 0L) }
    var penaltyVisible by rememberSaveable { mutableStateOf((existing?.penaltyMicros ?: 0L) > 0L) }
    var focusBonusOnExpand by remember { mutableStateOf(false) }
    var focusPenaltyOnExpand by remember { mutableStateOf(false) }

    val primaryFocusRequester = remember { FocusRequester() }
    val bonusFocusRequester = remember { FocusRequester() }
    val penaltyFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val activatePrimaryField: (PrimaryNumericField) -> Unit = { target ->
        if (target != activePrimaryField) {
            val currentBackingState = when (activePrimaryField) {
                PrimaryNumericField.Duration -> durationState
                PrimaryNumericField.Rate -> rateState
            }
            currentBackingState.setTextAndPlaceCursorAtEnd(primaryEditorState.text.toString())

            activePrimaryField = target
            val targetBackingState = when (target) {
                PrimaryNumericField.Duration -> durationState
                PrimaryNumericField.Rate -> rateState
            }
            primaryEditorState.setTextAndPlaceCursorAtEnd(targetBackingState.text.toString())
        }

        // A tap on the passive slot must behave like a tap directly on a text field.
        // Request focus only when the persistent editor is not already focused; a
        // normal Duration <-> Rate switch therefore keeps the existing input session.
        if (!primaryEditorHasFocus) {
            focusPrimaryOnActivate = true
        }
    }

    LaunchedEffect(focusPrimaryOnActivate, activePrimaryField) {
        if (focusPrimaryOnActivate) {
            primaryFocusRequester.requestFocus()
            keyboardController?.show()
            focusPrimaryOnActivate = false
        }
    }

    LaunchedEffect(primaryEditorState, activePrimaryField) {
        snapshotFlow { primaryEditorState.text.toString() }.collect { editorText ->
            val backingState = when (activePrimaryField) {
                PrimaryNumericField.Duration -> durationState
                PrimaryNumericField.Rate -> rateState
            }
            if (backingState.text.toString() != editorText) {
                backingState.setTextAndPlaceCursorAtEnd(editorText)
            }
        }
    }

    val showBonus = {
        bonusVisible = true
        focusBonusOnExpand = true
    }
    val showPenalty = {
        penaltyVisible = true
        focusPenaltyOnExpand = true
    }

    LaunchedEffect(focusBonusOnExpand, bonusVisible) {
        if (focusBonusOnExpand && bonusVisible) {
            bonusFocusRequester.requestFocus()
            focusBonusOnExpand = false
        }
    }
    LaunchedEffect(focusPenaltyOnExpand, penaltyVisible) {
        if (focusPenaltyOnExpand && penaltyVisible) {
            penaltyFocusRequester.requestFocus()
            focusPenaltyOnExpand = false
        }
    }
    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationErrorMessage)
        }
    }

    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val duration = if (activePrimaryField == PrimaryNumericField.Duration) {
        primaryEditorState.text.toString()
    } else {
        durationState.text.toString()
    }
    val rate = if (activePrimaryField == PrimaryNumericField.Rate) {
        primaryEditorState.text.toString()
    } else {
        rateState.text.toString()
    }
    val bonus = bonusState.text.toString()
    val penalty = penaltyState.text.toString()

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

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val primaryFieldWidth = (maxWidth - 8.dp) / 2f

                    // Only the inactive logical field gets a passive shell. The active
                    // slot is occupied solely by PrimaryEditorField, so labels/text are
                    // never drawn twice on top of each other.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (activePrimaryField == PrimaryNumericField.Rate) {
                            PassiveDurationField(
                                state = durationState,
                                label = stringResource(R.string.worked),
                                isError = durationHasError,
                                onClick = { activatePrimaryField(PrimaryNumericField.Duration) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }

                        if (activePrimaryField == PrimaryNumericField.Duration) {
                            PassiveMoneyField(
                                state = rateState,
                                label = stringResource(R.string.hourly_rate),
                                isError = rateHasError,
                                onClick = { activatePrimaryField(PrimaryNumericField.Rate) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }

                    PrimaryEditorField(
                        state = primaryEditorState,
                        activeField = activePrimaryField,
                        durationInputTransformation = durationInputTransformation,
                        moneyInputTransformation = moneyInputTransformation,
                        durationLabel = stringResource(R.string.worked),
                        rateLabel = stringResource(R.string.hourly_rate),
                        durationHasError = durationHasError,
                        rateHasError = rateHasError,
                        keyboardOptions = numericKeyboardOptions,
                        onNext = {
                            when (activePrimaryField) {
                                PrimaryNumericField.Duration -> {
                                    activatePrimaryField(PrimaryNumericField.Rate)
                                }
                                PrimaryNumericField.Rate -> {
                                    when {
                                        bonusVisible -> bonusFocusRequester.requestFocus()
                                        penaltyVisible -> penaltyFocusRequester.requestFocus()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .width(primaryFieldWidth)
                            .align(
                                if (activePrimaryField == PrimaryNumericField.Duration) {
                                    Alignment.CenterStart
                                } else {
                                    Alignment.CenterEnd
                                },
                            )
                            .focusRequester(primaryFocusRequester)
                            .onFocusChanged { primaryEditorHasFocus = it.isFocused },
                    )
                }

                if (!bonusVisible && !penaltyVisible) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = showBonus,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .focusProperties { canFocus = false },
                        ) {
                            Text(stringResource(R.string.add_bonus), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = showPenalty,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .focusProperties { canFocus = false },
                        ) {
                            Text(stringResource(R.string.add_penalty), maxLines = 1)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bonusVisible) {
                            MoneyField(
                                state = bonusState,
                                inputTransformation = moneyInputTransformation,
                                label = stringResource(R.string.bonus),
                                isError = bonusHasError,
                                keyboardOptions = numericKeyboardOptions,
                                modifier = Modifier.focusRequester(bonusFocusRequester),
                            )
                        } else {
                            OutlinedButton(
                                onClick = showBonus,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .focusProperties { canFocus = false },
                            ) {
                                Text(stringResource(R.string.add_bonus), maxLines = 1)
                            }
                        }

                        if (penaltyVisible) {
                            MoneyField(
                                state = penaltyState,
                                inputTransformation = moneyInputTransformation,
                                label = stringResource(R.string.penalty),
                                isError = penaltyHasError,
                                keyboardOptions = numericKeyboardOptions,
                                modifier = Modifier.focusRequester(penaltyFocusRequester),
                            )
                        } else {
                            OutlinedButton(
                                onClick = showPenalty,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .focusProperties { canFocus = false },
                            ) {
                                Text(stringResource(R.string.add_penalty), maxLines = 1)
                            }
                        }
                    }
                }

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
private fun PrimaryEditorField(
    state: TextFieldState,
    activeField: PrimaryNumericField,
    durationInputTransformation: InputTransformation,
    moneyInputTransformation: InputTransformation,
    durationLabel: String,
    rateLabel: String,
    durationHasError: Boolean,
    rateHasError: Boolean,
    keyboardOptions: KeyboardOptions,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDuration = activeField == PrimaryNumericField.Duration
    OutlinedTextField(
        state = state,
        inputTransformation = if (isDuration) {
            durationInputTransformation
        } else {
            moneyInputTransformation
        },
        label = { Text(if (isDuration) durationLabel else rateLabel, maxLines = 1) },
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
            lineHeight = MaterialTheme.typography.titleMedium.fontSize,
        ),
        isError = if (isDuration) durationHasError else rateHasError,
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
    PassiveFieldShell(
        value = state.text.toString(),
        label = label,
        isError = isError,
        placeholder = stringResource(R.string.duration_placeholder),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun PassiveMoneyField(
    state: TextFieldState,
    label: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PassiveFieldShell(
        value = state.text.toString(),
        label = label,
        isError = isError,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun PassiveFieldShell(
    value: String,
    label: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val outlineColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val labelColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val displayText = value.ifEmpty { placeholder.orEmpty() }
    val displayColor = if (value.isEmpty()) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        // OutlinedTextField normally moves an empty unfocused label into the field.
        // This shell is intentionally non-editable, so draw the outline and floating
        // label independently: the label therefore remains on the border at all times.
        Surface(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(4.dp),
            color = containerColor,
            border = BorderStroke(1.dp, outlineColor),
        ) {}

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp),
            color = containerColor,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                maxLines = 1,
            )
        }

        if (displayText.isNotEmpty()) {
            Text(
                text = displayText,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = displayColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
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
            lineHeight = MaterialTheme.typography.titleMedium.fontSize,
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
        label = { Text(label) },
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.titleMedium.fontSize,
        ),
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
    )
}
