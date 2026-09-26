package com.example.anm2editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.anm2editor.engine.Anm2PlaybackEngine
import com.example.anm2editor.engine.SampleProjects
import com.example.anm2editor.engine.SpritesheetManager
import com.example.anm2editor.model.*
import com.example.anm2editor.parser.Anm2Parser
import com.example.anm2editor.parser.Anm2Serializer
import com.example.anm2editor.ui.dialogs.PresetType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val spritesheetManager = SpritesheetManager(application)

    private val _actor = MutableStateFlow(SampleProjects.createIsaacCharacterProject())
    val actor: StateFlow<Anm2Actor> = _actor.asStateFlow()

    private val _currentAnimationName = MutableStateFlow("WalkDown")
    val currentAnimationName: StateFlow<String> = _currentAnimationName.asStateFlow()

    private val _currentTick = MutableStateFlow(0)
    val currentTick: StateFlow<Int> = _currentTick.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<Int?>(0)
    val selectedLayerId: StateFlow<Int?> = _selectedLayerId.asStateFlow()

    private var playbackJob: Job? = null

    init {
        startPlaybackLoop()
    }

    val currentAnimation: StateFlow<Anm2Animation> = combine(_actor, _currentAnimationName) { act, name ->
        act.animations.animationList.firstOrNull { it.name == name }
            ?: act.animations.animationList.firstOrNull()
            ?: Anm2Animation()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Anm2Animation())

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    val fps = _actor.value.info.fps.coerceIn(10, 120)
                    val speed = _playbackSpeed.value.coerceIn(0.1f, 5f)
                    val delayMs = (1000f / (fps * speed)).toLong().coerceAtLeast(10L)

                    delay(delayMs)

                    val anim = currentAnimation.value
                    val nextTick = _currentTick.value + 1
                    if (nextTick >= anim.frameNum) {
                        if (anim.loop) {
                            _currentTick.value = 0
                        } else {
                            _isPlaying.value = false
                            _currentTick.value = (anim.frameNum - 1).coerceAtLeast(0)
                        }
                    } else {
                        _currentTick.value = nextTick
                    }
                } else {
                    delay(50L)
                }
            }
        }
    }

    fun setTick(tick: Int) {
        val anim = currentAnimation.value
        _currentTick.value = tick.coerceIn(0, (anim.frameNum - 1).coerceAtLeast(0))
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun stepPrev() {
        _isPlaying.value = false
        val anim = currentAnimation.value
        val cur = _currentTick.value
        _currentTick.value = if (cur > 0) cur - 1 else if (anim.loop) (anim.frameNum - 1).coerceAtLeast(0) else 0
    }

    fun stepNext() {
        _isPlaying.value = false
        val anim = currentAnimation.value
        val cur = _currentTick.value
        _currentTick.value = if (cur < anim.frameNum - 1) cur + 1 else if (anim.loop) 0 else cur
    }

    fun jumpStart() {
        _currentTick.value = 0
    }

    fun jumpEnd() {
        val anim = currentAnimation.value
        _currentTick.value = (anim.frameNum - 1).coerceAtLeast(0)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun toggleLoop() {
        val anim = currentAnimation.value
        updateAnimationProps(anim.name, anim.frameNum, !anim.loop)
    }

    fun selectLayer(layerId: Int) {
        _selectedLayerId.value = layerId
    }

    fun toggleLayerVisibility(layerId: Int) {
        val act = _actor.value
        val anim = currentAnimation.value
        val updatedLayerAnims = anim.layerAnimations.map { la ->
            if (la.layerId == layerId) la.copy(visible = !la.visible) else la
        }
        updateAnimationInActor(anim.copy(layerAnimations = updatedLayerAnims))
    }

    fun updateSelectedFramePosition(dx: Float, dy: Float) {
        val layerId = _selectedLayerId.value ?: return
        val anim = currentAnimation.value
        val layerAnim = anim.layerAnimations.firstOrNull { it.layerId == layerId } ?: return
        val frames = layerAnim.frames
        if (frames.isEmpty()) return

        val kfIdx = Anm2PlaybackEngine.getKeyframeIndexAtTick(frames, _currentTick.value)
        val kf = frames[kfIdx]
        val updated = kf.copyWithOffset(dx, dy)
        updateFrame(layerId, kfIdx, updated)
    }

    fun updateSelectedPivotPosition(dx: Float, dy: Float) {
        val layerId = _selectedLayerId.value ?: return
        val anim = currentAnimation.value
        val layerAnim = anim.layerAnimations.firstOrNull { it.layerId == layerId } ?: return
        val frames = layerAnim.frames
        if (frames.isEmpty()) return

        val kfIdx = Anm2PlaybackEngine.getKeyframeIndexAtTick(frames, _currentTick.value)
        val kf = frames[kfIdx]
        val updated = kf.copy(xPivot = kf.xPivot + dx, yPivot = kf.yPivot + dy)
        updateFrame(layerId, kfIdx, updated)
    }

    fun updateFrame(layerId: Int, keyframeIndex: Int, updated: Anm2Frame) {
        val anim = currentAnimation.value
        val updatedLayerAnims = anim.layerAnimations.map { la ->
            if (la.layerId == layerId && keyframeIndex in la.frames.indices) {
                val newFrames = la.frames.toMutableList()
                newFrames[keyframeIndex] = updated
                la.copy(frames = newFrames)
            } else la
        }
        updateAnimationInActor(anim.copy(layerAnimations = updatedLayerAnims))
    }

    fun insertKeyframe(layerId: Int, tick: Int) {
        val anim = currentAnimation.value
        val updatedLayerAnims = anim.layerAnimations.map { la ->
            if (la.layerId == layerId) {
                val newFrames = Anm2PlaybackEngine.insertKeyframeAt(la.frames, tick)
                la.copy(frames = newFrames)
            } else la
        }
        updateAnimationInActor(anim.copy(layerAnimations = updatedLayerAnims))
    }

    fun duplicateKeyframe(layerId: Int, keyframeIndex: Int) {
        val anim = currentAnimation.value
        val updatedLayerAnims = anim.layerAnimations.map { la ->
            if (la.layerId == layerId) {
                val newFrames = Anm2PlaybackEngine.duplicateKeyframe(la.frames, keyframeIndex)
                la.copy(frames = newFrames)
            } else la
        }
        updateAnimationInActor(anim.copy(layerAnimations = updatedLayerAnims))
    }

    fun deleteKeyframe(layerId: Int, keyframeIndex: Int) {
        val anim = currentAnimation.value
        val updatedLayerAnims = anim.layerAnimations.map { la ->
            if (la.layerId == layerId) {
                val newFrames = Anm2PlaybackEngine.deleteKeyframe(la.frames, keyframeIndex)
                la.copy(frames = newFrames)
            } else la
        }
        updateAnimationInActor(anim.copy(layerAnimations = updatedLayerAnims))
    }

    fun updateKeyframeDelay(layerId: Int, keyframeIndex: Int, newDelay: Int) {
        val anim = currentAnimation.value
        val layerAnim = anim.layerAnimations.firstOrNull { it.layerId == layerId } ?: return
        val frame = layerAnim.frames.getOrNull(keyframeIndex) ?: return
        updateFrame(layerId, keyframeIndex, frame.copy(delay = newDelay.coerceAtLeast(1)))
    }

    fun toggleKeyframeInterpolated(layerId: Int, keyframeIndex: Int) {
        val anim = currentAnimation.value
        val layerAnim = anim.layerAnimations.firstOrNull { it.layerId == layerId } ?: return
        val frame = layerAnim.frames.getOrNull(keyframeIndex) ?: return
        updateFrame(layerId, keyframeIndex, frame.copy(interpolated = !frame.interpolated))
    }

    fun selectAnimation(name: String) {
        _currentAnimationName.value = name
        _currentTick.value = 0
    }

    fun addAnimation(name: String, frameNum: Int, loop: Boolean) {
        val act = _actor.value
        val newLayerAnims = act.content.layers.map { l ->
            Anm2LayerAnimation(layerId = l.id, visible = true, frames = listOf(Anm2Frame(delay = frameNum)))
        }
        val newAnim = Anm2Animation(name = name, frameNum = frameNum, loop = loop, layerAnimations = newLayerAnims)
        val updatedAnims = act.animations.animationList + newAnim
        _actor.value = act.copy(animations = act.animations.copy(animationList = updatedAnims))
        _currentAnimationName.value = name
        _currentTick.value = 0
    }

    fun duplicateAnimation(name: String) {
        val act = _actor.value
        val source = act.animations.animationList.firstOrNull { it.name == name } ?: return
        val copyName = "${name}_Copy"
        val duplicated = source.copy(name = copyName)
        val updatedList = act.animations.animationList + duplicated
        _actor.value = act.copy(animations = act.animations.copy(animationList = updatedList))
        _currentAnimationName.value = copyName
        _currentTick.value = 0
    }

    fun deleteAnimation(name: String) {
        val act = _actor.value
        if (act.animations.animationList.size <= 1) return
        val updatedList = act.animations.animationList.filterNot { it.name == name }
        val newSelected = if (_currentAnimationName.value == name) updatedList.first().name else _currentAnimationName.value
        _actor.value = act.copy(animations = act.animations.copy(animationList = updatedList))
        _currentAnimationName.value = newSelected
        _currentTick.value = 0
    }

    fun updateAnimationProps(newName: String, newFrameNum: Int, loop: Boolean) {
        val anim = currentAnimation.value
        val updated = anim.copy(name = newName, frameNum = newFrameNum.coerceAtLeast(1), loop = loop)
        updateAnimationInActor(updated)
        _currentAnimationName.value = newName
    }

    fun setDefaultAnimation(name: String) {
        val act = _actor.value
        _actor.value = act.copy(animations = act.animations.copy(defaultAnimation = name))
    }

    fun addLayer(name: String, sheetId: Int) {
        val act = _actor.value
        val newId = (act.content.layers.maxOfOrNull { it.id } ?: -1) + 1
        val newLayer = Anm2Layer(id = newId, name = name, spritesheetId = sheetId)
        val updatedLayers = act.content.layers + newLayer

        // Add layer track to all animations
        val updatedAnims = act.animations.animationList.map { anim ->
            val track = Anm2LayerAnimation(
                layerId = newId,
                visible = true,
                frames = listOf(Anm2Frame(delay = anim.frameNum))
            )
            anim.copy(layerAnimations = anim.layerAnimations + track)
        }

        _actor.value = act.copy(
            content = act.content.copy(layers = updatedLayers),
            animations = act.animations.copy(animationList = updatedAnims)
        )
        _selectedLayerId.value = newId
    }

    fun deleteLayer(layerId: Int) {
        val act = _actor.value
        if (act.content.layers.size <= 1) return
        val updatedLayers = act.content.layers.filterNot { it.id == layerId }
        val updatedAnims = act.animations.animationList.map { anim ->
            anim.copy(layerAnimations = anim.layerAnimations.filterNot { it.layerId == layerId })
        }
        _actor.value = act.copy(
            content = act.content.copy(layers = updatedLayers),
            animations = act.animations.copy(animationList = updatedAnims)
        )
        if (_selectedLayerId.value == layerId) {
            _selectedLayerId.value = updatedLayers.firstOrNull()?.id
        }
    }

    fun reorderLayer(fromIdx: Int, toIdx: Int) {
        val act = _actor.value
        val list = act.content.layers.toMutableList()
        if (fromIdx in list.indices && toIdx in list.indices) {
            val item = list.removeAt(fromIdx)
            list.add(toIdx, item)
            _actor.value = act.copy(content = act.content.copy(layers = list))
        }
    }

    fun renameLayer(layerId: Int, newName: String) {
        val act = _actor.value
        val updated = act.content.layers.map { if (it.id == layerId) it.copy(name = newName) else it }
        _actor.value = act.copy(content = act.content.copy(layers = updated))
    }

    fun addSpritesheet(path: String) {
        val act = _actor.value
        val newId = (act.content.spritesheets.maxOfOrNull { it.id } ?: -1) + 1
        val newSheet = Anm2Spritesheet(id = newId, path = path)
        _actor.value = act.copy(content = act.content.copy(spritesheets = act.content.spritesheets + newSheet))
    }

    fun setSpritesheetImage(sheetId: Int, uri: Uri): Boolean {
        return spritesheetManager.setSpritesheetUri(sheetId, uri)
    }

    fun addNull(name: String) {
        val act = _actor.value
        val newId = (act.content.nulls.maxOfOrNull { it.id } ?: -1) + 1
        val updatedNulls = act.content.nulls + Anm2Null(newId, name)
        val updatedAnims = act.animations.animationList.map { anim ->
            val track = Anm2NullAnimation(newId, true, listOf(Anm2Frame(delay = anim.frameNum)))
            anim.copy(nullAnimations = anim.nullAnimations + track)
        }
        _actor.value = act.copy(
            content = act.content.copy(nulls = updatedNulls),
            animations = act.animations.copy(animationList = updatedAnims)
        )
    }

    fun deleteNull(nullId: Int) {
        val act = _actor.value
        val updatedNulls = act.content.nulls.filterNot { it.id == nullId }
        val updatedAnims = act.animations.animationList.map { anim ->
            anim.copy(nullAnimations = anim.nullAnimations.filterNot { it.nullId == nullId })
        }
        _actor.value = act.copy(
            content = act.content.copy(nulls = updatedNulls),
            animations = act.animations.copy(animationList = updatedAnims)
        )
    }

    fun addTrigger(eventId: Int, atFrame: Int) {
        val anim = currentAnimation.value
        val updated = anim.copy(triggers = anim.triggers + Anm2Trigger(eventId, atFrame))
        updateAnimationInActor(updated)
    }

    fun deleteTrigger(index: Int) {
        val anim = currentAnimation.value
        if (index in anim.triggers.indices) {
            val list = anim.triggers.toMutableList()
            list.removeAt(index)
            updateAnimationInActor(anim.copy(triggers = list))
        }
    }

    fun loadPreset(type: PresetType) {
        val newActor = when (type) {
            PresetType.ISAAC_CHARACTER -> SampleProjects.createIsaacCharacterProject()
            PresetType.TEAR_PROJECTILE -> SampleProjects.createTearProjectileProject()
            PresetType.ITEM_PEDESTAL -> SampleProjects.createItemPedestalProject()
            PresetType.EMPTY -> SampleProjects.createEmptyProject()
        }
        _actor.value = newActor
        _currentAnimationName.value = newActor.animations.defaultAnimation.ifEmpty {
            newActor.animations.animationList.firstOrNull()?.name ?: "Default"
        }
        _selectedLayerId.value = newActor.content.layers.firstOrNull()?.id
        _currentTick.value = 0
        _isPlaying.value = true
    }

    fun loadFromXml(xml: String) {
        val parsed = Anm2Parser.parse(xml)
        _actor.value = parsed
        _currentAnimationName.value = parsed.animations.defaultAnimation.ifEmpty {
            parsed.animations.animationList.firstOrNull()?.name ?: "Default"
        }
        _selectedLayerId.value = parsed.content.layers.firstOrNull()?.id
        _currentTick.value = 0
    }

    fun loadFromInputStream(stream: InputStream) {
        val parsed = Anm2Parser.parse(stream)
        _actor.value = parsed
        _currentAnimationName.value = parsed.animations.defaultAnimation.ifEmpty {
            parsed.animations.animationList.firstOrNull()?.name ?: "Default"
        }
        _selectedLayerId.value = parsed.content.layers.firstOrNull()?.id
        _currentTick.value = 0
    }

    fun exportXml(): String {
        return Anm2Serializer.serialize(_actor.value)
    }

    private fun updateAnimationInActor(updated: Anm2Animation) {
        val act = _actor.value
        val updatedList = act.animations.animationList.map { if (it.name == updated.name) updated else it }
        _actor.value = act.copy(animations = act.animations.copy(animationList = updatedList))
    }
}
