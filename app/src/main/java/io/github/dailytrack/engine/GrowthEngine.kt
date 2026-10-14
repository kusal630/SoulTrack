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

data class GrowthScoreResult(
    val totalScore: Double,
    val learningComponent: Double,
    val productiveComponent: Double,
    val exerciseComponent: Double,
    val sleepComponent: Double,
    val socialComponent: Double,
    val noveltyComponent: Double,
    val consistencyComponent: Double
)

data class GrowthWeights(
    val learning: Double = 0.25,
    val productive: Double = 0.25,
    val exercise: Double = 0.20,
    val sleep: Double = 0.15,
    val social: Double = 0.05,
    val novelty: Double = 0.05,
    val consistency: Double = 0.05
)

/**
 * What "enough" means for each component.
 *
 * The defaults are the cold-start prior; [io.github.dailytrack.ui.viewmodel.MainViewModel]
 * replaces them with the user's own targets (Settings) or, when no target was
 * set, with their own trailing two weeks of history.
 */
data class GrowthTargets(
    val learningMinutes: Double = GrowthEngine.TARGET_LEARNING_MINUTES,
    val productiveMinutes: Double = GrowthEngine.TARGET_PRODUCTIVE_MINUTES,
    val exerciseMinutes: Double = GrowthEngine.TARGET_EXERCISE_MINUTES,
    val socialMinutes: Double = GrowthEngine.TARGET_SOCIAL_MINUTES,
    val sleepBand: ClosedFloatingPointRange<Double> = 6.0..9.0
)

class GrowthEngine(private val weights: GrowthWeights = GrowthWeights()) {

    fun calculateGrowthScore(
        learningMinutes: Double,
        productiveMinutes: Double,
        exerciseMinutes: Double,
        sleepHours: Double,
        socialMinutes: Double,
        noveltyScore: Double,
        consecutiveDaysActive: Int,
        targets: GrowthTargets = GrowthTargets()
    ): GrowthScoreResult {
        val hasAnyActivity = learningMinutes > 0 || productiveMinutes > 0 ||
                exerciseMinutes > 0 || sleepHours > 0 || socialMinutes > 0

        if (!hasAnyActivity && consecutiveDaysActive == 0) {
            return GrowthScoreResult(
                totalScore = 0.0,
                learningComponent = 0.0,
                productiveComponent = 0.0,
                exerciseComponent = 0.0,
                sleepComponent = 0.0,
                socialComponent = 0.0,
                noveltyComponent = 0.0,
                consistencyComponent = 0.0
            )
        }

        val learningScore = cappedNormalized(learningMinutes, targets.learningMinutes)
        val productiveScore = cappedNormalized(productiveMinutes, targets.productiveMinutes)
        val exerciseScore = cappedNormalized(exerciseMinutes, targets.exerciseMinutes)
        val socialScore = cappedNormalized(socialMinutes, targets.socialMinutes)

        val bandLow = targets.sleepBand.start
        val bandHigh = targets.sleepBand.endInclusive
        val sleepScore = when {
            sleepHours <= 0.0 -> 0.0
            sleepHours in bandLow..bandHigh -> 1.0
            sleepHours in (bandLow - 1.0)..<bandLow -> 0.6
            sleepHours in bandHigh..<(bandHigh + 1.0) -> 0.8
            sleepHours < bandLow - 1.0 -> 0.3
            else -> 0.5
        }

        // Novelty is always 0..1; callers on the 0..100 engine scale divide first.
        val noveltyVal = if (hasAnyActivity) noveltyScore.coerceIn(0.0, 1.0) else 0.0
        val consistencyVal = (consecutiveDaysActive.toDouble() / 7.0).coerceAtMost(1.0)

        val learningComponent = weights.learning * learningScore * 100.0
        val productiveComponent = weights.productive * productiveScore * 100.0
        val exerciseComponent = weights.exercise * exerciseScore * 100.0
        val sleepComponent = weights.sleep * sleepScore * 100.0
        val socialComponent = weights.social * socialScore * 100.0
        val noveltyComponent = weights.novelty * noveltyVal * 100.0
        val consistencyComponent = weights.consistency * consistencyVal * 100.0

        val baseScore = learningComponent + productiveComponent + exerciseComponent +
                sleepComponent + socialComponent + noveltyComponent + consistencyComponent

        val balanceBonus = calculateBalanceBonus(learningScore, productiveScore, exerciseScore)
        val intensityBonus = calculateIntensityBonus(productiveMinutes, learningMinutes)

        val totalScore = (baseScore + balanceBonus + intensityBonus).coerceIn(0.0, 100.0)

        return GrowthScoreResult(
            totalScore = totalScore,
            learningComponent = learningComponent,
            productiveComponent = productiveComponent,
            exerciseComponent = exerciseComponent,
            sleepComponent = sleepComponent,
            socialComponent = socialComponent,
            noveltyComponent = noveltyComponent,
            consistencyComponent = consistencyComponent
        )
    }

    private fun calculateBalanceBonus(learning: Double, productive: Double, exercise: Double): Double {
        val minScore = minOf(learning, productive, exercise)
        return when {
            minScore >= 0.7 -> 3.0
            minScore >= 0.5 -> 2.0
            minScore >= 0.3 -> 1.0
            else -> 0.0
        }
    }

    private fun calculateIntensityBonus(productiveMinutes: Double, learningMinutes: Double): Double {
        val totalFocus = productiveMinutes + learningMinutes
        return when {
            totalFocus >= 360.0 -> 2.0
            totalFocus >= 240.0 -> 1.5
            totalFocus >= 180.0 -> 1.0
            else -> 0.0
        }
    }

    private fun cappedNormalized(value: Double, target: Double): Double {
        if (value <= 0.0) return 0.0
        return (value / target).coerceAtMost(1.0)
    }

    companion object {
        /**
         * The weights for the layer the user enabled. With health off the
         * sleep/exercise share moves to the productivity components instead
         * of silently scoring zeros, so the score stays meaningful.
         */
        fun weightsFor(healthEnabled: Boolean): GrowthWeights =
            if (healthEnabled) {
                GrowthWeights()
            } else {
                GrowthWeights(
                    learning = 0.30,
                    productive = 0.30,
                    exercise = 0.0,
                    sleep = 0.0,
                    social = 0.10,
                    novelty = 0.15,
                    consistency = 0.15
                )
            }

        /** Cold-start priors, replaced as soon as user history or a user target exists. */
        const val TARGET_LEARNING_MINUTES = 120.0
        const val TARGET_PRODUCTIVE_MINUTES = 240.0
        const val TARGET_EXERCISE_MINUTES = 60.0
        const val TARGET_SOCIAL_MINUTES = 30.0
    }
}
