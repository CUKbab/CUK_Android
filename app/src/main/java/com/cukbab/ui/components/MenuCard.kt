package com.cukbab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cukbab.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.cukbab.data.MealNotificationManager
import com.cukbab.data.NotificationPreferences

@Composable
fun MenuCard(category: String, menu: String, date: String) {
    val context = LocalContext.current
    var isScheduled by remember(category, date) { 
        mutableStateOf(NotificationPreferences.isScheduled(context, category, date)) 
    }

    val categoryDisplay = when (category) {
        "Morning" -> stringResource(R.string.category_1000_won_morning)
        "Pranzo-Korean" -> stringResource(R.string.category_korean_cuisine)
        "Pranzo-Global-Noodle" -> stringResource(R.string.category_global_noodle)
        "Pranzo-Plus-Corner" -> stringResource(R.string.category_plus_corner)
        "Pranzo-Dinner" -> stringResource(R.string.category_dinner)
        "Bona-Rice-Bowl" -> stringResource(R.string.category_rice_bowl)
        else -> category
    }

    val notificationScheduledMsg = stringResource(R.string.notification_scheduled)
    val notificationCancelledMsg = stringResource(R.string.notification_cancelled)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (menu != stringResource(R.string.no_menu)) {
                    IconButton(
                        onClick = {
                            NotificationPreferences.toggleSchedule(context, category, date)
                            isScheduled = NotificationPreferences.isScheduled(context, category, date)
                            if (isScheduled) {
                                MealNotificationManager.scheduleNotification(context, category, date, menu)
                                Toast.makeText(context, notificationScheduledMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                MealNotificationManager.cancelNotification(context, category, date)
                                Toast.makeText(context, notificationCancelledMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isScheduled) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                            contentDescription = if (isScheduled) stringResource(R.string.cancel_reminder) else stringResource(R.string.set_reminder),
                            tint = if (isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = menu,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
