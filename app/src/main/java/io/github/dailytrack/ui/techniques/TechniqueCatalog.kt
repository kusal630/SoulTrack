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

/**
 * The single canonical library. Overlapping names from the original method
 * list are merged into one entry and kept as aliases, so the guide teaches one
 * practice instead of repeating the same idea under five names.
 *
 * The order here is the reading order of the screen, not alphabetical: it
 * follows a normal day from deciding what matters to reviewing how it went.
 */
object TechniqueCatalog {
    val all: List<Technique> =
        decideTechniques +
            timeFocusTechniques +
            systemsClarityTechniques +
            habitsLearningTechniques +
            energyReviewTogetherTechniques

    val byId: Map<String, Technique> = all.associateBy { it.id }

    fun find(id: String?): Technique? = id?.let(byId::get)

    fun inCategory(category: TechniqueCategory): List<Technique> =
        all.filter { it.category == category }

    /**
     * The smallest useful stack from the original note: one technique to
     * decide, one to start, one to focus and one to review. Semantically
     * different methods are deliberately kept; truly overlapping methods were
     * merged in the catalogue above.
     */
    val starterStack: List<Technique> = listOfNotNull(
        find("eisenhower-matrix"),
        find("two-minute-rule"),
        find("deep-work-blocks"),
        find("weekly-review")
    )

    val totalCount: Int get() = all.size
}
