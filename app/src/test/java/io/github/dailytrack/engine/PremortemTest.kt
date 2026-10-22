package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremortemTest {
    private val now = 1_700_000_000_000L
    private val day = 86_400_000L

    @Test
    fun every_risk_becomes_a_dated_prevention_task() {
        val tasks = Premortem.preventionTasks(
            listOf(
                PremortemRisk("No users", prevention = "Talk to 10 users", dueInDays = 3),
                PremortemRisk("No time", dueInDays = 0)
            ),
            now
        )
        assertEquals(2, tasks.size)
        assertEquals("Talk to 10 users", tasks[0].title)
        assertEquals(now + 3 * day, tasks[0].dueAt)
        assertTrue(tasks[1].title.startsWith("Prevent: No time"))
        assertEquals(now + 7 * day, tasks[1].dueAt)
    }

    @Test
    fun blank_risks_are_ignored() {
        val tasks = Premortem.preventionTasks(
            listOf(PremortemRisk("  "), PremortemRisk("Real risk")),
            now
        )
        assertEquals(1, tasks.size)
    }

    @Test
    fun milestone_review_surfaces_due_unresolved_risks() {
        val risks = listOf(
            RiskReview(1, "Late risk", now + day, false),
            RiskReview(2, "Due risk", now - day, false),
            RiskReview(3, "Done risk", now - day, true)
        )
        val review = Premortem.milestoneReview(risks, now)
        assertEquals(1, review.dueRisks.size)
        assertEquals(2, review.unresolvedCount)
        assertEquals(1, review.resolvedCount)
        assertTrue(review.headline.contains("need a check-in"))
        assertEquals(2L, Premortem.topRiskForReview(risks, now)?.id)
    }

    @Test
    fun top_risk_is_null_when_nothing_is_due() {
        assertNull(
            Premortem.topRiskForReview(
                listOf(RiskReview(1, "Later", now + day, false)),
                now
            )
        )
    }
}
