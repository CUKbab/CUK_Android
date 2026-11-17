package com.cukbab.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cukbab.R
import com.cukbab.data.MenuPreferences
import com.cukbab.ui.components.MenuList
import com.cukbab.ui.components.MenuScreenState

@Composable
fun CafeBonaScreen(
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
            R.string.screen_cafe_bona to listOf("Bona-Rice-Bowl")
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
