package com.example

import com.example.anm2editor.engine.Anm2PlaybackEngine
import com.example.anm2editor.engine.SampleProjects
import com.example.anm2editor.model.Anm2Frame
import com.example.anm2editor.model.Anm2LayerAnimation
import com.example.anm2editor.parser.Anm2Parser
import com.example.anm2editor.parser.Anm2Serializer
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testAnm2SerializationAndParsingRoundtrip() {
    val project = SampleProjects.createIsaacCharacterProject()
    val xml = Anm2Serializer.serialize(project)
    assertTrue(xml.contains("<AnimatedActor>"))
    assertTrue(xml.contains("<Spritesheet"))
    assertTrue(xml.contains("WalkDown"))

    val parsed = Anm2Parser.parse(xml)
    assertEquals(project.info.createdBy, parsed.info.createdBy)
    assertEquals(project.content.layers.size, parsed.content.layers.size)
    assertEquals(project.animations.animationList.size, parsed.animations.animationList.size)
    assertEquals("WalkDown", parsed.animations.defaultAnimation)
  }

  @Test
  fun testPlaybackEngineEvaluation() {
    val frames = listOf(
      Anm2Frame(xPosition = 0f, delay = 4, interpolated = true),
      Anm2Frame(xPosition = 20f, delay = 4, interpolated = false)
    )
    val layerAnim = Anm2LayerAnimation(layerId = 0, visible = true, frames = frames)

    // Tick 0: at start keyframe
    val eval0 = Anm2PlaybackEngine.evaluateLayer(layerAnim, 0, 8)
    assertNotNull(eval0)
    assertEquals(0f, eval0!!.frame.xPosition, 0.01f)

    // Tick 2: interpolated halfway (alpha = 2/4 = 0.5 -> xPosition = 10f)
    val eval2 = Anm2PlaybackEngine.evaluateLayer(layerAnim, 2, 8)
    assertNotNull(eval2)
    assertEquals(10f, eval2!!.frame.xPosition, 0.01f)
  }
}
