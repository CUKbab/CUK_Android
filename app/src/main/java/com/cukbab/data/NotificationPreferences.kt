package com.cukbab.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object NotificationPreferences {
    private const val PREFS_NAME = "cukbab_notifications"
    private const val KEY_MINUTES_BEFORE = "minutes_before"
    private const val KEY_SCHEDULED_ITEMS = "scheduled_items"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getMinutesBefore(context: Context): Int {
        return getPrefs(context).getInt(KEY_MINUTES_BEFORE, 15)
    }

    fun setMinutesBefore(context: Context, minutes: Int) {
        getPrefs(context).edit { putInt(KEY_MINUTES_BEFORE, minutes) }
    }

    fun getScheduledItems(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_SCHEDULED_ITEMS, emptySet()) ?: emptySet()
    }

    private fun getItemId(category: String, date: String): String {
        return "$category|$date"
    }

    fun isScheduled(context: Context, category: String, date: String): Boolean {
        return getScheduledItems(context).contains(getItemId(category, date))
    }

    fun toggleSchedule(context: Context, category: String, date: String) {
        val itemId = getItemId(category, date)
        val current = getScheduledItems(context).toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        saveScheduledItems(context, current)
    }

    fun addScheduledItem(context: Context, itemId: String) {
        val current = getScheduledItems(context).toMutableSet()
        current.add(itemId)
        saveScheduledItems(context, current)
    }

    fun removeScheduledItem(context: Context, itemId: String) {
        val current = getScheduledItems(context).toMutableSet()
        current.remove(itemId)
        saveScheduledItems(context, current)
    }

    private fun saveScheduledItems(context: Context, scheduled: Set<String>) {
        getPrefs(context).edit { putStringSet(KEY_SCHEDULED_ITEMS, scheduled) }
    }

    fun clearOldSchedules(context: Context, today: String) {
        val prefs = getPrefs(context)
        val scheduled = prefs.getStringSet(KEY_SCHEDULED_ITEMS, emptySet())?.toMutableSet() ?: return
        val filtered = scheduled.filter { it.split("|").last() >= today }.toSet()
        prefs.edit { putStringSet(KEY_SCHEDULED_ITEMS, filtered) }
    }
}
