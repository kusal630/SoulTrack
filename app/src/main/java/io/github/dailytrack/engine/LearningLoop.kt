package io.github.dailytrack.engine

/** The three Feynman/active-recall steps plus the follow-up review. */
enum class LearningStage(val title: String, val prompt: String) {
    RECALL(
        "Blank-page recall",
        "Close your notes and write everything you remember about this topic."
    ),
    COMPARE(
        "Compare with your notes",
        "Now look at the source. What did you miss or get wrong?"
    ),
    EXPLAIN(
        "Explain it simply",
        "Explain the missed parts in plain words, as if teaching a beginner."
    ),
    COMPLETE(
        "Scheduled",
        "Anything you could not explain gets a spaced review."
    )
}

data class LearningDraft(
    val topic: String,
    val recallText: String = "",
    val missedPoints: String = "",
    val explanation: String = "",
    val stillUnclear: Boolean = false,
    val attempt: Int = 0
)

data class LearningSchedule(
    val nextReviewAt: Long?,
    val intervalDays: Int,
    val message: String
)

object LearningLoop {
    private const val MIN_WORDS = 3
    private const val MIN_CHARS = 20

    /** Recall has to be a real attempt, not a single word. */
    fun isSubstantive(text: String): Boolean {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return text.trim().length >= MIN_CHARS && words.size >= MIN_WORDS
    }

    fun stageOf(draft: LearningDraft): LearningStage = when {
        !isSubstantive(draft.recallText) -> LearningStage.RECALL
        draft.missedPoints.isBlank() -> LearningStage.COMPARE
        !isSubstantive(draft.explanation) && !draft.stillUnclear -> LearningStage.EXPLAIN
        else -> LearningStage.COMPLETE
    }

    /**
     * What was missed gets reviewed sooner; something explained cleanly moves
     * out to a wider interval. A missed day never resets the whole loop.
     */
    fun schedule(draft: LearningDraft, now: Long): LearningSchedule {
        val days = when {
            draft.stillUnclear -> 1
            else -> when (draft.attempt.coerceAtLeast(0)) {
                0 -> 3
                1 -> 7
                2 -> 14
                else -> 30
            }
        }
        val message = when {
            draft.stillUnclear ->
                "Still fuzzy - it comes back tomorrow for another simple explanation."
            days <= 3 ->
                "Review in $days days, before the memory fades."
            else ->
                "Review in $days days. Spacing it out is what makes recall stick."
        }
        return LearningSchedule(
            nextReviewAt = now + days * 24L * 3_600_000L,
            intervalDays = days,
            message = message
        )
    }

    /** Plain-language label for a stored review's stage number. */
    fun stageLabel(stage: Int): String = when (stage) {
        0 -> LearningStage.RECALL.title
        1 -> LearningStage.COMPARE.title
        2 -> LearningStage.EXPLAIN.title
        else -> LearningStage.COMPLETE.title
    }

    fun summary(draft: LearningDraft): String {
        val missed = draft.missedPoints.trim()
        return when {
            missed.isEmpty() ->
                "You recalled ${draft.topic.trim().ifBlank { "the topic" }} with nothing flagged as missing."
            draft.stillUnclear ->
                "Missed: $missed. One more simple pass is scheduled."
            else ->
                "You closed the gap on: $missed."
        }
    }
}
