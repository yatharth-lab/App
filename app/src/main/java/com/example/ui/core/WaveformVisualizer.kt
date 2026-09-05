package com.example.ui.core

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkGoldTertiary
import com.example.ui.theme.StarkTealSecondary
import com.example.ui.theme.StarkWarningRed
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    state: AssistantState,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 1000
                    AssistantState.SPEAKING -> 800
                    AssistantState.THINKING -> 1200
                    else -> 2800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val activeColor = when (state) {
        AssistantState.IDLE -> StarkCyanPrimary.copy(alpha = 0.5f)
        AssistantState.LISTENING -> StarkCyanPrimary
        AssistantState.THINKING -> StarkGoldTertiary
        AssistantState.SPEAKING -> StarkTealSecondary
        AssistantState.ERROR -> StarkWarningRed
        AssistantState.BACKGROUND_ACTIVE -> StarkTealSecondary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val spacing = width / barCount
            val barWidth = spacing * 0.55f

            for (i in 0 until barCount) {
                val normalizedIndex = i.toFloat() / barCount
                val waveFactor = sin((normalizedIndex * 3.14f * 2f) + phase).toFloat()

                val minHeight = 4.dp.toPx()
                val calculatedHeight = when (state) {
                    AssistantState.LISTENING -> {
                        val levelMultiplier = (audioLevel.coerceIn(0f, 10f) / 10f)
                        val dynamicWave = (waveFactor + 1.2f) * 0.5f
                        minHeight + (height * 0.75f * (0.25f + levelMultiplier * 0.75f) * dynamicWave)
                    }
                    AssistantState.SPEAKING -> {
                        val speechWave = ((waveFactor * 0.5f) + 0.5f)
                        minHeight + (height * 0.65f * speechWave)
                    }
                    AssistantState.THINKING -> {
                        val thinkWave = ((waveFactor * 0.4f) + 0.4f)
                        minHeight + (height * 0.45f * thinkWave)
                    }
                    else -> {
                        minHeight + (height * 0.15f * ((waveFactor * 0.3f) + 0.3f))
                    }
                }.coerceIn(minHeight, height * 0.95f)

                val x = (i * spacing) + (spacing - barWidth) / 2f
                val y = centerY - (calculatedHeight / 2f)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeColor,
                            activeColor.copy(alpha = 0.35f)
                        ),
                        startY = y,
                        endY = y + calculatedHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, calculatedHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
