package com.cukbab.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cukbab.R
import com.cukbab.data.MenuPreferences
import com.cukbab.ui.components.MenuList
import com.cukbab.ui.components.MenuScreenState

@Composable
fun BuonPranzoScreen(
    state: MenuScreenState,
    dateString: String,
    showOperatingHours: Boolean
) {
    val context = LocalContext.current
    var collapsedGroups by remember { mutableStateOf(MenuPreferences.getCollapsedGroups(context)) }

    MenuList(
        state = state,
        today = dateString,
        showOperatingHours = showOperatingHours,
        groups = mapOf(
            R.string.group_morning to listOf("Morning"),
            R.string.group_lunch to listOf("Pranzo-Korean", "Pranzo-Global-Noodle", "Pranzo-Plus-Corner"),
            R.string.group_dinner to listOf("Pranzo-Dinner")
        ),
        collapsedGroups = collapsedGroups,
        onToggleGroup = { groupResId ->
            val isCurrentlyCollapsed = collapsedGroups.contains(groupResId)
            val nextState = !isCurrentlyCollapsed
            MenuPreferences.setGroupCollapsed(context, groupResId, nextState)
            collapsedGroups = if (nextState) {
                collapsedGroups + groupResId
            } else {
                collapsedGroups - groupResId
            }
        }
    )
}
