package com.cukbab.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cukbab.R
import com.cukbab.data.WidgetPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var widgetIds by remember { mutableStateOf(emptyList<Int>()) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        widgetIds = WidgetPreferences.getAllWidgetIds(context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 8.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.back))
        }

        Text(
            text = stringResource(R.string.widget_settings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.widget_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (widgetIds.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_widgets_found),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            widgetIds.forEachIndexed { index, widgetId ->
                WidgetConfigItem(
                    index = index + 1,
                    widgetId = widgetId,
                    onUpdate = {
                        scope.launch {
                            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                            try {
                                val glanceId = manager.getGlanceIdBy(widgetId)
                                // Fetch fresh data and update the widget state immediately
                                com.cukbab.widget.MenuWidget.fetchAndRefresh(context, glanceId)
                                Toast.makeText(context, "Widget Updated", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                // Widget might have been removed but ID still exists in prefs
                            }
                        }
                    }
                )
                if (index < widgetIds.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp).alpha(0.3f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigItem(index: Int, widgetId: Int, onUpdate: () -> Unit) {
    val context = LocalContext.current
    var selectedCafeteria by remember { mutableStateOf(WidgetPreferences.getSelectedCafeteria(context, widgetId)) }
    var selectedCategory by remember { mutableStateOf(WidgetPreferences.getSelectedCategory(context, widgetId)) }
    var fontSize by remember { mutableFloatStateOf(WidgetPreferences.getFontSize(context, widgetId)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.widget_number, index),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.widget_font_size) + ": ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = fontSize,
            onValueChange = { 
                fontSize = it 
                WidgetPreferences.setFontSize(context, widgetId, it)
            },
            onValueChangeFinished = { onUpdate() },
            valueRange = 8f..24f,
            steps = 16
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.cafeteria), style = MaterialTheme.typography.labelMedium)
        val cafeterias = listOf("Buon Pranzo", "Cafe Bona")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            cafeterias.forEachIndexed { cIndex, cafeteria ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = cIndex, count = cafeterias.size),
                    onClick = {
                        selectedCafeteria = cafeteria
                        WidgetPreferences.setSelectedCafeteria(context, widgetId, cafeteria)
                        if (cafeteria == "Cafe Bona") {
                            selectedCategory = "Bona-Rice-Bowl"
                            WidgetPreferences.setSelectedCategory(context, widgetId, "Bona-Rice-Bowl")
                        } else {
                            selectedCategory = "Pranzo-Korean"
                            WidgetPreferences.setSelectedCategory(context, widgetId, "Pranzo-Korean")
                        }
                        onUpdate()
                    },
                    selected = selectedCafeteria == cafeteria
                ) {
                    Text(cafeteria)
                }
            }
        }

        if (selectedCafeteria == "Buon Pranzo") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.category), style = MaterialTheme.typography.labelMedium)
            
            val categories = listOf(
                "Morning" to stringResource(R.string.category_1000_won_morning),
                "Pranzo-Korean" to stringResource(R.string.category_korean_cuisine),
                "Pranzo-Global-Noodle" to stringResource(R.string.category_global_noodle),
                "Pranzo-Dinner" to stringResource(R.string.category_dinner)
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = categories.find { it.first == selectedCategory }?.second ?: selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { (key, label) ->
                        val displayLabel = if (key == "Pranzo-Korean" || key == "Pranzo-Global-Noodle") {
                            "$label (+ ${stringResource(R.string.category_plus_corner)})"
                        } else {
                            label
                        }
                        DropdownMenuItem(
                            text = { Text(displayLabel) },
                            onClick = {
                                selectedCategory = key
                                WidgetPreferences.setSelectedCategory(context, widgetId, key)
                                expanded = false
                                onUpdate()
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}
