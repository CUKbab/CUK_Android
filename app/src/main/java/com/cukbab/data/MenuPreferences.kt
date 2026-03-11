package com.cukbab.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object MenuPreferences {
    private const val PREFS_NAME = "menu_prefs"
    private const val KEY_COLLAPSED_GROUPS = "collapsed_groups"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCollapsedGroups(context: Context): Set<Int> {
        return getPrefs(context).getStringSet(KEY_COLLAPSED_GROUPS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun setGroupCollapsed(context: Context, groupResId: Int, isCollapsed: Boolean) {
        val current = getCollapsedGroups(context).toMutableSet()
        if (isCollapsed) {
            current.add(groupResId)
        } else {
            current.remove(groupResId)
        }
        getPrefs(context).edit {
            putStringSet(KEY_COLLAPSED_GROUPS, current.map { it.toString() }.toSet())
        }
    }
}
