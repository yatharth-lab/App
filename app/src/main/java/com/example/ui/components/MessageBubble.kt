package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actions.DeviceController
import com.example.data.model.ConversationMessage
import com.example.data.model.MessageSender
import com.example.ui.theme.StarkCyanContainer
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkEmeraldActive
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkSurfaceVariant
import com.example.ui.theme.StarkTealSecondary
import com.example.ui.theme.StarkTextDim
import com.example.ui.theme.StarkTextPrimary
import com.example.ui.theme.StarkTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ConversationMessage,
    modifier: Modifier = Modifier
) {
    val isStark = message.sender == MessageSender.STARK
    val isSystem = message.sender == MessageSender.SYSTEM
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isStark || isSystem) Alignment.Start else Alignment.End
    ) {
        // Sender label
        Text(
            text = when {
                isStark -> "MR. STARK"
                isSystem -> "SYSTEM PROTOCOL"
                else -> "YOU"
            },
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = if (isStark) StarkCyanPrimary else if (isSystem) StarkTealSecondary else StarkTextDim,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        // Bubble card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isStark) 2.dp else 14.dp,
                        bottomEnd = if (isStark) 14.dp else 2.dp
                    )
                )
                .background(if (isStark) StarkDarkSurface else StarkSurfaceVariant)
                .border(
                    width = 1.dp,
                    color = if (isStark) StarkSurfaceBorder else StarkSurfaceBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = StarkTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Action badge chip if action was executed
                if (!message.actionDetails.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StarkCyanContainer.copy(alpha = 0.35f))
                            .border(1.dp, StarkCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = message.actionDetails,
                            color = StarkCyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bottom row: Time + Copy / Share actions for Stark's responses
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = StarkTextDim
                    )

                    if (isStark) {
                        Row {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                },
                                modifier = Modifier
                                    .testTag("copy_message_button")
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy message",
                                    tint = StarkTextDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    DeviceController(context).shareText(message.text)
                                },
                                modifier = Modifier
                                    .testTag("share_message_button")
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share message",
                                    tint = StarkTextDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
