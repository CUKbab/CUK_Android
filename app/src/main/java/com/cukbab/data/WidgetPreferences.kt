package com.cukbab.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object WidgetPreferences {
    private const val PREFS_NAME = "widget_prefs"
    private const val KEY_CAFETERIA = "widget_cafeteria"
    private const val KEY_CATEGORY = "widget_category"
    private const val KEY_FONT_SIZE = "widget_font_size"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedCafeteria(context: Context, widgetId: Int): String {
        return getPrefs(context).getString("${KEY_CAFETERIA}_$widgetId", "Buon-Pranzo") ?: "Buon-Pranzo"
    }

    fun setSelectedCafeteria(context: Context, widgetId: Int, cafeteria: String) {
        getPrefs(context).edit { putString("${KEY_CAFETERIA}_$widgetId", cafeteria) }
    }

    fun getSelectedCategory(context: Context, widgetId: Int): String {
        return getPrefs(context).getString("${KEY_CATEGORY}_$widgetId", "Morning") ?: "Morning"
    }

    fun setSelectedCategory(context: Context, widgetId: Int, category: String) {
        getPrefs(context).edit { putString("${KEY_CATEGORY}_$widgetId", category) }
    }

    fun getFontSize(context: Context, widgetId: Int): Float {
        return getPrefs(context).getFloat("${KEY_FONT_SIZE}_$widgetId", 12f)
    }

    fun setFontSize(context: Context, widgetId: Int, size: Float) {
        getPrefs(context).edit { putFloat("${KEY_FONT_SIZE}_$widgetId", size) }
    }

    fun getAllWidgetIds(context: Context): List<Int> {
        return getPrefs(context).all.keys
            .filter { it.startsWith(KEY_CAFETERIA) }
            .map { it.split("_").last().toInt() }
    }

    fun deleteWidgetData(context: Context, widgetId: Int) {
        getPrefs(context).edit {
            remove("${KEY_CAFETERIA}_$widgetId")
            remove("${KEY_CATEGORY}_$widgetId")
            remove("${KEY_FONT_SIZE}_$widgetId")
        }
    }
}
