package io.github.dailytrack.engine

enum class BreakKind {
    SHORT,
    LONG
}

data class BreakCoachSuggestion(
    val kind: BreakKind,
    val title: String,
    val suggestions: List<String>,
    val blocksNewTasks: Boolean
)

data class StopWarning(
    val shouldWarn: Boolean,
    val title: String,
    val message: String
)

object FocusGuardrails {
    /** A long block (25+ minutes) of deep work is worth a phone-away check. */
    fun requiresPhoneAwayCheck(durationMinutes: Int, deepWork: Boolean): Boolean =
        deepWork && durationMinutes >= 25

    /**
     * Warn when a live block is cancelled before it has had a chance to pay
     * off. Finishing the last few minutes is allowed without friction.
     */
    fun stopWarning(
        elapsedMinutes: Int,
        plannedMinutes: Int,
        deepWork: Boolean
    ): StopWarning {
        val remaining = (plannedMinutes - elapsedMinutes).coerceAtLeast(0)
        val shouldWarn = deepWork && plannedMinutes > 0 && remaining >= 5 &&
            elapsedMinutes < plannedMinutes
        return StopWarning(
            shouldWarn = shouldWarn,
            title = "End this deep-work block?",
            message = if (remaining > 0) {
                "$remaining minute(s) remain. You scheduled this as non-negotiable - " +
                    "leaving now still records the time you spent, but the block will not complete."
            } else {
                "This block is almost done. Finish it to keep the streak."
            }
        )
    }

    /** After every fourth completed focus cycle, protect a longer break. */
    fun breakCoach(completedCycles: Int, shortBreakMinutes: Int = 5): BreakCoachSuggestion {
        val long = completedCycles > 0 && completedCycles % 4 == 0
        return if (long) {
            BreakCoachSuggestion(
                kind = BreakKind.LONG,
                title = "Long break - ${shortBreakMinutes + 10} minutes",
                suggestions = listOf(
                    "Stand up and walk away from the screen",
                    "Drink a full glass of water",
                    "Step outside or look at something far away"
                ),
                blocksNewTasks = true
            )
        } else {
            BreakCoachSuggestion(
                kind = BreakKind.SHORT,
                title = "Short break - $shortBreakMinutes minutes",
                suggestions = listOf(
                    "Stretch your back and shoulders",
                    "Take five slow breaths",
                    "Walk to the window and look outside"
                ),
                blocksNewTasks = false
            )
        }
    }

    fun phoneAwayMessage(durationMinutes: Int): String =
        "Put the phone face down, out of reach, for the next $durationMinutes minutes. " +
            "The app will stay on this task."
}
