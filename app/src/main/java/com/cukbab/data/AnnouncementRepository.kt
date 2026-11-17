package com.cukbab.data

import android.content.Context
import com.cukbab.LanguagePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object AnnouncementRepository {
    /**
     * Content language folder (en/ja/ko/zn) for announcements and changelogs, based on the
     * in-app language setting, or the device locale when the setting is "System".
     */
    fun getAnnouncementLang(context: Context): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val name = prefs.getString("language", LanguagePreference.System.name)
        val pref = LanguagePreference.entries.firstOrNull { it.name == name } ?: LanguagePreference.System
        val tag = pref.tag.ifEmpty { Locale.getDefault().language }
        return contentLangFor(tag)
    }

    /** Maps a language tag (e.g. "ko", "zh-Hans-CN") to the server's folder name. */
    fun contentLangFor(languageTag: String): String = when {
        languageTag.startsWith("ko") -> "ko"
        languageTag.startsWith("ja") -> "ja"
        languageTag.startsWith("zh") -> "zn"
        else -> "en"
    }

    /** Throws on network/parse failure so callers can tell "error" from "no announcements". */
    suspend fun getAnnouncements(lang: String): List<Announcement> {
        return withContext(Dispatchers.IO) {
            RetrofitClient.menuService.getAnnouncements(lang)
        }
    }
}
