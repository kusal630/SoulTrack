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

/**
 * Small heuristic checks that keep tasks and goals *specific*, which is the
 * single most reliable lever on follow-through: specific, moderately difficult
 * goals outperform vague "do your best" intentions with a medium-to-large
 * effect (Locke & Latham, 2002), and the same holds for goals (Ordóñez et al.
 * framing). Nothing here is a hard rule - it only powers a gentle hint.
 */
object TaskQuality {

    private val actionVerbs = setOf(
        "write", "draft", "email", "call", "read", "review", "fix", "build", "create",
        "design", "plan", "send", "prepare", "study", "practice", "clean", "organize",
        "organise", "finish", "complete", "start", "submit", "update", "test",
        "refactor", "debug", "deploy", "record", "schedule", "buy", "book", "apply",
        "reply", "respond", "measure", "exercise", "run", "walk", "cook", "pay", "ask"
    )

    private val concreteWords = setOf(
        "chapter", "page", "section", "slide", "email", "report", "doc", "document",
        "pr", "issue", "bug", "ticket", "invoice", "budget", "list", "outline",
        "today", "tomorrow", "monday", "tuesday", "wednesday", "thursday", "friday",
        "minutes", "minute", "hours", "hour", "words", "paragraph", "commit"
    )

    private val vagueWords = setOf(
        "stuff", "things", "thing", "some", "misc", "various", "etc", "everything",
        "anything", "general", "more", "later"
    )

    /**
     * 0 (vague) .. 3 (very specific). A blank title is 0.
     *
     * A title scores on whether it names an action, carries an object beyond the
     * verb, and pins down a concrete quantity or deadline, and loses a point for
     * catch-all words like "stuff" or "things".
     */
    fun specificityOf(title: String): Int {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return 0
        val lower = trimmed.lowercase()
        val words = lower.split(' ', '\t', '\n').filter { it.isNotBlank() }

        var score = 0
        if (words.any { it.trimEnd('.', ',', ':') in actionVerbs }) score++
        if (words.size >= 3) score++
        if (lower.any { it.isDigit() } || words.any { it in concreteWords }) score++
        if (words.any { it in vagueWords }) score--

        return score.coerceIn(0, 3)
    }

    /** True when the title is non-blank but names no action or object. */
    fun isVague(title: String): Boolean {
        if (title.isBlank()) return false
        return wordCount(title) < 2 || specificityOf(title) == 0
    }

    /**
     * True when a task is long enough that it is worth breaking into smaller,
     * graded steps. "Graded tasks" is one of the behaviour-change techniques
     * repeatedly associated with better outcomes in digital interventions
     * (Singh et al., 2023 umbrella review), and smaller steps lower the
     * activation cost of starting.
     */
    fun needsBreakdown(estimatedMinutes: Int): Boolean = estimatedMinutes >= 60

    private fun wordCount(title: String): Int =
        title.trim().split(' ', '\t', '\n').count { it.isNotBlank() }

    /**
     * How a task actually ran against its own estimate. Ratio of actual to
     * estimated time; null when either side is unknown or zero.
     *
     * The planning fallacy is one of the most robust findings in judgement
     * research (Kahneman & Tversky, 1979; Buehler et al., 1994): people
     * systematically under-estimate task time, so we surface the real ratio
     * instead of pretending the estimate was right.
     */
    fun estimateCalibration(estimatedMinutes: Int, actualMinutes: Int): Float? {
        if (estimatedMinutes <= 0 || actualMinutes <= 0) return null
        return actualMinutes.toFloat() / estimatedMinutes
    }

    /** A short, honest note about an estimate, or null when it was on target. */
    fun calibrationNote(estimatedMinutes: Int, actualMinutes: Int): String? {
        val ratio = estimateCalibration(estimatedMinutes, actualMinutes) ?: return null
        return when {
            ratio >= 1.5f -> "took ${ratio.round1()}x longer than planned"
            ratio > 1.2f -> "ran a bit over your estimate"
            ratio <= 0.5f -> "finished much faster than planned"
            ratio < 0.8f -> "finished under your estimate"
            else -> null
        }
    }

    private fun Float.round1(): String {
        val rounded = Math.round(this * 10f) / 10f
        return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    }
}
