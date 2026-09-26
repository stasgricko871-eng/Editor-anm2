package com.example.anm2editor.parser

import com.example.anm2editor.model.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object Anm2Parser {

    fun parse(xmlString: String): Anm2Actor {
        return parse(ByteArrayInputStream(xmlString.toByteArray(Charsets.UTF_8)))
    }

    fun parse(inputStream: InputStream): Anm2Actor {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        val root = doc.documentElement

        var info = Anm2Info()
        val spritesheets = mutableListOf<Anm2Spritesheet>()
        val layers = mutableListOf<Anm2Layer>()
        val nulls = mutableListOf<Anm2Null>()
        val events = mutableListOf<Anm2Event>()
        var defaultAnimation = "WalkDown"
        val animations = mutableListOf<Anm2Animation>()

        val rootChildren = root.childNodes
        for (i in 0 until rootChildren.length) {
            val node = rootChildren.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val elem = node as Element

            when (elem.tagName) {
                "Info" -> {
                    info = Anm2Info(
                        createdBy = elem.getAttribute("CreatedBy").ifEmpty { "IsaacAnimationEditor" },
                        version = elem.getAttribute("Version").ifEmpty { "1.0" },
                        fps = elem.getAttribute("Fps").toIntOrNull() ?: 30
                    )
                }
                "Content" -> {
                    parseContent(elem, spritesheets, layers, nulls, events)
                }
                "Animations" -> {
                    val def = elem.getAttribute("DefaultAnimation")
                    if (def.isNotEmpty()) defaultAnimation = def

                    val animChildren = elem.childNodes
                    for (j in 0 until animChildren.length) {
                        val animNode = animChildren.item(j)
                        if (animNode.nodeType == Node.ELEMENT_NODE && animNode.nodeName == "Animation") {
                            animations.add(parseAnimation(animNode as Element))
                        }
                    }
                }
            }
        }

        return Anm2Actor(
            info = info,
            content = Anm2Content(
                spritesheets = spritesheets,
                layers = layers,
                nulls = nulls,
                events = events
            ),
            animations = Anm2Animations(
                defaultAnimation = defaultAnimation.ifEmpty { animations.firstOrNull()?.name ?: "Default" },
                animationList = animations
            )
        )
    }

    private fun parseContent(
        contentElem: Element,
        spritesheets: MutableList<Anm2Spritesheet>,
        layers: MutableList<Anm2Layer>,
        nulls: MutableList<Anm2Null>,
        events: MutableList<Anm2Event>
    ) {
        val children = contentElem.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val elem = node as Element

            when (elem.tagName) {
                "Spritesheets" -> {
                    val items = elem.childNodes
                    for (j in 0 until items.length) {
                        val n = items.item(j)
                        if (n.nodeType == Node.ELEMENT_NODE && n.nodeName == "Spritesheet") {
                            val el = n as Element
                            val id = el.getAttribute("Id").toIntOrNull() ?: spritesheets.size
                            val path = el.getAttribute("Path").ifEmpty { "sheet_$id.png" }
                            spritesheets.add(Anm2Spritesheet(id, path))
                        }
                    }
                }
                "Layers" -> {
                    val items = elem.childNodes
                    for (j in 0 until items.length) {
                        val n = items.item(j)
                        if (n.nodeType == Node.ELEMENT_NODE && n.nodeName == "Layer") {
                            val el = n as Element
                            val id = el.getAttribute("Id").toIntOrNull() ?: layers.size
                            val name = el.getAttribute("Name").ifEmpty { "Layer_$id" }
                            val sheetId = el.getAttribute("SpritesheetId").toIntOrNull() ?: 0
                            layers.add(Anm2Layer(id, name, sheetId))
                        }
                    }
                }
                "Nulls" -> {
                    val items = elem.childNodes
                    for (j in 0 until items.length) {
                        val n = items.item(j)
                        if (n.nodeType == Node.ELEMENT_NODE && n.nodeName == "Null") {
                            val el = n as Element
                            val id = el.getAttribute("Id").toIntOrNull() ?: nulls.size
                            val name = el.getAttribute("Name").ifEmpty { "Null_$id" }
                            nulls.add(Anm2Null(id, name))
                        }
                    }
                }
                "Events" -> {
                    val items = elem.childNodes
                    for (j in 0 until items.length) {
                        val n = items.item(j)
                        if (n.nodeType == Node.ELEMENT_NODE && n.nodeName == "Event") {
                            val el = n as Element
                            val id = el.getAttribute("Id").toIntOrNull() ?: events.size
                            val name = el.getAttribute("Name").ifEmpty { "Event_$id" }
                            events.add(Anm2Event(id, name))
                        }
                    }
                }
            }
        }
    }

    private fun parseAnimation(elem: Element): Anm2Animation {
        val name = elem.getAttribute("Name").ifEmpty { "Animation" }
        val frameNum = elem.getAttribute("FrameNum").toIntOrNull() ?: 16
        val loop = elem.getAttribute("Loop").toBooleanStrictOrNull() ?: true

        var rootAnimation = Anm2RootAnimation()
        val layerAnimations = mutableListOf<Anm2LayerAnimation>()
        val nullAnimations = mutableListOf<Anm2NullAnimation>()
        val triggers = mutableListOf<Anm2Trigger>()

        val children = elem.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val el = node as Element

            when (el.tagName) {
                "RootAnimation" -> {
                    rootAnimation = Anm2RootAnimation(frames = parseFrames(el))
                }
                "LayerAnimations" -> {
                    val layerNodes = el.childNodes
                    for (j in 0 until layerNodes.length) {
                        val ln = layerNodes.item(j)
                        if (ln.nodeType == Node.ELEMENT_NODE && ln.nodeName == "LayerAnimation") {
                            val layerEl = ln as Element
                            val layerId = layerEl.getAttribute("LayerId").toIntOrNull() ?: 0
                            val visible = layerEl.getAttribute("Visible").toBooleanStrictOrNull() ?: true
                            val frames = parseFrames(layerEl)
                            layerAnimations.add(Anm2LayerAnimation(layerId, visible, frames))
                        }
                    }
                }
                "NullAnimations" -> {
                    val nullNodes = el.childNodes
                    for (j in 0 until nullNodes.length) {
                        val nn = nullNodes.item(j)
                        if (nn.nodeType == Node.ELEMENT_NODE && nn.nodeName == "NullAnimation") {
                            val nullEl = nn as Element
                            val nullId = nullEl.getAttribute("NullId").toIntOrNull() ?: 0
                            val visible = nullEl.getAttribute("Visible").toBooleanStrictOrNull() ?: true
                            val frames = parseFrames(nullEl)
                            nullAnimations.add(Anm2NullAnimation(nullId, visible, frames))
                        }
                    }
                }
                "Triggers" -> {
                    val trigNodes = el.childNodes
                    for (j in 0 until trigNodes.length) {
                        val tn = trigNodes.item(j)
                        if (tn.nodeType == Node.ELEMENT_NODE && tn.nodeName == "Trigger") {
                            val trigEl = tn as Element
                            val eventId = trigEl.getAttribute("EventId").toIntOrNull() ?: 0
                            val atFrame = trigEl.getAttribute("AtFrame").toIntOrNull() ?: 0
                            triggers.add(Anm2Trigger(eventId, atFrame))
                        }
                    }
                }
            }
        }

        return Anm2Animation(
            name = name,
            frameNum = frameNum,
            loop = loop,
            rootAnimation = rootAnimation,
            layerAnimations = layerAnimations,
            nullAnimations = nullAnimations,
            triggers = triggers
        )
    }

    private fun parseFrames(parentElem: Element): List<Anm2Frame> {
        val frames = mutableListOf<Anm2Frame>()
        val children = parentElem.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.ELEMENT_NODE && node.nodeName == "Frame") {
                val el = node as Element
                frames.add(
                    Anm2Frame(
                        xPosition = el.getAttribute("XPosition").toFloatOrNull() ?: 0f,
                        yPosition = el.getAttribute("YPosition").toFloatOrNull() ?: 0f,
                        xPivot = el.getAttribute("XPivot").toFloatOrNull() ?: 0f,
                        yPivot = el.getAttribute("YPivot").toFloatOrNull() ?: 0f,
                        xCrop = el.getAttribute("XCrop").toIntOrNull() ?: 0,
                        yCrop = el.getAttribute("YCrop").toIntOrNull() ?: 0,
                        width = el.getAttribute("Width").toIntOrNull() ?: 32,
                        height = el.getAttribute("Height").toIntOrNull() ?: 32,
                        delay = el.getAttribute("Delay").toIntOrNull() ?: 1,
                        visible = el.getAttribute("Visible").toBooleanStrictOrNull() ?: true,
                        xScale = el.getAttribute("XScale").toFloatOrNull() ?: 100f,
                        yScale = el.getAttribute("YScale").toFloatOrNull() ?: 100f,
                        rotation = el.getAttribute("Rotation").toFloatOrNull() ?: 0f,
                        redTint = el.getAttribute("RedTint").toIntOrNull() ?: 255,
                        greenTint = el.getAttribute("GreenTint").toIntOrNull() ?: 255,
                        blueTint = el.getAttribute("BlueTint").toIntOrNull() ?: 255,
                        alphaTint = el.getAttribute("AlphaTint").toIntOrNull() ?: 255,
                        redOffset = el.getAttribute("RedOffset").toIntOrNull() ?: 0,
                        greenOffset = el.getAttribute("GreenOffset").toIntOrNull() ?: 0,
                        blueOffset = el.getAttribute("BlueOffset").toIntOrNull() ?: 0,
                        interpolated = el.getAttribute("Interpolated").toBooleanStrictOrNull() ?: false
                    )
                )
            }
        }
        return frames
    }
}
