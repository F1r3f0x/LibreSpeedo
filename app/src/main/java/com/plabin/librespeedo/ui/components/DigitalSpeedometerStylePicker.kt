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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.plabin.librespeedo.data.DigitalSpeedometerStyle
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.ui.SpeedometerUiState

@Composable
internal fun DigitalSpeedometerStylePicker(
    style: DigitalSpeedometerStyle,
    speedUnit: SpeedUnit,
    onStyleChange: (DigitalSpeedometerStyle) -> Unit
) {
    Text("Appearance", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(150.dp).clearAndSetSemantics {
            contentDescription = "Preview of ${style.label} style"
        }) {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            SpeedComponent(SpeedometerUiState(speedKmh = 128.4f, isLocationFresh = true,
                isSpeedAvailable = true), speedUnit, style = style)
        }
    }
    Spacer(Modifier.height(8.dp))
    DigitalSpeedometerStyle.entries.forEach { option ->
        Row(Modifier.fillMaxWidth().selectable(selected = option == style, role = Role.RadioButton,
            onClick = { onStyleChange(option) }).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = option == style, onClick = null)
            Spacer(Modifier.width(12.dp))
            Text(option.label)
        }
    }
}
