package com.worktime.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.calendar.RatePeriodUi
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.format.formatDecimalMicros
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RateHistoryScreen(
    periods: List<RatePeriodUi>,
    onDismiss: () -> Unit,
    onEditPeriod: (RatePeriodUi) -> Unit,
    onAddPeriod: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

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
                .padding(horizontal = 16.dp),
        ) {
            AppTopBar(
                title = stringResource(R.string.rate_history),
                onBack = onDismiss,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = if (periods.isEmpty()) Alignment.Center else Alignment.TopCenter,
            ) {
                if (periods.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rate_history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        val currentYear = LocalDate.now().year
                        periods.forEachIndexed { index, period ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            RatePeriodRow(
                                period = period,
                                isLatest = index == periods.lastIndex,
                                isOldest = index == 0 && periods.size > 1,
                                currentYear = currentYear,
                                onClick = { onEditPeriod(period) },
                            )
                        }
                    }
                }
            }

            AddPeriodButton(
                onClick = onAddPeriod,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Box(modifier = Modifier.navigationBarsPadding().height(8.dp))
        }
        }
    }
}

@Composable
private fun RatePeriodRow(
    period: RatePeriodUi,
    isLatest: Boolean,
    isOldest: Boolean,
    currentYear: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = periodLabel(period, isLatest, isOldest, currentYear),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = stringResource(
                R.string.rate_with_currency,
                formatDecimalMicros(period.rateMicros),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.size(width = 12.dp, height = 1.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun periodLabel(
    period: RatePeriodUi,
    isLatest: Boolean,
    isOldest: Boolean,
    currentYear: Int,
): String {
    val locale = LocalLocale.current.platformLocale
    val date = { value: LocalDate ->
        value.format(
            DateTimeFormatter.ofPattern(
                if (value.year == currentYear) "d MMMM" else "d MMMM yyyy",
                locale,
            ),
        )
    }
    return when {
        isLatest -> stringResource(R.string.rate_period_since, date(period.start))
        isOldest -> stringResource(R.string.rate_period_until, date(period.end))
        else -> stringResource(R.string.rate_period_range, date(period.start), date(period.end))
    }
}

@Composable
private fun AddPeriodButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.add_period),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
