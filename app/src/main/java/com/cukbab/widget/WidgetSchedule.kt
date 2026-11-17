package com.cukbab.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Widget category that follows the time of day instead of a fixed meal. */
const val WIDGET_CATEGORY_AUTO = "Auto"

/**
 * Which day and menu category a widget should show at [now].
 * Once a meal's serving window ends the widget moves on to the next weekday, so it never shows
 * a meal that is already over (or last week's menu on a weekend).
 */
data class WidgetTarget(val date: LocalDate, val jsonKey: String)

object WidgetSchedule {
    // End of each serving window; keep in sync with mealWindows in MenuList.kt.
    private val MORNING_END: LocalTime = LocalTime.of(9, 30)
    private val LUNCH_END: LocalTime = LocalTime.of(14, 0)
    private val DINNER_END: LocalTime = LocalTime.of(19, 0)

    private fun endOf(jsonKey: String): LocalTime = when (jsonKey) {
        "Morning" -> MORNING_END
        "Pranzo-Dinner" -> DINNER_END
        else -> LUNCH_END
    }

    /** [date] if it is a weekday, otherwise the following Monday. */
    private fun weekdayOnOrAfter(date: LocalDate): LocalDate = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> date.plusDays(2)
        DayOfWeek.SUNDAY -> date.plusDays(1)
        else -> date
    }

    private fun nextWeekday(date: LocalDate): LocalDate = weekdayOnOrAfter(date.plusDays(1))

    fun resolve(category: String, now: LocalDateTime = LocalDateTime.now()): WidgetTarget {
        val today = now.toLocalDate()
        val time = now.toLocalTime()
        val isWeekday = today.dayOfWeek != DayOfWeek.SATURDAY && today.dayOfWeek != DayOfWeek.SUNDAY

        // Old configs stored "1000-won-Morning"; the menu JSON key is "Morning".
        val key = if (category == "1000-won-Morning") "Morning" else category

        if (key == WIDGET_CATEGORY_AUTO) {
            if (!isWeekday) return WidgetTarget(weekdayOnOrAfter(today), "Morning")
            return when {
                time < MORNING_END -> WidgetTarget(today, "Morning")
                time < LUNCH_END -> WidgetTarget(today, "Pranzo-Korean")
                time < DINNER_END -> WidgetTarget(today, "Pranzo-Dinner")
                else -> WidgetTarget(nextWeekday(today), "Morning")
            }
        }

        val date = when {
            !isWeekday -> weekdayOnOrAfter(today)
            time >= endOf(key) -> nextWeekday(today)
            else -> today
        }
        return WidgetTarget(date, key)
    }

    /** Next moment the widget content may change: a meal end or just after midnight. */
    fun nextBoundary(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val today = now.toLocalDate()
        return listOf(MORNING_END, LUNCH_END, DINNER_END)
            .map { today.atTime(it) }
            .firstOrNull { it.isAfter(now) }
            ?: today.plusDays(1).atTime(0, 5)
    }

    private const val WORK_NAME = "widget_meal_boundary_refresh"
    const val BOUNDARY_TAG = "widget_boundary"

    /**
     * Refresh all widgets at the next meal boundary. Safe to call repeatedly: KEEP leaves an
     * already-pending refresh alone. Never REPLACE from onUpdate: enqueueing toggles WorkManager's
     * receivers, which makes the system send APPWIDGET_UPDATE again, and REPLACE then cancels the
     * running refresh, an endless cancel/re-enqueue loop.
     */
    fun scheduleNext(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
        val now = LocalDateTime.now()
        // +30 s so the clock is clearly past the boundary when the worker runs.
        val delayMs = Duration.between(now, nextBoundary(now)).toMillis() + 30_000
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(BOUNDARY_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, policy, request)
    }

    /** Refresh all widgets as soon as possible (network fetch runs in a worker). */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(REFRESH_NOW_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private const val REFRESH_NOW_NAME = "widget_refresh_now"

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(MenuWidget::class.java)
        ids.forEach { id ->
            try {
                MenuWidget.fetchAndRefresh(applicationContext, id)
            } catch (e: Exception) {
                // One broken widget shouldn't stop the others.
            }
        }
        if (ids.isNotEmpty()) {
            // The boundary job is itself the pending WORK_NAME entry, so KEEP would skip;
            // chain the next boundary after it. A manual refresh only fills a missing schedule.
            val policy = if (tags.contains(WidgetSchedule.BOUNDARY_TAG)) ExistingWorkPolicy.APPEND_OR_REPLACE
            else ExistingWorkPolicy.KEEP
            WidgetSchedule.scheduleNext(applicationContext, policy)
        }
        return Result.success()
    }
}
