package com.cukbab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cukbab.R
import com.cukbab.data.MenuData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Monday–Friday of the selected week at a glance: the main dish of each category per day.
 * Tapping a day selects it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekOverviewSheet(
    menuData: MenuData?,
    selectedDate: LocalDate,
    categories: List<String>,
    onDaySelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val monday = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val days = remember(monday) { (0L..4L).map { monday.plusDays(it) } }
    val locale = LocalConfiguration.current.locales[0]
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, MMM d", locale) }
    val today = LocalDate.now()
    val noMenu = stringResource(R.string.no_menu)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.CalendarViewWeek,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.week_overview),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (menuData == null) {
                Text(
                    text = noMenu,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(32.dp))
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(days, key = { it.toString() }) { day ->
                    val isSelected = day == selectedDate
                    val dateKey = day.toString()
                    Card(
                        onClick = { onDaySelected(day) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = day.format(dayFormatter),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (day == today) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.today),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            categories.forEach { category ->
                                val raw = menuData[category]?.get(dateKey)
                                // First line is the main dish; drop the "(914kcal)" note.
                                val main = raw
                                    ?.takeUnless { it.trim() == "No Menu" || it.isBlank() }
                                    ?.let { splitCalories(it).first.lineSequence().firstOrNull { l -> l.isNotBlank() }?.trim() }
                                    ?: noMenu
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        text = categoryLabel(category),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(110.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = main,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
