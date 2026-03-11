package com.cukbab.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cukbab.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.absoluteValue

object MealNotificationManager {
    private const val TAG = "MealNotificationManager"
    
    private val mealStartTimes = mapOf(
        "Morning" to LocalTime.of(8, 0),
        "Pranzo-Korean" to LocalTime.of(11, 30),
        "Pranzo-Global-Noodle" to LocalTime.of(11, 30),
        "Pranzo-Plus-Corner" to LocalTime.of(11, 30),
        "Pranzo-Dinner" to LocalTime.of(17, 30),
        "Bona-Rice-Bowl" to LocalTime.of(11, 30)
    )

    fun scheduleNotification(context: Context, category: String, date: String, menu: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startTime = mealStartTimes[category]
        if (startTime == null) {
            Log.e(TAG, "No start time found for category: $category")
            return
        }
        
        val mealDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: $date", e)
            return
        }
        
        val minutesBefore = NotificationPreferences.getMinutesBefore(context)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val notificationTime = LocalDateTime.of(mealDate, startTime).minusMinutes(minutesBefore.toLong())
        
        val triggerAtMillis = notificationTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        
        Log.d(TAG, "Scheduling $category for $date. CanExact: $canExact, MinutesBefore: $minutesBefore")
        Log.d(TAG, "Trigger: $notificationTime ($triggerAtMillis), Now: ${LocalDateTime.now()} ($now)")

        if (triggerAtMillis <= now) {
            Log.w(TAG, "Trigger time in the past, skipping.")
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("category", category)
            val cleanMenu = if (menu.length > 100) menu.take(97) + "..." else menu
            putExtra("menu", cleanMenu)
            action = "com.cukbab.MEAL_NOTIFICATION"
        }

        val requestCode = (category + date).hashCode().absoluteValue
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm set for $category")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Inexact alarm set for $category")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm, falling back to set()", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelNotification(context: Context, category: String, date: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.cukbab.MEAL_NOTIFICATION"
        }
        val requestCode = (category + date).hashCode().absoluteValue
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm cancelled for $category")
        }
    }
}
