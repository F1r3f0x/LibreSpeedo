package com.plabin.librespeedo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.R
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme
import com.plabin.librespeedo.utils.SpeedConverter
import kotlin.math.roundToInt

@Composable
fun SpeedometerScreen(
    uiState: SpeedometerUiState,
    activeWidgets: List<WidgetType>,
    isEditMode: Boolean,
    speedUnit: SpeedUnit,
    widgetHeights: Map<String, Int>,
    onReorderWidget: (Int, Int) -> Unit,
    onAddWidget: (WidgetType) -> Unit,
    onRemoveWidget: (WidgetType) -> Unit,
    onWidgetHeightChange: (WidgetType, Int) -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMenu by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gato_1),
                        contentDescription = "LibreSpeedo Logo",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LibreSpeedo",
                        fontSize = 24.sp,
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
            Spacer(modifier = Modifier.height(32.dp))

            ReorderableWidgetList(
                widgets = activeWidgets,
                isEditMode = isEditMode,
                speedUnit = speedUnit,
                widgetHeights = widgetHeights,
                onReorder = onReorderWidget,
                uiState = uiState,
                onRemove = onRemoveWidget,
                onSpeedUnitChange = onSpeedUnitChange,
                onWidgetHeightChange = onWidgetHeightChange
            )
        }
    }
}

@Composable
fun ReorderableWidgetList(
    widgets: List<WidgetType>,
    isEditMode: Boolean,
    speedUnit: SpeedUnit,
    widgetHeights: Map<String, Int>,
    onReorder: (Int, Int) -> Unit,
    uiState: SpeedometerUiState,
    onRemove: (WidgetType) -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onWidgetHeightChange: (WidgetType, Int) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetX by remember { mutableStateOf(0f) }
    var draggedOffsetY by remember { mutableStateOf(0f) }
    val gridState = rememberLazyStaggeredGridState()
    val density = LocalDensity.current

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
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
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(widgets, key = { _, w -> w.name }) { index, widget ->
            val isBeingDragged = index == draggedIndex
            val elevation = if (isBeingDragged) 8.dp else 0.dp
            val alpha = if (isBeingDragged) 0.5f else 1f
            
            var dragHeightOffset by remember { mutableStateOf(0f) }
            val baseHeight = widgetHeights[widget.name] ?: 200
            val currentHeightDp = (baseHeight.dp + with(density) { dragHeightOffset.toDp() }).coerceAtLeast(100.dp)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentHeightDp)
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
                    uiState = uiState,
                    isEditMode = isEditMode,
                    speedUnit = speedUnit,
                    onRemove = { onRemove(widget) },
                    onSpeedUnitChange = onSpeedUnitChange,
                    elevation = elevation
                )
                
                if (isEditMode && !isBeingDragged) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragHeightOffset += dragAmount.y
                                    },
                                    onDragEnd = {
                                        val finalHeight = (baseHeight.dp + with(density) { dragHeightOffset.toDp() }).coerceAtLeast(100.dp)
                                        onWidgetHeightChange(widget, finalHeight.value.roundToInt())
                                        dragHeightOffset = 0f
                                    },
                                    onDragCancel = { dragHeightOffset = 0f }
                                )
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetContainer(
    widget: WidgetType,
    uiState: SpeedometerUiState,
    isEditMode: Boolean,
    speedUnit: SpeedUnit,
    onRemove: () -> Unit,
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
                Row(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Widget")
                    }
                    if (widget == WidgetType.SPEEDOMETER) {
                        IconButton(onClick = { showSpeedSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Widget Settings")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (widget) {
                    WidgetType.SPEEDOMETER -> SpeedComponent(uiState, speedUnit)
                    WidgetType.COMPASS -> CompassComponent(uiState.heading)
                    WidgetType.POSITION -> PositionComponent(uiState)
                    WidgetType.DEBUG -> DebugComponent(uiState)
                    WidgetType.HELLO_WORLD -> HelloWorldComponent()
                }
            }
        }
    }
}

@Composable
fun SpeedComponent(uiState: SpeedometerUiState, speedUnit: SpeedUnit) {
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

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = String.format(LocalLocale.current.platformLocale, "%.1f", displaySpeed),
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = displayUnitStr,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun PositionComponent(uiState: SpeedometerUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = "Current Position",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        Text(
            text = String.format(LocalLocale.current.platformLocale, "Lat: %.5f", uiState.latitude),
            fontSize = 16.sp
        )
        Text(
            text = String.format(LocalLocale.current.platformLocale, "Lng: %.5f", uiState.longitude),
            fontSize = 16.sp
        )
    }
}

@Composable
fun CompassComponent(heading: Float) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "↑",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(-heading)
        )
        Text(
            text = "N",
            modifier = Modifier.align(Alignment.TopCenter).padding(4.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DebugComponent(uiState: SpeedometerUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Debug Info", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.error != null) {
            Text(text = "Error: ${uiState.error}", color = Color.Red, fontWeight = FontWeight.Bold)
        }
        DebugRow("Provider", uiState.provider)
        DebugRow("Raw Speed", "${uiState.speedKmh} km/h")
        DebugRow("Coordinates", "${uiState.latitude}, ${uiState.longitude}")
        DebugRow("Heading", "${uiState.heading}°")
        DebugRow("Altitude", "${uiState.altitude} m")
        DebugRow("Accuracy", "±${uiState.accuracy} m")
        DebugRow("Accelerometer", String.format(LocalLocale.current.platformLocale, "%.2f, %.2f, %.2f", uiState.accelX, uiState.accelY, uiState.accelZ))
        DebugRow("Gyroscope", String.format(LocalLocale.current.platformLocale, "%.2f, %.2f, %.2f", uiState.gyroX, uiState.gyroY, uiState.gyroZ))
        DebugRow("Magnetic Field", String.format(LocalLocale.current.platformLocale, "%.2f, %.2f, %.2f", uiState.magX, uiState.magY, uiState.magZ))
    }
}

@Composable
fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
            widgetHeights = mapOf(),
            onReorderWidget = { _, _ -> },
            onAddWidget = {},
            onRemoveWidget = {},
            onSpeedUnitChange = {},
            onWidgetHeightChange = { _, _ -> },
            onSettingsClick = {}
        )
    }
}
