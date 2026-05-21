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
            NotificationHelper.showNotification(context.applicationContext, displayTitle, menu, notificationId)
        } catch (e: Exception) {
            android.util.Log.e("NotificationReceiver", "Error in onReceive", e)
        }
    }
}
