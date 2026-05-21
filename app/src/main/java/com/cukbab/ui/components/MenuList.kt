package com.cukbab.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cukbab.R
import com.cukbab.data.MenuData

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuList(
    menuData: MenuData?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    error: String?,
    today: String,
    showOperatingHours: Boolean = false,
    groups: Map<Int, List<String>>,
    collapsedGroups: Set<Int> = emptySet(),
    onToggleGroup: (Int) -> Unit = {}
) {
    val ptrState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val thresholdPx = remember { with(density) { 80.dp.toPx() } }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
                .graphicsLayer {
                    translationY = ptrState.distanceFraction * thresholdPx
                }
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            } else if (menuData != null) {
                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn() + slideInVertically { it / 20 },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        groups.forEach { (headerRes, categories) ->
                            item(key = headerRes) {
                                val isCollapsed = collapsedGroups.contains(headerRes)
                                val rotationState by animateFloatAsState(
                                    targetValue = if (isCollapsed) 0f else 180f,
                                    label = "RotationAnimation"
                                )

                                Column {
                                    val headerText = stringResource(headerRes)
                                    val operatingHours = when (headerRes) {
                                        R.string.group_morning -> " (08:00 ~ 09:30)"
                                        R.string.group_lunch -> " (11:30 ~ 14:00)"
                                        R.string.group_dinner -> " (17:30 ~ 19:00)"
                                        R.string.screen_cafe_bona -> " (11:30 ~ 14:00)"
                                        else -> ""
                                    }
                                    
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
                                            @Suppress("DEPRECATION")
                                            Text(
                                                text = if (showOperatingHours) headerText + operatingHours else headerText,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (isCollapsed) "Expand" else "Collapse",
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
