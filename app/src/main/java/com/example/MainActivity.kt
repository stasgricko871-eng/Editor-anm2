package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.anm2editor.MainViewModel
import com.example.anm2editor.model.Anm2Frame
import com.example.anm2editor.ui.canvas.Anm2Canvas
import com.example.anm2editor.ui.dialogs.HelpDialog
import com.example.anm2editor.ui.dialogs.PresetsDialog
import com.example.anm2editor.ui.inspector.Anm2Inspector
import com.example.anm2editor.ui.inspector.InspectorSizeMode
import com.example.anm2editor.ui.spritesheet.SpritesheetPickerModal
import com.example.anm2editor.ui.timeline.Anm2Timeline
import com.example.anm2editor.ui.xmleditor.Anm2XmlViewerModal
import com.example.ui.theme.IsaacBone
import com.example.ui.theme.IsaacGold
import com.example.ui.theme.IsaacPrimary
import com.example.ui.theme.IsaacSurface
import com.example.ui.theme.MyApplicationTheme
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle opening .anm2 file passed via intent
        intent?.data?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    viewModel.loadFromInputStream(stream)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            MyApplicationTheme {
                MainEditorScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val actor by viewModel.actor.collectAsStateWithLifecycle()
    val currentAnimation by viewModel.currentAnimation.collectAsStateWithLifecycle()
    val currentTick by viewModel.currentTick.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val selectedLayerId by viewModel.selectedLayerId.collectAsStateWithLifecycle()

    var inspectorSizeMode by remember { mutableStateOf(InspectorSizeMode.COMPACT) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showXmlViewerModal by remember { mutableStateOf(false) }
    var activeCropPickerInfo by remember { mutableStateOf<Pair<Int, Anm2Frame>?>(null) }
    var pendingSpritesheetSlotId by remember { mutableIntStateOf(0) }

    // SAF Launchers
    val openAnm2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    viewModel.loadFromInputStream(stream)
                    Toast.makeText(context, "Файл .anm2 успешно загружен!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось загрузить .anm2: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val saveAnm2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outStream ->
                    OutputStreamWriter(outStream).use { writer ->
                        writer.write(viewModel.exportXml())
                    }
                }
                Toast.makeText(context, "Файл .anm2 успешно сохранён!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось сохранить файл: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val success = viewModel.setSpritesheetImage(pendingSpritesheetSlotId, uri)
            if (success) {
                Toast.makeText(context, "Пользовательский спрайтшит загружен!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Не удалось декодировать изображение", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Isaac ANM2",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = IsaacPrimary
                        )

                        Spacer(Modifier.width(12.dp))

                        // Quick Animation Switcher Dropdown
                        var showAnimMenu by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showAnimMenu = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = currentAnimation.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IsaacBone
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = IsaacBone,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showAnimMenu,
                                onDismissRequest = { showAnimMenu = false }
                            ) {
                                actor.animations.animationList.forEach { anim ->
                                    DropdownMenuItem(
                                        text = { Text(anim.name) },
                                        onClick = {
                                            viewModel.selectAnimation(anim.name)
                                            showAnimMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Presets / Templates
                    IconButton(
                        onClick = { showPresetsDialog = true },
                        modifier = Modifier.testTag("templates_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Шаблоны проектов", tint = IsaacBone)
                    }

                    // Open .anm2
                    IconButton(
                        onClick = { openAnm2Launcher.launch(arrayOf("*/*", "text/xml", "application/xml")) },
                        modifier = Modifier.testTag("open_file_button")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Открыть ANM2", tint = IsaacBone)
                    }

                    // Save / Export .anm2
                    IconButton(
                        onClick = { saveAnm2Launcher.launch("${currentAnimation.name.lowercase()}.anm2") },
                        modifier = Modifier.testTag("save_file_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить ANM2", tint = IsaacPrimary)
                    }

                    // XML Source Editor
                    IconButton(
                        onClick = { showXmlViewerModal = true },
                        modifier = Modifier.testTag("xml_source_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "Исходный XML", tint = IsaacGold)
                    }

                    // Help
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Справка", tint = IsaacBone)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IsaacSurface)
            )
        }
    ) { innerPadding ->
        if (isLandscape) {
            // Landscape Layout: Stage on left, Timeline & Inspector on right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Anm2Canvas(
                    actor = actor,
                    currentAnimation = currentAnimation,
                    currentTick = currentTick,
                    selectedLayerId = selectedLayerId,
                    spritesheetManager = viewModel.spritesheetManager,
                    onLayerSelected = { viewModel.selectLayer(it) },
                    onUpdateSelectedFramePosition = { dx, dy -> viewModel.updateSelectedFramePosition(dx, dy) },
                    onUpdateSelectedPivotPosition = { dx, dy -> viewModel.updateSelectedPivotPosition(dx, dy) },
                    modifier = Modifier.weight(1.1f).fillMaxHeight()
                )

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Anm2Timeline(
                        actor = actor,
                        currentAnimation = currentAnimation,
                        currentTick = currentTick,
                        isPlaying = isPlaying,
                        playbackSpeed = playbackSpeed,
                        selectedLayerId = selectedLayerId,
                        onTickChanged = { viewModel.setTick(it) },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onStepPrev = { viewModel.stepPrev() },
                        onStepNext = { viewModel.stepNext() },
                        onJumpStart = { viewModel.jumpStart() },
                        onJumpEnd = { viewModel.jumpEnd() },
                        onSpeedChanged = { viewModel.setPlaybackSpeed(it) },
                        onToggleLoop = { viewModel.toggleLoop() },
                        onLayerSelected = { viewModel.selectLayer(it) },
                        onToggleLayerVisibility = { viewModel.toggleLayerVisibility(it) },
                        onInsertKeyframe = { lid, tick -> viewModel.insertKeyframe(lid, tick) },
                        onDuplicateKeyframe = { lid, kidx -> viewModel.duplicateKeyframe(lid, kidx) },
                        onDeleteKeyframe = { lid, kidx -> viewModel.deleteKeyframe(lid, kidx) },
                        onUpdateKeyframeDelay = { lid, kidx, d -> viewModel.updateKeyframeDelay(lid, kidx, d) },
                        onToggleKeyframeInterpolated = { lid, kidx -> viewModel.toggleKeyframeInterpolated(lid, kidx) },
                        modifier = Modifier.weight(1f)
                    )

                    val landscapeInspectorModifier = when (inspectorSizeMode) {
                        InspectorSizeMode.COLLAPSED -> Modifier.wrapContentHeight()
                        InspectorSizeMode.COMPACT -> Modifier.height(180.dp)
                        InspectorSizeMode.EXPANDED -> Modifier.weight(1f)
                    }

                    Anm2Inspector(
                        actor = actor,
                        currentAnimation = currentAnimation,
                        currentTick = currentTick,
                        selectedLayerId = selectedLayerId,
                        sizeMode = inspectorSizeMode,
                        onSizeModeChanged = { inspectorSizeMode = it },
                        onUpdateFrame = { lid, kidx, upd -> viewModel.updateFrame(lid, kidx, upd) },
                        onOpenSpritesheetPicker = { sheetId, frame -> activeCropPickerInfo = sheetId to frame },
                        onAddLayer = { name, sheetId -> viewModel.addLayer(name, sheetId) },
                        onDeleteLayer = { viewModel.deleteLayer(it) },
                        onReorderLayer = { from, to -> viewModel.reorderLayer(from, to) },
                        onRenameLayer = { lid, name -> viewModel.renameLayer(lid, name) },
                        onSelectLayer = { viewModel.selectLayer(it) },
                        onSelectAnimation = { viewModel.selectAnimation(it) },
                        onAddAnimation = { name, fn, loop -> viewModel.addAnimation(name, fn, loop) },
                        onDuplicateAnimation = { viewModel.duplicateAnimation(it) },
                        onDeleteAnimation = { viewModel.deleteAnimation(it) },
                        onUpdateAnimProps = { name, fn, loop -> viewModel.updateAnimationProps(name, fn, loop) },
                        onSetDefaultAnimation = { viewModel.setDefaultAnimation(it) },
                        onAddSpritesheet = { viewModel.addSpritesheet(it) },
                        onPickSpritesheetImage = { sheetId ->
                            pendingSpritesheetSlotId = sheetId
                            pickImageLauncher.launch("image/*")
                        },
                        onAddNull = { viewModel.addNull(it) },
                        onDeleteNull = { viewModel.deleteNull(it) },
                        onAddTrigger = { evId, atFrame -> viewModel.addTrigger(evId, atFrame) },
                        onDeleteTrigger = { viewModel.deleteTrigger(it) },
                        modifier = landscapeInspectorModifier
                    )
                }
            }
        } else {
            // Portrait Layout: Canvas on top, Timeline in middle, Inspector on bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val canvasModifier = when (inspectorSizeMode) {
                    InspectorSizeMode.COLLAPSED -> Modifier.weight(1f).fillMaxWidth()
                    InspectorSizeMode.COMPACT -> Modifier.weight(1.35f).fillMaxWidth()
                    InspectorSizeMode.EXPANDED -> Modifier.weight(1f).fillMaxWidth()
                }

                val portraitInspectorModifier = when (inspectorSizeMode) {
                    InspectorSizeMode.COLLAPSED -> Modifier.wrapContentHeight().fillMaxWidth()
                    InspectorSizeMode.COMPACT -> Modifier.height(185.dp).fillMaxWidth()
                    InspectorSizeMode.EXPANDED -> Modifier.weight(1.1f).fillMaxWidth()
                }

                // Viewport Canvas
                Anm2Canvas(
                    actor = actor,
                    currentAnimation = currentAnimation,
                    currentTick = currentTick,
                    selectedLayerId = selectedLayerId,
                    spritesheetManager = viewModel.spritesheetManager,
                    onLayerSelected = { viewModel.selectLayer(it) },
                    onUpdateSelectedFramePosition = { dx, dy -> viewModel.updateSelectedFramePosition(dx, dy) },
                    onUpdateSelectedPivotPosition = { dx, dy -> viewModel.updateSelectedPivotPosition(dx, dy) },
                    modifier = canvasModifier
                )

                // Timeline
                Anm2Timeline(
                    actor = actor,
                    currentAnimation = currentAnimation,
                    currentTick = currentTick,
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                    selectedLayerId = selectedLayerId,
                    onTickChanged = { viewModel.setTick(it) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onStepPrev = { viewModel.stepPrev() },
                    onStepNext = { viewModel.stepNext() },
                    onJumpStart = { viewModel.jumpStart() },
                    onJumpEnd = { viewModel.jumpEnd() },
                    onSpeedChanged = { viewModel.setPlaybackSpeed(it) },
                    onToggleLoop = { viewModel.toggleLoop() },
                    onLayerSelected = { viewModel.selectLayer(it) },
                    onToggleLayerVisibility = { viewModel.toggleLayerVisibility(it) },
                    onInsertKeyframe = { lid, tick -> viewModel.insertKeyframe(lid, tick) },
                    onDuplicateKeyframe = { lid, kidx -> viewModel.duplicateKeyframe(lid, kidx) },
                    onDeleteKeyframe = { lid, kidx -> viewModel.deleteKeyframe(lid, kidx) },
                    onUpdateKeyframeDelay = { lid, kidx, d -> viewModel.updateKeyframeDelay(lid, kidx, d) },
                    onToggleKeyframeInterpolated = { lid, kidx -> viewModel.toggleKeyframeInterpolated(lid, kidx) },
                    modifier = Modifier.wrapContentHeight()
                )

                // Inspector Tabs
                Anm2Inspector(
                    actor = actor,
                    currentAnimation = currentAnimation,
                    currentTick = currentTick,
                    selectedLayerId = selectedLayerId,
                    sizeMode = inspectorSizeMode,
                    onSizeModeChanged = { inspectorSizeMode = it },
                    onUpdateFrame = { lid, kidx, upd -> viewModel.updateFrame(lid, kidx, upd) },
                    onOpenSpritesheetPicker = { sheetId, frame -> activeCropPickerInfo = sheetId to frame },
                    onAddLayer = { name, sheetId -> viewModel.addLayer(name, sheetId) },
                    onDeleteLayer = { viewModel.deleteLayer(it) },
                    onReorderLayer = { from, to -> viewModel.reorderLayer(from, to) },
                    onRenameLayer = { lid, name -> viewModel.renameLayer(lid, name) },
                    onSelectLayer = { viewModel.selectLayer(it) },
                    onSelectAnimation = { viewModel.selectAnimation(it) },
                    onAddAnimation = { name, fn, loop -> viewModel.addAnimation(name, fn, loop) },
                    onDuplicateAnimation = { viewModel.duplicateAnimation(it) },
                    onDeleteAnimation = { viewModel.deleteAnimation(it) },
                    onUpdateAnimProps = { name, fn, loop -> viewModel.updateAnimationProps(name, fn, loop) },
                    onSetDefaultAnimation = { viewModel.setDefaultAnimation(it) },
                    onAddSpritesheet = { viewModel.addSpritesheet(it) },
                    onPickSpritesheetImage = { sheetId ->
                        pendingSpritesheetSlotId = sheetId
                        pickImageLauncher.launch("image/*")
                    },
                    onAddNull = { viewModel.addNull(it) },
                    onDeleteNull = { viewModel.deleteNull(it) },
                    onAddTrigger = { evId, atFrame -> viewModel.addTrigger(evId, atFrame) },
                    onDeleteTrigger = { viewModel.deleteTrigger(it) },
                    modifier = portraitInspectorModifier
                )
            }
        }

        // Modals
        if (showPresetsDialog) {
            PresetsDialog(
                onSelectPreset = { preset -> viewModel.loadPreset(preset) },
                onDismiss = { showPresetsDialog = false }
            )
        }

        if (showHelpDialog) {
            HelpDialog(onDismiss = { showHelpDialog = false })
        }

        if (showXmlViewerModal) {
            Anm2XmlViewerModal(
                xmlContent = viewModel.exportXml(),
                onApplyXml = { newXml -> viewModel.loadFromXml(newXml) },
                onDismiss = { showXmlViewerModal = false }
            )
        }

        activeCropPickerInfo?.let { (sheetId, frame) ->
            SpritesheetPickerModal(
                sheetId = sheetId,
                initialFrame = frame,
                spritesheetManager = viewModel.spritesheetManager,
                onApply = { cropX, cropY, w, h, px, py ->
                    val lid = selectedLayerId
                    if (lid != null) {
                        val layerAnim = currentAnimation.layerAnimations.firstOrNull { it.layerId == lid }
                        val frames = layerAnim?.frames ?: emptyList()
                        val kidx = com.example.anm2editor.engine.Anm2PlaybackEngine.getKeyframeIndexAtTick(frames, currentTick)
                        if (kidx in frames.indices) {
                            val updated = frames[kidx].copy(
                                xCrop = cropX,
                                yCrop = cropY,
                                width = w,
                                height = h,
                                xPivot = px,
                                yPivot = py
                            )
                            viewModel.updateFrame(lid, kidx, updated)
                        }
                    }
                },
                onDismiss = { activeCropPickerInfo = null }
            )
        }
    }
}
