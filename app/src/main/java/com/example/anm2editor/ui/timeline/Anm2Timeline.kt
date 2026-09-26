package com.example.anm2editor.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun Anm2Timeline(
    actor: Anm2Actor,
    currentAnimation: Anm2Animation,
    currentTick: Int,
    isPlaying: Boolean,
    playbackSpeed: Float,
    selectedLayerId: Int?,
    onTickChanged: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStepPrev: () -> Unit,
    onStepNext: () -> Unit,
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onLayerSelected: (Int) -> Unit,
    onToggleLayerVisibility: (Int) -> Unit,
    onInsertKeyframe: (layerId: Int, tick: Int) -> Unit,
    onDuplicateKeyframe: (layerId: Int, keyframeIndex: Int) -> Unit,
    onDeleteKeyframe: (layerId: Int, keyframeIndex: Int) -> Unit,
    onUpdateKeyframeDelay: (layerId: Int, keyframeIndex: Int, newDelay: Int) -> Unit,
    onToggleKeyframeInterpolated: (layerId: Int, keyframeIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        color = IsaacSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // 1. Transport Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Playback buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onJumpStart,
                        modifier = Modifier.size(32.dp).testTag("jump_start_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "В начало",
                            tint = IsaacBone,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onStepPrev,
                        modifier = Modifier.size(32.dp).testTag("step_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Предыдущий кадр",
                            tint = IsaacBone,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(36.dp).testTag("play_pause_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = IsaacPrimary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                            tint = Color(0xFF002244),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onStepNext,
                        modifier = Modifier.size(32.dp).testTag("step_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Следующий кадр",
                            tint = IsaacBone,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onJumpEnd,
                        modifier = Modifier.size(32.dp).testTag("jump_end_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "В конец",
                            tint = IsaacBone,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Frame Counter Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = IsaacSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = String.format("%02d / %02d", currentTick, currentAnimation.frameNum - 1),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = IsaacPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Loop Toggle
                    IconButton(
                        onClick = onToggleLoop,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Зациклить",
                            tint = if (currentAnimation.loop) IsaacGold else IsaacOnSurfaceMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Speed Selector
                    var showSpeedMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { showSpeedMenu = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = IsaacBone
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = { Text("${spd}x") },
                                    onClick = {
                                        onSpeedChanged(spd)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Timeline Scrubber Slider
            Slider(
                value = currentTick.toFloat(),
                onValueChange = { onTickChanged(it.toInt().coerceIn(0, currentAnimation.frameNum - 1)) },
                valueRange = 0f..(currentAnimation.frameNum - 1).coerceAtLeast(1).toFloat(),
                steps = (currentAnimation.frameNum - 2).coerceAtLeast(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("timeline_scrubber"),
                colors = SliderDefaults.colors(
                    thumbColor = IsaacPrimary,
                    activeTrackColor = IsaacPrimary,
                    inactiveTrackColor = IsaacSurfaceVariant
                )
            )

            // 3. Multi-Track Layers View
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                // Header Ruler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "СЛОИ",
                        style = MaterialTheme.typography.labelSmall,
                        color = IsaacOnSurfaceMuted,
                        modifier = Modifier.width(90.dp)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        for (tick in 0 until currentAnimation.frameNum) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (tick == currentTick) IsaacPrimary.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable { onTickChanged(tick) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$tick",
                                    fontSize = 9.sp,
                                    color = if (tick == currentTick) IsaacPrimary else IsaacOnSurfaceMuted
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                // Track Rows
                actor.content.layers.forEach { layer ->
                    val layerAnim = currentAnimation.layerAnimations.firstOrNull { it.layerId == layer.id }
                    val isSelected = selectedLayerId == layer.id
                    val isVisible = layerAnim?.visible ?: true
                    val frames = layerAnim?.frames ?: emptyList()
                    val keyframePositions = remember(frames) { Anm2PlaybackEngine.getKeyframeTimelinePositions(frames) }
                    val activeKeyframeIdx = Anm2PlaybackEngine.getKeyframeIndexAtTick(frames, currentTick)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) IsaacSurfaceVariant else Color.Transparent)
                            .clickable { onLayerSelected(layer.id) }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Layer Label + Visibility Toggle
                        Row(
                            modifier = Modifier.width(90.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleLayerVisibility(layer.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Видимость слоя",
                                    tint = if (isVisible) IsaacBone else IsaacOnSurfaceMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = layer.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) IsaacPrimary else IsaacBone,
                                maxLines = 1
                            )
                        }

                        // Layer Keyframe Track Blocks
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var currentFrameIdx = 0
                            var runningTick = 0

                            for (kfIndex in frames.indices) {
                                val kf = frames[kfIndex]
                                val dur = kf.delay.coerceAtLeast(1)
                                val blockWidth = (dur * 22 + (dur - 1) * 1).dp
                                val isKfActive = (currentTick >= runningTick && (currentTick < runningTick + dur || kfIndex == frames.lastIndex))

                                Box(
                                    modifier = Modifier
                                        .width(blockWidth)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            when {
                                                isKfActive -> IsaacPrimaryDark
                                                kf.interpolated -> Color(0xFF6A1B9A) // Purple for interpolated
                                                else -> Color(0xFF37474F)
                                            }
                                        )
                                        .border(
                                            width = if (isKfActive) 1.5.dp else 0.5.dp,
                                            color = if (isKfActive) IsaacPrimary else Color(0xFF546E7A),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        .clickable { onTickChanged(runningTick) }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "#${kfIndex + 1} (${dur}т)",
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                        if (kf.interpolated) {
                                            Text(
                                                text = "∼",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = IsaacGold
                                            )
                                        }
                                    }
                                }

                                runningTick += dur
                            }
                        }
                    }
                }
            }

            // 4. Keyframe Quick Actions for Selected Layer
            if (selectedLayerId != null) {
                val layerAnim = currentAnimation.layerAnimations.firstOrNull { it.layerId == selectedLayerId }
                val frames = layerAnim?.frames ?: emptyList()
                val currentKfIndex = if (frames.isNotEmpty()) Anm2PlaybackEngine.getKeyframeIndexAtTick(frames, currentTick) else -1
                val currentKf = frames.getOrNull(currentKfIndex)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Insert Keyframe
                    Button(
                        onClick = { onInsertKeyframe(selectedLayerId, currentTick) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("insert_keyframe_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IsaacPrimaryDark)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Новый кадр", fontSize = 11.sp)
                    }

                    if (currentKf != null) {
                        // Delay steppers
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Длит: ", fontSize = 11.sp, color = IsaacBone)
                            IconButton(
                                onClick = { onUpdateKeyframeDelay(selectedLayerId, currentKfIndex, (currentKf.delay - 1).coerceAtLeast(1)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Меньше", modifier = Modifier.size(14.dp), tint = IsaacBone)
                            }
                            Text("${currentKf.delay}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IsaacPrimary)
                            IconButton(
                                onClick = { onUpdateKeyframeDelay(selectedLayerId, currentKfIndex, currentKf.delay + 1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Больше", modifier = Modifier.size(14.dp), tint = IsaacBone)
                            }
                        }

                        // Interpolated toggle
                        FilterChip(
                            selected = currentKf.interpolated,
                            onClick = { onToggleKeyframeInterpolated(selectedLayerId, currentKfIndex) },
                            label = { Text("Интерполяция", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )

                        // Duplicate & Delete
                        Row {
                            IconButton(
                                onClick = { onDuplicateKeyframe(selectedLayerId, currentKfIndex) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Копия", modifier = Modifier.size(14.dp), tint = IsaacBone)
                            }
                            IconButton(
                                onClick = { onDeleteKeyframe(selectedLayerId, currentKfIndex) },
                                modifier = Modifier.size(26.dp),
                                enabled = frames.size > 1
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(14.dp), tint = if (frames.size > 1) IsaacCrimson else IsaacOnSurfaceMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
