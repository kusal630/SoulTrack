package io.github.dailytrack.engine

data class PremortemRisk(
    val text: String,
    val likelihood: Int = 3,
    val prevention: String = "",
    val dueInDays: Int = 7
)

data class PreventionTask(
    val riskIndex: Int,
    val riskText: String,
    val title: String,
    val dueAt: Long
)

data class RiskReview(
    val id: Long,
    val text: String,
    val dueAt: Long?,
    val isResolved: Boolean
) {
    fun needsReview(now: Long): Boolean =
        !isResolved && dueAt != null && dueAt <= now
}

data class MilestoneReview(
    val dueRisks: List<RiskReview>,
    val unresolvedCount: Int,
    val resolvedCount: Int
) {
    val headline: String get() = when {
        dueRisks.isNotEmpty() -> "${dueRisks.size} pre-mortem risk(s) need a check-in"
        unresolvedCount > 0 -> "$unresolvedCount risk(s) still open - still on track?"
        resolvedCount > 0 -> "Every named risk has been addressed"
        else -> "No risks named yet"
    }
}

object Premortem {
    val PROMPTS = listOf(
        "Imagine it failed in six months. What is the single most likely reason?",
        "Which risk would be embarrassing to admit you saw coming?",
        "What would a calm outsider say you are ignoring?"
    )

    private const val DAY_MS = 24L * 3_600_000L

    /**
     * Every risk becomes a dated prevention task, so the pre-mortem does not
     * stay a thought experiment - it turns into work that can be checked off.
     */
    fun preventionTasks(
        risks: List<PremortemRisk>,
        now: Long,
        defaultDueDays: Int = 7
    ): List<PreventionTask> = risks
        .filter { it.text.isNotBlank() }
        .mapIndexed { index, risk ->
            val days = risk.dueInDays.takeIf { it > 0 } ?: defaultDueDays
            val title = risk.prevention.trim().ifBlank {
                "Prevent: ${risk.text.trim().take(120)}"
            }
            PreventionTask(
                riskIndex = index,
                riskText = risk.text.trim(),
                title = title,
                dueAt = now + days * DAY_MS
            )
        }

    fun milestoneReview(risks: List<RiskReview>, now: Long): MilestoneReview =
        MilestoneReview(
            dueRisks = risks.filter { it.needsReview(now) },
            unresolvedCount = risks.count { !it.isResolved },
            resolvedCount = risks.count { it.isResolved }
        )

    /** The risk that should be shown first at a milestone check. */
    fun topRiskForReview(risks: List<RiskReview>, now: Long): RiskReview? =
        milestoneReview(risks, now).dueRisks
            .sortedWith(compareBy({ it.dueAt ?: Long.MAX_VALUE }, { it.id }))
            .firstOrNull()

    fun stillOnTrackMessage(resolved: Boolean): String =
        if (resolved) "Risk addressed - prevention task closed."
        else "Still open. Name the next small step, or drop the project honestly."
}
