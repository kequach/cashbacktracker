package com.cashbacktracker.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class CelebrationSoundStyle {
    PAID,
    MILESTONE,
}

class CelebrationSoundPlayer : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = Channel<CelebrationSoundStyle>(capacity = Channel.BUFFERED)

    init {
        scope.launch {
            for (style in requests) {
                runCatching { playImmediately(style) }
            }
        }
    }

    fun enqueue(style: CelebrationSoundStyle) {
        requests.trySend(style)
    }

    override fun close() {
        requests.close()
        scope.cancel()
    }

    private fun playImmediately(style: CelebrationSoundStyle) {
        val notes = when (style) {
            CelebrationSoundStyle.PAID -> listOf(
                RewardNote(frequency = 659.25, durationMs = 85, volume = 0.22, gapMs = 18),
                RewardNote(frequency = 783.99, durationMs = 95, volume = 0.24, gapMs = 18),
                RewardNote(frequency = 987.77, durationMs = 150, volume = 0.22),
            )

            CelebrationSoundStyle.MILESTONE -> listOf(
                RewardNote(frequency = 523.25, durationMs = 80, volume = 0.24, gapMs = 16),
                RewardNote(frequency = 659.25, durationMs = 90, volume = 0.27, gapMs = 14),
                RewardNote(frequency = 783.99, durationMs = 105, volume = 0.30, gapMs = 12),
                RewardNote(frequency = 1046.50, durationMs = 190, volume = 0.30, gapMs = 18),
                RewardNote(frequency = 1318.51, durationMs = 120, volume = 0.18),
            )
        }
        val samples = notes.renderSamples()
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(minBufferSize, samples.size * BYTES_PER_SAMPLE))
            .build()

        try {
            audioTrack.play()
            var offset = 0
            while (offset < samples.size) {
                val written = audioTrack.write(
                    samples,
                    offset,
                    samples.size - offset,
                    AudioTrack.WRITE_BLOCKING,
                )
                if (written <= 0) break
                offset += written
            }
            Thread.sleep(notes.sumOf { it.durationMs + it.gapMs }.toLong() + 90L)
            audioTrack.stop()
        } finally {
            audioTrack.release()
        }
    }

    private data class RewardNote(
        val frequency: Double,
        val durationMs: Int,
        val volume: Double,
        val gapMs: Int = 0,
    )

    private fun List<RewardNote>.renderSamples(): ShortArray {
        val sampleCount = sumOf { (it.durationMs + it.gapMs) * SAMPLE_RATE / 1_000 }
        val output = ShortArray(sampleCount)
        var offset = 0
        forEach { note ->
            val noteSamples = note.durationMs * SAMPLE_RATE / 1_000
            val gapSamples = note.gapMs * SAMPLE_RATE / 1_000
            val attackSamples = min(noteSamples / 3, SAMPLE_RATE / 90).coerceAtLeast(1)
            val releaseSamples = min(noteSamples / 2, SAMPLE_RATE / 25).coerceAtLeast(1)
            repeat(noteSamples) { index ->
                val time = index.toDouble() / SAMPLE_RATE.toDouble()
                val attack = index.toDouble() / attackSamples.toDouble()
                val release = (noteSamples - index).toDouble() / releaseSamples.toDouble()
                val envelope = min(1.0, min(attack, release))
                val angle = 2.0 * PI * note.frequency * time
                val tone = (sin(angle) + 0.18 * sin(angle * 2.0)) / 1.18
                output[offset + index] = (tone * envelope * note.volume * Short.MAX_VALUE).toInt().toShort()
            }
            offset += noteSamples + gapSamples
        }
        return output
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val BYTES_PER_SAMPLE = 2
    }
}
