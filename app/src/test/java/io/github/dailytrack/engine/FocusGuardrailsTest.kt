package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusGuardrailsTest {

    @Test
    fun phone_away_check_only_for_long_deep_work() {
        assertTrue(FocusGuardrails.requiresPhoneAwayCheck(25, deepWork = true))
        assertTrue(FocusGuardrails.requiresPhoneAwayCheck(60, deepWork = true))
        assertFalse(FocusGuardrails.requiresPhoneAwayCheck(15, deepWork = true))
        assertFalse(FocusGuardrails.requiresPhoneAwayCheck(60, deepWork = false))
    }

    @Test
    fun deep_work_warns_before_an_early_stop() {
        val warning = FocusGuardrails.stopWarning(
            elapsedMinutes = 5,
            plannedMinutes = 25,
            deepWork = true
        )
        assertTrue(warning.shouldWarn)
        assertTrue(warning.message.contains("20 minute"))
    }

    @Test
    fun finishing_the_block_does_not_nag() {
        assertFalse(
            FocusGuardrails.stopWarning(25, 25, deepWork = true).shouldWarn
        )
        assertFalse(
            FocusGuardrails.stopWarning(60, 25, deepWork = true).shouldWarn
        )
        assertFalse(
            FocusGuardrails.stopWarning(3, 25, deepWork = false).shouldWarn
        )
    }

    @Test
    fun every_fourth_cycle_earns_a_long_break_that_blocks_new_tasks() {
        val fourth = FocusGuardrails.breakCoach(completedCycles = 4, shortBreakMinutes = 5)
        assertEquals(BreakKind.LONG, fourth.kind)
        assertTrue(fourth.blocksNewTasks)
        assertEquals(3, fourth.suggestions.size)

        val third = FocusGuardrails.breakCoach(completedCycles = 3)
        assertEquals(BreakKind.SHORT, third.kind)
        assertFalse(third.blocksNewTasks)
    }

    @Test
    fun phone_away_message_restates_the_block_length() {
        assertTrue(FocusGuardrails.phoneAwayMessage(45).contains("45 minutes"))
    }
}
