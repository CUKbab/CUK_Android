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
            val announcements = AnnouncementRepository.getAnnouncements()
            if (announcements.isNotEmpty()) {
                val latest = announcements.maxByOrNull { it.id } ?: return@withContext Result.success()
                val lastSeenId = AnnouncementPreferences.getLastAnnouncementId(applicationContext)

                if (latest.id > lastSeenId) {
                    val title = if (latest.type.lowercase() == "urgent") {
                        "[URGENT] ${latest.title}"
                    } else {
                        applicationContext.getString(R.string.announcements) + ": " + latest.title
                    }

                    NotificationHelper.showNotification(
                        context = applicationContext,
                        title = title,
                        message = latest.content,
                        notificationId = 1000 + latest.id,
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
                    notificationId = 2000,
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
