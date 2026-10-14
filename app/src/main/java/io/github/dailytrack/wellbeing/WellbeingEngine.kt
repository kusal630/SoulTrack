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


package io.github.dailytrack.wellbeing

import java.time.Duration
import java.time.Instant

/** How a stretch of on-device usage was judged. */
enum class UsageKind { PRODUCTIVE, NEUTRAL, WASTED }

/** One app's foreground usage inside a sync window, in milliseconds. */
data class AppUsage(
    val packageName: String,
    val label: String,
    val usageMillis: Long,
    /** Android application category, or null when the platform has none. */
    val platformCategory: Int? = null
)

/** A contiguous stretch of time the user was in one app. */
data class AppInterval(
    val packageName: String,
    val start: Instant,
    val end: Instant
) {
    val millis: Long get() = Duration.between(start, end).toMillis().coerceAtLeast(0L)
}

/** The result of classifying one usage bucket. */
data class UsageVerdict(
    val kind: UsageKind,
    /** 0f..1f - how sure the model is, from its own weights. */
    val confidence: Float
)

/**
 * On-device, weight-based classifier.
 *
 * Soul Track never sends usage anywhere, so the model has to live here too.
 * It is a tiny logistic-style scorer: three weights per app (prior, platform
 * category, duration) that are *learned from the user's own corrections* and
 * persisted locally. There is no server, no upload and no baked-in list of
 * "bad apps" - the starting priors are a neutral default the user overrides
 * by labelling an app once.
 */
class UsageClassifier(
    private val store: ClassifierStore
) {
    /** Keys for a package: prior, category weight, duration weight. */
    private data class Weights(val prior: Float, val category: Float, val duration: Float)

    private fun weights(packageName: String): Weights {
        val saved = store.weightsFor(packageName)
        return Weights(
            prior = saved[0] ?: DEFAULT_PRIOR,
            category = saved[1] ?: 0f,
            duration = saved[2] ?: 0f
        )
    }

    /**
     * Scores one usage bucket. The prior carries most of the vote; the
     * platform category (social, game, video, productivity, ...) nudges it,
     * and long unbroken stretches of the same app lean toward waste.
     */
    fun classify(usage: AppUsage): UsageVerdict {
        val w = weights(usage.packageName)
        val minutes = usage.usageMillis / 60_000.0
        // Long single-app stretches are a weaker signal than who the app is.
        val durationFeature = (minutes / 45.0).coerceIn(0.0, 1.0)

        val categoryScore = when (usage.platformCategory) {
            CATEGORY_SOCIAL, CATEGORY_VIDEO, CATEGORY_GAME -> 1.0
            CATEGORY_NEWS -> 0.6
            CATEGORY_MAPS, CATEGORY_PRODUCTIVITY, CATEGORY_REFERENCE,
            CATEGORY_EDUCATION, CATEGORY_FINANCE -> -1.0
            else -> 0.0
        }

        val raw = w.prior + w.category * categoryScore + w.duration * durationFeature
        // Squash into 0..1 around the zero decision boundary.
        val productiveScore = (1.0 / (1.0 + Math.exp(-raw))).toFloat()

        val kind = when {
            productiveScore >= HIGH_THRESHOLD -> UsageKind.PRODUCTIVE
            productiveScore <= LOW_THRESHOLD -> UsageKind.WASTED
            else -> UsageKind.NEUTRAL
        }
        val confidence = when (kind) {
            UsageKind.PRODUCTIVE -> productiveScore
            UsageKind.WASTED -> 1f - productiveScore
            UsageKind.NEUTRAL -> 1f - Math.abs(productiveScore - 0.5f) * 2f
        }
        return UsageVerdict(kind, confidence.coerceIn(0f, 1f))
    }

    /**
     * One gradient step toward the label the user just gave this app.
     * `label` is the kind they picked; the step size keeps a single correction
     * meaningful without letting it dominate the prior.
     */
    fun learn(packageName: String, label: UsageKind, minutes: Double) {
        val w = weights(packageName)
        val target = when (label) {
            UsageKind.PRODUCTIVE -> 1f
            UsageKind.WASTED -> -1f
            UsageKind.NEUTRAL -> 0f
        }
        val durationFeature = (minutes / 45.0).coerceIn(0.0, 1.0).toFloat()
        // Nudge the prior toward the target, weighted by how long the stretch was.
        val newPrior = (w.prior + LEARN_RATE * target * (0.4f + 0.6f * durationFeature))
            .coerceIn(-4f, 4f)
        store.saveWeights(packageName, listOf(newPrior, w.category, w.duration))
    }

    /** Remembers an explicit override the user set for an app. */
    fun setOverride(packageName: String, label: UsageKind) {
        val target = when (label) {
            UsageKind.PRODUCTIVE -> 2.5f
            UsageKind.WASTED -> -2.5f
            UsageKind.NEUTRAL -> 0f
        }
        store.saveWeights(packageName, listOf(target, 0f, 0f))
    }

    companion object {
        /** Neutral starting point: nothing is assumed good or bad. */
        const val DEFAULT_PRIOR = 0f
        const val LEARN_RATE = 0.35f
        const val HIGH_THRESHOLD = 0.55f
        const val LOW_THRESHOLD = 0.45f

        // Mirrors android.content.pm.ApplicationInfo categories.
        const val CATEGORY_OTHER = -1
        const val CATEGORY_GAME = 0
        const val CATEGORY_AUDIO = 1
        const val CATEGORY_VIDEO = 2
        const val CATEGORY_IMAGE = 3
        const val CATEGORY_SOCIAL = 4
        const val CATEGORY_NEWS = 5
        const val CATEGORY_MAPS = 6
        const val CATEGORY_PRODUCTIVITY = 7
        const val CATEGORY_BROWSER = 8
        const val CATEGORY_REFERENCE = 9
        const val CATEGORY_EDUCATION = 10
        const val CATEGORY_FINANCE = 11
    }
}

/** Where the classifier keeps its learned weights. */
interface ClassifierStore {
    fun weightsFor(packageName: String): Map<Int, Float>
    fun saveWeights(packageName: String, weights: List<Float>)
    fun overrideFor(packageName: String): UsageKind?
}

/**
 * Turns raw usage intervals into the gap-free stretches of time that feed the
 * session database.
 *
 * The contract: every second inside the window is either already tracked
 * (kept out) or becomes a usage-backed session. Nothing is invented, and
 * nothing already recorded is counted twice.
 */
object UsageGapFiller {
    /** Maximum length of one generated session, so a single app cannot own a day. */
    const val MAX_SEGMENT_MILLIS = 60L * 60_000L

    data class Segment(
        val start: Instant,
        val end: Instant,
        val kind: UsageKind,
        val label: String
    ) {
        val millis: Long get() = Duration.between(start, end).toMillis().coerceAtLeast(0L)
    }

    /**
     * [intervals] are foreground stretches sorted by start; [tracked] are the
     * existing session intervals for the same window. Returns one segment per
     * untracked stretch, already classified.
     */
    fun fillGaps(
        intervals: List<AppInterval>,
        tracked: List<Pair<Instant, Instant>>,
        classifier: UsageClassifier,
        labels: Map<String, String>
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        var cursor = intervals.firstOrNull()?.start ?: return emptyList()

        for (interval in intervals) {
            val start = maxOf(cursor, interval.start)
            if (start >= interval.end) continue

            val untracked = subtractTracked(start to interval.end, tracked)
            for ((gapStart, gapEnd) in untracked) {
                if (gapEnd <= gapStart) continue
                val usage = AppUsage(
                    packageName = interval.packageName,
                    label = labels[interval.packageName] ?: interval.packageName,
                    usageMillis = Duration.between(gapStart, gapEnd).toMillis()
                )
                val verdict = classifier.classify(usage)
                var cursorStart = gapStart
                while (cursorStart < gapEnd) {
                    val segmentEnd = minOf(
                        gapEnd,
                        Instant.ofEpochMilli(cursorStart.toEpochMilli() + MAX_SEGMENT_MILLIS)
                    )
                    if (segmentEnd > cursorStart) {
                        segments += Segment(cursorStart, segmentEnd, verdict.kind, usage.label)
                    }
                    cursorStart = segmentEnd
                }
            }
            cursor = maxOf(cursor, interval.end)
        }
        return segments
    }

    /** Subtracts tracked intervals from one candidate interval. */
    private fun subtractTracked(
        interval: Pair<Instant, Instant>,
        tracked: List<Pair<Instant, Instant>>
    ): List<Pair<Instant, Instant>> {
        var pieces = listOf(interval)
        for ((tStart, tEnd) in tracked) {
            val next = mutableListOf<Pair<Instant, Instant>>()
            for ((start, end) in pieces) {
                if (tEnd <= start || tStart >= end) {
                    next += start to end
                    continue
                }
                if (tStart > start) next += start to minOf(end, tStart)
                if (tEnd < end) next += maxOf(start, tEnd) to end
            }
            pieces = next
        }
        return pieces.filter { it.second > it.first }
    }
}

/**
 * One week of on-device usage, reduced to where the user is actually headed.
 * Pure so the dashboard can show it without a database round-trip.
 */
data class WellbeingSummary(
    val productiveMillis: Long,
    val neutralMillis: Long,
    val wastedMillis: Long,
    val untrackedMillis: Long
) {
    val trackedMillis: Long get() = productiveMillis + neutralMillis + wastedMillis
    val totalMillis: Long get() = trackedMillis + untrackedMillis

    /** Share of accounted time that went to work, 0f..1f. */
    val productiveShare: Float
        get() = if (trackedMillis <= 0) 0f
        else productiveMillis.toFloat() / trackedMillis

    /** Share of the window nobody wrote down - unrecorded time is waste. */
    val untrackedShare: Float
        get() = if (totalMillis <= 0) 0f
        else untrackedMillis.toFloat() / totalMillis

    companion object {
        fun from(
            segments: List<UsageGapFiller.Segment>,
            windowMillis: Long,
            alreadyTrackedMillis: Long
        ): WellbeingSummary {
            var productive = 0L
            var neutral = 0L
            var wasted = 0L
            for (segment in segments) {
                when (segment.kind) {
                    UsageKind.PRODUCTIVE -> productive += segment.millis
                    UsageKind.NEUTRAL -> neutral += segment.millis
                    UsageKind.WASTED -> wasted += segment.millis
                }
            }
            val accounted = productive + neutral + wasted + alreadyTrackedMillis
            val untracked = (windowMillis - accounted).coerceAtLeast(0L)
            return WellbeingSummary(productive, neutral, wasted, untracked)
        }
    }
}
