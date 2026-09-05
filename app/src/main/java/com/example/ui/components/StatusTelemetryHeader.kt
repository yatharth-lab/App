package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkEmeraldActive
import com.example.ui.theme.StarkGoldTertiary
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkTealSecondary
import com.example.ui.theme.StarkTextPrimary
import com.example.ui.theme.StarkTextSecondary
import com.example.ui.theme.StarkWarningRed

@Composable
fun StatusTelemetryHeader(
    assistantState: AssistantState,
    isOnline: Boolean,
    providerName: String,
    isBackgroundActive: Boolean,
    onNavigateMemory: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateColor by animateColorAsState(
        targetValue = when (assistantState) {
            AssistantState.IDLE -> StarkCyanPrimary
            AssistantState.LISTENING -> StarkCyanPrimary
            AssistantState.THINKING -> StarkGoldTertiary
            AssistantState.SPEAKING -> StarkTealSecondary
            AssistantState.ERROR -> StarkWarningRed
            AssistantState.BACKGROUND_ACTIVE -> StarkEmeraldActive
        },
        label = "TelemetryStateColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(StarkDarkSurface)
                .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Glowing state dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )

            Text(
                text = when (assistantState) {
                    AssistantState.IDLE -> "STANDBY"
                    AssistantState.LISTENING -> "LISTENING"
                    AssistantState.THINKING -> "PROCESSING"
                    AssistantState.SPEAKING -> "TRANSMITTING"
                    AssistantState.ERROR -> "SYSTEM ALERT"
                    AssistantState.BACKGROUND_ACTIVE -> "SURVEILLANCE"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = stateColor,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 8.dp)
            )

            // Online / Offline icon
            Icon(
                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = if (isOnline) "Online" else "Offline",
                tint = if (isOnline) StarkCyanPrimary else StarkWarningRed,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(13.dp)
            )

            if (isBackgroundActive) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StarkEmeraldActive.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "BG",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarkEmeraldActive
                    )
                }
            }
        }

        // Action Buttons: Memory & Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateMemory,
                modifier = Modifier
                    .testTag("memory_nav_button")
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(StarkDarkSurface)
                    .border(1.dp, StarkSurfaceBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Memory Bank",
                    tint = StarkCyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onNavigateSettings,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("settings_nav_button")
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(StarkDarkSurface)
                    .border(1.dp, StarkSurfaceBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = StarkTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
