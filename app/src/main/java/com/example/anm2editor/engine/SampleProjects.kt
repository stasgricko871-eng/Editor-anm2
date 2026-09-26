package com.example.anm2editor.engine

import com.example.anm2editor.model.*

object SampleProjects {

    fun createIsaacCharacterProject(): Anm2Actor {
        val spritesheet = Anm2Spritesheet(id = 0, path = "isaac.png")
        val headLayer = Anm2Layer(id = 0, name = "Head", spritesheetId = 0)
        val bodyLayer = Anm2Layer(id = 1, name = "Body", spritesheetId = 0)
        val tearNull = Anm2Null(id = 0, name = "TearPoint")
        val shootEvent = Anm2Event(id = 0, name = "Shoot")
        val stepEvent = Anm2Event(id = 1, name = "Step")

        // WalkDown Animation (16 frames, 4 keyframes of delay 4)
        val walkDownHeadFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 0, width = 32, height = 32, delay = 4, interpolated = false),
            Anm2Frame(xPosition = 0f, yPosition = -11f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 0, width = 32, height = 32, delay = 4, interpolated = false),
            Anm2Frame(xPosition = 0f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 0, width = 32, height = 32, delay = 4, interpolated = false),
            Anm2Frame(xPosition = 0f, yPosition = -11f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 0, width = 32, height = 32, delay = 4, interpolated = false)
        )

        val walkDownBodyFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = 2f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 32, width = 32, height = 32, delay = 4),
            Anm2Frame(xPosition = 0f, yPosition = 2f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 32, width = 32, height = 32, delay = 4),
            Anm2Frame(xPosition = 0f, yPosition = 2f, xPivot = 16f, yPivot = 16f, xCrop = 64, yCrop = 32, width = 32, height = 32, delay = 4),
            Anm2Frame(xPosition = 0f, yPosition = 2f, xPivot = 16f, yPivot = 16f, xCrop = 96, yCrop = 32, width = 32, height = 32, delay = 4)
        )

        val walkDownAnim = Anm2Animation(
            name = "WalkDown",
            frameNum = 16,
            loop = true,
            layerAnimations = listOf(
                Anm2LayerAnimation(layerId = 0, visible = true, frames = walkDownHeadFrames),
                Anm2LayerAnimation(layerId = 1, visible = true, frames = walkDownBodyFrames)
            ),
            nullAnimations = listOf(
                Anm2NullAnimation(
                    nullId = 0,
                    visible = true,
                    frames = listOf(Anm2Frame(xPosition = 0f, yPosition = 0f, delay = 16))
                )
            ),
            triggers = listOf(
                Anm2Trigger(eventId = 1, atFrame = 4),
                Anm2Trigger(eventId = 1, atFrame = 12)
            )
        )

        // ShootDown Animation (8 frames)
        val shootHeadFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = -13f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 0, width = 32, height = 32, delay = 3),
            Anm2Frame(xPosition = 0f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 0, width = 32, height = 32, delay = 5)
        )
        val shootBodyFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = 2f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 32, width = 32, height = 32, delay = 8)
        )

        val shootAnim = Anm2Animation(
            name = "ShootDown",
            frameNum = 8,
            loop = false,
            layerAnimations = listOf(
                Anm2LayerAnimation(layerId = 0, visible = true, frames = shootHeadFrames),
                Anm2LayerAnimation(layerId = 1, visible = true, frames = shootBodyFrames)
            ),
            nullAnimations = listOf(
                Anm2NullAnimation(
                    nullId = 0,
                    visible = true,
                    frames = listOf(Anm2Frame(xPosition = 0f, yPosition = 0f, delay = 8))
                )
            ),
            triggers = listOf(
                Anm2Trigger(eventId = 0, atFrame = 1)
            )
        )

        // WalkSide Animation
        val walkSideHeadFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 96, yCrop = 0, width = 32, height = 32, delay = 8)
        )
        val walkSideAnim = Anm2Animation(
            name = "WalkSide",
            frameNum = 16,
            loop = true,
            layerAnimations = listOf(
                Anm2LayerAnimation(layerId = 0, visible = true, frames = walkSideHeadFrames),
                Anm2LayerAnimation(layerId = 1, visible = true, frames = walkDownBodyFrames)
            )
        )

        // Hurt Animation (flashing red, shake)
        val hurtHeadFrames = listOf(
            Anm2Frame(xPosition = -2f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 64, yCrop = 0, width = 32, height = 32, delay = 2, redTint = 255, greenTint = 80, blueTint = 80),
            Anm2Frame(xPosition = 2f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 64, yCrop = 0, width = 32, height = 32, delay = 2, redTint = 255, greenTint = 180, blueTint = 180),
            Anm2Frame(xPosition = 0f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 64, yCrop = 0, width = 32, height = 32, delay = 4)
        )
        val hurtAnim = Anm2Animation(
            name = "Hurt",
            frameNum = 8,
            loop = false,
            layerAnimations = listOf(
                Anm2LayerAnimation(layerId = 0, visible = true, frames = hurtHeadFrames),
                Anm2LayerAnimation(layerId = 1, visible = true, frames = shootBodyFrames)
            )
        )

        return Anm2Actor(
            info = Anm2Info("IsaacMobileAnm2Editor", "1.0", 30),
            content = Anm2Content(
                spritesheets = listOf(spritesheet),
                layers = listOf(headLayer, bodyLayer),
                nulls = listOf(tearNull),
                events = listOf(shootEvent, stepEvent)
            ),
            animations = Anm2Animations(
                defaultAnimation = "WalkDown",
                animationList = listOf(walkDownAnim, shootAnim, walkSideAnim, hurtAnim)
            )
        )
    }

    fun createTearProjectileProject(): Anm2Actor {
        val sheet = Anm2Spritesheet(id = 0, path = "tear.png")
        val tearLayer = Anm2Layer(id = 0, name = "Tear", spritesheetId = 0)
        val hitEvent = Anm2Event(id = 0, name = "Pop")

        val flyFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = 0f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 64, width = 32, height = 32, delay = 4, xScale = 100f, yScale = 100f, interpolated = true),
            Anm2Frame(xPosition = 0f, yPosition = -3f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 64, width = 32, height = 32, delay = 4, xScale = 110f, yScale = 95f, interpolated = true),
            Anm2Frame(xPosition = 0f, yPosition = 0f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 64, width = 32, height = 32, delay = 4, xScale = 100f, yScale = 100f, interpolated = true)
        )

        val popFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = 0f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 64, width = 32, height = 32, delay = 2, xScale = 90f, yScale = 90f),
            Anm2Frame(xPosition = 0f, yPosition = 0f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 64, width = 32, height = 32, delay = 3, xScale = 120f, yScale = 120f, alphaTint = 200),
            Anm2Frame(xPosition = 0f, yPosition = 0f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 64, width = 32, height = 32, delay = 3, xScale = 140f, yScale = 140f, alphaTint = 0)
        )

        return Anm2Actor(
            content = Anm2Content(
                spritesheets = listOf(sheet),
                layers = listOf(tearLayer),
                events = listOf(hitEvent)
            ),
            animations = Anm2Animations(
                defaultAnimation = "Fly",
                animationList = listOf(
                    Anm2Animation(name = "Fly", frameNum = 12, loop = true, layerAnimations = listOf(Anm2LayerAnimation(0, true, flyFrames))),
                    Anm2Animation(name = "Pop", frameNum = 8, loop = false, layerAnimations = listOf(Anm2LayerAnimation(0, true, popFrames)), triggers = listOf(Anm2Trigger(0, 0)))
                )
            )
        )
    }

    fun createItemPedestalProject(): Anm2Actor {
        val sheet = Anm2Spritesheet(id = 0, path = "items.png")
        val pedestalLayer = Anm2Layer(id = 0, name = "Pedestal", spritesheetId = 0)
        val itemLayer = Anm2Layer(id = 1, name = "Item", spritesheetId = 0)

        // Smooth floating item with interpolation!
        val pedestalFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = 8f, xPivot = 16f, yPivot = 16f, xCrop = 0, yCrop = 96, width = 32, height = 32, delay = 20)
        )
        val itemFloatingFrames = listOf(
            Anm2Frame(xPosition = 0f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 96, width = 32, height = 32, delay = 10, interpolated = true),
            Anm2Frame(xPosition = 0f, yPosition = -18f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 96, width = 32, height = 32, delay = 10, interpolated = true),
            Anm2Frame(xPosition = 0f, yPosition = -12f, xPivot = 16f, yPivot = 16f, xCrop = 32, yCrop = 96, width = 32, height = 32, delay = 10, interpolated = true)
        )

        return Anm2Actor(
            content = Anm2Content(
                spritesheets = listOf(sheet),
                layers = listOf(pedestalLayer, itemLayer)
            ),
            animations = Anm2Animations(
                defaultAnimation = "Float",
                animationList = listOf(
                    Anm2Animation(
                        name = "Float",
                        frameNum = 20,
                        loop = true,
                        layerAnimations = listOf(
                            Anm2LayerAnimation(0, true, pedestalFrames),
                            Anm2LayerAnimation(1, true, itemFloatingFrames)
                        )
                    )
                )
            )
        )
    }

    fun createEmptyProject(): Anm2Actor {
        return Anm2Actor(
            info = Anm2Info("IsaacMobileAnm2Editor", "1.0", 30),
            content = Anm2Content(
                spritesheets = listOf(Anm2Spritesheet(0, "spritesheet.png")),
                layers = listOf(Anm2Layer(0, "Layer_0", 0))
            ),
            animations = Anm2Animations(
                defaultAnimation = "Idle",
                animationList = listOf(
                    Anm2Animation(
                        name = "Idle",
                        frameNum = 8,
                        loop = true,
                        layerAnimations = listOf(
                            Anm2LayerAnimation(
                                layerId = 0,
                                visible = true,
                                frames = listOf(
                                    Anm2Frame(
                                        xPosition = 0f,
                                        yPosition = 0f,
                                        xPivot = 16f,
                                        yPivot = 16f,
                                        xCrop = 0,
                                        yCrop = 0,
                                        width = 32,
                                        height = 32,
                                        delay = 8
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
