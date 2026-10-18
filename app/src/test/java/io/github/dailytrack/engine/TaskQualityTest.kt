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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskQualityTest {

    @Test
    fun `blank titles are not judged as vague`() {
        assertEquals(0, TaskQuality.specificityOf("   "))
        assertFalse(TaskQuality.isVague(""))
    }

    @Test
    fun `a bare vague title is flagged`() {
        assertTrue(TaskQuality.isVague("stuff"))
        assertTrue(TaskQuality.isVague("things"))
        assertTrue(TaskQuality.isVague("email"))
    }

    @Test
    fun `an action with an object and detail is not vague`() {
        assertFalse(TaskQuality.isVague("write the quarterly report"))
        assertFalse(TaskQuality.isVague("read chapter 4"))
        assertFalse(TaskQuality.isVague("draft 3 slides for the review"))
    }

    @Test
    fun `specificity rises with verbs, objects and concrete detail`() {
        val specific = TaskQuality.specificityOf("draft the budget report by 5pm")
        val vague = TaskQuality.specificityOf("stuff")
        assertTrue(specific > vague)
        assertTrue(specific >= 3)
    }

    @Test
    fun `catch all words lower the score`() {
        val clean = TaskQuality.specificityOf("organize the desk drawers")
        val noisy = TaskQuality.specificityOf("organize various things")
        assertTrue(clean > noisy)
    }

    @Test
    fun `calibration is null without both sides of the estimate`() {
        assertNull(TaskQuality.estimateCalibration(0, 30))
        assertNull(TaskQuality.estimateCalibration(30, 0))
    }

    @Test
    fun `calibration reflects actual over estimated`() {
        assertEquals(2f, TaskQuality.estimateCalibration(15, 30)!!, 1e-6f)
        assertEquals(0.5f, TaskQuality.estimateCalibration(30, 15)!!, 1e-6f)
    }

    @Test
    fun `calibration note is silent when the estimate held`() {
        assertNull(TaskQuality.calibrationNote(30, 30))
        assertNull(TaskQuality.calibrationNote(30, 33))
    }

    @Test
    fun `calibration note calls out big overruns and big undershoots`() {
        val over = TaskQuality.calibrationNote(20, 60)
        val under = TaskQuality.calibrationNote(60, 20)
        assertTrue(over!!.contains("longer"))
        assertTrue(under!!.contains("faster"))
    }
}
