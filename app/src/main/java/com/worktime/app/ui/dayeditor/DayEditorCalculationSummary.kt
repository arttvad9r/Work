package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.formatDurationCompact

@Composable
internal fun CalculationSummary(
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
