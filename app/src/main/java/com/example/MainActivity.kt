package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StarkDarkBackground
import com.example.viewmodel.StarkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val starkViewModel: StarkViewModel = viewModel()
            val darkMode = starkViewModel.preferences.darkModeEnabled

            MyApplicationTheme(darkTheme = darkMode) {
                StarkAppRoot(viewModel = starkViewModel)
            }
        }
    }
}

@Composable
fun StarkAppRoot(viewModel: StarkViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    // Runtime permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordGranted && viewModel.preferences.wakeWordEnabled) {
            // Permission granted, ready for voice interaction
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StarkDarkBackground),
        containerColor = StarkDarkBackground
    ) { innerPadding ->
        if (!uiState.isAppUnlocked) {
            LockScreen(viewModel = viewModel)
        } else {
            NavHost(
                navController = navController,
                startDestination = "assistant",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("assistant") {
                    AssistantScreen(
                        viewModel = viewModel,
                        onNavigateMemory = { navController.navigate("memory") },
                        onNavigateSettings = { navController.navigate("settings") },
                        onMicPermissionRequested = {
                            val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionsLauncher.launch(permissions.toTypedArray())
                        }
                    )
                }

                composable("memory") {
                    MemoryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

