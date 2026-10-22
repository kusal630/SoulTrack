package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningLoopTest {
    private val now = 1_700_000_000_000L
    private val day = 86_400_000L

    @Test
    fun recall_must_be_a_real_attempt() {
        assertFalse(LearningLoop.isSubstantive(""))
        assertFalse(LearningLoop.isSubstantive("spacing"))
        assertTrue(LearningLoop.isSubstantive("Spacing repeats recall at widening intervals."))
    }

    @Test
    fun the_three_steps_advance_in_order() {
        var draft = LearningDraft(topic = "Spacing")
        assertEquals(LearningStage.RECALL, LearningLoop.stageOf(draft))

        draft = draft.copy(recallText = "I remember that spacing repeats recall over time.")
        assertEquals(LearningStage.COMPARE, LearningLoop.stageOf(draft))

        draft = draft.copy(missedPoints = "I forgot the forgetting curve.")
        assertEquals(LearningStage.EXPLAIN, LearningLoop.stageOf(draft))

        draft = draft.copy(explanation = "Memory fades fast, so reviewing later rebuilds it.")
        assertEquals(LearningStage.COMPLETE, LearningLoop.stageOf(draft))
    }

    @Test
    fun an_unclear_point_returns_tomorrow() {
        val schedule = LearningLoop.schedule(
            LearningDraft("Topic", stillUnclear = true, attempt = 3),
            now
        )
        assertEquals(1, schedule.intervalDays)
        assertEquals(now + day, schedule.nextReviewAt)
        assertTrue(schedule.message.contains("tomorrow"))
    }

    @Test
    fun clean_explanations_move_to_wider_intervals() {
        val expected = listOf(3, 7, 14, 30)
        expected.forEachIndexed { attempt, days ->
            val schedule = LearningLoop.schedule(
                LearningDraft("Topic", attempt = attempt),
                now
            )
            assertEquals(days, schedule.intervalDays)
            assertNotNull(schedule.nextReviewAt)
            assertEquals(now + days * day, schedule.nextReviewAt)
        }
    }

    @Test
    fun summary_is_honest_about_what_was_missed() {
        assertEquals(
            "You closed the gap on: the forgetting curve.",
            LearningLoop.summary(
                LearningDraft("Spacing", missedPoints = "the forgetting curve")
            )
        )
        assertTrue(
            LearningLoop.summary(LearningDraft("Spacing")).contains("nothing flagged as missing")
        )
    }
}
