package com.example.ui.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkEmeraldActive
import com.example.ui.theme.StarkGoldTertiary
import com.example.ui.theme.StarkTealSecondary
import com.example.ui.theme.StarkWarningRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcCoreVisualizer(
    state: AssistantState,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcCoreTransition")

    // Rotation angles
    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 1500
                    AssistantState.LISTENING -> 3500
                    AssistantState.SPEAKING -> 4000
                    else -> 8000
                },
                easing = LinearEasing
            )
        ),
        label = "RotationFast"
    )

    val rotationCounter by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 2200
                    else -> 12000
                },
                easing = LinearEasing
            )
        ),
        label = "RotationCounter"
    )

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 800
                    AssistantState.SPEAKING -> 600
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathScale"
    )

    // Dynamic state theme color
    val primaryCoreColor by animateColorAsState(
        targetValue = when (state) {
            AssistantState.IDLE -> StarkCyanPrimary
            AssistantState.LISTENING -> StarkCyanPrimary
            AssistantState.THINKING -> StarkGoldTertiary
            AssistantState.SPEAKING -> StarkTealSecondary
            AssistantState.ERROR -> StarkWarningRed
            AssistantState.BACKGROUND_ACTIVE -> StarkEmeraldActive
        },
        label = "CoreColor"
    )

    val secondaryCoreColor by animateColorAsState(
        targetValue = when (state) {
            AssistantState.IDLE -> StarkTealSecondary
            AssistantState.LISTENING -> Color(0xFF80EEFF)
            AssistantState.THINKING -> Color(0xFFFF8800)
            AssistantState.SPEAKING -> StarkCyanPrimary
            AssistantState.ERROR -> Color(0xFFFF5252)
            AssistantState.BACKGROUND_ACTIVE -> Color(0xFF00BFA5)
        },
        label = "CoreSecondaryColor"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f

            // 1. Outer ambient radial aura glow
            val auraRadius = (baseRadius * 0.95f) * (if (state == AssistantState.LISTENING) {
                1f + (audioLevel.coerceIn(0f, 10f) * 0.03f)
            } else breathScale)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCoreColor.copy(alpha = if (state == AssistantState.LISTENING) 0.35f else 0.18f),
                        secondaryCoreColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = center
            )

            // 2. Outer segmented ring (clockwise)
            rotate(rotationFast, pivot = center) {
                drawArc(
                    color = primaryCoreColor.copy(alpha = 0.6f),
                    startAngle = 0f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.85f, center.y - baseRadius * 0.85f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.7f, baseRadius * 1.7f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryCoreColor.copy(alpha = 0.6f),
                    startAngle = 120f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.85f, center.y - baseRadius * 0.85f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.7f, baseRadius * 1.7f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryCoreColor.copy(alpha = 0.6f),
                    startAngle = 240f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.85f, center.y - baseRadius * 0.85f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.7f, baseRadius * 1.7f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Middle dashed telemetry ring (counter-clockwise)
            rotate(rotationCounter, pivot = center) {
                drawCircle(
                    color = secondaryCoreColor.copy(alpha = 0.45f),
                    radius = baseRadius * 0.72f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                    )
                )

                // 4 Small orbital nodes
                val nodeRadius = baseRadius * 0.72f
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90.0))
                    val nodePos = Offset(
                        center.x + (nodeRadius * cos(angle)).toFloat(),
                        center.y + (nodeRadius * sin(angle)).toFloat()
                    )
                    drawCircle(
                        color = secondaryCoreColor,
                        radius = 2.5.dp.toPx(),
                        center = nodePos
                    )
                }
            }

            // 4. Inner resonant arc ring
            val innerRingRadius = baseRadius * 0.55f * breathScale
            drawCircle(
                color = primaryCoreColor.copy(alpha = 0.75f),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 5. Central glowing neural core
            val corePulseRadius = baseRadius * 0.38f * (
                if (state == AssistantState.LISTENING) {
                    1f + (audioLevel.coerceIn(0f, 10f) * 0.05f)
                } else if (state == AssistantState.SPEAKING) {
                    breathScale
                } else {
                    0.95f
                }
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryCoreColor,
                        secondaryCoreColor.copy(alpha = 0.7f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = corePulseRadius
                ),
                radius = corePulseRadius,
                center = center
            )

            // 6. Central geometric iris pip
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 4.dp.toPx(),
                center = center
            )
        }
    }
}
