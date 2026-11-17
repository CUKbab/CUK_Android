package com.cukbab.widget

import android.appwidget.AppWidgetManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

class MenuWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val menuContentKey = stringPreferencesKey("menu_content")
        val headerTitleKey = stringPreferencesKey("header_title")
        val lastUpdateKey = stringPreferencesKey("last_update_time")
        val menuFontSizeKey = floatPreferencesKey("menu_font_size")

        private fun getCategoryDisplayName(context: Context, category: String): String {
            return when (category) {
                "1000-won-Morning", "Morning" -> context.getString(R.string.category_1000_won_morning)
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
            
            // Pick the meal and day to show; after a meal ends this moves on to the next weekday.
            val target = WidgetSchedule.resolve(category)
            val jsonKey = target.jsonKey
            val queryDate = target.date
            val todayDate = LocalDate.now()

            val categoryDisplay = getCategoryDisplayName(context, jsonKey)
            val dayLabel = when (queryDate) {
                todayDate -> null
                todayDate.plusDays(1) -> context.getString(R.string.tomorrow)
                else -> queryDate.dayOfWeek.getDisplayName(TextStyle.SHORT, context.resources.configuration.locales[0])
            }
            val headerTitle = "$cafeteria\n$categoryDisplay" + (dayLabel?.let { " · $it" } ?: "")

            val today = queryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            var menuText = context.getString(R.string.widget_no_menu_data)
            try {
                val menuData = withContext(Dispatchers.IO) {
                    com.cukbab.data.MenuRepository.getMenu(context, queryDate, forceRefresh = true)
                }
                val rawMenu = menuData[jsonKey]?.get(today) ?: "No Menu"
                menuText = if (rawMenu.trim() == "No Menu" || rawMenu.isEmpty()) context.getString(R.string.widget_no_menu_today) else rawMenu

                if (jsonKey == "Pranzo-Korean" || jsonKey == "Pranzo-Global-Noodle") {
                    val plusMenu = menuData["Pranzo-Plus-Corner"]?.get(today) ?: "No Menu"
                    if (plusMenu.trim() != "No Menu" && plusMenu.isNotEmpty()) {
                        menuText += "\n\n[${context.getString(R.string.category_plus_corner)}]\n$plusMenu"
                    }
                }
            } catch (e: com.cukbab.data.MenuNotPublishedException) {
                menuText = context.getString(R.string.menu_not_published)
            } catch (e: Exception) {
                menuText = context.getString(R.string.widget_error)
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
            val menu = prefs[menuContentKey] ?: context.getString(R.string.widget_tap_to_refresh)
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

    // Periodic APPWIDGET_UPDATE (updatePeriodMillis) only re-renders stored state, so fetch
    // fresh menu data here; otherwise the widget keeps showing yesterday's menu.
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSchedule.scheduleNext(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                appWidgetIds.forEach { id ->
                    try {
                        MenuWidget.fetchAndRefresh(context, manager.getGlanceIdBy(id))
                    } catch (e: Exception) {
                        // Widget removed or not yet bound; skip.
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetSchedule.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id ->
            WidgetPreferences.deleteWidgetData(context, id)
        }
    }
}
