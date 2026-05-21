package com.cukbab.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.cukbab.R
import com.cukbab.data.RetrofitClient
import com.cukbab.data.WidgetPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MenuWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val menuContentKey = stringPreferencesKey("menu_content")
        val headerTitleKey = stringPreferencesKey("header_title")
        val lastUpdateKey = stringPreferencesKey("last_update_time")
        val menuFontSizeKey = floatPreferencesKey("menu_font_size")

        private fun getCategoryDisplayName(context: Context, category: String): String {
            return when (category) {
                "1000-won-Morning" -> context.getString(R.string.category_1000_won_morning)
                "Pranzo-Korean" -> context.getString(R.string.category_korean_cuisine)
                "Pranzo-Global-Noodle" -> context.getString(R.string.category_global_noodle)
                "Pranzo-Plus-Corner" -> context.getString(R.string.category_plus_corner)
                "Pranzo-Dinner" -> context.getString(R.string.category_dinner)
                "Bona-Rice-Bowl" -> context.getString(R.string.category_rice_bowl)
                else -> category
            }
        }

        /**
         * Force a data fetch and update for a specific widget
         */
        suspend fun fetchAndRefresh(context: Context, glanceId: GlanceId) {
            val manager = GlanceAppWidgetManager(context)
            val widgetId = manager.getAppWidgetId(glanceId)
            
            val cafeteria = WidgetPreferences.getSelectedCafeteria(context, widgetId)
            val category = WidgetPreferences.getSelectedCategory(context, widgetId)
            val fontSize = WidgetPreferences.getFontSize(context, widgetId)
            
            // Fix for Morning mapping
            val jsonKey = if (category == "1000-won-Morning") "Morning" else category
            
            val categoryDisplay = getCategoryDisplayName(context, category)
            val headerTitle = "$cafeteria\n$categoryDisplay"
            
            val todayDate = LocalDate.now()
            val queryDate = when (todayDate.dayOfWeek) {
                java.time.DayOfWeek.SATURDAY -> todayDate.minusDays(5)
                java.time.DayOfWeek.SUNDAY -> todayDate.minusDays(6)
                else -> todayDate
            }
            val today = queryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            var menuText = "No menu data."
            try {
                val menuData = withContext(Dispatchers.IO) {
                    com.cukbab.data.MenuRepository.getMenu(context, queryDate)
                }
                val rawMenu = menuData[jsonKey]?.get(today) ?: "No Menu"
                menuText = if (rawMenu.trim() == "No Menu" || rawMenu.isEmpty()) "No menu for today." else rawMenu

                if (category == "Pranzo-Korean" || category == "Pranzo-Global-Noodle") {
                    val plusMenu = menuData["Pranzo-Plus-Corner"]?.get(today) ?: "No Menu"
                    if (plusMenu.trim() != "No Menu" && plusMenu.isNotEmpty()) {
                        menuText += "\n\n[Plus Corner]\n$plusMenu"
                    }
                }
            } catch (e: Exception) {
                menuText = "Error: ${e.localizedMessage}"
            }

            // Update the internal state of the widget
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[menuContentKey] = menuText
                    this[headerTitleKey] = headerTitle
                    this[lastUpdateKey] = nowTime
                    this[menuFontSizeKey] = fontSize
                }
            }
            // Trigger the UI update
            MenuWidget().update(context, glanceId)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val headerTitle = prefs[headerTitleKey] ?: "CUKbab"
            val menu = prefs[menuContentKey] ?: "Tap to refresh"
            val lastUpdate = prefs[lastUpdateKey] ?: "--:--:--"
            val fontSize = prefs[menuFontSizeKey] ?: 11f

            GlanceTheme {
                WidgetContent(headerTitle, menu, lastUpdate, fontSize)
            }
        }
    }

    @Composable
    private fun WidgetContent(headerTitle: String, menu: String, lastUpdate: String, fontSize: Float) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionRunCallback<RefreshAction>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = GlanceModifier.size(18.dp)
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = headerTitle,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = lastUpdate,
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(GlanceModifier.height(6.dp))

            Text(
                text = menu,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = fontSize.sp
                )
            )
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MenuWidget.fetchAndRefresh(context, glanceId)
    }
}

class MenuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MenuWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id ->
            WidgetPreferences.deleteWidgetData(context, id)
        }
    }
}
