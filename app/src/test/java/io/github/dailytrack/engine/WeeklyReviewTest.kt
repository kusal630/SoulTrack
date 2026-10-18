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


package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyReviewTest {

    @Test
    fun `duration formatting reads naturally`() {
        assertEquals("0m", WeeklyReview.formatDuration(0))
        assertEquals("45m", WeeklyReview.formatDuration(45))
        assertEquals("1h", WeeklyReview.formatDuration(60))
        assertEquals("8h 30m", WeeklyReview.formatDuration(510))
        assertEquals("0m", WeeklyReview.formatDuration(-5))
    }

    @Test
    fun `a productive week is described with tasks and focus time`() {
        val line = WeeklyReview.digestLine(completedTasks = 12, productiveMinutes = 510, activeDays = 5)
        assertTrue(line.contains("12"))
        assertTrue(line.contains("8h 30m"))
        assertTrue(line.contains("5"))
    }

    @Test
    fun `a quiet week is never shamed`() {
        val line = WeeklyReview.digestLine(completedTasks = 0, productiveMinutes = 0, activeDays = 0)
        assertTrue(line.contains("Quiet"))
        assertFalse(line.contains("0h"))
    }

    @Test
    fun `tasks with no focus time still get a sensible line`() {
        val line = WeeklyReview.digestLine(completedTasks = 3, productiveMinutes = 0, activeDays = 0)
        assertTrue(line.contains("3"))
    }

    @Test
    fun `nudge points at momentum after a strong week`() {
        val strong = WeeklyReview.reviewNudge(completedTasks = 8, productiveMinutes = 400)
        val weak = WeeklyReview.reviewNudge(completedTasks = 0, productiveMinutes = 0)
        assertTrue(strong.contains("Momentum"))
        assertTrue(weak.contains("one small task"))
    }

    @Test
    fun `long tasks are flagged for breakdown`() {
        assertFalse(TaskQuality.needsBreakdown(45))
        assertTrue(TaskQuality.needsBreakdown(60))
        assertTrue(TaskQuality.needsBreakdown(120))
    }
}
