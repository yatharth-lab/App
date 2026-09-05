package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkTextDim
import com.example.ui.theme.StarkTextPrimary
import com.example.ui.theme.StarkWarningRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "CONFIRM",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .testTag("action_confirmation_dialog")
            .clip(RoundedCornerShape(16.dp))
            .background(StarkDarkSurface)
            .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .background(StarkDarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StarkTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = StarkTextDim,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dialog_cancel_button")
                ) {
                    Text("CANCEL", color = StarkTextPrimary)
                }

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = StarkWarningRed),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .testTag("dialog_confirm_button")
                ) {
                    Text(confirmButtonText, color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
