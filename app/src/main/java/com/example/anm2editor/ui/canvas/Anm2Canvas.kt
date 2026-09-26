package com.example.anm2editor.ui.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.anm2editor.engine.Anm2PlaybackEngine
import com.example.anm2editor.engine.EvaluatedFrame
import com.example.anm2editor.engine.SpritesheetManager
import com.example.anm2editor.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class CanvasInteractionMode {
    NAVIGATE, // Pan and pinch to zoom
    MOVE_LAYER, // Drag selected layer
    MOVE_PIVOT  // Drag pivot point
}

enum class CanvasBackgroundStyle {
    BASEMENT_GRID,
    CHECKERBOARD,
    DARK_PLAIN
}

@Composable
fun Anm2Canvas(
    actor: Anm2Actor,
    currentAnimation: Anm2Animation,
    currentTick: Int,
    selectedLayerId: Int?,
    spritesheetManager: SpritesheetManager,
    onLayerSelected: (Int) -> Unit,
    onUpdateSelectedFramePosition: (dx: Float, dy: Float) -> Unit,
    onUpdateSelectedPivotPosition: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(3.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var interactionMode by remember { mutableStateOf(CanvasInteractionMode.NAVIGATE) }
    var bgStyle by remember { mutableStateOf(CanvasBackgroundStyle.BASEMENT_GRID) }
    var showOnionSkin by remember { mutableStateOf(false) }
    var showNulls by remember { mutableStateOf(true) }
    var showOriginCrosshair by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(IsaacBackground)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("animation_canvas")
                .pointerInput(interactionMode) {
                    when (interactionMode) {
                        CanvasInteractionMode.NAVIGATE -> {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 15f)
                                panOffset += pan
                            }
                        }
                        CanvasInteractionMode.MOVE_LAYER -> {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / scale
                                val dy = dragAmount.y / scale
                                onUpdateSelectedFramePosition(dx, dy)
                            }
                        }
                        CanvasInteractionMode.MOVE_PIVOT -> {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / scale
                                val dy = dragAmount.y / scale
                                onUpdateSelectedPivotPosition(dx, dy)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Could hit-test layers or toggle
                    }
                }
        ) {
            val center = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)

            // 1. Draw Background
            drawCanvasBackground(bgStyle, center, scale)

            // 2. Origin Crosshair
            if (showOriginCrosshair) {
                drawOriginCrosshair(center, scale)
            }

            // 3. Onion Skinning (Previous frame in red tint, Next frame in blue tint)
            if (showOnionSkin && currentAnimation.frameNum > 1) {
                val prevTick = if (currentTick > 0) currentTick - 1 else if (currentAnimation.loop) currentAnimation.frameNum - 1 else 0
                val nextTick = if (currentTick < currentAnimation.frameNum - 1) currentTick + 1 else if (currentAnimation.loop) 0 else currentAnimation.frameNum - 1

                if (prevTick != currentTick) {
                    drawAnimationState(
                        actor = actor,
                        anim = currentAnimation,
                        tick = prevTick,
                        center = center,
                        scale = scale,
                        spritesheetManager = spritesheetManager,
                        tintOverride = Color(0x66FF5252),
                        selectedLayerId = null
                    )
                }
                if (nextTick != currentTick) {
                    drawAnimationState(
                        actor = actor,
                        anim = currentAnimation,
                        tick = nextTick,
                        center = center,
                        scale = scale,
                        spritesheetManager = spritesheetManager,
                        tintOverride = Color(0x66448AFF),
                        selectedLayerId = null
                    )
                }
            }

            // 4. Draw Current Frame Layers (in layer list order)
            drawAnimationState(
                actor = actor,
                anim = currentAnimation,
                tick = currentTick,
                center = center,
                scale = scale,
                spritesheetManager = spritesheetManager,
                tintOverride = null,
                selectedLayerId = selectedLayerId
            )

            // 5. Draw Nulls (anchor positions)
            if (showNulls) {
                drawNullPoints(
                    actor = actor,
                    anim = currentAnimation,
                    tick = currentTick,
                    center = center,
                    scale = scale
                )
            }
        }

        // Overlay Toolbars
        // Top Toolbar: Zoom & Mode indicator
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(IsaacSurface.copy(alpha = 0.88f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${(scale * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = IsaacBone
            )
            IconButton(
                onClick = {
                    scale = 3.0f
                    panOffset = Offset.Zero
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = "Сброс / Центр",
                    tint = IsaacPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { scale = (scale * 1.25f).coerceAtMost(15f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Приблизить",
                    tint = IsaacBone,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Отдалить",
                    tint = IsaacBone,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Top Right Toolbar: Quick Toggles (InteractionMode, Onion skin, Grid)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(IsaacSurface.copy(alpha = 0.88f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Mode: Navigate vs Move Layer vs Move Pivot
            IconButton(
                onClick = {
                    interactionMode = when (interactionMode) {
                        CanvasInteractionMode.NAVIGATE -> CanvasInteractionMode.MOVE_LAYER
                        CanvasInteractionMode.MOVE_LAYER -> CanvasInteractionMode.MOVE_PIVOT
                        CanvasInteractionMode.MOVE_PIVOT -> CanvasInteractionMode.NAVIGATE
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                val (icon, tint) = when (interactionMode) {
                    CanvasInteractionMode.NAVIGATE -> Icons.Default.PanTool to IsaacBone
                    CanvasInteractionMode.MOVE_LAYER -> Icons.Default.OpenWith to IsaacPrimary
                    CanvasInteractionMode.MOVE_PIVOT -> Icons.Default.GpsFixed to IsaacPivotColor
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Режим инструмента (Перемещение / Слой / Pivot)",
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Onion Skin Toggle
            IconButton(
                onClick = { showOnionSkin = !showOnionSkin },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Калька (Onion Skin)",
                    tint = if (showOnionSkin) IsaacPrimary else IsaacOnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Nulls Toggle
            IconButton(
                onClick = { showNulls = !showNulls },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = "Точки привязки",
                    tint = if (showNulls) IsaacGold else IsaacOnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Background Style
            IconButton(
                onClick = {
                    bgStyle = when (bgStyle) {
                        CanvasBackgroundStyle.BASEMENT_GRID -> CanvasBackgroundStyle.CHECKERBOARD
                        CanvasBackgroundStyle.CHECKERBOARD -> CanvasBackgroundStyle.DARK_PLAIN
                        CanvasBackgroundStyle.DARK_PLAIN -> CanvasBackgroundStyle.BASEMENT_GRID
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Стиль фона",
                    tint = IsaacBone,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Mode badge indicator at bottom left
        if (interactionMode != CanvasInteractionMode.NAVIGATE) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (interactionMode == CanvasInteractionMode.MOVE_LAYER) IsaacPrimaryDark else IsaacCrimson
            ) {
                Text(
                    text = if (interactionMode == CanvasInteractionMode.MOVE_LAYER) "Перемещение слоя (X/Y)" else "Перемещение Pivot (X/Y)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawCanvasBackground(style: CanvasBackgroundStyle, center: Offset, scale: Float) {
    when (style) {
        CanvasBackgroundStyle.DARK_PLAIN -> {
            drawRect(IsaacBackground)
        }
        CanvasBackgroundStyle.BASEMENT_GRID -> {
            drawRect(IsaacBackground)
            // Draw Isaac stone room grid tiles
            val tileSize = 32f * scale
            val startX = (center.x % tileSize) - tileSize
            val startY = (center.y % tileSize) - tileSize

            var x = startX
            while (x < size.width + tileSize) {
                drawLine(
                    color = IsaacGridLight.copy(alpha = 0.4f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += tileSize
            }

            var y = startY
            while (y < size.height + tileSize) {
                drawLine(
                    color = IsaacGridLight.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += tileSize
            }
        }
        CanvasBackgroundStyle.CHECKERBOARD -> {
            val cell = 16f * scale
            val cols = (size.width / cell).toInt() + 2
            val rows = (size.height / cell).toInt() + 2
            val offsetX = (center.x % (cell * 2)) - cell * 2
            val offsetY = (center.y % (cell * 2)) - cell * 2

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val color = if ((r + c) % 2 == 0) IsaacGridDark else IsaacGridLight
                    drawRect(
                        color = color,
                        topLeft = Offset(offsetX + c * cell, offsetY + r * cell),
                        size = Size(cell, cell)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawOriginCrosshair(center: Offset, scale: Float) {
    val axisColor = Color(0x77FFFFFF)
    // Horizontal axis line
    drawLine(
        color = axisColor,
        start = Offset(0f, center.y),
        end = Offset(size.width, center.y),
        strokeWidth = 1.2f
    )
    // Vertical axis line
    drawLine(
        color = axisColor,
        start = Offset(center.x, 0f),
        end = Offset(center.x, size.height),
        strokeWidth = 1.2f
    )
    // Small center crosshair
    drawCircle(
        color = IsaacPrimary,
        radius = 3f * scale.coerceAtMost(2f),
        center = center,
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawAnimationState(
    actor: Anm2Actor,
    anim: Anm2Animation,
    tick: Int,
    center: Offset,
    scale: Float,
    spritesheetManager: SpritesheetManager,
    tintOverride: Color?,
    selectedLayerId: Int?
) {
    // Root evaluation
    val rootEval = Anm2PlaybackEngine.evaluateRoot(anim.rootAnimation.frames, tick, anim.frameNum)
    val rootPos = rootEval?.frame?.let { Offset(it.xPosition, it.yPosition) } ?: Offset.Zero

    // Render layers in layer list order
    actor.content.layers.forEach { layerDef ->
        val layerAnim = anim.layerAnimations.firstOrNull { it.layerId == layerDef.id }
        if (layerAnim != null && layerAnim.visible) {
            val eval = Anm2PlaybackEngine.evaluateLayer(layerAnim, tick, anim.frameNum)
            if (eval != null && eval.frame.visible) {
                val f = eval.frame
                val imageBitmap = spritesheetManager.getImageBitmap(layerDef.spritesheetId, null)

                // Isaac Coordinate Transform:
                // Origin (0,0) -> Root Pos -> Layer Pos
                // In Isaac: Pivot is anchor point on the cropped sprite.
                // When rotation/scale happen, they happen around the pivot (xPivot, yPivot).
                withTransform({
                    translate(center.x + (rootPos.x + f.xPosition) * scale, center.y + (rootPos.y + f.yPosition) * scale)
                    rotate(f.rotation)
                    scale(f.xScale / 100f * scale, f.yScale / 100f * scale)
                    translate(-f.xPivot, -f.yPivot)
                }) {
                    val srcW = f.width.coerceAtLeast(1)
                    val srcH = f.height.coerceAtLeast(1)
                    val cropX = f.xCrop.coerceIn(0, imageBitmap.width - 1)
                    val cropY = f.yCrop.coerceIn(0, imageBitmap.height - 1)
                    val actualW = srcW.coerceAtMost(imageBitmap.width - cropX)
                    val actualH = srcH.coerceAtMost(imageBitmap.height - cropY)

                    if (actualW > 0 && actualH > 0) {
                        val colorFilter = if (tintOverride != null) {
                            ColorFilter.tint(tintOverride, BlendMode.SrcAtop)
                        } else {
                            val r = f.redTint / 255f
                            val g = f.greenTint / 255f
                            val b = f.blueTint / 255f
                            val a = f.alphaTint / 255f
                            if (r < 0.99f || g < 0.99f || b < 0.99f || a < 0.99f) {
                                ColorFilter.tint(Color(r, g, b, a), BlendMode.Modulate)
                            } else null
                        }

                        drawImage(
                            image = imageBitmap,
                            srcOffset = IntOffset(cropX, cropY),
                            srcSize = IntSize(actualW, actualH),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(actualW, actualH),
                            filterQuality = FilterQuality.None,
                            colorFilter = colorFilter
                        )

                        // If this layer is selected and not in onion skin mode, draw selection border
                        if (tintOverride == null && selectedLayerId == layerDef.id) {
                            drawRect(
                                color = IsaacSelectionBorder,
                                topLeft = Offset(0f, 0f),
                                size = Size(actualW.toFloat(), actualH.toFloat()),
                                style = Stroke(width = 1.5f / scale.coerceAtLeast(1f))
                            )
                            // Pivot point indicator
                            drawCircle(
                                color = IsaacPivotColor,
                                radius = 3.5f / scale.coerceAtLeast(1f),
                                center = Offset(f.xPivot, f.yPivot)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawNullPoints(
    actor: Anm2Actor,
    anim: Anm2Animation,
    tick: Int,
    center: Offset,
    scale: Float
) {
    anim.nullAnimations.forEach { nullAnim ->
        if (nullAnim.visible) {
            val eval = Anm2PlaybackEngine.evaluateNull(nullAnim, tick, anim.frameNum)
            if (eval != null && eval.frame.visible) {
                val f = eval.frame
                val nullDef = actor.content.nulls.firstOrNull { it.id == nullAnim.nullId }
                val pos = Offset(center.x + f.xPosition * scale, center.y + f.yPosition * scale)

                // Draw null crosshair / circle
                drawCircle(
                    color = IsaacGold,
                    radius = 4f * scale.coerceIn(1f, 3f),
                    center = pos,
                    style = Stroke(width = 1.5f)
                )
                drawLine(
                    color = IsaacGold,
                    start = Offset(pos.x - 6f, pos.y),
                    end = Offset(pos.x + 6f, pos.y),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = IsaacGold,
                    start = Offset(pos.x, pos.y - 6f),
                    end = Offset(pos.x, pos.y + 6f),
                    strokeWidth = 1.2f
                )
            }
        }
    }
}
