package com.cukbab.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cukbab.R
import com.cukbab.data.MenuData
import com.cukbab.data.MenuPreferences
import com.cukbab.ui.components.MenuList

@Composable
fun CafeBonaScreen(
    menuData: MenuData?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    error: String?,
    dateString: String,
    showOperatingHours: Boolean
) {
    val context = LocalContext.current
    var collapsedGroups by remember { mutableStateOf(MenuPreferences.getCollapsedGroups(context)) }

    val hasOnlyLunchDinner = menuData != null && menuData.keys.isNotEmpty() && menuData.keys.all { it == "Lunch" || it == "Dinner" }
    val groups = if (hasOnlyLunchDinner) {
        mapOf(
            R.string.group_lunch to listOf("Lunch"),
            R.string.group_dinner to listOf("Dinner")
        )
    } else {
        mapOf(
            R.string.screen_cafe_bona to listOf("Bona-Rice-Bowl")
        )
    }

    MenuList(
        menuData = menuData,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        error = error,
        today = dateString,
        showOperatingHours = showOperatingHours,
        groups = groups,
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
