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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseMonitorTest {

    @Test
    fun `silence maps to full quietness`() {
        assertEquals(100, NoiseMonitor.toQuietness(0.0))
        assertEquals(100, NoiseMonitor.toQuietness(-1.0))
    }

    @Test
    fun `loud audio maps to low quietness`() {
        assertEquals(0, NoiseMonitor.toQuietness(1.0))
        assertEquals(0, NoiseMonitor.toQuietness(0.5))
    }

    @Test
    fun `quietness is monotonic as noise grows`() {
        val samples = listOf(1e-5, 1e-4, 1e-3, 1e-2, 1e-1, 0.5)
        val values = samples.map { NoiseMonitor.toQuietness(it) }
        values.zipWithNext().forEach { (a, b) ->
            assertTrue("quietness must not rise as noise rises: $a -> $b", a >= b)
        }
    }

    @Test
    fun `quietness stays inside the 0 to 100 range`() {
        listOf(-5.0, 0.0, 1e-7, 1e-4, 3.16e-3, 0.02, 1.0, 1e6).forEach { rms ->
            val q = NoiseMonitor.toQuietness(rms)
            assertTrue("out of range for rms=$rms: $q", q in 0..100)
        }
    }

    @Test
    fun `mid floor amplitude lands near the middle of the scale`() {
        // -47.5 dBFS is exactly halfway between the -70 and -25 floor/ceiling.
        val rms = Math.pow(10.0, -47.5 / 20.0)
        assertEquals(50, NoiseMonitor.toQuietness(rms))
    }

    @Test
    fun `labels cover every band`() {
        assertEquals("Not listening", NoiseMonitor.label(null))
        assertEquals("Quiet - great for deep work", NoiseMonitor.label(90))
        assertEquals("Moderate - workable", NoiseMonitor.label(60))
        assertEquals("Lively - expect interruptions", NoiseMonitor.label(40))
        assertEquals("Noisy - pick something light", NoiseMonitor.label(10))
    }
}
