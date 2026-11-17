package com.cukbab.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cukbab.R
import com.cukbab.data.MenuData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

/** Everything a menu screen needs to render one day. */
data class MenuScreenState(
    val menuData: MenuData?,
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val error: String?,
    /** The selected week has no menu on the server yet. */
    val notPublished: Boolean,
    /** Network failed; [menuData] is the saved copy. */
    val isStale: Boolean,
    val onRefresh: () -> Unit,
    val onGoToCurrentWeek: () -> Unit,
    /** forward = true for the next weekday. */
    val onSwipeDay: (forward: Boolean) -> Unit,
    val onShowWeekOverview: () -> Unit = {}
)

/** Serving window per menu group header. Also drives the "(08:00 ~ 09:30)" labels. */
private val mealWindows: Map<Int, Pair<LocalTime, LocalTime>> = mapOf(
    R.string.group_morning to (LocalTime.of(8, 0) to LocalTime.of(9, 30)),
    R.string.group_lunch to (LocalTime.of(11, 30) to LocalTime.of(14, 0)),
    R.string.group_dinner to (LocalTime.of(17, 30) to LocalTime.of(19, 0)),
    R.string.screen_cafe_bona to (LocalTime.of(11, 30) to LocalTime.of(14, 0))
)

private enum class MealBadge { NowServing, UpNext }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuList(
    state: MenuScreenState,
    today: String,
    showOperatingHours: Boolean = false,
    groups: Map<Int, List<String>>,
    collapsedGroups: Set<Int> = emptySet(),
    onToggleGroup: (Int) -> Unit = {}
) {
    val menuData = state.menuData
    val isRefreshing = state.isRefreshing
    val ptrState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val thresholdPx = remember { with(density) { 80.dp.toPx() } }
    val swipeThresholdPx = remember { with(density) { 72.dp.toPx() } }

    // Ticks every 30 s so the "Now serving" badge moves with the clock.
    val now by produceState(LocalTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalTime.now()
        }
    }
    val isToday = today == LocalDate.now().toString()
    // The meal being served now, or the next one today.
    val badgeGroup: Pair<Int, MealBadge>? = if (!isToday) null else {
        groups.keys.firstNotNullOfOrNull { res ->
            val (start, end) = mealWindows[res] ?: return@firstNotNullOfOrNull null
            when {
                now < start -> res to MealBadge.UpNext
                now < end -> res to MealBadge.NowServing
                else -> null
            }
        }
    }

    val listState = rememberLazyListState()
    val leadingItems = if (state.isStale) 1 else 0
    // Per day viewed: jump to the current meal today, otherwise start at the top
    // (the list state is shared across days, so it would keep the old position).
    val hasData = menuData != null && !state.isLoading
    LaunchedEffect(today, hasData) {
        if (!hasData) return@LaunchedEffect
        val index = badgeGroup?.first?.let { groups.keys.indexOf(it) } ?: -1
        if (index > 0) {
            listState.animateScrollToItem(index + leadingItems)
        } else {
            listState.scrollToItem(0)
        }
    }

    // Horizontal swipe changes the day; content follows the finger a little.
    val onSwipeDay by rememberUpdatedState(state.onSwipeDay)
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = state.onRefresh,
        state = ptrState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            val scaleFraction = if (isRefreshing) 1f else ptrState.distanceFraction.coerceIn(0f, 1f)
            val alphaFraction = if (isRefreshing) 1f else ptrState.distanceFraction.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .graphicsLayer {
                        alpha = alphaFraction
                        scaleX = scaleFraction
                        scaleY = scaleFraction
                    }
            ) {
                if (isRefreshing) {
                    LoadingIndicator()
                } else {
                    LoadingIndicator(
                        progress = { ptrState.distanceFraction }
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (abs(total) > swipeThresholdPx) onSwipeDay(total < 0)
                            scope.launch { swipeOffset.animateTo(0f) }
                        },
                        onDragCancel = { scope.launch { swipeOffset.animateTo(0f) } },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            total += amount
                            scope.launch { swipeOffset.snapTo(total * 0.35f) }
                        }
                    )
                }
                .graphicsLayer {
                    translationY = ptrState.distanceFraction * thresholdPx
                    translationX = swipeOffset.value
                    alpha = 1f - (abs(swipeOffset.value) / (size.width.coerceAtLeast(1f))).coerceIn(0f, 0.5f)
                }
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
                state.notPublished -> NotPublishedContent(onGoToCurrentWeek = state.onGoToCurrentWeek)
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = state.onRefresh) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                menuData != null -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (state.isStale) {
                            item(key = "stale_banner") { StaleBanner() }
                        }
                        groups.forEach { (headerRes, categories) ->
                            item(key = headerRes) {
                                val isCollapsed = collapsedGroups.contains(headerRes)
                                val badge = badgeGroup?.takeIf { it.first == headerRes }?.second
                                val rotationState by animateFloatAsState(
                                    targetValue = if (isCollapsed) 0f else 180f,
                                    label = "RotationAnimation"
                                )

                                Column {
                                    val headerText = stringResource(headerRes)
                                    val operatingHours = mealWindows[headerRes]?.let { (start, end) ->
                                        " ($start ~ $end)"
                                    } ?: ""

                                    Surface(
                                        onClick = { onToggleGroup(headerRes) },
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (showOperatingHours) headerText + operatingHours else headerText,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = if (badge == MealBadge.NowServing) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (badge != null) {
                                                    Spacer(Modifier.width(8.dp))
                                                    MealBadgeChip(badge)
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = stringResource(if (isCollapsed) R.string.expand else R.string.collapse),
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.rotate(rotationState)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = !isCollapsed,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            categories.forEach { category ->
                                                val noMenuString = stringResource(R.string.no_menu)
                                                val rawMenu = menuData[category]?.get(today) ?: "No Menu"
                                                val menuForToday = if (rawMenu.trim() == "No Menu") noMenuString else rawMenu
                                                MenuCard(category, menuForToday, today)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealBadgeChip(badge: MealBadge) {
    val (container, content) = when (badge) {
        MealBadge.NowServing -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        MealBadge.UpNext -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = stringResource(if (badge == MealBadge.NowServing) R.string.now_serving else R.string.up_next),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StaleBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.saved_menu_banner), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NotPublishedContent(onGoToCurrentWeek: () -> Unit) {
    // Scrollable so pull-to-refresh still works on this screen.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.EventBusy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.menu_not_published),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onGoToCurrentWeek) {
            Text(stringResource(R.string.back_to_this_week))
        }
    }
}
