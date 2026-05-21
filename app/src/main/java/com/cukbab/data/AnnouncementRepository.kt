package com.cukbab.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnnouncementRepository {
    suspend fun getAnnouncements(): List<Announcement> {
        return withContext(Dispatchers.IO) {
            try {
                RetrofitClient.menuService.getAnnouncements()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
