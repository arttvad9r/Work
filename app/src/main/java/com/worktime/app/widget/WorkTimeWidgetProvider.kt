package com.worktime.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.worktime.app.MainActivity
import com.worktime.app.R
import com.worktime.app.WorkTimeApplication
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.repository.WorkEntryRepository
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import java.time.YearMonth
import java.time.Duration
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class WorkTimeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val repository = (context.applicationContext as WorkTimeApplication)
            .container.workEntryRepository
        WorkTimeWidgetScope.launch {
            try {
                refresh(context, repository)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in TIME_INVALIDATION_ACTIONS) {
            widgetTimeInvalidations.tryEmit(Unit)
        }
    }
}

// Widget data flows:
// - process alive: Application observes the current month and pushes on every change;
// - process dead: system refreshes at least every updatePeriodMillis (30 min).
private val WorkTimeWidgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private val widgetTimeInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
private val TIME_INVALIDATION_ACTIONS = setOf(
    Intent.ACTION_DATE_CHANGED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)

suspend fun refresh(context: Context, repository: WorkEntryRepository) {
    // ponytail: month is captured per read; a stale home-screen process catches up
    // via the 30-minute appwidget tick.
    val entries = repository.observeMonth(YearMonth.now()).first()
    push(context, SalaryCalculator.monthSummary(entries))
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun observeForWidget(context: Context, repository: WorkEntryRepository): Job =
    WorkTimeWidgetScope.launch {
        currentMonthFlow().flatMapLatest { month -> repository.observeMonth(month) }.collect { entries ->
            push(context, SalaryCalculator.monthSummary(entries))
        }
    }

internal fun currentMonthFlow(
    now: () -> YearMonth = { YearMonth.now() },
    waitUntilNextMonth: suspend (YearMonth) -> Unit = { month ->
        val nextMonth = month.plusMonths(1).atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
        val waitMillis = Duration.between(java.time.Instant.now(), nextMonth.toInstant())
            .toMillis()
            .coerceAtLeast(1_000L)
        delay(waitMillis)
    },
    invalidations: kotlinx.coroutines.flow.Flow<Unit> = widgetTimeInvalidations,
) = merge(
    flow {
        var current = now()
        emit(current)
        while (true) {
            waitUntilNextMonth(current)
            val next = now()
            if (next != current) {
                current = next
                emit(current)
            }
        }
    },
    invalidations.map { now() },
).distinctUntilChanged()

private fun push(context: Context, summary: MonthSummary) {
    val appContext = context.applicationContext
    val manager = AppWidgetManager.getInstance(appContext)
    val views = remoteViews(appContext, summary)
    manager.getAppWidgetIds(ComponentName(appContext, WorkTimeWidgetProvider::class.java))
        .forEach { id -> manager.updateAppWidget(id, views) }
}

private fun remoteViews(context: Context, summary: MonthSummary): RemoteViews =
    RemoteViews(context.packageName, R.layout.work_time_widget).apply {
        setTextViewText(R.id.widget_days_label, context.getString(R.string.shift_count_label) + ":")
        setTextViewText(R.id.widget_hours_label, context.getString(R.string.worked_duration) + ":")
        setTextViewText(R.id.widget_income_label, context.getString(R.string.monthly_income) + ":")
        setTextViewText(R.id.widget_days_value, summary.shiftCount.toString())
        setTextViewText(R.id.widget_hours_value, formatDurationCompact(summary.workedMinutes))
        setTextViewText(R.id.widget_income_value, formatAmountMicros(summary.totalPayMicros))
        setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
    }

private fun openAppPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
