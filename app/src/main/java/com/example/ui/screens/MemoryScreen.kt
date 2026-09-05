package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.MemoryItem
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkDarkBackground
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkSurfaceVariant
import com.example.ui.theme.StarkTealSecondary
import com.example.ui.theme.StarkTextDim
import com.example.ui.theme.StarkTextPrimary
import com.example.ui.theme.StarkTextSecondary
import com.example.ui.theme.StarkWarningRed
import com.example.viewmodel.StarkViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: StarkViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("general") }

    val filteredMemories = uiState.memories.filter {
        it.key.contains(searchQuery, ignoreCase = true) ||
                it.value.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    if (showClearConfirm) {
        ActionConfirmationDialog(
            title = "Purge Memory Banks?",
            message = "This will permanently wipe all stored facts, preferences, and recalled data from Mr. Stark's local neural store.",
            confirmButtonText = "PURGE ALL",
            onConfirm = {
                viewModel.clearMemories()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    Scaffold(
        containerColor = StarkDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "NEURAL MEMORY BANKS",
                            fontSize = 16.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = StarkCyanPrimary
                        )
                        Text(
                            text = "${uiState.memories.size} records securely stored offline",
                            fontSize = 11.sp,
                            color = StarkTextDim
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("memory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StarkTextPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.memories.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("clear_memories_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Clear All Memories",
                                tint = StarkWarningRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarkDarkSurface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter memory records...", color = StarkTextDim) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = StarkCyanPrimary
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = StarkTextPrimary,
                    unfocusedTextColor = StarkTextPrimary,
                    focusedBorderColor = StarkCyanPrimary,
                    unfocusedBorderColor = StarkSurfaceBorder,
                    focusedContainerColor = StarkDarkSurface,
                    unfocusedContainerColor = StarkDarkSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memory_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Add Memory Toggle Card
            if (!showAddDialog) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StarkDarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StarkCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("add_memory_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = StarkCyanPrimary)
                    Text(
                        text = "ADD NEW RECORD",
                        color = StarkCyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                // Add memory card form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StarkDarkSurface)
                        .border(1.dp, StarkCyanPrimary, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "NEW MEMORY ENTRY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarkCyanPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        placeholder = { Text("Topic / Title (e.g. Car Parking)", color = StarkTextDim) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StarkTextPrimary,
                            unfocusedTextColor = StarkTextPrimary,
                            focusedBorderColor = StarkCyanPrimary,
                            unfocusedBorderColor = StarkSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        placeholder = { Text("Fact details (e.g. 3rd floor slot B12)", color = StarkTextDim) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StarkTextPrimary,
                            unfocusedTextColor = StarkTextPrimary,
                            focusedBorderColor = StarkCyanPrimary,
                            unfocusedBorderColor = StarkSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = StarkSurfaceVariant)
                        ) {
                            Text("Cancel", color = StarkTextSecondary)
                        }
                        Button(
                            onClick = {
                                if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                    viewModel.saveMemory(newKey.trim(), newValue.trim(), newCategory)
                                    newKey = ""
                                    newValue = ""
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StarkCyanPrimary),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Save", color = StarkDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Memory List
            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No records matching query." else "Memory banks clear. Say \"Mr. Stark, remember that...\" to record facts.",
                        color = StarkTextDim,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            item = memory,
                            onDelete = { viewModel.deleteMemory(memory.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryItemCard(
    item: MemoryItem,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(item.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StarkDarkSurface)
            .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.key,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarkCyanPrimary
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(StarkTealSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StarkTealSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.value,
                    fontSize = 13.sp,
                    color = StarkTextPrimary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = StarkTextDim
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = StarkWarningRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
