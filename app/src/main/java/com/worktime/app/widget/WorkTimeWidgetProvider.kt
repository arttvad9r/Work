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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
}

// Widget data flows:
// - process alive: Application observes the current month and pushes on every change;
// - process dead: system refreshes at least every updatePeriodMillis (30 min).
private val WorkTimeWidgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

suspend fun refresh(context: Context, repository: WorkEntryRepository) {
    // ponytail: month is captured per read; a stale home-screen process catches up
    // via the 30-minute appwidget tick.
    val entries = repository.observeMonth(YearMonth.now()).first()
    push(context, SalaryCalculator.monthSummary(entries))
}

fun observeForWidget(context: Context, repository: WorkEntryRepository): Job =
    WorkTimeWidgetScope.launch {
        // ponytail: month fixed for this process lifetime; month rollover is covered
        // by the 30-minute system tick while the app is closed.
        repository.observeMonth(YearMonth.now()).collect { entries ->
            push(context, SalaryCalculator.monthSummary(entries))
        }
    }

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
