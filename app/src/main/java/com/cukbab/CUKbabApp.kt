package com.cukbab

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.cukbab.ui.components.AnnouncementBottomSheet
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.cukbab.data.*
import com.cukbab.ui.components.DateSelector
import com.cukbab.ui.components.WeekOverviewSheet
import com.cukbab.ui.components.MenuScreenState
import com.cukbab.ui.components.adjacentWeekday
import com.cukbab.ui.components.AnimatedTutorialOverlay
import com.cukbab.ui.components.TutorialStep
import com.cukbab.ui.screens.BuonPranzoScreen
import com.cukbab.ui.screens.CafeBonaScreen
import com.cukbab.ui.screens.SettingsScreen
import com.cukbab.ui.theme.ThemePreference
import com.cukbab.ui.screens.settings.SettingsSubMenu
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object BuonPranzo : Screen("buon_pranzo", R.string.screen_buon_pranzo, Icons.Default.Restaurant)
    data object CafeBona : Screen("cafe_bona", R.string.screen_cafe_bona, Icons.Default.Coffee)
    data object Settings : Screen("settings", R.string.screen_settings, Icons.Default.Settings)
}

sealed class MenuUiState {
    data object Loading : MenuUiState()
    data class Refreshing(val menuData: MenuData?) : MenuUiState()
    /** [isStale]: network failed, showing the saved copy. */
    data class Success(val menuData: MenuData, val isStale: Boolean = false) : MenuUiState()
    /** The selected week has no menu on the server yet. */
    data object NotPublished : MenuUiState()
    data class Error(val message: String) : MenuUiState()
}

val screenList = listOf(Screen.BuonPranzo, Screen.CafeBona, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CUKbabApp(
    windowWidthSizeClass: WindowWidthSizeClass,
    /** Screen.route to open first (from a launcher shortcut), or null for the default. */
    initialScreenRoute: String? = null,
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
    customAccentColor: androidx.compose.ui.graphics.Color?,
    onAccentColorChange: (androidx.compose.ui.graphics.Color?) -> Unit,
    onCheckForUpdates: suspend () -> Boolean
) {
    var selectedScreen by remember {
        mutableStateOf(screenList.firstOrNull { it.route == initialScreenRoute } ?: Screen.BuonPranzo)
    }
    var currentSubMenu by remember { mutableStateOf<SettingsSubMenu?>(null) }
    var showAnnouncements by remember { mutableStateOf(false) }
    var showWeekOverview by remember { mutableStateOf(false) }
    var uiState by remember { mutableStateOf<MenuUiState>(MenuUiState.Loading) }
    
    val initialDate = remember {
        val today = LocalDate.now()
        when (today.dayOfWeek) {
            DayOfWeek.SATURDAY -> today.minusDays(5)
            DayOfWeek.SUNDAY -> today.minusDays(6)
            else -> today
        }
    }
    var selectedDate by remember { mutableStateOf(initialDate) }
    
    var showChangelog by remember { mutableStateOf(false) }
    var pendingChangelog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val foldingFeature = remember(context) {
        WindowInfoTracker.getOrCreate(context).windowLayoutInfo(context as android.app.Activity)
    }.collectAsState(initial = null).value?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()

    val isDualPaneEnabled = experimentalDualPane && (windowWidthSizeClass != WindowWidthSizeClass.Compact || foldingFeature != null)

    val scope = rememberCoroutineScope()
    val failedToFetchMenu = stringResource(R.string.failed_to_fetch_menu)

    // Tutorial States
    var isTutorialActive by remember { 
        mutableStateOf(!TutorialRepository.isMainTutorialComplete(context) || !TutorialRepository.isSettingsTutorialComplete(context)) 
    }
    var tutorialStepIdx by remember { mutableIntStateOf(0) }
    
    var calendarCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var displayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var widgetCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var notifCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var feedbackCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val onboardingSteps = remember(calendarCoords, displayCoords, widgetCoords, notifCoords, feedbackCoords) {
        listOfNotNull(
            TutorialStep(
                titleRes = R.string.tutorial_welcome_title,
                descriptionRes = R.string.tutorial_welcome_desc
            ),
            TutorialStep(
                titleRes = R.string.tutorial_calendar_title,
                descriptionRes = R.string.tutorial_calendar_desc,
                targetCoords = calendarCoords,
                isCircle = false,
                padding = 16f
            ),
            // Phase 2: Settings
            TutorialStep(
                titleRes = R.string.tutorial_settings_title,
                descriptionRes = R.string.tutorial_settings_desc
            ),
            if (displayCoords != null) TutorialStep(
                descriptionRes = R.string.tutorial_display_desc,
                targetCoords = displayCoords
            ) else null,
            if (widgetCoords != null) TutorialStep(
                descriptionRes = R.string.tutorial_widget_desc,
                targetCoords = widgetCoords
            ) else null,
            if (notifCoords != null) TutorialStep(
                descriptionRes = R.string.tutorial_notif_desc,
                targetCoords = notifCoords
            ) else null,
            if (feedbackCoords != null) TutorialStep(
                descriptionRes = R.string.tutorial_feedback_desc,
                targetCoords = feedbackCoords
            ) else null,
            // Phase 3: Final Thank you (back on main screen)
            TutorialStep(
                titleRes = R.string.tutorial_thank_you_title,
                descriptionRes = R.string.tutorial_thank_you_desc
            )
        )
    }

    LaunchedEffect(Unit) {
        val currentVersion = VersionPreferences.getCurrentVersion(context)
        val lastVersion = VersionPreferences.getLastSeenVersion(context)
        val mainTutorialComplete = TutorialRepository.isMainTutorialComplete(context)
        
        if (lastVersion == 0) {
            isTutorialActive = true
            pendingChangelog = true
        } else if (!mainTutorialComplete && currentVersion == TutorialRepository.TUTORIAL_VERSION) {
            isTutorialActive = true
            pendingChangelog = true
        } else if (currentVersion > lastVersion) {
            if (lastVersion >= TutorialRepository.TUTORIAL_VERSION) {
                TutorialRepository.setMainTutorialComplete(context)
                TutorialRepository.setSettingsTutorialComplete(context)
            }
            showChangelog = true
        }
    }

    // Check for updates only once the tutorial and changelog are out of the way,
    // so the update dialog never stacks on top of them.
    var checkedForUpdates by remember { mutableStateOf(false) }
    LaunchedEffect(isTutorialActive, showChangelog, pendingChangelog) {
        if (!checkedForUpdates && !isTutorialActive && !showChangelog && !pendingChangelog) {
            checkedForUpdates = true
            onCheckForUpdates()
        }
    }

    var menuJob by remember { mutableStateOf<Job?>(null) }

    val refreshMenu = { force: Boolean, date: LocalDate ->
        // Cancel any in-flight load so a slow, older response can't overwrite a newer week.
        menuJob?.cancel()
        menuJob = scope.launch {
            val currentMenuData = when (val state = uiState) {
                is MenuUiState.Success -> state.menuData
                is MenuUiState.Refreshing -> state.menuData
                else -> null
            }

            val cached = MenuRepository.getCachedMenu(context, date)
            uiState = when {
                force -> MenuUiState.Refreshing(currentMenuData)
                cached != null -> MenuUiState.Success(cached)
                else -> MenuUiState.Loading
            }

            try {
                // Always hit the network so mid-week menu changes show up; cached data stays
                // on screen meanwhile.
                val data = MenuRepository.getMenu(context, date, forceRefresh = true, fallbackToCache = false)
                uiState = MenuUiState.Success(data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: MenuNotPublishedException) {
                uiState = MenuUiState.NotPublished
            } catch (e: Exception) {
                uiState = if (cached != null) {
                    MenuUiState.Success(cached, isStale = true)
                } else {
                    MenuUiState.Error(failedToFetchMenu)
                }
            }
        }
    }

    var lastLoadedWeek by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val weekFields = remember { WeekFields.ISO }
    
    // Refresh menu only when moving to a different week
    LaunchedEffect(selectedDate) {
        val weekNumber = selectedDate.get(weekFields.weekOfWeekBasedYear())
        val year = selectedDate.get(weekFields.weekBasedYear())
        
        val currentWeekInfo = year to weekNumber
        if (lastLoadedWeek != currentWeekInfo) {
            refreshMenu(false, selectedDate)
            lastLoadedWeek = currentWeekInfo
        }
    }

    val menuState = MenuScreenState(
        menuData = when (val state = uiState) {
            is MenuUiState.Success -> state.menuData
            is MenuUiState.Refreshing -> state.menuData
            else -> null
        },
        isLoading = uiState is MenuUiState.Loading,
        isRefreshing = uiState is MenuUiState.Refreshing,
        error = (uiState as? MenuUiState.Error)?.message,
        notPublished = uiState is MenuUiState.NotPublished,
        isStale = (uiState as? MenuUiState.Success)?.isStale == true,
        onRefresh = { refreshMenu(true, selectedDate) },
        onGoToCurrentWeek = { selectedDate = initialDate },
        onSwipeDay = { forward -> selectedDate = adjacentWeekday(selectedDate, forward) },
        onShowWeekOverview = { showWeekOverview = true }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                val topAppBarState = rememberSaveable(selectedScreen, currentSubMenu, saver = TopAppBarState.Saver) {
                    TopAppBarState(0f, 0f, 0f)
                }
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
                
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        if (!isDualPaneEnabled) {
                            CenterAlignedTopAppBar(
                                title = {
                                    val title = if (selectedScreen == Screen.Settings && currentSubMenu != null) {
                                        when (currentSubMenu) {
                                            SettingsSubMenu.Account -> stringResource(R.string.account_settings)
                                            SettingsSubMenu.Display -> stringResource(R.string.display_settings)
                                            SettingsSubMenu.Notifications -> stringResource(R.string.notification_settings)
                                            SettingsSubMenu.Admin -> stringResource(R.string.admin_panel)
                                            SettingsSubMenu.Widget -> stringResource(R.string.widget_settings)
                                            SettingsSubMenu.About -> stringResource(R.string.about)
                                            else -> stringResource(selectedScreen.labelRes)
                                        }
                                    } else {
                                        stringResource(selectedScreen.labelRes)
                                    }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                navigationIcon = {
                                    if (selectedScreen == Screen.Settings && currentSubMenu != null) {
                                        IconButton(onClick = { currentSubMenu = null }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                                        }
                                    }
                                },
                                actions = {
                                    if (selectedScreen != Screen.Settings) {
                                        IconButton(onClick = { showWeekOverview = true }) {
                                            Icon(Icons.Default.CalendarViewWeek, contentDescription = stringResource(R.string.week_overview))
                                        }
                                        IconButton(onClick = { showAnnouncements = true }) {
                                            Icon(Icons.Default.Campaign, contentDescription = stringResource(R.string.announcements))
                                        }
                                        IconButton(onClick = { refreshMenu(true, selectedDate) }) {
                                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                                        }
                                    }
                                },
                                scrollBehavior = scrollBehavior
                            )
                        }
                    },
                    bottomBar = {
                        if (!isDualPaneEnabled) {
                            NavigationBar {
                                screenList.forEach { screen ->
                                    val label = stringResource(screen.labelRes)
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = label) },
                                        label = { Text(label) },
                                        selected = selectedScreen == screen,
                                        onClick = { 
                                            selectedScreen = screen
                                            currentSubMenu = null
                                        }
                                    )
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets.systemBars
                ) { innerPadding ->
                    AnimatedContentArea(
                        selectedScreen,
                        currentSubMenu,
                        onSubMenuChange = { currentSubMenu = it },
                        menuState,
                        homeDate = initialDate,
                        selectedDate,
                        onDateChange = { selectedDate = it },
                        weekFields,
                        themePreference,
                        onThemeChange,
                        baseFontSize,
                        onFontSizeChange,
                        showOperatingHours,
                        onShowOperatingHoursChange,
                        experimentalDualPane,
                        onExperimentalDualPaneChange,
                        languagePreference,
                        onLanguageChange,
                        customAccentColor,
                        onAccentColorChange,
                        onCheckForUpdates = onCheckForUpdates,
                        onShowChangelog = { showChangelog = true },
                        onShowAnnouncements = { showAnnouncements = true },
                        onCalendarCoordsMeasured = { calendarCoords = it },
                        onDisplayCoordsMeasured = { displayCoords = it },
                        onWidgetCoordsMeasured = { widgetCoords = it },
                        onNotifCoordsMeasured = { notifCoords = it },
                        onFeedbackCoordsMeasured = { feedbackCoords = it },
                        isDualPaneEnabled = isDualPaneEnabled,
                        onSettingsClick = { selectedScreen = Screen.Settings },
                        onMenuClick = { selectedScreen = Screen.BuonPranzo },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isDualPaneEnabled) {
                        NavigationRail(
                            modifier = Modifier.systemBarsPadding(),
                            header = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = { showWeekOverview = true }) {
                                        Icon(Icons.Default.CalendarViewWeek, contentDescription = stringResource(R.string.week_overview))
                                    }
                                    IconButton(onClick = { showAnnouncements = true }) {
                                        Icon(Icons.Default.Campaign, contentDescription = stringResource(R.string.announcements))
                                    }
                                    IconButton(onClick = { refreshMenu(true, selectedDate) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                                    }
                                }
                            }
                        ) {
                            Spacer(Modifier.weight(1f))
                            screenList.forEach { screen ->
                                val label = stringResource(screen.labelRes)
                                NavigationRailItem(
                                    icon = { Icon(screen.icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = selectedScreen == screen,
                                    onClick = {
                                        selectedScreen = screen
                                        currentSubMenu = null
                                    }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    AnimatedContentArea(
                        selectedScreen,
                        currentSubMenu,
                        onSubMenuChange = { currentSubMenu = it },
                        menuState,
                        homeDate = initialDate,
                        selectedDate,
                        onDateChange = { selectedDate = it },
                        weekFields,
                        themePreference,
                        onThemeChange,
                        baseFontSize,
                        onFontSizeChange,
                        showOperatingHours,
                        onShowOperatingHoursChange,
                        experimentalDualPane,
                        onExperimentalDualPaneChange,
                        languagePreference,
                        onLanguageChange,
                        customAccentColor,
                        onAccentColorChange,
                        onCheckForUpdates = onCheckForUpdates,
                        onShowChangelog = { showChangelog = true },
                        onShowAnnouncements = { showAnnouncements = true },
                        onCalendarCoordsMeasured = { calendarCoords = it },
                        onDisplayCoordsMeasured = { displayCoords = it },
                        onWidgetCoordsMeasured = { widgetCoords = it },
                        onNotifCoordsMeasured = { notifCoords = it },
                        onFeedbackCoordsMeasured = { feedbackCoords = it },
                        isDualPaneEnabled = isDualPaneEnabled,
                        onSettingsClick = { selectedScreen = Screen.Settings },
                        onMenuClick = { selectedScreen = Screen.BuonPranzo },
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    )
                }
            }

            // UNIFIED TUTORIAL OVERLAY
            if (isTutorialActive && onboardingSteps.isNotEmpty()) {
                AnimatedTutorialOverlay(
                    steps = onboardingSteps,
                    currentStepIdx = tutorialStepIdx,
                    onStepChange = { nextIdx ->
                        // Automatically switch screen when moving to Settings part of tutorial
                        if (tutorialStepIdx == 1 && nextIdx == 2) {
                            selectedScreen = Screen.Settings
                        }
                        // Automatically switch back to Main screen for the last step
                        if (nextIdx == onboardingSteps.size - 1) {
                            selectedScreen = Screen.BuonPranzo
                        }
                        tutorialStepIdx = nextIdx
                    },
                    onFinish = {
                        TutorialRepository.setMainTutorialComplete(context)
                        TutorialRepository.setSettingsTutorialComplete(context)
                        isTutorialActive = false
                        // Once tutorial is over, check if we need to show the changelog
                        if (pendingChangelog) {                            showChangelog = true
                            pendingChangelog = false
                        }
                    }
                )
            }
        }
    }

    if (showWeekOverview) {
        WeekOverviewSheet(
            menuData = menuState.menuData,
            selectedDate = selectedDate,
            categories = when {
                menuState.menuData?.keys?.let { k -> k.isNotEmpty() && k.all { it == "Lunch" || it == "Dinner" } } == true ->
                    listOf("Lunch", "Dinner")
                selectedScreen == Screen.CafeBona && !isDualPaneEnabled -> listOf("Bona-Rice-Bowl")
                isDualPaneEnabled -> listOf("Morning", "Pranzo-Korean", "Pranzo-Global-Noodle", "Pranzo-Dinner", "Bona-Rice-Bowl")
                else -> listOf("Morning", "Pranzo-Korean", "Pranzo-Global-Noodle", "Pranzo-Plus-Corner", "Pranzo-Dinner")
            },
            onDaySelected = {
                selectedDate = it
                showWeekOverview = false
            },
            onDismiss = { showWeekOverview = false }
        )
    }

    if (showAnnouncements) {
        AnnouncementBottomSheet(onDismiss = { showAnnouncements = false })
    }

    if (showChangelog) {
        val currentLang = if (languagePreference.tag.isEmpty()) {
            java.util.Locale.getDefault().language
        } else {
            languagePreference.tag
        }
        ChangelogDialog(
            language = currentLang,
            onDismiss = { 
                showChangelog = false
                val currentVersion = VersionPreferences.getCurrentVersion(context)
                VersionPreferences.setLastSeenVersion(context, currentVersion)
            }
        )
    }
}

@Composable
fun AnimatedContentArea(
    screen: Screen,
    currentSubMenu: SettingsSubMenu?,
    onSubMenuChange: (SettingsSubMenu?) -> Unit,
    menuState: MenuScreenState,
    homeDate: LocalDate,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    weekFields: WeekFields,
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
    customAccentColor: androidx.compose.ui.graphics.Color?,
    onAccentColorChange: (androidx.compose.ui.graphics.Color?) -> Unit,
    onCheckForUpdates: suspend () -> Boolean,
    onShowChangelog: () -> Unit,
    onShowAnnouncements: () -> Unit,
    onCalendarCoordsMeasured: (LayoutCoordinates) -> Unit,
    onDisplayCoordsMeasured: (LayoutCoordinates) -> Unit,
    onWidgetCoordsMeasured: (LayoutCoordinates) -> Unit,
    onNotifCoordsMeasured: (LayoutCoordinates) -> Unit,
    onFeedbackCoordsMeasured: (LayoutCoordinates) -> Unit,
    isDualPaneEnabled: Boolean,
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val fromIndex = screenList.indexOf(initialState)
            val toIndex = screenList.indexOf(targetState)
            if (toIndex > fromIndex) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "TabTransition"
    ) { targetScreen ->
        ContentArea(
            targetScreen, 
            currentSubMenu,
            onSubMenuChange,
            menuState,
            homeDate,
            selectedDate, 
            onDateChange, 
            weekFields,
            themePreference, 
            onThemeChange, 
            baseFontSize,
            onFontSizeChange,
            showOperatingHours,
            onShowOperatingHoursChange,
            experimentalDualPane,
            onExperimentalDualPaneChange,
            languagePreference,
            onLanguageChange,
            customAccentColor,
            onAccentColorChange,
            onCheckForUpdates,
            onShowChangelog,
            onShowAnnouncements,
            onCalendarCoordsMeasured,
            onDisplayCoordsMeasured,
            onWidgetCoordsMeasured,
            onNotifCoordsMeasured,
            onFeedbackCoordsMeasured,
            isDualPaneEnabled,
            onSettingsClick,
            onMenuClick,
            modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentArea(
    screen: Screen,
    currentSubMenu: SettingsSubMenu?,
    onSubMenuChange: (SettingsSubMenu?) -> Unit,
    menuState: MenuScreenState,
    homeDate: LocalDate,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    weekFields: WeekFields,
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
    customAccentColor: androidx.compose.ui.graphics.Color?,
    onAccentColorChange: (androidx.compose.ui.graphics.Color?) -> Unit,
    onCheckForUpdates: suspend () -> Boolean,
    onShowChangelog: () -> Unit,
    onShowAnnouncements: () -> Unit,
    onCalendarCoordsMeasured: (LayoutCoordinates) -> Unit,
    onDisplayCoordsMeasured: (LayoutCoordinates) -> Unit,
    onWidgetCoordsMeasured: (LayoutCoordinates) -> Unit,
    onNotifCoordsMeasured: (LayoutCoordinates) -> Unit,
    onFeedbackCoordsMeasured: (LayoutCoordinates) -> Unit,
    isDualPaneEnabled: Boolean,
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember(selectedDate) { selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isDualPaneEnabled) {
                if (screen !is Screen.Settings) {
                    DateSelector(
                        selectedDate = selectedDate,
                        onDateChange = onDateChange,
                        homeDate = homeDate,
                        onCoordsMeasured = onCalendarCoordsMeasured
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (screen) {
                    is Screen.BuonPranzo -> BuonPranzoScreen(
                        menuState, dateString, showOperatingHours
                    )
                    is Screen.CafeBona -> CafeBonaScreen(
                        menuState, dateString, showOperatingHours
                    )
                    is Screen.Settings -> SettingsScreen(
                        currentSubMenu, onSubMenuChange,
                        themePreference, onThemeChange, baseFontSize, onFontSizeChange, 
                        showOperatingHours, onShowOperatingHoursChange,
                        experimentalDualPane, onExperimentalDualPaneChange,
                        languagePreference, onLanguageChange,
                        customAccentColor, onAccentColorChange,
                        onShowChangelog = onShowChangelog,
                        onCheckForUpdates = onCheckForUpdates,
                        onDisplayCoordsMeasured = onDisplayCoordsMeasured,
                        onWidgetCoordsMeasured = onWidgetCoordsMeasured,
                        onNotifCoordsMeasured = onNotifCoordsMeasured,
                        onFeedbackCoordsMeasured = onFeedbackCoordsMeasured
                    )
                }
            } else {
                // Dual Pane Mode Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (screen is Screen.Settings) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.screen_buon_pranzo))
                            }
                        } else {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.screen_settings))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = menuState.onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onShowAnnouncements) {
                            Icon(Icons.Default.Campaign, contentDescription = stringResource(R.string.announcements))
                        }
                        if (screen !is Screen.Settings) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = menuState.onShowWeekOverview) {
                                Icon(Icons.Default.CalendarViewWeek, contentDescription = stringResource(R.string.week_overview))
                            }
                        }
                    }
                    
                    if (screen !is Screen.Settings) {
                        DateSelector(
                            selectedDate = selectedDate,
                            onDateChange = onDateChange,
                            homeDate = homeDate,
                            onCoordsMeasured = onCalendarCoordsMeasured
                        )
                    } else {
                        Text(
                            text = stringResource(screen.labelRes),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (screen is Screen.Settings) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(
                            currentSubMenu, onSubMenuChange,
                            themePreference, onThemeChange, baseFontSize, onFontSizeChange, 
                            showOperatingHours, onShowOperatingHoursChange,
                            experimentalDualPane, onExperimentalDualPaneChange,
                            languagePreference, onLanguageChange,
                            customAccentColor, onAccentColorChange,
                            onShowChangelog = onShowChangelog,
                            onCheckForUpdates = onCheckForUpdates,
                            onDisplayCoordsMeasured = onDisplayCoordsMeasured,
                            onWidgetCoordsMeasured = onWidgetCoordsMeasured,
                            onNotifCoordsMeasured = onNotifCoordsMeasured,
                            onFeedbackCoordsMeasured = onFeedbackCoordsMeasured
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = stringResource(Screen.BuonPranzo.labelRes),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                                )
                                BuonPranzoScreen(
                                    menuState, dateString, showOperatingHours
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp))
                        Spacer(modifier = Modifier.width(16.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = stringResource(Screen.CafeBona.labelRes),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                                )
                                CafeBonaScreen(
                                    menuState, dateString, showOperatingHours
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangelogDialog(language: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var changelogText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val workerLang = AnnouncementRepository.contentLangFor(language)

    LaunchedEffect(Unit) {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        changelogText = ReporterClient.fetchChangelog(workerLang, versionName)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.whats_new),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 450.dp).fillMaxWidth()) {
                if (isLoading) {
                    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val lines = changelogText?.split("\n") ?: listOf("No changelog available.")
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        lines.forEach { line ->
                            val trimmedLine = line.trim()
                            when {
                                trimmedLine.startsWith("# ") -> {
                                }
                                trimmedLine.startsWith("## ") -> {
                                    Text(
                                        text = trimmedLine.removePrefix("## "),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                    )
                                }
                                trimmedLine.startsWith("- ") -> {
                                    Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = trimmedLine.removePrefix("- "),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                trimmedLine.isNotEmpty() -> {
                                    Text(
                                        text = trimmedLine,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
