package com.worktime.app.ui.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun SummaryStrip(
    state: CalendarUiState,
    locale: Locale,
    expanded: Boolean,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val summaryText = summaryLine(
        shiftCount = summary.shiftCount,
        workedMinutes = summary.workedMinutes,
        totalPayMicros = summary.totalPayMicros,
        locale = locale,
    )
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val visualDragProgress by animateFloatAsState(
        targetValue = dragProgress,
        animationSpec = if (dragging) {
            snap()
        } else {
            spring(
                dampingRatio = AppMotion.NoBounceDampingRatio,
                stiffness = AppMotion.ControlStiffness,
            )
        },
        label = "summary drag progress",
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.StandardMillis,
            easing = AppMotion.StandardEasing,
        ),
        label = "summary chevron",
    )
    val maxLiftPx = with(density) { 6.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -maxLiftPx * visualDragProgress
                }
                .pointerInput(haptics, onClick, onSwipeUp) {
                    val threshold = 40.dp.toPx()
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var thresholdActive = false
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                            thresholdActive = false
                            dragging = true
                            dragProgress = 0f
                        },
                        onDrag = { change, dragAmount ->
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                            dragProgress = (-totalDragY / threshold).coerceIn(0f, 1f)
                            val isEligible = totalDragY <= -threshold
                            if (isEligible && !thresholdActive) {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                thresholdActive = true
                            } else if (!isEligible) {
                                thresholdActive = false
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            val dragDistanceSquared =
                                totalDragX * totalDragX + totalDragY * totalDragY
                            when {
                                totalDragY <= -threshold -> onSwipeUp()
                                dragDistanceSquared < threshold * threshold -> onClick()
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                            thresholdActive = false
                            dragging = false
                            dragProgress = 0f
                        },
                        onDragCancel = {
                            totalDragX = 0f
                            totalDragY = 0f
                            thresholdActive = false
                            dragging = false
                            dragProgress = 0f
                        },
                    )
                }
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(
                    onClickLabel = stringResource(R.string.monthly_summary),
                    onClick = onClick,
                )
                .testTag("monthly-summary-strip")
                .padding(horizontal = AppDimens.screenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summaryText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Icon(
                Icons.Filled.KeyboardArrowUp,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun summaryLine(
    shiftCount: Int,
    workedMinutes: Int,
    locale: Locale,
    totalPayMicros: Long? = null,
): String {
    val shifts = pluralStringResource(R.plurals.shifts_short, shiftCount, shiftCount)
    val hours = stringResource(R.string.hours_short, formatDurationCompact(workedMinutes))
    val line = "$shifts · $hours"
    if (totalPayMicros == null) return line
    val money = stringResource(
        R.string.amount_with_currency,
        formatWholeAmountMicros(totalPayMicros, locale),
    )
    return "$line · $money"
}

@Composable
internal fun MonthlySummaryPanel(
    state: CalendarUiState,
    onOpenYearSummary: () -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val totalText = stringResource(
        R.string.amount_with_currency,
        formatAmountMicros(summary.totalPayMicros, locale),
    )
    val detailText = summaryLine(
        shiftCount = summary.shiftCount,
        workedMinutes = summary.workedMinutes,
        locale = locale,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly-report-panel"),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = AppDimens.screenHorizontalPadding,
                vertical = 4.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = totalText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (shouldUseErrorColorForTotal(summary.totalPayMicros)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            LabelValueRow(
                label = stringResource(R.string.calculation_base),
                value = formatAmountMicros(summary.basePayMicros, locale),
            )
            if (summary.bonusMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_bonus),
                    value = "+${formatAmountMicros(summary.bonusMicros, locale)}",
                )
            }
            if (summary.penaltyMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_penalty),
                    value = "−${formatAmountMicros(summary.penaltyMicros, locale)}",
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (summary.shiftCount > 0) {
                LabelValueRow(
                    label = stringResource(R.string.average_shift),
                    value = formatDurationCompact(summary.workedMinutes / summary.shiftCount),
                )
                LabelValueRow(
                    label = stringResource(R.string.average_shift_income),
                    value = formatAmountMicros(summary.totalPayMicros / summary.shiftCount, locale),
                )
            }

            AppNavigationRow(
                label = stringResource(R.string.year_stats_title),
                onClick = onOpenYearSummary,
            )
        }
    }
}

internal fun shouldUseErrorColorForTotal(totalPayMicros: Long): Boolean = totalPayMicros < 0L
