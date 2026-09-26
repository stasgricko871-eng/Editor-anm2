package com.example.anm2editor.parser

import com.example.anm2editor.model.*
import java.util.Locale

object Anm2Serializer {

    fun serialize(actor: Anm2Actor): String {
        val sb = StringBuilder()
        sb.append("<AnimatedActor>\n")

        // Info
        sb.append("    <Info CreatedBy=\"${escapeXml(actor.info.createdBy)}\" ")
        sb.append("Version=\"${escapeXml(actor.info.version)}\" ")
        sb.append("Fps=\"${actor.info.fps}\"/>\n")

        // Content
        sb.append("    <Content>\n")

        // Spritesheets
        sb.append("        <Spritesheets>\n")
        actor.content.spritesheets.forEach { s ->
            sb.append("            <Spritesheet Id=\"${s.id}\" Path=\"${escapeXml(s.path)}\"/>\n")
        }
        sb.append("        </Spritesheets>\n")

        // Layers
        sb.append("        <Layers>\n")
        actor.content.layers.forEach { l ->
            sb.append("            <Layer Id=\"${l.id}\" Name=\"${escapeXml(l.name)}\" SpritesheetId=\"${l.spritesheetId}\"/>\n")
        }
        sb.append("        </Layers>\n")

        // Nulls
        sb.append("        <Nulls>\n")
        actor.content.nulls.forEach { n ->
            sb.append("            <Null Id=\"${n.id}\" Name=\"${escapeXml(n.name)}\"/>\n")
        }
        sb.append("        </Nulls>\n")

        // Events
        sb.append("        <Events>\n")
        actor.content.events.forEach { e ->
            sb.append("            <Event Id=\"${e.id}\" Name=\"${escapeXml(e.name)}\"/>\n")
        }
        sb.append("        </Events>\n")

        sb.append("    </Content>\n")

        // Animations
        val defaultAnim = actor.animations.defaultAnimation.ifEmpty {
            actor.animations.animationList.firstOrNull()?.name ?: "Default"
        }
        sb.append("    <Animations DefaultAnimation=\"${escapeXml(defaultAnim)}\">\n")

        actor.animations.animationList.forEach { anim ->
            sb.append("        <Animation Name=\"${escapeXml(anim.name)}\" FrameNum=\"${anim.frameNum}\" Loop=\"${anim.loop}\">\n")

            // RootAnimation
            sb.append("            <RootAnimation>\n")
            anim.rootAnimation.frames.forEach { f ->
                sb.append("                ${serializeFrame(f)}\n")
            }
            sb.append("            </RootAnimation>\n")

            // LayerAnimations
            sb.append("            <LayerAnimations>\n")
            anim.layerAnimations.forEach { la ->
                sb.append("                <LayerAnimation LayerId=\"${la.layerId}\" Visible=\"${la.visible}\">\n")
                la.frames.forEach { f ->
                    sb.append("                    ${serializeFrame(f)}\n")
                }
                sb.append("                </LayerAnimation>\n")
            }
            sb.append("            </LayerAnimations>\n")

            // NullAnimations
            sb.append("            <NullAnimations>\n")
            anim.nullAnimations.forEach { na ->
                sb.append("                <NullAnimation NullId=\"${na.nullId}\" Visible=\"${na.visible}\">\n")
                na.frames.forEach { f ->
                    sb.append("                    ${serializeFrame(f)}\n")
                }
                sb.append("                </NullAnimation>\n")
            }
            sb.append("            </NullAnimations>\n")

            // Triggers
            sb.append("            <Triggers>\n")
            anim.triggers.forEach { tr ->
                sb.append("                <Trigger EventId=\"${tr.eventId}\" AtFrame=\"${tr.atFrame}\"/>\n")
            }
            sb.append("            </Triggers>\n")

            sb.append("        </Animation>\n")
        }

        sb.append("    </Animations>\n")
        sb.append("</AnimatedActor>\n")
        return sb.toString()
    }

    private fun serializeFrame(f: Anm2Frame): String {
        return String.format(
            Locale.US,
            "<Frame XPosition=\"%.2f\" YPosition=\"%.2f\" XPivot=\"%.2f\" YPivot=\"%.2f\" " +
                    "XCrop=\"%d\" YCrop=\"%d\" Width=\"%d\" Height=\"%d\" Delay=\"%d\" Visible=\"%b\" " +
                    "XScale=\"%.2f\" YScale=\"%.2f\" Rotation=\"%.2f\" " +
                    "RedTint=\"%d\" GreenTint=\"%d\" BlueTint=\"%d\" AlphaTint=\"%d\" " +
                    "RedOffset=\"%d\" GreenOffset=\"%d\" BlueOffset=\"%d\" Interpolated=\"%b\"/>",
            f.xPosition, f.yPosition, f.xPivot, f.yPivot,
            f.xCrop, f.yCrop, f.width, f.height, f.delay, f.visible,
            f.xScale, f.yScale, f.rotation,
            f.redTint, f.greenTint, f.blueTint, f.alphaTint,
            f.redOffset, f.greenOffset, f.blueOffset, f.interpolated
        )
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
