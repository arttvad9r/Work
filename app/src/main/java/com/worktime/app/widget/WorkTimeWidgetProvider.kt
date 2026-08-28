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
import java.time.Duration
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
    val month = YearMonth.now()
    val entries = repository.observeMonth(month).first()
    push(context, month, SalaryCalculator.monthSummary(entries))
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun observeForWidget(context: Context, repository: WorkEntryRepository): Job =
    WorkTimeWidgetScope.launch {
        currentMonthFlow()
            .flatMapLatest { month ->
                repository.observeMonth(month).map { entries -> month to entries }
            }
            .collect { (month, entries) ->
                push(context, month, SalaryCalculator.monthSummary(entries))
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
) = channelFlow {
    val reschedules = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    var timer: Job? = null
    launch {
        merge(flowOf(Unit), invalidations, reschedules).collect {
            timer?.cancelAndJoin()
            val current = now()
            send(current)
            timer = launch {
                waitUntilNextMonth(current)
                reschedules.emit(Unit)
            }
        }
    }
    awaitCancellation()
}.distinctUntilChanged()

private fun push(context: Context, month: YearMonth, summary: MonthSummary) {
    val appContext = context.applicationContext
    val manager = AppWidgetManager.getInstance(appContext)
    val views = remoteViews(appContext, month, summary)
    manager.getAppWidgetIds(ComponentName(appContext, WorkTimeWidgetProvider::class.java))
        .forEach { id -> manager.updateAppWidget(id, views) }
}

private fun remoteViews(context: Context, month: YearMonth, summary: MonthSummary): RemoteViews =
    RemoteViews(context.packageName, R.layout.work_time_widget).apply {
        setTextViewText(R.id.widget_month, widgetMonthLabel(month))
        setTextViewText(R.id.widget_days_value, summary.shiftCount.toString())
        setTextViewText(R.id.widget_hours_value, formatDurationCompact(summary.workedMinutes))
        setTextViewText(
            R.id.widget_income_value,
            context.getString(R.string.amount_with_currency, formatAmountMicros(summary.totalPayMicros)),
        )
        val openApp = openAppPendingIntent(context)
        setOnClickPendingIntent(R.id.widget_root, openApp)
        setOnClickPendingIntent(R.id.widget_add, openApp)
    }

private fun widgetMonthLabel(month: YearMonth): String {
    val locale = Locale.getDefault()
    val label = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    return label.take(1).uppercase(locale) + label.drop(1)
}

private fun openAppPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
