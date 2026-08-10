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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onViewLicense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About LibreSpeedo") },
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
            Text(
                text = "LibreSpeedo",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Package: com.plabin.librespeedo")
            Text("Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            Text("Git Commit: ${BuildConfig.GIT_COMMIT}")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Developer",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Patricio Labin Correa (f1r3f0x)")
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Logo Design",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Tamar Labin Correa (tmlabin99@gmail.com)")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Links",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "GitHub Repository",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { uriHandler.openUri("https://github.com/F1r3f0x/LibreSpeedo") }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onViewLicense,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View GPLv3 License")
            }
        }
    }
}
