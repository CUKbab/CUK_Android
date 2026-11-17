package com.cukbab.data

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.cukbab.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AnnouncementWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (!NotificationPreferences.isAnnouncementsEnabled(applicationContext)) {
                return@withContext Result.success()
            }

            // 1. Check for new announcements
            val lang = AnnouncementRepository.getAnnouncementLang(applicationContext)
            // Don't let an announcement fetch failure skip the menu check below.
            val announcements = try {
                AnnouncementRepository.getAnnouncements(lang)
            } catch (e: Exception) {
                emptyList()
            }
            if (announcements.isNotEmpty()) {
                val latest = announcements.maxByOrNull { it.id } ?: return@withContext Result.success()
                val lastSeenId = AnnouncementPreferences.getLastAnnouncementId(applicationContext)

                if (lastSeenId == -1) {
                    // First run: mark existing announcements as seen instead of notifying about old ones.
                    AnnouncementPreferences.setLastAnnouncementId(applicationContext, latest.id)
                } else if (latest.id > lastSeenId) {
                    val title = if (latest.type.lowercase() == "urgent") {
                        applicationContext.getString(R.string.urgent_prefix, latest.title)
                    } else {
                        applicationContext.getString(R.string.announcements) + ": " + latest.title
                    }

                    NotificationHelper.showNotification(
                        context = applicationContext,
                        title = title,
                        message = latest.content,
                        notificationId = ANNOUNCEMENT_NOTIFICATION_ID,
                        category = NotificationCompat.CATEGORY_EVENT
                    )

                    AnnouncementPreferences.setLastAnnouncementId(applicationContext, latest.id)
                }
            }

            // 2. Check for menu updates
            val latestMenu = RetrofitClient.menuService.getLatestMenu()
            val currentMenuHash = latestMenu.hashCode()
            val lastMenuHash = MenuPreferences.getLastMenuHash(applicationContext)

            if (lastMenuHash != -1 && currentMenuHash != lastMenuHash) {
                // Menu has changed!
                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = applicationContext.getString(R.string.menu_updated_title),
                    message = applicationContext.getString(R.string.menu_updated_desc),
                    notificationId = MENU_UPDATE_NOTIFICATION_ID,
                    category = NotificationCompat.CATEGORY_STATUS
                )
            }
            MenuPreferences.setLastMenuHash(applicationContext, currentMenuHash)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "announcement_polling_work"
        // Fixed IDs: a newer announcement replaces the older one, and the two never collide
        // (previously 1000 + id clashed with the menu-update ID 2000 when id == 1000).
        private const val ANNOUNCEMENT_NOTIFICATION_ID = 1000
        private const val MENU_UPDATE_NOTIFICATION_ID = 2000

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AnnouncementWorker>(4, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
