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
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.plabin.librespeedo.data.SettingsRepository
import com.plabin.librespeedo.ui.AboutScreen
import com.plabin.librespeedo.ui.LicenseScreen
import com.plabin.librespeedo.ui.SettingsScreen
import com.plabin.librespeedo.ui.SpeedometerScreen
import com.plabin.librespeedo.ui.SpeedometerViewModel
import com.plabin.librespeedo.ui.TripHistoryScreen
import com.plabin.librespeedo.trips.TripEndReason
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * The primary entry point for LibreSpeedo.
 * 
 * Handles the runtime permission requests required for location tracking and injects
 * the [SpeedometerViewModel] into the main Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SpeedometerViewModel by viewModels()
    private lateinit var settingsRepository: SettingsRepository
    private var pendingAutomaticLoggingEnable = false

    private val bluetoothPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.refreshPairedCars()
            if (pendingAutomaticLoggingEnable) settingsRepository.setAutomaticTripLogging(true)
        } else if (pendingAutomaticLoggingEnable) {
            settingsRepository.setAutomaticTripLogging(false)
        }
        pendingAutomaticLoggingEnable = false
    }

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
        
        settingsRepository = SettingsRepository(this)
        
        checkPermissionsAndStart()

        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val tripLogger by viewModel.tripLogger.collectAsState()
            val autoLogging by settingsRepository.automaticTripLogging.collectAsState()
            val selectedCarName by settingsRepository.selectedCarName.collectAsState()
            val tripRetentionDays by settingsRepository.tripRetentionDays.collectAsState()
            val storeTripRoute by settingsRepository.storeTripRoute.collectAsState()
            val stableMountConfirmed by settingsRepository.stableMountConfirmed.collectAsState()
            val activeWidgets by viewModel.activeWidgets.collectAsState()
            val isOledTheme by settingsRepository.isOledTheme.collectAsState()
            val isKeepScreenOn by settingsRepository.isKeepScreenOn.collectAsState()
            val isEditMode by settingsRepository.isEditMode.collectAsState()
            val speedUnit by settingsRepository.speedUnit.collectAsState()
            val analogStyle by settingsRepository.analogSpeedometerStyle.collectAsState()
            val digitalStyle by settingsRepository.digitalSpeedometerStyle.collectAsState()
            val widgetSpans by viewModel.widgetSpans.collectAsState()
            
            val navController = rememberNavController()

            LaunchedEffect(isKeepScreenOn) {
                if (isKeepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            
            LibreSpeedoTheme(isOledTheme = isOledTheme) {
                NavHost(navController = navController, startDestination = "speedometer") {
                    composable("speedometer") {
                        SpeedometerScreen(
                            uiState = uiState,
                            activeWidgets = activeWidgets,
                            isEditMode = isEditMode,
                            speedUnit = speedUnit,
                            widgetSpans = widgetSpans,
                            onReorderWidget = viewModel::reorderWidget,
                            onAddWidget = viewModel::addWidget,
                            onRemoveWidget = viewModel::removeWidget,
                            onWidgetSpanChange = viewModel::setWidgetSpan,
                            onSpeedUnitChange = { settingsRepository.setSpeedUnit(it) },
                            onSettingsClick = { navController.navigate("settings") },
                            modifier = Modifier.fillMaxSize(),
                            chronometerState = viewModel.chronometer,
                            analogStyle = analogStyle,
                            onAnalogStyleChange = settingsRepository::setAnalogSpeedometerStyle,
                            digitalStyle = digitalStyle,
                            onDigitalStyleChange = settingsRepository::setDigitalSpeedometerStyle,
                            tripLogger = tripLogger,
                            onTripStart = viewModel::startManualTrip,
                            onTripFinish = { viewModel.finishTrip() },
                            onTripHistory = { navController.navigate("trip-history") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            isOledTheme = isOledTheme,
                            isKeepScreenOn = isKeepScreenOn,
                            isEditMode = isEditMode,
                            onOledThemeChanged = { settingsRepository.setOledTheme(it) },
                            onKeepScreenOnChanged = { settingsRepository.setKeepScreenOn(it) },
                            onEditModeChanged = { settingsRepository.setEditMode(it) },
                            onResetLayout = viewModel::resetLayout,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAbout = { navController.navigate("about") },
                            automaticTripLogging = autoLogging,
                            selectedCarName = selectedCarName,
                            carConnection = tripLogger.carConnection,
                            bluetoothPermissionGranted = hasBluetoothConnectPermission(),
                            pairedCars = tripLogger.pairedCars,
                            onAutomaticTripLoggingChanged = { settingsRepository.setAutomaticTripLogging(it) },
                            onRequestBluetoothPermission = { enableLogging -> requestBluetoothConnectPermission(enableLogging) },
                            onRefreshPairedCars = { viewModel.refreshPairedCars() },
                            onSelectCar = { car ->
                                settingsRepository.setSelectedCar(car.address, car.name)
                                viewModel.refreshPairedCars()
                            },
                            tripRetentionDays = tripRetentionDays,
                            onTripRetentionDaysChange = settingsRepository::setTripRetentionDays,
                            storeTripRoute = storeTripRoute,
                            onStoreTripRouteChanged = settingsRepository::setStoreTripRoute,
                            stableMountConfirmed = stableMountConfirmed,
                            onStableMountConfirmedChanged = settingsRepository::setStableMountConfirmed
                        )
                    }
                    composable("trip-history") {
                        TripHistoryScreen(
                            trips = tripLogger.trips,
                            onDelete = viewModel::deleteTrip,
                            onBack = { navController.popBackStack() },
                            loadSamples = viewModel::loadTripSamples,
                            loadEvents = viewModel::loadDrivingEvents
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onViewLicense = { navController.navigate("license") }
                        )
                    }
                    composable("license") {
                        LicenseScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun requestBluetoothConnectPermission(enableLogging: Boolean) {
        pendingAutomaticLoggingEnable = enableLogging
        if (hasBluetoothConnectPermission()) {
            viewModel.refreshPairedCars()
            if (enableLogging) settingsRepository.setAutomaticTripLogging(true)
            pendingAutomaticLoggingEnable = false
        } else {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setAppVisible(true)
        if (hasLocationPermission()) viewModel.startTracking()
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) return
        viewModel.setAppVisible(false)
        viewModel.finishTrip(TripEndReason.BACKGROUND)
        viewModel.stopTracking()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

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
