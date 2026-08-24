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
package com.plabin.librespeedo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A screen for user preferences, such as the OLED theme, Screen Lock, and Dashboard Edit toggles.
 *
 * @param isOledTheme Current state of the OLED theme toggle.
 * @param isKeepScreenOn Current state of the screen wake lock toggle.
 * @param isEditMode Current state of the dashboard edit mode toggle.
 * @param onOledThemeChanged Callback when the OLED theme switch is toggled.
 * @param onKeepScreenOnChanged Callback when the Keep Screen On switch is toggled.
 * @param onEditModeChanged Callback when the Edit Dashboard switch is toggled.
 * @param onResetLayout Callback when confirming a dashboard layout reset.
 * @param onNavigateBack Callback when the top app bar back arrow is pressed.
 * @param onNavigateToAbout Callback when the About row is clicked.
 * @param modifier Optional [Modifier] for screen root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isOledTheme: Boolean,
    isKeepScreenOn: Boolean,
    onOledThemeChanged: (Boolean) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onEditModeChanged: (Boolean) -> Unit = {},
    onResetLayout: () -> Unit = {}
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Layout") },
            text = { Text("Are you sure you want to reset your dashboard layout? All custom widgets and size configurations will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetLayout()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            SettingRow(
                title = "OLED Theme",
                description = "Use pure black backgrounds for maximum contrast and battery saving on OLED displays.",
                isChecked = isOledTheme,
                onCheckedChange = onOledThemeChanged
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SettingRow(
                title = "Keep Screen On",
                description = "Prevent the screen from turning off automatically while the app is open.",
                isChecked = isKeepScreenOn,
                onCheckedChange = onKeepScreenOnChanged
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SettingRow(
                title = "Edit Dashboard",
                description = "Enable Edit Mode to rearrange, add, or remove widgets on the main screen.",
                isChecked = isEditMode,
                onCheckedChange = onEditModeChanged
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Dashboard Layout")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAbout() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "About LibreSpeedo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * Reusable settings row displaying a title, multi-line description, and a togglable [Switch].
 *
 * @param title Primary heading text for the preference.
 * @param description Detailed secondary explanation text.
 * @param isChecked Current state of the switch toggle.
 * @param onCheckedChange Callback when the switch is clicked.
 * @param modifier Optional [Modifier] for this settings row.
 */
@Composable
fun SettingRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
