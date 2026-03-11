package com.cukbab

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import com.cukbab.data.AppVersion
import com.cukbab.data.NotificationPreferences
import com.cukbab.data.VersionRepository
import com.cukbab.ui.theme.CUKbabTheme
import com.cukbab.ui.theme.ThemePreference
import java.time.LocalDate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Clean up old notification queue in background
        lifecycleScope.launch(Dispatchers.IO) {
            NotificationPreferences.clearOldSchedules(this@MainActivity, LocalDate.now().toString())
        }
        
        enableEdgeToEdge()
        @Suppress("DEPRECATION")
        window.isStatusBarContrastEnforced = false
        @Suppress("DEPRECATION")
        window.isNavigationBarContrastEnforced = false

        // Load preferences synchronously for initial state to avoid recomposition flicker
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val themeStr = prefs.getString("theme", ThemePreference.System.name) ?: ThemePreference.System.name
        val initialThemePreference = try { ThemePreference.valueOf(themeStr) } catch(e: Exception) { ThemePreference.System }
        val initialBaseFontSize = prefs.getFloat("font_size", DEFAULT_FONT_SIZE)
        val initialShowOperatingHours = prefs.getBoolean("show_operating_hours", true)
        val initialColorArgb = prefs.getLong("accent_color", -1L)
        val initialCustomAccentColor = if (initialColorArgb != -1L) {
            Color(initialColorArgb.toInt())
        } else null

        val langStr = prefs.getString("language", LanguagePreference.System.name) ?: LanguagePreference.System.name
        val initialLanguage = try { LanguagePreference.valueOf(langStr) } catch(e: Exception) { LanguagePreference.System }

        // Apply initial language preference to AppCompatDelegate
        val initialAppLocales = if (initialLanguage == LanguagePreference.System) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(initialLanguage.tag)
        }
        if (AppCompatDelegate.getApplicationLocales() != initialAppLocales) {
            AppCompatDelegate.setApplicationLocales(initialAppLocales)
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            var themePreference by remember { mutableStateOf(initialThemePreference) }
            var baseFontSize by remember { mutableFloatStateOf(initialBaseFontSize) }
            var showOperatingHours by remember { mutableStateOf(initialShowOperatingHours) }
            var customAccentColor by remember { mutableStateOf<Color?>(initialCustomAccentColor) }
            var updateInfo by remember { mutableStateOf<AppVersion?>(null) }
            var languagePreference by remember { mutableStateOf(initialLanguage) }

            // Still check for updates and potentially refresh state from other sources if needed
            LaunchedEffect(Unit) {
                updateInfo = VersionRepository.checkForUpdate(this@MainActivity)
            }

            CUKbabTheme(
                themePreference = themePreference,
                baseFontSize = baseFontSize,
                customAccentColor = customAccentColor
            ) {
                CUKbabApp(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    themePreference = themePreference,
                    onThemeChange = {
                        themePreference = it
                        prefs.edit { putString("theme", it.name) }
                    },
                    baseFontSize = baseFontSize,
                    onFontSizeChange = {
                        baseFontSize = it
                        prefs.edit { putFloat("font_size", it) }
                    },
                    showOperatingHours = showOperatingHours,
                    onShowOperatingHoursChange = {
                        showOperatingHours = it
                        prefs.edit { putBoolean("show_operating_hours", it) }
                    },
                    languagePreference = languagePreference,
                    onLanguageChange = {
                        languagePreference = it
                        prefs.edit { putString("language", it.name) }
                        val appLocales = if (it == LanguagePreference.System) {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(it.tag)
                        }
                        AppCompatDelegate.setApplicationLocales(appLocales)
                    },
                    customAccentColor = customAccentColor,
                    onAccentColorChange = { color ->
                        customAccentColor = color
                        if (color == null) {
                            prefs.edit { putLong("accent_color", -1L) }
                        } else {
                            prefs.edit { putLong("accent_color", color.toArgb().toLong()) }
                        }
                    },
                    onUpdateFound = { updateInfo = it }
                )

                if (updateInfo != null) {
                    UpdateDialog(
                        updateInfo!!,
                        onDismiss = { updateInfo = null },
                        onUpdate = {
                            VersionRepository.openPlayStore(this@MainActivity)
                            updateInfo = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(appVersion: AppVersion, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_available)) },
        text = { 
            Text(
                stringResource(R.string.update_available_desc, appVersion.versionName)
            ) 
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text(stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

const val DEFAULT_FONT_SIZE = 16f
