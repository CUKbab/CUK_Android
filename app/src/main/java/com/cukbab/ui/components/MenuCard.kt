package com.cukbab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cukbab.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.cukbab.data.MealNotificationManager
import com.cukbab.data.NotificationPreferences

/** Trailing "(914kcal)" on the menu text, e.g. "배추김치 (914kcal)". */
private val kcalRegex = Regex("""\s*\((\d+)\s*kcal\)\s*$""", RegexOption.IGNORE_CASE)

/** Splits the calorie note off the menu text: ("...배추김치", 914). */
internal fun splitCalories(menu: String): Pair<String, Int?> {
    val match = kcalRegex.find(menu) ?: return menu to null
    return menu.removeRange(match.range).trimEnd() to match.groupValues[1].toIntOrNull()
}

/** Display name for a menu JSON category key. */
@Composable
fun categoryLabel(category: String): String = when (category) {
    "Morning" -> stringResource(R.string.category_1000_won_morning)
    "Pranzo-Korean" -> stringResource(R.string.category_korean_cuisine)
    "Pranzo-Global-Noodle" -> stringResource(R.string.category_global_noodle)
    "Pranzo-Plus-Corner" -> stringResource(R.string.category_plus_corner)
    "Pranzo-Dinner" -> stringResource(R.string.category_dinner)
    "Bona-Rice-Bowl" -> stringResource(R.string.category_rice_bowl)
    else -> category
}

@Composable
fun MenuCard(category: String, menu: String, date: String) {
    val (menuText, kcal) = remember(menu) { splitCalories(menu) }
    val context = LocalContext.current
    var isScheduled by remember(category, date) { 
        mutableStateOf(NotificationPreferences.isScheduled(context, category, date)) 
    }

    val categoryDisplay = categoryLabel(category)

    val notificationScheduledMsg = stringResource(R.string.notification_scheduled)
    val notificationCancelledMsg = stringResource(R.string.notification_cancelled)
    val notificationTimePassedMsg = stringResource(R.string.notification_time_passed)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.weight(1f))

                if (kcal != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.kcal_format, kcal),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (menu != stringResource(R.string.no_menu)) {
                    IconButton(
                        onClick = {
                            val itemId = "$category|$date"
                            if (!isScheduled) {
                                // Only mark as scheduled if an alarm was actually set.
                                if (MealNotificationManager.scheduleNotification(context, category, date, menu)) {
                                    NotificationPreferences.addScheduledItem(context, itemId)
                                    isScheduled = true
                                    Toast.makeText(context, notificationScheduledMsg, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, notificationTimePassedMsg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                MealNotificationManager.cancelNotification(context, category, date)
                                NotificationPreferences.removeScheduledItem(context, itemId)
                                isScheduled = false
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
                text = menuText,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
            )
        }
    }
}
