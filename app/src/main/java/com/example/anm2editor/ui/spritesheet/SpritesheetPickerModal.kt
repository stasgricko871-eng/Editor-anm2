package com.example.anm2editor.ui.spritesheet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.anm2editor.engine.SpritesheetManager
import com.example.anm2editor.model.Anm2Frame
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SpritesheetPickerModal(
    sheetId: Int,
    initialFrame: Anm2Frame,
    spritesheetManager: SpritesheetManager,
    onApply: (cropX: Int, cropY: Int, width: Int, height: Int, pivotX: Float, pivotY: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap = remember(sheetId) { spritesheetManager.getImageBitmap(sheetId, null) }

    var cropX by remember { mutableIntStateOf(initialFrame.xCrop) }
    var cropY by remember { mutableIntStateOf(initialFrame.yCrop) }
    var cropW by remember { mutableIntStateOf(initialFrame.width.coerceAtLeast(16)) }
    var cropH by remember { mutableIntStateOf(initialFrame.height.coerceAtLeast(16)) }
    var pivotX by remember { mutableFloatStateOf(initialFrame.xPivot) }
    var pivotY by remember { mutableFloatStateOf(initialFrame.yPivot) }

    var gridSnapSize by remember { mutableIntStateOf(32) } // 16, 32, 64, or 0 (free)
    var isPlacingPivot by remember { mutableStateOf(false) }

    var zoom by remember { mutableFloatStateOf(2.5f) }
    var panOffset by remember { mutableStateOf(Offset(20f, 20f)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, IsaacPrimary, RoundedCornerShape(12.dp)),
            color = IsaacSurface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Crop, contentDescription = null, tint = IsaacPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Выбор спрайта (Лист #$sheetId)",
                            style = MaterialTheme.typography.titleMedium,
                            color = IsaacBone
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = IsaacBone)
                    }
                }

                // Grid snap options
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Сетка:", style = MaterialTheme.typography.labelSmall, color = IsaacBone)
                    listOf(16, 32, 64, 0).forEach { size ->
                        FilterChip(
                            selected = gridSnapSize == size,
                            onClick = {
                                gridSnapSize = size
                                if (size > 0) {
                                    cropW = size
                                    cropH = size
                                    pivotX = (size / 2).toFloat()
                                    pivotY = (size / 2).toFloat()
                                }
                            },
                            label = { Text(if (size == 0) "Свободно" else "${size}x${size}", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    FilterChip(
                        selected = isPlacingPivot,
                        onClick = { isPlacingPivot = !isPlacingPivot },
                        label = { Text(if (isPlacingPivot) "Нажатие: Pivot" else "Нажатие: Спрайт", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IsaacPivotColor.copy(alpha = 0.3f),
                            selectedLabelColor = IsaacPivotColor
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                // Main Interactive Spritesheet Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(IsaacBackground)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("spritesheet_canvas")
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, z, _ ->
                                    zoom = (zoom * z).coerceIn(0.5f, 10f)
                                    panOffset += pan
                                }
                            }
                            .pointerInput(gridSnapSize, isPlacingPivot, cropX, cropY, cropW, cropH) {
                                detectTapGestures { tapOffset ->
                                    val localX = ((tapOffset.x - panOffset.x) / zoom).roundToInt()
                                    val localY = ((tapOffset.y - panOffset.y) / zoom).roundToInt()

                                    if (isPlacingPivot) {
                                        // Place pivot relative to top-left of crop
                                        pivotX = (localX - cropX).toFloat().coerceIn(0f, cropW.toFloat())
                                        pivotY = (localY - cropY).toFloat().coerceIn(0f, cropH.toFloat())
                                        isPlacingPivot = false
                                    } else {
                                        // Select sprite tile
                                        if (gridSnapSize > 0) {
                                            cropX = ((localX / gridSnapSize) * gridSnapSize).coerceIn(0, bitmap.width - gridSnapSize)
                                            cropY = ((localY / gridSnapSize) * gridSnapSize).coerceIn(0, bitmap.height - gridSnapSize)
                                            cropW = gridSnapSize
                                            cropH = gridSnapSize
                                        } else {
                                            cropX = localX.coerceIn(0, bitmap.width - 16)
                                            cropY = localY.coerceIn(0, bitmap.height - 16)
                                        }
                                    }
                                }
                            }
                    ) {
                        // Draw full spritesheet
                        val sheetW = bitmap.width.toFloat() * zoom
                        val sheetH = bitmap.height.toFloat() * zoom

                        // Spritesheet boundary
                        drawRect(
                            color = Color(0xFF1E1A26),
                            topLeft = panOffset,
                            size = Size(sheetW, sheetH)
                        )

                        drawImage(
                            image = bitmap,
                            dstOffset = IntOffset(panOffset.x.toInt(), panOffset.y.toInt()),
                            dstSize = IntSize(sheetW.toInt(), sheetH.toInt()),
                            filterQuality = FilterQuality.None
                        )

                        // Grid Lines
                        if (gridSnapSize > 0) {
                            val gridPx = gridSnapSize * zoom
                            var gx = panOffset.x
                            while (gx <= panOffset.x + sheetW) {
                                drawLine(Color(0x33FFFFFF), Offset(gx, panOffset.y), Offset(gx, panOffset.y + sheetH), 0.8f)
                                gx += gridPx
                            }
                            var gy = panOffset.y
                            while (gy <= panOffset.y + sheetH) {
                                drawLine(Color(0x33FFFFFF), Offset(panOffset.x, gy), Offset(panOffset.x + sheetW, gy), 0.8f)
                                gy += gridPx
                            }
                        }

                        // Selected Crop Rectangle
                        val selLeft = panOffset.x + cropX * zoom
                        val selTop = panOffset.y + cropY * zoom
                        val selWidth = cropW * zoom
                        val selHeight = cropH * zoom

                        // Highlight overlay
                        drawRect(
                            color = IsaacPrimary.copy(alpha = 0.25f),
                            topLeft = Offset(selLeft, selTop),
                            size = Size(selWidth, selHeight)
                        )
                        drawRect(
                            color = IsaacPrimary,
                            topLeft = Offset(selLeft, selTop),
                            size = Size(selWidth, selHeight),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Pivot Marker
                        val pX = selLeft + pivotX * zoom
                        val pY = selTop + pivotY * zoom
                        drawCircle(color = IsaacPivotColor, radius = 5.dp.toPx(), center = Offset(pX, pY))
                        drawLine(IsaacPivotColor, Offset(pX - 10f, pY), Offset(pX + 10f, pY), 2f)
                        drawLine(IsaacPivotColor, Offset(pX, pY - 10f), Offset(pX, pY + 10f), 2f)
                    }

                    // Zoom indicator overlay
                    Text(
                        text = "Масштаб: ${(zoom * 100).roundToInt()}% | Нажмите для выбора кадра",
                        fontSize = 10.sp,
                        color = IsaacBone,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(IsaacSurface.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Footer Preview & Apply Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview box of cropped sprite
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(IsaacBackground)
                                .border(1.dp, IsaacPrimary, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val s = (size.width / cropW.toFloat().coerceAtLeast(1f)).coerceAtMost(size.height / cropH.toFloat().coerceAtLeast(1f))
                                val dw = (cropW * s).toInt()
                                val dh = (cropH * s).toInt()
                                drawImage(
                                    image = bitmap,
                                    srcOffset = IntOffset(cropX, cropY),
                                    srcSize = IntSize(cropW, cropH),
                                    dstOffset = IntOffset((size.width - dw).toInt() / 2, (size.height - dh).toInt() / 2),
                                    dstSize = IntSize(dw, dh),
                                    filterQuality = FilterQuality.None
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Обрезка: ($cropX, $cropY) — ${cropW}x${cropH}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = IsaacPrimary
                            )
                            Text(
                                text = "Pivot: (${pivotX.roundToInt()}, ${pivotY.roundToInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaacPivotColor
                            )
                        }
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                onApply(cropX, cropY, cropW, cropH, pivotX, pivotY)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaacPrimary)
                        ) {
                            Text("Применить", color = Color(0xFF002244), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
