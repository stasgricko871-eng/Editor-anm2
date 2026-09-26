package com.example.anm2editor.model

data class Anm2Actor(
    val info: Anm2Info = Anm2Info(),
    val content: Anm2Content = Anm2Content(),
    val animations: Anm2Animations = Anm2Animations()
)

data class Anm2Info(
    val createdBy: String = "IsaacMobileAnm2Editor",
    val version: String = "1.0",
    val fps: Int = 30
)

data class Anm2Content(
    val spritesheets: List<Anm2Spritesheet> = emptyList(),
    val layers: List<Anm2Layer> = emptyList(),
    val nulls: List<Anm2Null> = emptyList(),
    val events: List<Anm2Event> = emptyList()
)

data class Anm2Spritesheet(
    val id: Int = 0,
    val path: String = "isaac.png"
)

data class Anm2Layer(
    val id: Int = 0,
    val name: String = "Layer",
    val spritesheetId: Int = 0
)

data class Anm2Null(
    val id: Int = 0,
    val name: String = "Null"
)

data class Anm2Event(
    val id: Int = 0,
    val name: String = "Event"
)

data class Anm2Animations(
    val defaultAnimation: String = "WalkDown",
    val animationList: List<Anm2Animation> = emptyList()
)

data class Anm2Animation(
    val name: String = "WalkDown",
    val frameNum: Int = 16,
    val loop: Boolean = true,
    val rootAnimation: Anm2RootAnimation = Anm2RootAnimation(),
    val layerAnimations: List<Anm2LayerAnimation> = emptyList(),
    val nullAnimations: List<Anm2NullAnimation> = emptyList(),
    val triggers: List<Anm2Trigger> = emptyList()
)

data class Anm2RootAnimation(
    val frames: List<Anm2Frame> = emptyList()
)

data class Anm2LayerAnimation(
    val layerId: Int = 0,
    val visible: Boolean = true,
    val frames: List<Anm2Frame> = emptyList()
)

data class Anm2NullAnimation(
    val nullId: Int = 0,
    val visible: Boolean = true,
    val frames: List<Anm2Frame> = emptyList()
)

data class Anm2Trigger(
    val eventId: Int = 0,
    val atFrame: Int = 0
)

data class Anm2Frame(
    val xPosition: Float = 0f,
    val yPosition: Float = 0f,
    val xPivot: Float = 0f,
    val yPivot: Float = 0f,
    val xCrop: Int = 0,
    val yCrop: Int = 0,
    val width: Int = 32,
    val height: Int = 32,
    val delay: Int = 1,
    val visible: Boolean = true,
    val xScale: Float = 100f,
    val yScale: Float = 100f,
    val rotation: Float = 0f,
    val redTint: Int = 255,
    val greenTint: Int = 255,
    val blueTint: Int = 255,
    val alphaTint: Int = 255,
    val redOffset: Int = 0,
    val greenOffset: Int = 0,
    val blueOffset: Int = 0,
    val interpolated: Boolean = false
) {
    fun copyWithOffset(dx: Float, dy: Float): Anm2Frame =
        copy(xPosition = xPosition + dx, yPosition = yPosition + dy)
}
