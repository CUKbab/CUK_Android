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
import com.cukbab.data.TutorialRepository
import com.cukbab.ui.components.AnimatedTutorialOverlay
import com.cukbab.ui.components.TutorialStep
import com.cukbab.ui.screens.settings.*
import com.cukbab.ui.theme.ThemePreference

@Composable
fun SettingsScreen(
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
    onShowChangelog: () -> Unit,
    onUpdateFound: (AppVersion) -> Unit,
    onDisplayCoordsMeasured: (LayoutCoordinates) -> Unit,
    onWidgetCoordsMeasured: (LayoutCoordinates) -> Unit,
    onNotifCoordsMeasured: (LayoutCoordinates) -> Unit,
    onFeedbackCoordsMeasured: (LayoutCoordinates) -> Unit
) {
    var currentSubMenu by remember { mutableStateOf<SettingsSubMenu?>(null) }

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
                onNavigate = { currentSubMenu = it },
                onShowChangelog = onShowChangelog,
                onDisplayCoordsMeasured = onDisplayCoordsMeasured,
                onWidgetCoordsMeasured = onWidgetCoordsMeasured,
                onNotifCoordsMeasured = onNotifCoordsMeasured,
                onFeedbackCoordsMeasured = onFeedbackCoordsMeasured
            )
            SettingsSubMenu.Account -> AccountSettingsScreen(onBack = { currentSubMenu = null })
            SettingsSubMenu.Display -> DisplaySettingsScreen(
                themePreference, onThemeChange, baseFontSize, onFontSizeChange,
                showOperatingHours, onShowOperatingHoursChange, languagePreference, onLanguageChange,
                customAccentColor, onAccentColorChange,
                onBack = { currentSubMenu = null }
            )
            SettingsSubMenu.Widget -> WidgetSettingsScreen(onBack = { currentSubMenu = null })
            SettingsSubMenu.Notifications -> NotificationSettingsScreen(onBack = { currentSubMenu = null })
            SettingsSubMenu.Admin -> AdminPanelScreen(onBack = { currentSubMenu = null })
            SettingsSubMenu.About -> AboutSettingsScreen(
                onBack = { currentSubMenu = null },
                onUpdateFound = onUpdateFound
            )
        }
    }
}
