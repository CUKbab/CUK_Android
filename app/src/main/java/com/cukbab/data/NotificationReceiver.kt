package com.cukbab.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.cukbab.MainActivity
import com.cukbab.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            android.util.Log.d("NotificationReceiver", "Broadcast onReceive: action=$action")
            
            val category = intent.getStringExtra("category") ?: ""
            val menu = intent.getStringExtra("menu") ?: ""
            // Ensure ID is positive and consistent
            val notificationId = category.hashCode().let { 
                if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it)
            }

            val displayTitle = when (category) {
                "Morning" -> context.getString(R.string.category_1000_won_morning)
                "Pranzo-Korean" -> context.getString(R.string.category_korean_cuisine)
                "Pranzo-Global-Noodle" -> context.getString(R.string.category_global_noodle)
                "Pranzo-Plus-Corner" -> context.getString(R.string.category_plus_corner)
                "Pranzo-Dinner" -> context.getString(R.string.category_dinner)
                "Bona-Rice-Bowl" -> context.getString(R.string.category_rice_bowl)
                "TEST" -> "Test Notification"
                else -> if (category.isEmpty()) context.getString(R.string.meal_reminders) else category
            }
            
            android.util.Log.d("NotificationReceiver", "Triggering notification: $displayTitle (ID: $notificationId)")
            showNotification(context.applicationContext, displayTitle, menu, notificationId)
        } catch (e: Exception) {
            android.util.Log.e("NotificationReceiver", "Error in onReceive", e)
        }
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "meal_reminders_v3" // Bumping to v3 to force fresh channel settings

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.meal_reminders),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Meal reminders and updates"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX) // Use MAX for strongest priority
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        notificationManager.notify(notificationId, notification)
        android.util.Log.d("NotificationReceiver", "notificationManager.notify called for ID: $notificationId")
    }
}
