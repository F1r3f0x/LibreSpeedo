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
package com.plabin.librespeedo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.plabin.librespeedo.data.AnalogSpeedometerStyle
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.ui.SpeedometerUiState

/** A sample gauge and accessible preset choices inside the widget settings dialog. */
@Composable
fun AnalogSpeedometerStylePicker(
    style: AnalogSpeedometerStyle,
    speedUnit: SpeedUnit,
    onStyleChange: (AnalogSpeedometerStyle) -> Unit
) {
    Text("Appearance", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(150.dp).clearAndSetSemantics {
            contentDescription = "Preview of ${style.label} style"
        }
    ) {
        AnalogSpeedometerComponent(
            uiState = SpeedometerUiState(speedKmh = 72f, isLocationFresh = true, isSpeedAvailable = true),
            speedUnit = speedUnit,
            style = style
        )
    }
    Text("Sample speed", style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
    AnalogSpeedometerStyle.entries.forEach { option ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(
                    if (option == style) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
                .selectable(selected = option == style, role = Role.RadioButton,
                    onClick = { onStyleChange(option) })
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = option == style, onClick = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                Text(option.description, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
