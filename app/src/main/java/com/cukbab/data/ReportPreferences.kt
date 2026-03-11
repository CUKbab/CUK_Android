package com.cukbab.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object ReportPreferences {
    private const val PREFS_NAME = "report_prefs"
    private const val KEY_POLICY_AGREED = "policy_agreed"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasAgreedToPolicy(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_POLICY_AGREED, false)
    }

    fun setPolicyAgreed(context: Context, agreed: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_POLICY_AGREED, agreed) }
    }
}
