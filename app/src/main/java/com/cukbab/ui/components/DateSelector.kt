package com.cukbab.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cukbab.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    isBackEnabled: Boolean = true,
    isForwardEnabled: Boolean = true,
    onCoordsMeasured: ((LayoutCoordinates) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var dateAreaCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var parentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { parentCoords = it }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { 
                    var prev = selectedDate.minusDays(1)
                    while (prev.dayOfWeek == DayOfWeek.SATURDAY || prev.dayOfWeek == DayOfWeek.SUNDAY) {
                        prev = prev.minusDays(1)
                    }
                    onDateChange(prev)
                },
                enabled = isBackEnabled && !isExpanded
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.previous_day))
            }

            val configuration = LocalConfiguration.current
            val locale = configuration.locales[0]
            val formatter = remember(locale) {
                DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL).withLocale(locale)
            }
            
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            dateAreaCoords = coords
                            onCoordsMeasured?.invoke(coords)
                        }
                    },
                shape = CircleShape,
                color = Color.Transparent,
                onClick = { 
                    if (!isExpanded && dateAreaCoords != null && dateAreaCoords!!.isAttached) {
                        isExpanded = true 
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedDate.format(formatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            IconButton(
                onClick = { 
                    var next = selectedDate.plusDays(1)
                    while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                        next = next.plusDays(1)
                    }
                    onDateChange(next)
                },
                enabled = isForwardEnabled && !isExpanded
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.next_day))
            }
        }
    }

    if (isExpanded && dateAreaCoords != null && parentCoords != null) {
        val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
        
        DynamicIslandDatePicker(
            initialSelectedDate = selectedDate,
            sourceCoords = dateAreaCoords!!,
            anchorCoords = parentCoords!!,
            windowSize = windowSize,
            onDismiss = { isExpanded = false },
            onDateSelected = { onDateChange(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicIslandDatePicker(
    initialSelectedDate: LocalDate,
    sourceCoords: LayoutCoordinates,
    anchorCoords: LayoutCoordinates,
    windowSize: IntSize,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val transitionState = remember { 
        MutableTransitionState(false).apply { targetState = true } 
    }
    val transition = rememberTransition(transitionState, label = "IslandTransition")
    var pendingSelectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val targetWidthPx = with(density) { 360.dp.toPx().coerceAtMost(windowSize.width * 0.95f) }
    val targetHeightPx = with(density) { 520.dp.toPx().coerceAtMost(windowSize.height * 0.85f) }
    
    val posSpring = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
    val sizeSpring = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

    val sourcePos by remember { derivedStateOf { sourceCoords.positionInWindow() } }
    val sourceSize by remember { derivedStateOf { sourceCoords.size } }
    val sourceCenter by remember { 
        derivedStateOf { Offset(sourcePos.x + sourceSize.width/2f, sourcePos.y + sourceSize.height/2f) } 
    }
    val windowCenter = Offset(windowSize.width/2f, windowSize.height/2f)

    val currentCenterX by transition.animateFloat(label = "CenterX", transitionSpec = { posSpring }) {
        if (it) windowCenter.x else sourceCenter.x
    }
    val currentCenterY by transition.animateFloat(label = "CenterY", transitionSpec = { posSpring }) {
        if (it) windowCenter.y else sourceCenter.y
    }
    val currentWidth by transition.animateFloat(label = "Width", transitionSpec = { sizeSpring }) {
        if (it) targetWidthPx else sourceSize.width.toFloat()
    }
    val currentHeight by transition.animateFloat(label = "Height", transitionSpec = { sizeSpring }) {
        if (it) targetHeightPx else sourceSize.height.toFloat()
    }
    
    val containerAlpha by transition.animateFloat(label = "Alpha", transitionSpec = { if (targetState) tween(150) else tween(350) }) {
        if (it) 1f else 0f
    }
    val scrimAlpha by transition.animateFloat(label = "Scrim", transitionSpec = { tween(400) }) {
        if (it) 0.5f else 0f
    }
    val contentAlpha by transition.animateFloat(label = "Content", transitionSpec = { tween(200, 150) }) {
        if (it) 1f else 0f
    }

    val closeWithAnimation = { transitionState.targetState = false }
    if (transitionState.isIdle && !transitionState.targetState) {
        SideEffect {
            pendingSelectedDate?.let { onDateSelected(it) }
            onDismiss()
        }
    }

    val anchorPos = remember { anchorCoords.positionInWindow() }
    val popupOffset = IntOffset(-anchorPos.x.roundToInt(), -anchorPos.y.roundToInt())

    Popup(
        onDismissRequest = closeWithAnimation,
        properties = PopupProperties(focusable = true, excludeFromSystemGesture = true),
        offset = popupOffset
    ) {
        Box(
            modifier = Modifier
                .size(with(density) { windowSize.width.toDp() }, with(density) { windowSize.height.toDp() }),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(Color.Black)
                    .clickable { closeWithAnimation() }
            )

            Surface(
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            (currentCenterX - targetWidthPx/2f).roundToInt(),
                            (currentCenterY - targetHeightPx/2f).roundToInt()
                        )
                    }
                    .requiredSize(with(density) { targetWidthPx.toDp() }, with(density) { targetHeightPx.toDp() })
                    .graphicsLayer {
                        scaleX = currentWidth / targetWidthPx
                        scaleY = currentHeight / targetHeightPx
                        alpha = containerAlpha
                        shape = RoundedCornerShape(28.dp)
                        clip = true
                    },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = if (transitionState.targetState) 12.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = contentAlpha }
                ) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = initialSelectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                                val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                                val minDate = LocalDate.of(2026, 3, 2)
                                val maxDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                                return !isWeekend && !date.isBefore(minDate) && !date.isAfter(maxDate)
                            }
                        }
                    )

                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.weight(1f),
                        showModeToggle = false,
                        title = null,
                        headline = null
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = closeWithAnimation) { Text(stringResource(R.string.cancel)) }
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    pendingSelectedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                    closeWithAnimation()
                                }
                            }
                        ) { Text(stringResource(R.string.submit)) }
                    }
                }
            }
        }
    }
}
