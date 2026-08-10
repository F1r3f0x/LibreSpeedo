package com.plabin.librespeedo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.R
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme

@Composable
fun SpeedometerScreen(
    uiState: SpeedometerUiState,
    activeWidgets: List<WidgetType>,
    onReorderWidget: (Int, Int) -> Unit,
    onAddWidget: (WidgetType) -> Unit,
    onRemoveWidget: (WidgetType) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
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
                onReorder = onReorderWidget,
                uiState = uiState,
                onRemove = onRemoveWidget
            )
        }
    }
}

@Composable
fun ReorderableWidgetList(
    widgets: List<WidgetType>,
    onReorder: (Int, Int) -> Unit,
    uiState: SpeedometerUiState,
    onRemove: (WidgetType) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetX by remember { mutableStateOf(0f) }
    var draggedOffsetY by remember { mutableStateOf(0f) }
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
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
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(widgets, key = { _, w -> w.name }) { index, widget ->
            val isBeingDragged = index == draggedIndex
            val elevation = if (isBeingDragged) 8.dp else 0.dp
            val alpha = if (isBeingDragged) 0.5f else 1f
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    onRemove = { onRemove(widget) },
                    elevation = elevation
                )
            }
        }
    }
}

@Composable
fun WidgetContainer(
    widget: WidgetType,
    uiState: SpeedometerUiState,
    onRemove: () -> Unit,
    elevation: Dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Widget")
                }
                IconButton(onClick = { /* TODO: Implement individual component settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Widget Settings")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (widget) {
                    WidgetType.SPEEDOMETER -> SpeedComponent(uiState)
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
fun SpeedComponent(uiState: SpeedometerUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format(LocalLocale.current.platformLocale, "%.1f", uiState.speedKmh),
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "km/h",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun PositionComponent(uiState: SpeedometerUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            onReorderWidget = { _, _ -> },
            onAddWidget = {},
            onRemoveWidget = {},
            onSettingsClick = {}
        )
    }
}
