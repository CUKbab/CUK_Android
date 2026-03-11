package com.cukbab.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cukbab.R
import kotlin.math.max

data class TutorialStep(
    val titleRes: Int? = null,
    val descriptionRes: Int,
    val targetCoords: LayoutCoordinates? = null,
    val isCircle: Boolean = false,
    val padding: Float = 16f,
    val radiusMultiplier: Float = 1.0f,
    val buttonTextRes: Int? = null
)

@Composable
fun AnimatedTutorialOverlay(
    steps: List<TutorialStep>,
    currentStepIdx: Int,
    onStepChange: (Int) -> Unit,
    onFinish: () -> Unit
) {
    var isFinishing by remember { mutableStateOf(false) }
    
    val containerAlpha by animateFloatAsState(
        targetValue = if (isFinishing) 0f else 1f,
        animationSpec = tween(600),
        label = "ContainerAlpha",
        finishedListener = { if (it == 0f) onFinish() }
    )

    val currentStep = steps.getOrNull(currentStepIdx) ?: return
    
    val density = LocalDensity.current
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val screenWidthPx = windowSize.width.toFloat()
    val screenHeightPx = windowSize.height.toFloat()

    var overlayPosInRoot by remember { mutableStateOf(Offset.Zero) }
    var lastValidCenter by remember { mutableStateOf(Offset(screenWidthPx / 2f, screenHeightPx / 2f)) }

    val targetRectLocal by remember(currentStep.targetCoords, overlayPosInRoot) {
        derivedStateOf {
            val coords = currentStep.targetCoords
            if (coords != null && coords.isAttached) {
                val targetPos = coords.positionInRoot()
                val localX = targetPos.x - overlayPosInRoot.x
                val localY = targetPos.y - overlayPosInRoot.y
                val size = coords.size
                val p = currentStep.padding
                
                val rect = Rect(localX - p, localY - p, localX + size.width + p, localY + size.height + p)
                lastValidCenter = rect.center
                rect
            } else {
                null
            }
        }
    }

    val animRectX by animateFloatAsState(targetValue = targetRectLocal?.left ?: (lastValidCenter.x), label = "RectX", animationSpec = spring(stiffness = Spring.StiffnessLow))
    val animRectY by animateFloatAsState(targetValue = targetRectLocal?.top ?: (lastValidCenter.y), label = "RectY", animationSpec = spring(stiffness = Spring.StiffnessLow))
    val animRectW by animateFloatAsState(targetValue = targetRectLocal?.width ?: 0f, label = "RectW", animationSpec = spring(stiffness = Spring.StiffnessLow))
    val animRectH by animateFloatAsState(targetValue = targetRectLocal?.height ?: 0f, label = "RectH", animationSpec = spring(stiffness = Spring.StiffnessLow))

    val isLastStep = currentStepIdx == steps.size - 1
    val targetBiasY = remember(targetRectLocal) {
        if (targetRectLocal == null) 0f 
        else if (targetRectLocal!!.top > screenHeightPx / 2f) -0.7f 
        else 0.7f 
    }
    val animBiasY by animateFloatAsState(targetValue = targetBiasY, label = "BiasY", animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = containerAlpha }
            .onGloballyPositioned { overlayPosInRoot = it.positionInRoot() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block clicks */ }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.85f)) {
            if (animRectW > 1f) {
                val holePath = Path().apply {
                    if (currentStep.isCircle) {
                        val baseRadius = max(animRectW, animRectH) / 2f
                        val radius = baseRadius * currentStep.radiusMultiplier
                        addOval(Rect(
                            center = Offset(animRectX + animRectW/2f, animRectY + animRectH/2f),
                            radius = radius
                        ))
                    } else {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(animRectX, animRectY, animRectX + animRectW, animRectY + animRectH),
                                cornerRadius = CornerRadius(16.dp.toPx())
                            )
                        )
                    }
                }
                
                clipPath(holePath, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black)
                }
            } else {
                drawRect(Color.Black)
            }
        }

        if (!isLastStep && !isFinishing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                TextButton(
                    onClick = { onStepChange(steps.size - 1) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_skip),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(32.dp),
            contentAlignment = BiasAlignment(0f, animBiasY)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TextFade"
                ) { step ->
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        step.titleRes?.let {
                            Text(
                                text = stringResource(it),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        
                        Text(
                            text = stringResource(step.descriptionRes),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                if (currentStepIdx < steps.size - 1) {
                                    onStepChange(currentStepIdx + 1)
                                } else {
                                    isFinishing = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val defaultText = if (currentStepIdx < steps.size - 1) 
                                stringResource(R.string.tutorial_next) 
                            else 
                                stringResource(R.string.tutorial_finish)
                            
                            Text(
                                text = step.buttonTextRes?.let { stringResource(it) } ?: defaultText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
