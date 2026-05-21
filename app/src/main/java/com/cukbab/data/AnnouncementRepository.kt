package com.cukbab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnnouncementRepository {
    fun getAnnouncementLang(context: Context): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "System") ?: "System"
        
        return when {
            lang.startsWith("Korean") || lang.contains("ko") -> "ko"
            lang.startsWith("Japanese") || lang.contains("ja") -> "ja"
            lang.startsWith("Chinese") || lang.contains("zh") -> "zn"
            else -> "en"
        }
    }

    suspend fun getAnnouncements(lang: String): List<Announcement> {
        return withContext(Dispatchers.IO) {
            try {
                RetrofitClient.menuService.getAnnouncements(lang)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
