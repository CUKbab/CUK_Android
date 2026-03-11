package com.cukbab.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cukbab.data.WidgetPreferences
import com.cukbab.ui.theme.CUKbabTheme
import com.cukbab.ui.theme.ThemePreference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cukbab.R

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            CUKbabTheme(themePreference = ThemePreference.System, baseFontSize = 16f) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigScreen(
                        onConfigComplete = { cafeteria, category, fontSize ->
                            saveConfig(cafeteria, category, fontSize)
                        }
                    )
                }
            }
        }
    }

    private fun saveConfig(cafeteria: String, category: String, fontSize: Float) {
        WidgetPreferences.setSelectedCafeteria(this, appWidgetId, cafeteria)
        WidgetPreferences.setSelectedCategory(this, appWidgetId, category)
        WidgetPreferences.setFontSize(this, appWidgetId, fontSize)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)

        MainScope().launch {
            // Give the system a moment to register the widget
            delay(300)
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(this@WidgetConfigActivity)
            try {
                val glanceId = manager.getGlanceIdBy(appWidgetId)
                MenuWidget.fetchAndRefresh(this@WidgetConfigActivity, glanceId)
            } catch (e: Exception) {
                // If it fails, the user can still tap the widget to refresh
            }
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(onConfigComplete: (String, String, Float) -> Unit) {
    var selectedCafeteria by remember { mutableStateOf("Buon Pranzo") }
    var selectedCategory by remember { mutableStateOf("Pranzo-Korean") }
    var fontSize by remember { mutableFloatStateOf(11f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.widget_settings),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(text = stringResource(R.string.widget_font_size) + ": ${fontSize.toInt()}sp", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 8f..24f,
            steps = 16
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.select_cafeteria), style = MaterialTheme.typography.titleMedium)
        val cafeterias = listOf("Buon Pranzo", "Cafe Bona")
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            cafeterias.forEach { cafeteria ->
                FilterChip(
                    selected = selectedCafeteria == cafeteria,
                    onClick = { 
                        selectedCafeteria = cafeteria 
                        selectedCategory = if (cafeteria == "Cafe Bona") "Bona-Rice-Bowl" else "Pranzo-Korean"
                    },
                    label = { Text(cafeteria) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        if (selectedCafeteria == "Buon Pranzo") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.select_category), style = MaterialTheme.typography.titleMedium)
            
            val categories = listOf(
                "Morning" to stringResource(R.string.category_1000_won_morning),
                "Pranzo-Korean" to stringResource(R.string.category_korean_cuisine),
                "Pranzo-Global-Noodle" to stringResource(R.string.category_global_noodle),
                "Pranzo-Dinner" to stringResource(R.string.category_dinner)
            )

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                categories.forEach { (key, label) ->
                    val displayLabel = if (key == "Pranzo-Korean" || key == "Pranzo-Global-Noodle") "$label (+ ${stringResource(R.string.plus_corner_included)})" else label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key }
                        )
                        Text(
                            text = displayLabel,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onConfigComplete(selectedCafeteria, selectedCategory, fontSize) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.submit))
        }
    }
}
