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


package io.github.dailytrack.sync

/**
 * Hybrid logical clock. The wall component tracks real time for human-friendly
 * ordering; the counter keeps edits totally ordered even when two devices make
 * changes in the same millisecond or when a device clock is behind.
 */
data class Hlc(val wall: Long, val counter: Int) : Comparable<Hlc> {
    fun encode(): String = "${wall.toString().padStart(16, '0')}-${counter.toString().padStart(6, '0')}"

    override fun compareTo(other: Hlc): Int {
        val wallCompare = wall.compareTo(other.wall)
        return if (wallCompare != 0) wallCompare else counter.compareTo(other.counter)
    }

    companion object {
        val ZERO = Hlc(0, 0)

        fun parse(value: String?): Hlc {
            if (value.isNullOrBlank()) return ZERO
            val dash = value.lastIndexOf('-')
            if (dash <= 0) return ZERO
            return try {
                Hlc(value.substring(0, dash).toLong(), value.substring(dash + 1).toInt())
            } catch (_: NumberFormatException) {
                ZERO
            }
        }

        fun max(a: Hlc, b: Hlc): Hlc = if (a >= b) a else b
    }
}

/**
 * Thread-safe clock shared by every sync operation on this device. The value
 * is persisted so logical time never moves backwards across process restarts.
 */
class HlcClock(
    private val load: () -> String?,
    private val save: (String) -> Unit,
    private val wallClock: () -> Long = System::currentTimeMillis
) {
    private var last: Hlc = Hlc.parse(load())

    @Synchronized
    fun next(): Hlc {
        val now = wallClock()
        val next = if (now > last.wall) {
            Hlc(now, 0)
        } else {
            Hlc(last.wall, last.counter + 1)
        }
        last = next
        save(next.encode())
        return next
    }

    /** Merge a remote timestamp into our clock without emitting an event. */
    @Synchronized
    fun observe(remote: Hlc) {
        if (remote > last) {
            last = remote
            save(remote.encode())
        }
    }

    @Synchronized
    fun current(): Hlc = last
}
