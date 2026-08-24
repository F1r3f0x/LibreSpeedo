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

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.R
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme
import com.plabin.librespeedo.utils.SpeedConverter

fun getDefaultWidgetSpan(widget: WidgetType): WidgetSpan = when (widget) {
    WidgetType.SPEEDOMETER -> WidgetSpan.FULL_WIDTH
    WidgetType.COMPASS -> WidgetSpan.HALF
    WidgetType.POSITION -> WidgetSpan.HALF
    WidgetType.DEBUG -> WidgetSpan.LARGE
    WidgetType.HELLO_WORLD -> WidgetSpan.HALF
}

@Composable
fun SpeedometerScreen(
    uiState: SpeedometerUiState,
    activeWidgets: List<WidgetType>,
    isEditMode: Boolean,
    speedUnit: SpeedUnit,
    widgetSpans: Map<String, WidgetSpan>,
    onReorderWidget: (Int, Int) -> Unit,
    onAddWidget: (WidgetType) -> Unit,
    onRemoveWidget: (WidgetType) -> Unit,
    onWidgetSpanChange: (WidgetType, WidgetSpan) -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMenu by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (isEditMode) {
                Box {
                    FloatingActionButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Component")
                    }
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        WidgetType.entries.forEach { widget ->
                            if (!activeWidgets.contains(widget)) {
                                DropdownMenuItem(
                                    text = { Text(widget.name) },
                                    onClick = {
                                        onAddWidget(widget)
                                        showAddMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gato_1),
                        contentDescription = "LibreSpeedo Logo",
                        modifier = Modifier.size(if (isLandscape) 28.dp else 36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LibreSpeedo",
                        fontSize = if (isLandscape) 20.sp else 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 24.dp))

            ReorderableWidgetGrid(
                widgets = activeWidgets,
                isEditMode = isEditMode,
                speedUnit = speedUnit,
                widgetSpans = widgetSpans,
                onReorder = onReorderWidget,
                uiState = uiState,
                onRemove = onRemoveWidget,
                onSpeedUnitChange = onSpeedUnitChange,
                onWidgetSpanChange = onWidgetSpanChange
            )
        }
    }
}

@Composable
fun ReorderableWidgetGrid(
    widgets: List<WidgetType>,
    isEditMode: Boolean,
    speedUnit: SpeedUnit,
    widgetSpans: Map<String, WidgetSpan>,
    onReorder: (Int, Int) -> Unit,
    uiState: SpeedometerUiState,
    onRemove: (WidgetType) -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onWidgetSpanChange: (WidgetType, WidgetSpan) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetX by remember { mutableStateOf(0f) }
    var draggedOffsetY by remember { mutableStateOf(0f) }
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                                offset.x.toInt() in it.offset.x..(it.offset.x + it.size.width) &&
                                offset.y.toInt() in it.offset.y..(it.offset.y + it.size.height)
                            }
                            if (item != null) {
                                draggedIndex = item.index
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggedOffsetX += dragAmount.x
                            draggedOffsetY += dragAmount.y

                            val draggedIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                            val currentItemInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggedIdx }
                            if (currentItemInfo != null) {
                                val centerX = currentItemInfo.offset.x + (currentItemInfo.size.width / 2) + draggedOffsetX
                                val centerY = currentItemInfo.offset.y + (currentItemInfo.size.height / 2) + draggedOffsetY

                                val targetItem = gridState.layoutInfo.visibleItemsInfo.find {
                                    it.index != draggedIdx &&
                                    centerX > it.offset.x && centerX < (it.offset.x + it.size.width) &&
                                    centerY > it.offset.y && centerY < (it.offset.y + it.size.height)
                                }

                                if (targetItem != null) {
                                    onReorder(draggedIdx, targetItem.index)
                                    draggedIndex = targetItem.index
                                    draggedOffsetX = 0f
                                    draggedOffsetY = 0f
                                }
                            }
                        },
                        onDragEnd = {
                            draggedIndex = null
                            draggedOffsetX = 0f
                            draggedOffsetY = 0f
                        },
                        onDragCancel = {
                            draggedIndex = null
                            draggedOffsetX = 0f
                            draggedOffsetY = 0f
                        }
                    )
                }
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(
            items = widgets,
            key = { _, w -> w.name },
            span = { _, widget ->
                val span = widgetSpans[widget.name] ?: getDefaultWidgetSpan(widget)
                GridItemSpan(span.colSpan.coerceAtMost(maxLineSpan))
            }
        ) { index, widget ->
            val isBeingDragged = index == draggedIndex
            val elevation = if (isBeingDragged) 8.dp else 0.dp
            val alpha = if (isBeingDragged) 0.5f else 1f
            val span = widgetSpans[widget.name] ?: getDefaultWidgetSpan(widget)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(span.heightDp.dp)
                    .alpha(alpha)
                    .graphicsLayer {
                        if (isBeingDragged) {
                            translationX = draggedOffsetX
                            translationY = draggedOffsetY
                        }
                    }
            ) {
                WidgetContainer(
                    widget = widget,
                    span = span,
                    uiState = uiState,
                    isEditMode = isEditMode,
                    speedUnit = speedUnit,
                    onRemove = { onRemove(widget) },
                    onToggleSpan = { onWidgetSpanChange(widget, span.next()) },
                    onSpeedUnitChange = onSpeedUnitChange,
                    elevation = elevation
                )
            }
        }
    }
}

@Composable
fun WidgetContainer(
    widget: WidgetType,
    span: WidgetSpan,
    uiState: SpeedometerUiState,
    isEditMode: Boolean,
    speedUnit: SpeedUnit,
    onRemove: () -> Unit,
    onToggleSpan: () -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    elevation: Dp
) {
    var showSpeedSettings by remember { mutableStateOf(false) }

    if (showSpeedSettings && widget == WidgetType.SPEEDOMETER) {
        AlertDialog(
            onDismissRequest = { showSpeedSettings = false },
            title = { Text("Speedometer Settings") },
            text = {
                Column {
                    Text("Select Unit:")
                    Spacer(modifier = Modifier.height(8.dp))
                    SpeedUnit.entries.forEach { unit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = speedUnit == unit,
                                onClick = { onSpeedUnitChange(unit) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = unit.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedSettings = false }) {
                    Text("Done")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isEditMode) {
                Surface(
                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = span.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleSpan) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Resize Widget (${span.label})"
                        )
                    }
                    if (widget == WidgetType.SPEEDOMETER) {
                        IconButton(onClick = { showSpeedSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Widget Settings"
                            )
                        }
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Widget"
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isEditMode) PaddingValues(top = 28.dp, start = 12.dp, end = 12.dp, bottom = 12.dp) else PaddingValues(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (widget) {
                    WidgetType.SPEEDOMETER -> SpeedComponent(uiState, speedUnit, span)
                    WidgetType.COMPASS -> CompassComponent(uiState.heading, span)
                    WidgetType.POSITION -> PositionComponent(uiState, span)
                    WidgetType.DEBUG -> DebugComponent(uiState, span)
                    WidgetType.HELLO_WORLD -> HelloWorldComponent()
                }
            }
        }
    }
}

@Composable
fun SpeedComponent(uiState: SpeedometerUiState, speedUnit: SpeedUnit, span: WidgetSpan = WidgetSpan.FULL_WIDTH) {
    val speedRawMs = uiState.speedKmh / 3.6f

    val displaySpeed = when (speedUnit) {
        SpeedUnit.KMH -> uiState.speedKmh
        SpeedUnit.MPH -> SpeedConverter.msToMph(speedRawMs)
        SpeedUnit.MS -> speedRawMs
    }

    val displayUnitStr = when (speedUnit) {
        SpeedUnit.KMH -> "km/h"
        SpeedUnit.MPH -> "mph"
        SpeedUnit.MS -> "m/s"
    }

    val speedFontSize = when (span) {
        WidgetSpan.HALF -> 44.sp
        WidgetSpan.FULL_WIDTH -> 72.sp
        WidgetSpan.LARGE -> 84.sp
    }

    val unitFontSize = when (span) {
        WidgetSpan.HALF -> 16.sp
        WidgetSpan.FULL_WIDTH -> 22.sp
        WidgetSpan.LARGE -> 26.sp
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = String.format(LocalLocale.current.platformLocale, "%.1f", displaySpeed),
            fontSize = speedFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Text(
            text = displayUnitStr,
            fontSize = unitFontSize,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun PositionComponent(uiState: SpeedometerUiState, span: WidgetSpan = WidgetSpan.HALF) {
    val titleSize = if (span == WidgetSpan.HALF) 13.sp else 16.sp
    val coordSize = if (span == WidgetSpan.HALF) 12.sp else 15.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = "Current Position",
            fontWeight = FontWeight.SemiBold,
            fontSize = titleSize,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(LocalLocale.current.platformLocale, "Lat: %.5f", uiState.latitude),
            fontSize = coordSize,
            maxLines = 1
        )
        Text(
            text = String.format(LocalLocale.current.platformLocale, "Lng: %.5f", uiState.longitude),
            fontSize = coordSize,
            maxLines = 1
        )
    }
}

@Composable
fun CompassComponent(heading: Float, span: WidgetSpan = WidgetSpan.HALF) {
    val dialSize = when (span) {
        WidgetSpan.HALF -> 95.dp
        WidgetSpan.FULL_WIDTH -> 115.dp
        WidgetSpan.LARGE -> 140.dp
    }
    val arrowSize = when (span) {
        WidgetSpan.HALF -> 36.sp
        WidgetSpan.FULL_WIDTH -> 44.sp
        WidgetSpan.LARGE -> 54.sp
    }

    Box(
        modifier = Modifier
            .size(dialSize)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "↑",
            fontSize = arrowSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(-heading)
        )
        Text(
            text = "N",
            modifier = Modifier.align(Alignment.TopCenter).padding(4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = if (span == WidgetSpan.HALF) 11.sp else 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DebugComponent(uiState: SpeedometerUiState, span: WidgetSpan = WidgetSpan.LARGE) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (span != WidgetSpan.LARGE) Modifier.verticalScroll(rememberScrollState()) else Modifier)
    ) {
        Text(
            text = "Debug Info",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = if (span == WidgetSpan.HALF) 12.sp else 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (uiState.error != null) {
            Text(text = "Error: ${uiState.error}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        DebugRow("Provider", uiState.provider, span)
        DebugRow("Raw Speed", "${uiState.speedKmh} km/h", span)
        DebugRow("Coordinates", "${uiState.latitude}, ${uiState.longitude}", span)
        DebugRow("Heading", "${uiState.heading}°", span)
        DebugRow("Altitude", "${uiState.altitude} m", span)
        DebugRow("Accuracy", "±${uiState.accuracy} m", span)
        DebugRow("Accelerometer", String.format(LocalLocale.current.platformLocale, "%.2f, %.2f, %.2f", uiState.accelX, uiState.accelY, uiState.accelZ), span)
        DebugRow("Gyroscope", String.format(LocalLocale.current.platformLocale, "%.2f, %.2f, %.2f", uiState.gyroX, uiState.gyroY, uiState.gyroZ), span)
        DebugRow("Magnetic Field", String.format(LocalLocale.current.platformLocale, "%.2f, %.2f, %.2f", uiState.magX, uiState.magY, uiState.magZ), span)
    }
}

@Composable
fun DebugRow(label: String, value: String, span: WidgetSpan = WidgetSpan.LARGE) {
    val fontSize = if (span == WidgetSpan.HALF) 10.sp else 12.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = fontSize, maxLines = 1)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = fontSize, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun HelloWorldComponent() {
    Text(
        text = "Hello World!",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview(showBackground = true)
@Composable
fun SpeedometerScreenPreview() {
    LibreSpeedoTheme {
        SpeedometerScreen(
            uiState = SpeedometerUiState(
                speedKmh = 42.5f,
                latitude = 40.7128,
                longitude = -74.0060,
                heading = 45f,
                altitude = 10.5,
                accuracy = 3.2,
                provider = "Preview",
                accelX = 0f,
                accelY = 0f,
                accelZ = 9.8f,
                gyroX = 0f,
                gyroY = 0f,
                gyroZ = 0f,
                magX = 0f,
                magY = 0f,
                magZ = 0f
            ),
            activeWidgets = listOf(WidgetType.SPEEDOMETER, WidgetType.HELLO_WORLD, WidgetType.COMPASS, WidgetType.DEBUG),
            isEditMode = true,
            speedUnit = SpeedUnit.KMH,
            widgetSpans = mapOf(),
            onReorderWidget = { _, _ -> },
            onAddWidget = {},
            onRemoveWidget = {},
            onSpeedUnitChange = {},
            onWidgetSpanChange = { _, _ -> },
            onSettingsClick = {}
        )
    }
}
