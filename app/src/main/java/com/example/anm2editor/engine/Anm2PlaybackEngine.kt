package com.example.anm2editor.engine

import com.example.anm2editor.model.Anm2Animation
import com.example.anm2editor.model.Anm2Frame
import com.example.anm2editor.model.Anm2LayerAnimation
import com.example.anm2editor.model.Anm2NullAnimation
import kotlin.math.roundToInt

data class EvaluatedFrame(
    val keyframeIndex: Int,
    val frame: Anm2Frame,
    val isInterpolatedStep: Boolean,
    val interpolationAlpha: Float = 0f
)

object Anm2PlaybackEngine {

    /**
     * Finds which keyframe is active at [tick] (0 until frameNum) and computes interpolated values
     * if the keyframe has interpolated=true and has a subsequent keyframe.
     */
    fun evaluateLayer(layerAnim: Anm2LayerAnimation, tick: Int, totalFrames: Int): EvaluatedFrame? {
        if (!layerAnim.visible || layerAnim.frames.isEmpty()) return null
        return evaluateFrames(layerAnim.frames, tick, totalFrames)
    }

    fun evaluateNull(nullAnim: Anm2NullAnimation, tick: Int, totalFrames: Int): EvaluatedFrame? {
        if (!nullAnim.visible || nullAnim.frames.isEmpty()) return null
        return evaluateFrames(nullAnim.frames, tick, totalFrames)
    }

    fun evaluateRoot(frames: List<Anm2Frame>, tick: Int, totalFrames: Int): EvaluatedFrame? {
        if (frames.isEmpty()) return null
        return evaluateFrames(frames, tick, totalFrames)
    }

    private fun evaluateFrames(frames: List<Anm2Frame>, tick: Int, totalFrames: Int): EvaluatedFrame {
        var accumulated = 0
        var activeIndex = 0

        for (i in frames.indices) {
            val f = frames[i]
            val duration = f.delay.coerceAtLeast(1)
            if (tick >= accumulated && (tick < accumulated + duration || i == frames.lastIndex)) {
                activeIndex = i
                val offsetInKeyframe = tick - accumulated
                if (f.interpolated && offsetInKeyframe > 0 && i < frames.lastIndex) {
                    val next = frames[i + 1]
                    val alpha = (offsetInKeyframe.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    val interpolatedFrame = lerpFrames(f, next, alpha)
                    return EvaluatedFrame(activeIndex, interpolatedFrame, true, alpha)
                }
                return EvaluatedFrame(activeIndex, f, false, 0f)
            }
            accumulated += duration
        }

        // If tick exceeds accumulated, clamp to last
        val lastIdx = frames.lastIndex
        return EvaluatedFrame(lastIdx, frames[lastIdx], false, 0f)
    }

    private fun lerpFrames(a: Anm2Frame, b: Anm2Frame, alpha: Float): Anm2Frame {
        return a.copy(
            xPosition = lerp(a.xPosition, b.xPosition, alpha),
            yPosition = lerp(a.yPosition, b.yPosition, alpha),
            xPivot = lerp(a.xPivot, b.xPivot, alpha),
            yPivot = lerp(a.yPivot, b.yPivot, alpha),
            xScale = lerp(a.xScale, b.xScale, alpha),
            yScale = lerp(a.yScale, b.yScale, alpha),
            rotation = lerp(a.rotation, b.rotation, alpha),
            redTint = lerp(a.redTint.toFloat(), b.redTint.toFloat(), alpha).roundToInt().coerceIn(0, 255),
            greenTint = lerp(a.greenTint.toFloat(), b.greenTint.toFloat(), alpha).roundToInt().coerceIn(0, 255),
            blueTint = lerp(a.blueTint.toFloat(), b.blueTint.toFloat(), alpha).roundToInt().coerceIn(0, 255),
            alphaTint = lerp(a.alphaTint.toFloat(), b.alphaTint.toFloat(), alpha).roundToInt().coerceIn(0, 255),
            redOffset = lerp(a.redOffset.toFloat(), b.redOffset.toFloat(), alpha).roundToInt().coerceIn(-255, 255),
            greenOffset = lerp(a.greenOffset.toFloat(), b.greenOffset.toFloat(), alpha).roundToInt().coerceIn(-255, 255),
            blueOffset = lerp(a.blueOffset.toFloat(), b.blueOffset.toFloat(), alpha).roundToInt().coerceIn(-255, 255)
        )
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }

    fun getKeyframeTimelinePositions(frames: List<Anm2Frame>): List<Int> {
        val list = mutableListOf<Int>()
        var acc = 0
        for (f in frames) {
            list.add(acc)
            acc += f.delay.coerceAtLeast(1)
        }
        return list
    }

    fun getKeyframeIndexAtTick(frames: List<Anm2Frame>, tick: Int): Int {
        var acc = 0
        for (i in frames.indices) {
            val dur = frames[i].delay.coerceAtLeast(1)
            if (tick >= acc && (tick < acc + dur || i == frames.lastIndex)) {
                return i
            }
            acc += dur
        }
        return (frames.size - 1).coerceAtLeast(0)
    }

    /**
     * Splits or appends a keyframe at the specified timeline [tick].
     */
    fun insertKeyframeAt(frames: List<Anm2Frame>, tick: Int): List<Anm2Frame> {
        if (frames.isEmpty()) {
            return listOf(Anm2Frame(delay = 1))
        }

        val result = mutableListOf<Anm2Frame>()
        var acc = 0
        var inserted = false

        for (i in frames.indices) {
            val f = frames[i]
            val dur = f.delay.coerceAtLeast(1)

            if (!inserted && tick >= acc && tick < acc + dur) {
                val delayBefore = tick - acc
                if (delayBefore == 0) {
                    // Exact keyframe match, duplicate or split
                    result.add(f.copy(delay = 1))
                    if (dur > 1) {
                        result.add(f.copy(delay = dur - 1))
                    }
                } else {
                    val delayAfter = dur - delayBefore
                    result.add(f.copy(delay = delayBefore))
                    result.add(f.copy(delay = delayAfter))
                }
                inserted = true
            } else {
                result.add(f)
            }
            acc += dur
        }

        if (!inserted) {
            val last = frames.last()
            result.add(last.copy(delay = 1))
        }

        return result
    }

    fun duplicateKeyframe(frames: List<Anm2Frame>, index: Int): List<Anm2Frame> {
        if (index !in frames.indices) return frames
        val list = frames.toMutableList()
        val copy = list[index].copy()
        list.add(index + 1, copy)
        return list
    }

    fun deleteKeyframe(frames: List<Anm2Frame>, index: Int): List<Anm2Frame> {
        if (frames.size <= 1 || index !in frames.indices) return frames
        val list = frames.toMutableList()
        list.removeAt(index)
        return list
    }
}
