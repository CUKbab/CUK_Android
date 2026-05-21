package com.cukbab.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import com.cukbab.R
import com.cukbab.LanguagePreference
import com.cukbab.data.AppVersion
import com.cukbab.ui.screens.settings.*
import com.cukbab.ui.theme.ThemePreference

@Composable
fun SettingsScreen(
    currentSubMenu: SettingsSubMenu?,
    onSubMenuChange: (SettingsSubMenu?) -> Unit,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    baseFontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    showOperatingHours: Boolean,
    onShowOperatingHoursChange: (Boolean) -> Unit,
    experimentalDualPane: Boolean,
    onExperimentalDualPaneChange: (Boolean) -> Unit,
    languagePreference: LanguagePreference,
    onLanguageChange: (LanguagePreference) -> Unit,
    customAccentColor: Color?,
    onAccentColorChange: (Color?) -> Unit,
    onShowChangelog: () -> Unit,
    onUpdateFound: (AppVersion) -> Unit,
    onDisplayCoordsMeasured: (LayoutCoordinates) -> Unit,
    onWidgetCoordsMeasured: (LayoutCoordinates) -> Unit,
    onNotifCoordsMeasured: (LayoutCoordinates) -> Unit,
    onFeedbackCoordsMeasured: (LayoutCoordinates) -> Unit
) {
    AnimatedContent(
        targetState = currentSubMenu,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "SettingsTransition"
    ) { subMenu ->
        when (subMenu) {
            null, SettingsSubMenu.Main -> SettingsMainScreen(
                onNavigate = { onSubMenuChange(it) },
                onShowChangelog = onShowChangelog,
                onDisplayCoordsMeasured = onDisplayCoordsMeasured,
                onWidgetCoordsMeasured = onWidgetCoordsMeasured,
                onNotifCoordsMeasured = onNotifCoordsMeasured,
                onFeedbackCoordsMeasured = onFeedbackCoordsMeasured
            )
            SettingsSubMenu.Account -> AccountSettingsScreen(onBack = { onSubMenuChange(null) })
            SettingsSubMenu.Display -> DisplaySettingsScreen(
                themePreference, onThemeChange, baseFontSize, onFontSizeChange,
                showOperatingHours, onShowOperatingHoursChange,
                experimentalDualPane, onExperimentalDualPaneChange,
                languagePreference, onLanguageChange,
                customAccentColor, onAccentColorChange,
                onBack = { onSubMenuChange(null) }
            )
            SettingsSubMenu.Widget -> WidgetSettingsScreen(onBack = { onSubMenuChange(null) })
            SettingsSubMenu.Notifications -> NotificationSettingsScreen(onBack = { onSubMenuChange(null) })
            SettingsSubMenu.Admin -> AdminPanelScreen(onBack = { onSubMenuChange(null) })
            SettingsSubMenu.About -> AboutSettingsScreen(
                onBack = { onSubMenuChange(null) },
                onUpdateFound = onUpdateFound
            )
        }
    }
}
