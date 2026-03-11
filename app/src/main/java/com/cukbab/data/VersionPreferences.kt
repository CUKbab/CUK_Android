package com.cukbab.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object VersionPreferences {
    private const val PREFS_NAME = "version_prefs"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLastSeenVersion(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_SEEN_VERSION, 0)
    }

    fun setLastSeenVersion(context: Context, version: Int) {
        getPrefs(context).edit { putInt(KEY_LAST_SEEN_VERSION, version) }
    }

    fun getCurrentVersion(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode.toInt()
        } catch (e: Exception) {
            0
        }
    }
}
