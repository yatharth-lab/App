package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkTextPrimary

@Composable
fun QuickActionRow(
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        "yt kholo aur movie chalo random",
        "yt per search karo Jarvis assistant",
        "Generate image of futuristic iron man",
        "Write a paragraph on Artificial Intelligence",
        "yaha search karo",
        "Read screen",
        "Torch on karo",
        "Battery kitni hai?"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickActions.forEachIndexed { index, action ->
            Box(
                modifier = Modifier
                    .testTag("quick_action_chip_$index")
                    .clip(RoundedCornerShape(16.dp))
                    .background(StarkDarkSurface)
                    .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(16.dp))
                    .clickable { onActionSelected(action) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = action,
                    color = StarkTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
