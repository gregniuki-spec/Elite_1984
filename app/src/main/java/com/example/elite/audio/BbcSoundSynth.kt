package com.example.elite.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Authentic BBC Micro SN76489 8-bit sound synthesizer.
 * Generates square waves, noise bursts, and retro melodies using native AudioTrack.
 */
class BbcSoundSynth(private val scope: CoroutineScope) {

    private val sampleRate = 22050
    private var dockingJob: Job? = null

    private fun playPcm(samples: ShortArray) {
        scope.launch(Dispatchers.IO) {
            try {
                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBuffer, samples.size * 2)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                // Let it play then release
                delay((samples.size * 1000L / sampleRate) + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio initialization errors gracefully
            }
        }
    }

    /**
     * BBC Micro Laser Pulse: frequency sweeps down from 1200Hz to 250Hz.
     */
    fun playLaser() {
        val durationMs = 90
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)

        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 1200.0 - (progress * 950.0)
            phase += (2.0 * Math.PI * freq) / sampleRate
            // Square wave with high volume
            val amp = (1.0 - progress * 0.7) * 16000.0
            val value = if (sin(phase) > 0) amp else -amp
            buffer[i] = value.toInt().toShort()
        }
        playPcm(buffer)
    }

    /**
     * Explosion / Shield Impact: Noise burst with decay.
     */
    fun playExplosion() {
        val durationMs = 280
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val amp = (1.0 - progress) * (1.0 - progress) * 18000.0
            val noise = (Random.nextDouble() * 2.0 - 1.0) * amp
            buffer[i] = noise.toInt().toShort()
        }
        playPcm(buffer)
    }

    /**
     * Missile Launch: Rising frequency tone with exhaust noise.
     */
    fun playMissileLaunch() {
        val durationMs = 240
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)

        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 200.0 + progress * 600.0
            phase += (2.0 * Math.PI * freq) / sampleRate
            val tone = if (sin(phase) > 0) 10000.0 else -10000.0
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 8000.0 * (1.0 - progress * 0.5)
            buffer[i] = ((tone + noise) * (1.0 - progress * 0.3)).toInt().toShort()
        }
        playPcm(buffer)
    }

    /**
     * Hyperspace Warp sound: 3 resonant rising tones.
     */
    fun playHyperspace() {
        val durationMs = 500
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)

        var phase1 = 0.0
        var phase2 = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val f1 = 300.0 + progress * 900.0
            val f2 = 450.0 + progress * 1350.0
            phase1 += (2.0 * Math.PI * f1) / sampleRate
            phase2 += (2.0 * Math.PI * f2) / sampleRate
            val v1 = if (sin(phase1) > 0) 8000.0 else -8000.0
            val v2 = if (sin(phase2) > 0) 6000.0 else -6000.0
            buffer[i] = ((v1 + v2) * (1.0 - progress * 0.2)).toInt().toShort()
        }
        playPcm(buffer)
    }

    /**
     * UI Confirmation Beep: 880Hz square beep.
     */
    fun playBeep(high: Boolean = true) {
        val durationMs = 45
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        val freq = if (high) 880.0 else 440.0
        var phase = 0.0
        for (i in 0 until totalSamples) {
            phase += (2.0 * Math.PI * freq) / sampleRate
            val value = if (sin(phase) > 0) 10000.0 else -10000.0
            buffer[i] = value.toInt().toShort()
        }
        playPcm(buffer)
    }

    /**
     * The Blue Danube waltz snippet (Johann Strauss Op. 314)
     * Played by the docking computer autopilot in BBC Micro Elite.
     */
    fun startBlueDanube() {
        stopBlueDanube()
        dockingJob = scope.launch(Dispatchers.IO) {
            // Notes in Hz: D4, F#4, A4, A4, F#4, F#4, D4, D4, F#4, A4...
            val melody = listOf(
                Pair(294, 250), // D4
                Pair(294, 250),
                Pair(370, 250), // F#4
                Pair(440, 500), // A4
                Pair(440, 250),
                Pair(370, 250), // F#4
                Pair(370, 500),
                Pair(294, 250), // D4
                Pair(294, 250),
                Pair(370, 250),
                Pair(440, 500),
                Pair(440, 250),
                Pair(392, 250), // G4
                Pair(392, 500),
                Pair(330, 250), // E4
                Pair(330, 250),
                Pair(392, 250),
                Pair(494, 500), // B4
                Pair(494, 250)
            )

            while (isActive) {
                for ((freq, dur) in melody) {
                    if (!isActive) break
                    playTone(freq, dur - 30)
                    delay(dur.toLong())
                }
            }
        }
    }

    fun stopBlueDanube() {
        dockingJob?.cancel()
        dockingJob = null
    }

    private fun playTone(freq: Int, durationMs: Int) {
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            phase += (2.0 * Math.PI * freq) / sampleRate
            val value = if (sin(phase) > 0) 7000.0 else -7000.0
            buffer[i] = value.toInt().toShort()
        }
        playPcm(buffer)
    }
}
