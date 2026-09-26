package com.example.anm2editor.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.anm2editor.engine.Anm2PlaybackEngine
import com.example.anm2editor.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class InspectorTab {
    FRAME,
    LAYERS,
    ANIMATIONS,
    SPRITESHEETS,
    NULLS_EVENTS
}

enum class InspectorSizeMode {
    COLLAPSED, // Свёрнуто (~42dp)
    COMPACT,   // Компактно (~165dp)
    EXPANDED   // Полный размер
}

@Composable
fun Anm2Inspector(
    actor: Anm2Actor,
    currentAnimation: Anm2Animation,
    currentTick: Int,
    selectedLayerId: Int?,
    sizeMode: InspectorSizeMode = InspectorSizeMode.COMPACT,
    onSizeModeChanged: (InspectorSizeMode) -> Unit = {},
    onUpdateFrame: (layerId: Int, keyframeIndex: Int, updated: Anm2Frame) -> Unit,
    onOpenSpritesheetPicker: (currentSheetId: Int, initialCrop: Anm2Frame) -> Unit,
    // Layer actions
    onAddLayer: (name: String, sheetId: Int) -> Unit,
    onDeleteLayer: (layerId: Int) -> Unit,
    onReorderLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onRenameLayer: (layerId: Int, newName: String) -> Unit,
    onSelectLayer: (layerId: Int) -> Unit,
    // Animation actions
    onSelectAnimation: (name: String) -> Unit,
    onAddAnimation: (name: String, frameNum: Int, loop: Boolean) -> Unit,
    onDuplicateAnimation: (name: String) -> Unit,
    onDeleteAnimation: (name: String) -> Unit,
    onUpdateAnimProps: (newName: String, newFrameNum: Int, loop: Boolean) -> Unit,
    onSetDefaultAnimation: (name: String) -> Unit,
    // Spritesheet actions
    onAddSpritesheet: (path: String) -> Unit,
    onPickSpritesheetImage: (sheetId: Int) -> Unit,
    // Nulls & Events actions
    onAddNull: (name: String) -> Unit,
    onDeleteNull: (nullId: Int) -> Unit,
    onAddTrigger: (eventId: Int, atFrame: Int) -> Unit,
    onDeleteTrigger: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(InspectorTab.FRAME) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        color = IsaacSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Верхняя полоса быстрого переключения размера панели
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(0xFF1E1B24))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (sizeMode) {
                            InspectorSizeMode.COLLAPSED -> Icons.Default.KeyboardArrowUp
                            InspectorSizeMode.COMPACT -> Icons.Default.ViewAgenda
                            InspectorSizeMode.EXPANDED -> Icons.Default.Fullscreen
                        },
                        contentDescription = null,
                        tint = IsaacPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Панель: ${
                            when (sizeMode) {
                                InspectorSizeMode.COLLAPSED -> "Свёрнута"
                                InspectorSizeMode.COMPACT -> "Компактная"
                                InspectorSizeMode.EXPANDED -> "Развёрнута"
                            }
                        }",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = IsaacBone
                    )
                }

                // Кнопки быстрого переключения размера: Свернуть / Компактно / На весь экран
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка Свернуть
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (sizeMode == InspectorSizeMode.COLLAPSED) IsaacPrimary.copy(alpha = 0.3f) else IsaacSurfaceVariant,
                        modifier = Modifier
                            .height(20.dp)
                            .clickable { onSizeModeChanged(InspectorSizeMode.COLLAPSED) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Minimize, contentDescription = "Свернуть", modifier = Modifier.size(10.dp), tint = IsaacBone)
                            Spacer(Modifier.width(2.dp))
                            Text("Скрыть", fontSize = 9.sp, color = IsaacBone)
                        }
                    }

                    // Кнопка Компактно
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (sizeMode == InspectorSizeMode.COMPACT) IsaacPrimary.copy(alpha = 0.3f) else IsaacSurfaceVariant,
                        modifier = Modifier
                            .height(20.dp)
                            .clickable { onSizeModeChanged(InspectorSizeMode.COMPACT) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text("Компактно", fontSize = 9.sp, color = IsaacBone)
                        }
                    }

                    // Кнопка Развернуть
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (sizeMode == InspectorSizeMode.EXPANDED) IsaacPrimary.copy(alpha = 0.3f) else IsaacSurfaceVariant,
                        modifier = Modifier
                            .height(20.dp)
                            .clickable { onSizeModeChanged(InspectorSizeMode.EXPANDED) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Развернуть", modifier = Modifier.size(10.dp), tint = IsaacBone)
                            Spacer(Modifier.width(2.dp))
                            Text("Больше", fontSize = 9.sp, color = IsaacBone)
                        }
                    }
                }
            }

            // Header: Вкладки инспектора
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(IsaacSurfaceVariant),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = IsaacBone,
                    edgePadding = 6.dp,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Tab(
                        selected = activeTab == InspectorTab.FRAME,
                        onClick = {
                            activeTab = InspectorTab.FRAME
                            if (sizeMode == InspectorSizeMode.COLLAPSED) {
                                onSizeModeChanged(InspectorSizeMode.COMPACT)
                            }
                        },
                        text = { Text("Кадр", fontSize = 11.sp, fontWeight = if (activeTab == InspectorTab.FRAME) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == InspectorTab.LAYERS,
                        onClick = {
                            activeTab = InspectorTab.LAYERS
                            if (sizeMode == InspectorSizeMode.COLLAPSED) {
                                onSizeModeChanged(InspectorSizeMode.COMPACT)
                            }
                        },
                        text = { Text("Слои", fontSize = 11.sp, fontWeight = if (activeTab == InspectorTab.LAYERS) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == InspectorTab.ANIMATIONS,
                        onClick = {
                            activeTab = InspectorTab.ANIMATIONS
                            if (sizeMode == InspectorSizeMode.COLLAPSED) {
                                onSizeModeChanged(InspectorSizeMode.COMPACT)
                            }
                        },
                        text = { Text("Анимации", fontSize = 11.sp, fontWeight = if (activeTab == InspectorTab.ANIMATIONS) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == InspectorTab.SPRITESHEETS,
                        onClick = {
                            activeTab = InspectorTab.SPRITESHEETS
                            if (sizeMode == InspectorSizeMode.COLLAPSED) {
                                onSizeModeChanged(InspectorSizeMode.COMPACT)
                            }
                        },
                        text = { Text("Спрайтшиты", fontSize = 11.sp, fontWeight = if (activeTab == InspectorTab.SPRITESHEETS) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == InspectorTab.NULLS_EVENTS,
                        onClick = {
                            activeTab = InspectorTab.NULLS_EVENTS
                            if (sizeMode == InspectorSizeMode.COLLAPSED) {
                                onSizeModeChanged(InspectorSizeMode.COMPACT)
                            }
                        },
                        text = { Text("Якоря / Звуки", fontSize = 11.sp, fontWeight = if (activeTab == InspectorTab.NULLS_EVENTS) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                // Кнопка развернуть / свернуть рядом с табами
                IconButton(
                    onClick = {
                        val nextMode = when (sizeMode) {
                            InspectorSizeMode.COLLAPSED -> InspectorSizeMode.COMPACT
                            InspectorSizeMode.COMPACT -> InspectorSizeMode.COLLAPSED
                            InspectorSizeMode.EXPANDED -> InspectorSizeMode.COMPACT
                        }
                        onSizeModeChanged(nextMode)
                    },
                    modifier = Modifier.size(34.dp).padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = if (sizeMode == InspectorSizeMode.COLLAPSED) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (sizeMode == InspectorSizeMode.COLLAPSED) "Развернуть" else "Свернуть",
                        tint = IsaacPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Контент инспектора (отображается только если панель не свёрнута)
            if (sizeMode != InspectorSizeMode.COLLAPSED) {
                val contentMaxHeight = if (sizeMode == InspectorSizeMode.COMPACT) 115.dp else 320.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    when (activeTab) {
                        InspectorTab.FRAME -> FrameInspectorView(
                            actor = actor,
                            currentAnimation = currentAnimation,
                            currentTick = currentTick,
                            selectedLayerId = selectedLayerId,
                            maxHeight = contentMaxHeight,
                            onUpdateFrame = onUpdateFrame,
                            onOpenSpritesheetPicker = onOpenSpritesheetPicker
                        )
                        InspectorTab.LAYERS -> LayersInspectorView(
                            actor = actor,
                            selectedLayerId = selectedLayerId,
                            maxHeight = contentMaxHeight,
                            onSelectLayer = onSelectLayer,
                            onAddLayer = onAddLayer,
                            onDeleteLayer = onDeleteLayer,
                            onReorderLayer = onReorderLayer,
                            onRenameLayer = onRenameLayer
                        )
                        InspectorTab.ANIMATIONS -> AnimationsInspectorView(
                            actor = actor,
                            currentAnimation = currentAnimation,
                            maxHeight = contentMaxHeight,
                            onSelectAnimation = onSelectAnimation,
                            onAddAnimation = onAddAnimation,
                            onDuplicateAnimation = onDuplicateAnimation,
                            onDeleteAnimation = onDeleteAnimation,
                            onUpdateAnimProps = onUpdateAnimProps,
                            onSetDefaultAnimation = onSetDefaultAnimation
                        )
                        InspectorTab.SPRITESHEETS -> SpritesheetsInspectorView(
                            actor = actor,
                            maxHeight = contentMaxHeight,
                            onAddSpritesheet = onAddSpritesheet,
                            onPickSpritesheetImage = onPickSpritesheetImage
                        )
                        InspectorTab.NULLS_EVENTS -> NullsAndEventsInspectorView(
                            actor = actor,
                            currentAnimation = currentAnimation,
                            maxHeight = contentMaxHeight,
                            onAddNull = onAddNull,
                            onDeleteNull = onDeleteNull,
                            onAddTrigger = onAddTrigger,
                            onDeleteTrigger = onDeleteTrigger
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameInspectorView(
    actor: Anm2Actor,
    currentAnimation: Anm2Animation,
    currentTick: Int,
    selectedLayerId: Int?,
    maxHeight: androidx.compose.ui.unit.Dp,
    onUpdateFrame: (layerId: Int, keyframeIndex: Int, updated: Anm2Frame) -> Unit,
    onOpenSpritesheetPicker: (currentSheetId: Int, initialCrop: Anm2Frame) -> Unit
) {
    if (selectedLayerId == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Выберите слой для настройки кадров", color = IsaacOnSurfaceMuted, fontSize = 12.sp)
        }
        return
    }

    val layerDef = actor.content.layers.firstOrNull { it.id == selectedLayerId }
    val layerAnim = currentAnimation.layerAnimations.firstOrNull { it.layerId == selectedLayerId }
    val frames = layerAnim?.frames ?: emptyList()

    if (frames.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("В слое нет ключевых кадров", color = IsaacOnSurfaceMuted, fontSize = 12.sp)
        }
        return
    }

    val kfIndex = Anm2PlaybackEngine.getKeyframeIndexAtTick(frames, currentTick)
    val frame = frames[kfIndex]

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Шапка слоя и кадра
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${layerDef?.name ?: "Слой"} — Кадр #${kfIndex + 1}",
                style = MaterialTheme.typography.titleSmall,
                color = IsaacPrimary,
                fontSize = 12.sp
            )
            FilledTonalButton(
                onClick = { onOpenSpritesheetPicker(layerDef?.spritesheetId ?: 0, frame) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp).testTag("pick_crop_button")
            ) {
                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Выбрать с листа", fontSize = 11.sp)
            }
        }

        // Позиция и Pivot (точка вращения/привязки)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Позиция X и Y
            Column(modifier = Modifier.weight(1f)) {
                Text("Позиция (X, Y)", style = MaterialTheme.typography.labelSmall, color = IsaacBone, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NumberStepper(
                        label = "X",
                        value = frame.xPosition,
                        onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(xPosition = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberStepper(
                        label = "Y",
                        value = frame.yPosition,
                        onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(yPosition = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pivot X и Y
            Column(modifier = Modifier.weight(1f)) {
                Text("Pivot точка (X, Y)", style = MaterialTheme.typography.labelSmall, color = IsaacPivotColor, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NumberStepper(
                        label = "PX",
                        value = frame.xPivot,
                        onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(xPivot = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberStepper(
                        label = "PY",
                        value = frame.yPivot,
                        onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(yPivot = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Обрезка спрайта (XCrop, YCrop, Width, Height)
        Text("Обрезка спрайтшита (X, Y, Ширина, Высота)", style = MaterialTheme.typography.labelSmall, color = IsaacBone, fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IntStepper(label = "X", value = frame.xCrop, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(xCrop = it)) }, modifier = Modifier.weight(1f))
            IntStepper(label = "Y", value = frame.yCrop, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(yCrop = it)) }, modifier = Modifier.weight(1f))
            IntStepper(label = "Ш", value = frame.width, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(width = it)) }, modifier = Modifier.weight(1f))
            IntStepper(label = "В", value = frame.height, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(height = it)) }, modifier = Modifier.weight(1f))
        }

        // Масштаб и Поворот
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Масштаб % (X, Y)", style = MaterialTheme.typography.labelSmall, color = IsaacBone, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NumberStepper(label = "SX", value = frame.xScale, step = 5f, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(xScale = it)) }, modifier = Modifier.weight(1f))
                    NumberStepper(label = "SY", value = frame.yScale, step = 5f, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(yScale = it)) }, modifier = Modifier.weight(1f))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Поворот (°)", style = MaterialTheme.typography.labelSmall, color = IsaacBone, fontSize = 10.sp)
                NumberStepper(label = "Град", value = frame.rotation, step = 5f, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(rotation = it)) })
            }
        }

        // Оттенок цвета (Tint R, G, B, Alpha)
        Text("Цветовой оттенок (R, G, B, Прозрачность)", style = MaterialTheme.typography.labelSmall, color = IsaacBone, fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IntStepper(label = "R", value = frame.redTint, minVal = 0, maxVal = 255, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(redTint = it)) }, modifier = Modifier.weight(1f))
            IntStepper(label = "G", value = frame.greenTint, minVal = 0, maxVal = 255, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(greenTint = it)) }, modifier = Modifier.weight(1f))
            IntStepper(label = "B", value = frame.blueTint, minVal = 0, maxVal = 255, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(blueTint = it)) }, modifier = Modifier.weight(1f))
            IntStepper(label = "A", value = frame.alphaTint, minVal = 0, maxVal = 255, onValueChange = { onUpdateFrame(selectedLayerId, kfIndex, frame.copy(alphaTint = it)) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LayersInspectorView(
    actor: Anm2Actor,
    selectedLayerId: Int?,
    maxHeight: androidx.compose.ui.unit.Dp,
    onSelectLayer: (Int) -> Unit,
    onAddLayer: (name: String, sheetId: Int) -> Unit,
    onDeleteLayer: (Int) -> Unit,
    onReorderLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onRenameLayer: (Int, String) -> Unit
) {
    var newLayerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        // Добавление слоя
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = newLayerName,
                onValueChange = { newLayerName = it },
                label = { Text("Имя нового слоя", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(46.dp)
            )
            Button(
                onClick = {
                    val name = newLayerName.ifBlank { "Слой_${actor.content.layers.size}" }
                    onAddLayer(name, 0)
                    newLayerName = ""
                },
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Добавить", fontSize = 11.sp)
            }
        }

        // Список слоёв
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(actor.content.layers) { index, layer ->
                val isSelected = selectedLayerId == layer.id
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) IsaacSurfaceVariant else Color(0xFF26212D),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, IsaacPrimary) else null,
                    modifier = Modifier.fillMaxWidth().clickable { onSelectLayer(layer.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${index + 1}", fontSize = 11.sp, color = IsaacOnSurfaceMuted, modifier = Modifier.width(28.dp))
                            Text(layer.name, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) IsaacPrimary else IsaacBone, fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("(Лист #${layer.spritesheetId})", fontSize = 10.sp, color = IsaacOnSurfaceMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Поднять вверх
                            IconButton(
                                onClick = { onReorderLayer(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", modifier = Modifier.size(14.dp))
                            }
                            // Опустить вниз
                            IconButton(
                                onClick = { onReorderLayer(index, index + 1) },
                                enabled = index < actor.content.layers.lastIndex,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз", modifier = Modifier.size(14.dp))
                            }
                            // Удалить
                            IconButton(
                                onClick = { onDeleteLayer(layer.id) },
                                enabled = actor.content.layers.size > 1,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = IsaacCrimson, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimationsInspectorView(
    actor: Anm2Actor,
    currentAnimation: Anm2Animation,
    maxHeight: androidx.compose.ui.unit.Dp,
    onSelectAnimation: (String) -> Unit,
    onAddAnimation: (name: String, frameNum: Int, loop: Boolean) -> Unit,
    onDuplicateAnimation: (String) -> Unit,
    onDeleteAnimation: (String) -> Unit,
    onUpdateAnimProps: (newName: String, newFrameNum: Int, loop: Boolean) -> Unit,
    onSetDefaultAnimation: (String) -> Unit
) {
    var newAnimName by remember { mutableStateOf("") }
    var animFrameNum by remember(currentAnimation.name) { mutableIntStateOf(currentAnimation.frameNum) }
    var animLoop by remember(currentAnimation.name) { mutableStateOf(currentAnimation.loop) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        // Свойства выбранной анимации
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Кадров (FrameNum)", style = MaterialTheme.typography.labelSmall, color = IsaacBone, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val f = (animFrameNum - 1).coerceAtLeast(1)
                            animFrameNum = f
                            onUpdateAnimProps(currentAnimation.name, f, animLoop)
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                    Text("$animFrameNum", fontWeight = FontWeight.Bold, color = IsaacPrimary, fontSize = 13.sp)
                    IconButton(
                        onClick = {
                            val f = animFrameNum + 1
                            animFrameNum = f
                            onUpdateAnimProps(currentAnimation.name, f, animLoop)
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            FilterChip(
                selected = animLoop,
                onClick = {
                    animLoop = !animLoop
                    onUpdateAnimProps(currentAnimation.name, animFrameNum, animLoop)
                },
                label = { Text(if (animLoop) "Повтор: Вкл" else "Повтор: Выкл", fontSize = 10.sp) },
                modifier = Modifier.height(28.dp)
            )

            FilledTonalButton(
                onClick = { onDuplicateAnimation(currentAnimation.name) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text("Копия", fontSize = 10.sp)
            }
        }

        // Добавить новую анимацию
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = newAnimName,
                onValueChange = { newAnimName = it },
                label = { Text("Имя анимации", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(46.dp)
            )
            Button(
                onClick = {
                    if (newAnimName.isNotBlank()) {
                        onAddAnimation(newAnimName.trim(), 16, true)
                        newAnimName = ""
                    }
                },
                modifier = Modifier.height(40.dp)
            ) {
                Text("Добавить", fontSize = 11.sp)
            }
        }

        // Список анимаций
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(actor.animations.animationList) { anim ->
                val isSelected = anim.name == currentAnimation.name
                val isDefault = anim.name == actor.animations.defaultAnimation

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) IsaacSurfaceVariant else Color(0xFF26212D),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, IsaacPrimary) else null,
                    modifier = Modifier.fillMaxWidth().clickable { onSelectAnimation(anim.name) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(anim.name, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) IsaacPrimary else IsaacBone, fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("(${anim.frameNum} кадр.)", fontSize = 10.sp, color = IsaacOnSurfaceMuted)
                            if (isDefault) {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(3.dp), color = IsaacGold.copy(alpha = 0.2f)) {
                                    Text("ОСНОВНАЯ", fontSize = 9.sp, color = IsaacGold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isDefault) {
                                TextButton(
                                    onClick = { onSetDefaultAnimation(anim.name) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("По умолч.", fontSize = 10.sp, color = IsaacGold)
                                }
                            }
                            IconButton(
                                onClick = { onDeleteAnimation(anim.name) },
                                enabled = actor.animations.animationList.size > 1,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = IsaacCrimson, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpritesheetsInspectorView(
    actor: Anm2Actor,
    maxHeight: androidx.compose.ui.unit.Dp,
    onAddSpritesheet: (path: String) -> Unit,
    onPickSpritesheetImage: (sheetId: Int) -> Unit
) {
    var newSheetPath by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = newSheetPath,
                onValueChange = { newSheetPath = it },
                label = { Text("Путь к файлу (напр. boss.png)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(46.dp)
            )
            Button(
                onClick = {
                    val p = newSheetPath.ifBlank { "sheet_${actor.content.spritesheets.size}.png" }
                    onAddSpritesheet(p)
                    newSheetPath = ""
                },
                modifier = Modifier.height(40.dp)
            ) {
                Text("Добавить", fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(actor.content.spritesheets) { sheet ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF26212D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Лист #${sheet.id}: ${sheet.path}", style = MaterialTheme.typography.bodyMedium, color = IsaacBone, fontSize = 12.sp)
                            Text(if (sheet.id == 0) "Стандартный спрайтшит Айзека" else "Пользовательский спрайтшит", fontSize = 10.sp, color = IsaacOnSurfaceMuted)
                        }

                        Button(
                            onClick = { onPickSpritesheetImage(sheet.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaacPrimaryDark),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Загрузить PNG", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NullsAndEventsInspectorView(
    actor: Anm2Actor,
    currentAnimation: Anm2Animation,
    maxHeight: androidx.compose.ui.unit.Dp,
    onAddNull: (String) -> Unit,
    onDeleteNull: (Int) -> Unit,
    onAddTrigger: (eventId: Int, atFrame: Int) -> Unit,
    onDeleteTrigger: (Int) -> Unit
) {
    var newNullName by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Секция Null-якорей
        Text("Якоря Null (TearPoint, HeadAttach, ItemCenter)", style = MaterialTheme.typography.titleSmall, color = IsaacGold, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newNullName,
                onValueChange = { newNullName = it },
                label = { Text("Имя якоря Null", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(46.dp)
            )
            Button(
                onClick = {
                    if (newNullName.isNotBlank()) {
                        onAddNull(newNullName.trim())
                        newNullName = ""
                    }
                },
                modifier = Modifier.height(40.dp)
            ) {
                Text("Добавить", fontSize = 11.sp)
            }
        }

        actor.content.nulls.forEach { n ->
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF26212D), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Null #${n.id}: ${n.name}", color = IsaacBone, fontSize = 11.sp)
                IconButton(onClick = { onDeleteNull(n.id) }, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = IsaacCrimson, modifier = Modifier.size(13.dp))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Триггеры событий и звуков
        Text("Триггеры событий и звуков", style = MaterialTheme.typography.titleSmall, color = IsaacCrimson, fontSize = 11.sp)
        if (currentAnimation.triggers.isEmpty()) {
            Text("Нет триггеров в этой анимации", color = IsaacOnSurfaceMuted, fontSize = 10.sp)
        } else {
            currentAnimation.triggers.forEachIndexed { idx, tr ->
                val ev = actor.content.events.firstOrNull { it.id == tr.eventId }
                val eventName = ev?.name ?: "Event_${tr.eventId}"
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF26212D), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Триггер '$eventName' на кадре ${tr.atFrame}", color = IsaacBone, fontSize = 11.sp)
                    IconButton(onClick = { onDeleteTrigger(idx) }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = IsaacCrimson, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        Button(
            onClick = { onAddTrigger(0, 0) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Добавить триггер на кадр 0", fontSize = 10.sp)
        }
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Float,
    step: Float = 1f,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = IsaacSurfaceVariant,
        modifier = modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            IconButton(onClick = { onValueChange(value - step) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(11.dp), tint = IsaacBone)
            }
            Text(
                text = "$label: ${value.roundToInt()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = IsaacBone
            )
            IconButton(onClick = { onValueChange(value + step) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(11.dp), tint = IsaacBone)
            }
        }
    }
}

@Composable
private fun IntStepper(
    label: String,
    value: Int,
    step: Int = 1,
    minVal: Int = Int.MIN_VALUE,
    maxVal: Int = Int.MAX_VALUE,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = IsaacSurfaceVariant,
        modifier = modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            IconButton(onClick = { onValueChange((value - step).coerceIn(minVal, maxVal)) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(11.dp), tint = IsaacBone)
            }
            Text(
                text = "$label: $value",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = IsaacBone
            )
            IconButton(onClick = { onValueChange((value + step).coerceIn(minVal, maxVal)) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(11.dp), tint = IsaacBone)
            }
        }
    }
}
