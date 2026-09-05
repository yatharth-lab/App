package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkDarkBackground
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkTextDim
import com.example.ui.theme.StarkTextPrimary
import com.example.ui.theme.StarkWarningRed
import com.example.viewmodel.StarkViewModel

@Composable
fun LockScreen(
    viewModel: StarkViewModel,
    modifier: Modifier = Modifier
) {
    var enteredDigits by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StarkDarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock Icon
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(StarkDarkSurface)
                .border(1.dp, StarkCyanPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Security Lock",
                tint = StarkCyanPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SECURITY PROTOCOL ACTIVE",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = StarkCyanPrimary,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter authorization code for Mr. Stark",
            fontSize = 12.sp,
            color = StarkTextDim
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            for (i in 0 until 4) {
                val filled = i < enteredDigits.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) StarkCyanPrimary else StarkDarkSurface)
                        .border(1.dp, StarkCyanPrimary, CircleShape)
                )
            }
        }

        if (showError) {
            Text(
                text = "Authorization Denied. Invalid PIN.",
                color = StarkWarningRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Keypad Grid
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("CLEAR", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in buttons) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (btn in row) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(StarkDarkSurface)
                                .border(1.dp, StarkSurfaceBorder, CircleShape)
                                .clickable {
                                    showError = false
                                    when (btn) {
                                        "CLEAR" -> enteredDigits = ""
                                        "DEL" -> if (enteredDigits.isNotEmpty()) enteredDigits = enteredDigits.dropLast(1)
                                        else -> {
                                            if (enteredDigits.length < 6) {
                                                val next = enteredDigits + btn
                                                enteredDigits = next
                                                if (next.length >= 4) {
                                                    val success = viewModel.verifyPin(next)
                                                    if (!success && next.length == (viewModel.preferences.appLockPin?.length ?: 4)) {
                                                        showError = true
                                                        enteredDigits = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("pin_key_$btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (btn == "DEL") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete",
                                    tint = StarkTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = btn,
                                    fontSize = if (btn == "CLEAR") 11.sp else 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (btn == "CLEAR") StarkWarningRed else StarkTextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
