package com.cukbab.data

import android.content.Context

object AnnouncementPreferences {
    private const val PREFS_NAME = "announcement_prefs"
    private const val KEY_LAST_ID = "last_announcement_id"

    fun getLastAnnouncementId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_ID, -1)
    }

    fun setLastAnnouncementId(context: Context, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_ID, id).apply()
    }
}
