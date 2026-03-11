package com.cukbab.data

import android.content.Context
import androidx.core.content.edit

object TutorialRepository {
    const val TUTORIAL_VERSION = 23 // Introduced in v2.1
    private const val PREFS_NAME = "tutorial_prefs"
    private const val KEY_MAIN_TUTORIAL_COMPLETE = "main_tutorial_complete"
    private const val KEY_SETTINGS_TUTORIAL_COMPLETE = "settings_tutorial_complete"

    fun isMainTutorialComplete(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MAIN_TUTORIAL_COMPLETE, false)
    }

    fun setMainTutorialComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_MAIN_TUTORIAL_COMPLETE, true)
        }
    }

    fun isSettingsTutorialComplete(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SETTINGS_TUTORIAL_COMPLETE, false)
    }

    fun setSettingsTutorialComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_SETTINGS_TUTORIAL_COMPLETE, true)
        }
    }
}
