/*
 * Copyright 2024 Soul Track Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.github.dailytrack.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Samples the room with the microphone and turns it into a quietness score.
 *
 * Privacy: audio is read as raw PCM, reduced to a single RMS number, and the
 * samples are thrown away immediately. Nothing is recorded, stored or sent
 * anywhere - only the resulting 0-100 quietness value leaves this object, and
 * only while a session is running.
 */
object NoiseMonitor {

    private const val PREFS_NAME = "noise_prefs"
    private const val KEY_LAST_QUIETNESS = "last_quietness"
    private const val KEY_LAST_AT = "last_at"
    private const val SAMPLE_RATE = 8_000
    private const val STALE_AFTER_MS = 5 * 60_000L

    private const val FLOOR_DB = -70.0
    private const val CEILING_DB = -25.0

    data class Reading(val quietness: Int, val rms: Double, val at: Long)

    private val _reading = MutableStateFlow<Reading?>(null)
    val reading: StateFlow<Reading?> = _reading

    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var worker: Thread? = null
    @Volatile private var running: Boolean = false

    private var window = DoubleArray(0)
    private var windowIndex = 0
    private var windowFilled = false

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun isRunning(): Boolean = running

    /**
     * Latest quietness 0-100 (100 = silent) from this process or from the last
     * time the sensor ran. Null when we have never listened.
     */
    fun quietness(context: Context? = null): Int? {
        _reading.value?.let { reading ->
            if (System.currentTimeMillis() - reading.at <= STALE_AFTER_MS) return reading.quietness
        }
        context ?: return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val at = prefs.getLong(KEY_LAST_AT, 0L)
        if (at == 0L || System.currentTimeMillis() - at > STALE_AFTER_MS) return null
        return prefs.getInt(KEY_LAST_QUIETNESS, -1).takeIf { it >= 0 }
    }

    fun start(context: Context) {
        if (running) return
        if (!hasPermission(context)) return
        runCatching { startUnsafe(context) }
    }

    private fun startUnsafe(context: Context) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return
        val bufferSamples = (minBuffer / 2).coerceAtLeast(SAMPLE_RATE / 4)
        val created = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSamples * 2
        )
        if (created.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { created.release() }
            return
        }
        recorder = created
        window = DoubleArray(SAMPLE_RATE / 2) // ~0.5s window
        windowIndex = 0
        windowFilled = false
        running = true
        created.startRecording()

        worker = Thread({
            val buffer = ShortArray(bufferSamples)
            while (running) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val sample = buffer[i] / 32768.0
                        sum += sample * sample
                    }
                    val rms = kotlin.math.sqrt(sum / read)
                    recordSample(rms, context)
                }
            }
        }, "soultrack-noise").apply {
            isDaemon = true
            start()
        }
    }

    private fun recordSample(rms: Double, context: Context) {
        window[windowIndex] = rms
        windowIndex = (windowIndex + 1) % window.size
        if (windowIndex == 0) windowFilled = true
        val count = if (windowFilled) window.size else windowIndex
        if (count < 8) return
        var sum = 0.0
        for (i in 0 until count) sum += window[i]
        val avg = sum / count
        val reading = Reading(
            quietness = toQuietness(avg),
            rms = avg,
            at = System.currentTimeMillis()
        )
        _reading.value = reading
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_LAST_QUIETNESS, reading.quietness)
            .putLong(KEY_LAST_AT, reading.at)
            .apply()
    }

    /** Maps RMS (0..1) onto 0-100 where 100 is a silent room. */
    fun toQuietness(rms: Double): Int {
        if (rms <= 0.0) return 100
        val db = 20.0 * log10(rms.coerceAtLeast(1e-7))
        val normalized = ((db - FLOOR_DB) / (CEILING_DB - FLOOR_DB)).coerceIn(0.0, 1.0)
        return ((1.0 - normalized) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun stop() {
        running = false
        val thread = worker
        worker = null
        thread?.join(500)
        runCatching {
            recorder?.stop()
        }
        runCatching { recorder?.release() }
        recorder = null
        _reading.value = null
    }

    /** Rough environment label for the UI. */
    fun label(quietness: Int?): String = when {
        quietness == null -> "Not listening"
        quietness >= 78 -> "Quiet - great for deep work"
        quietness >= 55 -> "Moderate - workable"
        quietness >= 35 -> "Lively - expect interruptions"
        else -> "Noisy - pick something light"
    }
}
