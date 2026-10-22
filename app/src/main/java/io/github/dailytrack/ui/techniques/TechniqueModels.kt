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

package io.github.dailytrack.ui.techniques

import java.util.Locale

/**
 * The eleven jobs a productivity technique can do. The order is the order a
 * person normally meets them: decide, start, schedule, focus, organise, clear
 * the head, build habits, learn, recover, review, and work with people.
 */
enum class TechniqueCategory(
    val key: String,
    val label: String,
    val subtitle: String,
    val tint: Long
) {
    DECIDE(
        "decide",
        "Decide",
        "Choose what deserves your attention before the day chooses for you",
        0xFF7CF5C4
    ),
    START(
        "start",
        "Start",
        "Get moving when the task is clear but the first step feels heavy",
        0xFFFFB84D
    ),
    TIME(
        "time",
        "Time",
        "Give work a place on the calendar instead of hoping it fits",
        0xFF4FD1E8
    ),
    FOCUS(
        "focus",
        "Focus",
        "Protect attention from the things designed to steal it",
        0xFF5B9DFF
    ),
    SYSTEMS(
        "systems",
        "Systems",
        "Keep projects, goals and commitments in a trusted structure",
        0xFFB39DFF
    ),
    CLARITY(
        "clarity",
        "Clarity",
        "Capture, connect and clear the ideas in your head",
        0xFF60A5FA
    ),
    HABITS(
        "habits",
        "Habits",
        "Make the useful behaviour the default, not a daily decision",
        0xFF5EF2B4
    ),
    LEARNING(
        "learning",
        "Learning",
        "Remember more, understand deeper and practise on purpose",
        0xFFFACC15
    ),
    ENERGY(
        "energy",
        "Energy",
        "Manage the body and mind that attention depends on",
        0xFFFF8A65
    ),
    REVIEW(
        "review",
        "Review",
        "Zoom out, notice what is working and change the system",
        0xFF94A3B8
    ),
    TOGETHER(
        "together",
        "Together",
        "Move work through other people without losing clarity",
        0xFFF472B6
    );

    companion object {
        fun fromKey(key: String?): TechniqueCategory? =
            entries.firstOrNull { it.key == key }
    }
}

/**
 * A small, theme-aware illustration family. Each value is a different diagram
 * shape, not a different picture file: the figures are drawn with Compose so
 * they stay sharp at every density and follow the app's colour palette.
 *
 * Several techniques deliberately share a figure when they share the same
 * mental model (for example everything built on a 2x2 grid uses [MATRIX]).
 */
enum class FigureKind {
    MATRIX,
    PARETO,
    THREE_CARDS,
    FROG,
    ORDERED_LIST,
    TARGET,
    SCALES,
    COUNTDOWN,
    TWO_MINUTE,
    IF_THEN,
    LINK,
    BIG_TO_SMALL,
    LOCK,
    PEOPLE,
    NAG,
    NOTE,
    CALENDAR,
    CLOCK,
    WEEK_THEME,
    BATCH,
    CURVE,
    AUDIT,
    BUFFER,
    ROUTINE,
    DEEP_BLOCK,
    SINGLE_TASK,
    SHIELD,
    BELL,
    FOCUS_CUES,
    FLOW,
    MONK,
    KANBAN,
    BULLET,
    SPRINT,
    MILESTONE,
    BACKWARD,
    MVP,
    CRACK,
    MAGNIFY,
    CHECKLIST,
    INBOX,
    PARA,
    LINKS,
    MINDMAP,
    STACK,
    MIRROR,
    TROPHY,
    OPEN_BOOK,
    SPEECH,
    SPACED,
    WEAVE,
    PALACE,
    DUAL,
    BUILD,
    MOON,
    BATTERY,
    WALK,
    WATER,
    PAUSE,
    BREATH,
    UNPLUG,
    FENCE,
    HEART,
    STAIRS,
    REVIEW_ZOOM,
    LIGHTS,
    ARROW_STEPS,
    TRASH,
    HANDOFF,
    MEETING,
    PLANE,
    DOC
}

/** Deep links into features that already exist in Soul Track. */
enum class AppAction(val label: String) {
    TODO("Open Todo"),
    TIMER("Open Timer"),
    POMODORO("Open Pomodoro"),
    HABITS("Open Habits"),
    INSIGHTS("Open Insights"),
    EXPLORE("Open Explore"),
    FOCUS("Open Focus mode"),
    REMINDERS("Open Reminders")
}

/**
 * One canonical technique. The original list contained many near-duplicates;
 * merged names live in [aliases] so search still finds them while the guide
 * teaches a single method.
 *
 * @param summary the one-breath explanation used on cards.
 * @param steps the concrete procedure, in the order a person would do it.
 * @param daily how to actually fit it into a normal day.
 * @param pitfall the common failure mode and how to avoid it.
 * @param figureCaption what the illustration is showing.
 */
data class Technique(
    val id: String,
    val title: String,
    val aliases: List<String>,
    val category: TechniqueCategory,
    val figure: FigureKind,
    val bestFor: String,
    val summary: String,
    val steps: List<String>,
    val daily: String,
    val pitfall: String,
    val figureCaption: String,
    val appActions: List<AppAction> = emptyList()
)

/** Normalises text for search and for overlap tests. */
fun normalizeTechniqueText(value: String): String =
    value.lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

/**
 * Token search across the title, merged names, summary and daily guidance.
 * Every whitespace-separated token must match somewhere, so "focus phone"
 * narrows to techniques that mention both ideas.
 */
fun searchTechniques(
    techniques: List<Technique>,
    query: String,
    category: TechniqueCategory? = null
): List<Technique> {
    val tokens = normalizeTechniqueText(query).split(' ').filter { it.isNotBlank() }
    return techniques.filter { technique ->
        if (category != null && technique.category != category) return@filter false
        if (tokens.isEmpty()) return@filter true
        val haystack = normalizeTechniqueText(
            buildString {
                append(technique.title).append(' ')
                append(technique.aliases.joinToString(" ")).append(' ')
                append(technique.summary).append(' ')
                append(technique.bestFor).append(' ')
                append(technique.steps.joinToString(" ")).append(' ')
                append(technique.daily)
            }
        )
        tokens.all { haystack.contains(it) }
    }
}

/** Groups a filtered list into category sections, preserving catalogue order. */
fun groupTechniques(techniques: List<Technique>): List<Pair<TechniqueCategory, List<Technique>>> =
    TechniqueCategory.entries.mapNotNull { category ->
        val items = techniques.filter { it.category == category }
        if (items.isEmpty()) null else category to items
    }
