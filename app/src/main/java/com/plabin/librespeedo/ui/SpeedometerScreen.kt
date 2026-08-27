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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.R
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.components.WidgetContainer
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme

/**
 * Returns the default [WidgetSpan] size configuration for a given [WidgetType].
 *
 * @param widget The [WidgetType] to query.
 * @return The recommended default [WidgetSpan].
 */
fun getDefaultWidgetSpan(widget: WidgetType): WidgetSpan = when (widget) {
    WidgetType.SPEEDOMETER -> WidgetSpan.FULL_WIDTH
    WidgetType.COMPASS -> WidgetSpan.HALF
    WidgetType.ANALOG_COMPASS -> WidgetSpan.FULL_WIDTH
    WidgetType.CHRONOMETER -> WidgetSpan.FULL_WIDTH
    WidgetType.POSITION -> WidgetSpan.HALF
    WidgetType.DEBUG -> WidgetSpan.LARGE
    WidgetType.HELLO_WORLD -> WidgetSpan.HALF
}

/**
 * The primary dashboard screen of LibreSpeedo.
 *
 * Displays the modular grid of active widgets, handles floating action buttons for adding
 * widgets during edit mode, and adapts the UI layout based on device orientation.
 *
 * @param uiState Current sensor metrics, GPS coordinates, speed, and tracking status.
 * @param activeWidgets Ordered list of widgets to render on the dashboard.
 * @param isEditMode True if editing (reordering, resizing, adding/removing) is unlocked.
 * @param speedUnit The active unit of measurement for speed (KMH, MPH, MS).
 * @param widgetSpans Mapping of widget identifiers to their current [WidgetSpan] grid sizes.
 * @param onReorderWidget Callback when a widget is dragged and moved from one position to another.
 * @param onAddWidget Callback when a new widget is selected from the add menu.
 * @param onRemoveWidget Callback when a widget's remove button is pressed.
 * @param onWidgetSpanChange Callback when a widget's span size is cycled or changed.
 * @param onSpeedUnitChange Callback when the speed unit is changed from widget settings.
 * @param onSettingsClick Callback to navigate to the settings screen.
 * @param modifier Optional [Modifier] for screen root layout.
 */
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

/**
 * A responsive, reorderable grid component that renders dashboard widgets.
 *
 * Implements long-press drag-and-drop gesture detection across multi-column adaptive grid cells.
 *
 * @param widgets Ordered list of [WidgetType] items to display.
 * @param isEditMode True if drag-and-drop and size modification controls are active.
 * @param speedUnit Active speed unit preference.
 * @param widgetSpans Custom span configurations for each widget.
 * @param onReorder Callback when dragging an item reorders it in the active list.
 * @param uiState Current sensor and location metrics.
 * @param onRemove Callback when dismissing a widget.
 * @param onSpeedUnitChange Callback when updating the speed measurement unit.
 * @param onWidgetSpanChange Callback when cycling a widget's span size.
 */
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

/**
 * Preview composable for [SpeedometerScreen] in Jetpack Compose UI tooling.
 */
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
                provider = "Preview"
            ),
            activeWidgets = listOf(
                WidgetType.SPEEDOMETER,
                WidgetType.ANALOG_COMPASS,
                WidgetType.CHRONOMETER,
                WidgetType.POSITION
            ),
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
