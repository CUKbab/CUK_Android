package com.cukbab.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cukbab.LanguagePreference
import com.cukbab.R
import com.cukbab.ui.theme.MAX_FONT_SIZE
import com.cukbab.ui.theme.MIN_FONT_SIZE
import com.cukbab.ui.theme.ThemePreference

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.cukbab.ui.theme.AccentColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    baseFontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    showOperatingHours: Boolean,
    onShowOperatingHoursChange: (Boolean) -> Unit,
    languagePreference: LanguagePreference,
    onLanguageChange: (LanguagePreference) -> Unit,
    customAccentColor: Color?,
    onAccentColorChange: (Color?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

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
            text = stringResource(R.string.theme_selection),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            ThemePreference.entries.forEachIndexed { index, preference ->
                val label = when (preference) {
                    ThemePreference.Light -> stringResource(R.string.theme_light)
                    ThemePreference.Dark -> stringResource(R.string.theme_dark)
                    ThemePreference.System -> stringResource(R.string.theme_system)
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemePreference.entries.size),
                    onClick = { onThemeChange(preference) },
                    selected = themePreference == preference
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.accent_color),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(AccentColors) { color ->
                val isSelected = customAccentColor == color
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color ?: MaterialTheme.colorScheme.primary)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onAccentColorChange(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (color == null) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.system_default),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.show_operating_hours),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.show_operating_hours_desc),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = showOperatingHours,
                onCheckedChange = onShowOperatingHoursChange
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.font_size_selection),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${baseFontSize.toInt()}px",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Slider(
                value = baseFontSize,
                onValueChange = { onFontSizeChange(it) },
                valueRange = MIN_FONT_SIZE..MAX_FONT_SIZE
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${MIN_FONT_SIZE.toInt()}px", style = MaterialTheme.typography.labelMedium)
                Text("${MAX_FONT_SIZE.toInt()}px", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.language_selection),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = languagePreference.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            MaterialTheme(
                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
            ) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    LanguagePreference.entries.forEach { preference ->
                        DropdownMenuItem(
                            text = { Text(preference.label) },
                            onClick = {
                                onLanguageChange(preference)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
