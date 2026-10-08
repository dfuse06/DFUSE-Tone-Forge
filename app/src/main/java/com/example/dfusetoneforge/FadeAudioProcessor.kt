package com.example.dfusetoneforge

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

@androidx.annotation.OptIn(UnstableApi::class)
internal class FadeAudioProcessor(
    private val durationMs: Long,
    private val fadeInMs: Int,
    private val fadeOutMs: Int
) : BaseAudioProcessor() {
    private var frame = 0L
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }
    override fun onFlush() { frame = 0L }
    override fun queueInput(inputBuffer: ByteBuffer) {
        inputBuffer.order(ByteOrder.nativeOrder())
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        val bytesPerSample = if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val frameBytes = inputAudioFormat.channelCount * bytesPerSample
        val totalFrames = (durationMs * inputAudioFormat.sampleRate / 1000L).coerceAtLeast(1L)
        val inFrames = min(fadeInMs.toLong() * inputAudioFormat.sampleRate / 1000L, totalFrames / 2)
        val outFrames = min(fadeOutMs.toLong() * inputAudioFormat.sampleRate / 1000L, totalFrames / 2)
        while (inputBuffer.remaining() >= frameBytes) {
            val inGain = if (inFrames > 0) (frame.toDouble() / inFrames).coerceIn(0.0, 1.0) else 1.0
            val outGain = if (outFrames > 0) ((totalFrames - 1 - frame).toDouble() / outFrames).coerceIn(0.0, 1.0) else 1.0
            val gain = min(inGain, outGain)
            repeat(inputAudioFormat.channelCount) {
                if (bytesPerSample == 4) output.putFloat((inputBuffer.float * gain).toFloat())
                else output.putShort((inputBuffer.short * gain).toInt().coerceIn(-32768, 32767).toShort())
            }
            frame++
        }
        check(!inputBuffer.hasRemaining()) { "Incomplete PCM audio frame" }
        output.flip()
    }
}
