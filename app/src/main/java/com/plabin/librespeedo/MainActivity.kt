/*
 * Copyright (C) 2026 Patricio Labin Correa (f1r3f0x)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plabin.librespeedo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.plabin.librespeedo.data.SettingsRepository
import com.plabin.librespeedo.ui.SettingsScreen
import com.plabin.librespeedo.ui.SpeedometerScreen
import com.plabin.librespeedo.ui.SpeedometerViewModel
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme

/**
 * The primary entry point for LibreSpeedo.
 * 
 * Handles the runtime permission requests required for location tracking and injects
 * the [SpeedometerViewModel] into the main Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SpeedometerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.startTracking()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val settingsRepository = SettingsRepository(this)
        
        checkPermissionsAndStart()

        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val isOledTheme by settingsRepository.isOledTheme.collectAsState()
            val isKeepScreenOn by settingsRepository.isKeepScreenOn.collectAsState()
            
            var showSettings by remember { mutableStateOf(false) }

            LaunchedEffect(isKeepScreenOn) {
                if (isKeepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            
            LibreSpeedoTheme(isOledTheme = isOledTheme) {
                if (showSettings) {
                    SettingsScreen(
                        isOledTheme = isOledTheme,
                        isKeepScreenOn = isKeepScreenOn,
                        onOledThemeChanged = { settingsRepository.setOledTheme(it) },
                        onKeepScreenOnChanged = { settingsRepository.setKeepScreenOn(it) },
                        onNavigateBack = { showSettings = false }
                    )
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        SpeedometerScreen(
                            uiState = uiState,
                            onSettingsClick = { showSettings = true },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks if the required location permissions have been granted.
     * If they are, it immediately instructs the ViewModel to start tracking.
     * Otherwise, it prompts the user using the [requestPermissionLauncher].
     */
    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startTracking()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}