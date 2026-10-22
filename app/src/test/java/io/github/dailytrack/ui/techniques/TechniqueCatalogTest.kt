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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechniqueCatalogTest {

    private val all = TechniqueCatalog.all

    private val searchableNames: Set<String> = all.flatMap { technique ->
        listOf(technique.title) + technique.aliases
    }.map(::normalizeTechniqueText).toSet()

    @Test
    fun `catalogue is populated and consistently ordered`() {
        assertTrue("The library needs a useful number of techniques", all.size >= 80)
        assertEquals(TechniqueCatalog.totalCount, all.size)
        assertEquals(all.size, TechniqueCatalog.byId.size)
    }

    @Test
    fun `ids are unique and url friendly`() {
        val ids = all.map { it.id }
        assertEquals("Every technique needs its own id", ids.size, ids.toSet().size)
        ids.forEach { id ->
            assertTrue("Bad id: $id", id.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*")))
        }
    }

    @Test
    fun `every technique has substantive in-depth guidance`() {
        all.forEach { technique ->
            assertTrue("${technique.id} needs a real summary", technique.summary.length >= 80)
            assertTrue("${technique.id} needs at least four steps", technique.steps.size >= 4)
            technique.steps.forEach { step ->
                assertTrue("${technique.id} has an empty step", step.isNotBlank())
            }
            assertTrue(
                "${technique.id} needs a daily-life explanation, not only a definition",
                technique.daily.length >= 260
            )
            assertTrue("${technique.id} needs a pitfall", technique.pitfall.length >= 55)
            assertTrue("${technique.id} needs a figure caption", technique.figureCaption.isNotBlank())
            assertTrue("${technique.id} needs a best-for line", technique.bestFor.isNotBlank())
            assertTrue("${technique.id} needs an alias or a merge note", technique.aliases.isNotEmpty())
        }
    }

    @Test
    fun `every category has content`() {
        TechniqueCategory.entries.forEach { category ->
            assertTrue(
                "Category ${category.key} is empty",
                TechniqueCatalog.inCategory(category).size >= 4
            )
        }
    }

    @Test
    fun `overlapping source methods are merged rather than repeated`() {
        val sources = sourceMethodNames()
        val missing = sources.filter { normalizeTechniqueText(it) !in searchableNames }
        assertTrue(
            "These source methods were not merged into any canonical entry: $missing",
            missing.isEmpty()
        )
        assertTrue(
            "Merging should reduce the list; found ${all.size} canonical from ${sources.size} source names",
            all.size < sources.size
        )
    }

    @Test
    fun `aliases are unique across the catalogue`() {
        val seen = mutableMapOf<String, String>()
        all.forEach { technique ->
            (listOf(technique.title) + technique.aliases).forEach { name ->
                val normalized = normalizeTechniqueText(name)
                val previous = seen[normalized]
                assertTrue(
                    "Duplicate name '$name' in ${technique.id} and $previous. Merge the entries instead of repeating them.",
                    previous == null || previous == technique.id
                )
                seen[normalized] = technique.id
            }
        }
    }

    @Test
    fun `starter stack covers decide start focus and review`() {
        assertEquals(4, TechniqueCatalog.starterStack.size)
        assertEquals(
            listOf(
                TechniqueCategory.DECIDE,
                TechniqueCategory.START,
                TechniqueCategory.FOCUS,
                TechniqueCategory.REVIEW
            ),
            TechniqueCatalog.starterStack.map { it.category }
        )
        assertTrue(TechniqueCatalog.starterStack.all { it.id == TechniqueCatalog.find(it.id)?.id })
    }

    @Test
    fun `search finds title alias and practical problem`() {
        assertEquals("eisenhower-matrix", searchTechniques(all, "eisenhower").single().id)
        assertEquals("pareto-principle", searchTechniques(all, "80/20").single().id)
        assertTrue(searchTechniques(all, "phone").any { it.id == "distraction-firewall" })
        assertTrue(searchTechniques(all, "sleep").any { it.id == "sleep-priority" })
        assertTrue(searchTechniques(all, "gtd").any { it.id == "gtd" })
        assertTrue(searchTechniques(all, "rubber duck").any { it.id == "explain-out-loud" })
    }

    @Test
    fun `search is token based and category filter is strict`() {
        val focusPhone = searchTechniques(all, "focus phone")
        assertTrue(focusPhone.isNotEmpty())
        assertTrue(focusPhone.none { it.id == "spaced-repetition" })

        val learning = searchTechniques(all, "", TechniqueCategory.LEARNING)
        assertEquals(TechniqueCatalog.inCategory(TechniqueCategory.LEARNING).size, learning.size)
        assertTrue(learning.all { it.category == TechniqueCategory.LEARNING })

        val noMatch = searchTechniques(all, "category-that-does-not-exist")
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `grouping preserves category order and does not lose results`() {
        val grouped = groupTechniques(all)
        assertEquals(TechniqueCategory.entries.filter { c -> all.any { it.category == c } }, grouped.map { it.first })
        assertEquals(all.size, grouped.sumOf { it.second.size })
        grouped.forEach { (category, items) ->
            assertTrue(items.all { it.category == category })
        }
    }

    @Test
    fun `every technique has a figure and a usable app shortcut`() {
        all.forEach { technique ->
            assertNotNull("${technique.id} has no figure", technique.figure)
            assertTrue("${technique.id} needs a valid category", technique.category in TechniqueCategory.entries)
        }
        val withShortcuts = all.count { it.appActions.isNotEmpty() }
        assertTrue("Most techniques should connect to an existing app feature", withShortcuts >= 40)
    }

    private fun sourceMethodNames(): List<String> = listOf(
        // Decide
        "Eisenhower Matrix",
        "Pareto Principle",
        "80/20 Rule",
        "MIT",
        "Most Important Tasks",
        "Eat the Frog",
        "Ivy Lee Method",
        "ABCDE Method",
        "MoSCoW",
        "RICE scoring",
        "Value vs Effort matrix",
        "Warren Buffett 25/5",
        "Rule of 3",
        "Opportunity cost thinking",
        "10-10-10 rule",
        "Regret minimization",
        // Start
        "5-Second Rule",
        "2-Minute Rule",
        "Implementation intentions",
        "Temptation bundling",
        "Swiss Cheese Method",
        "Just 5 minutes",
        "Task chunking",
        "Worst-first",
        "Ugly-first",
        "Commitment devices",
        "Body doubling",
        "Eat the elephant one bite at a time",
        "Zeigarnik effect trick",
        "Procrastination journaling",
        "Set up the environment the night before",
        // Time
        "Pomodoro Technique",
        "Time blocking",
        "Timeboxing",
        "Day theming",
        "Parkinson's Law",
        "Hofstadter's Law reminder",
        "Task batching",
        "Flowtime Technique",
        "52/17 method",
        "90-minute ultradian cycles",
        "Rule of 52 variants and custom intervals",
        "Calendar-first planning",
        "Time audit",
        "Buffer time",
        "Shutdown routine",
        "Morning routine",
        "Evening routine",
        "Early bird or night owl alignment",
        // Focus
        "Focused Work Blocks",
        "Deep Work",
        "Single-tasking",
        "Phone in another room",
        "Notification batching",
        "Website and app blockers",
        "Inbox zero",
        "Email batching",
        "Distraction list",
        "Time-restricted environment cues",
        "Music or noise strategy",
        "Flow state conditions",
        "Attention residue awareness",
        "Digital minimalism",
        "Monk mode",
        // Systems
        "Getting Things Done",
        "GTD",
        "Kanban",
        "Bullet Journal",
        "Personal Kanban",
        "WIP limits",
        "Scrum",
        "Sprints",
        "Agile retrospectives",
        "Next action",
        "Someday/Maybe list",
        "Waiting-for list",
        "Projects list + weekly review",
        "OKRs",
        "SMART goals",
        "WOOP",
        "Backward planning",
        "Work breakdown structure",
        "Critical path thinking",
        "Minimum viable product thinking",
        "Pre-mortem",
        "Post-mortem",
        "After-action review",
        "Checklists",
        "Templates and SOPs",
        // Clarity
        "Brain dump",
        "Inbox capture",
        "Second brain",
        "PARA",
        "Zettelkasten",
        "Daily notes",
        "Journaling",
        "Morning pages",
        "Mind mapping",
        "Gratitude and reflection prompts",
        "Decision journal",
        // Habits
        "1% Rule",
        "Habit stacking",
        "Never miss twice",
        "Don't break the chain",
        "Atomic habits four laws",
        "Two-day rule and minimum version",
        "Environment design",
        "Habit tracking",
        "Keystone habits",
        "Identity-based habits",
        "Reward systems",
        "Accountability partner",
        // Learning
        "Active Recall",
        "Feynman Technique",
        "Spaced repetition",
        "Interleaving",
        "Retrieval practice and practice tests",
        "SQ3R",
        "Cornell notes",
        "Mnemonics",
        "Memory palace",
        "Chunking",
        "Elaboration",
        "Dual coding",
        "Teach someone else",
        "Deliberate practice",
        "Learning by projects",
        "Pretesting",
        "Rubber duck debugging",
        "Learn in layers",
        "Overview first",
        "Sleep consolidation",
        // Energy
        "Energy management",
        "Sleep priority",
        "Exercise breaks",
        "Walking",
        "Hydration and nutrition rhythm",
        "Strategic breaks",
        "Non-sleep deep rest",
        "Short naps",
        "Mindfulness and breathing",
        "Digital sabbath",
        "Cold start rituals",
        "Saying no",
        "Boundaries",
        "Self-compassion",
        "Growth mindset",
        "Visualization",
        "Mental contrasting",
        "Progress principle",
        // Review
        "Weekly review",
        "Daily review",
        "Evening shutdown",
        "Monthly and quarterly reviews",
        "Start/Stop/Continue",
        "Time and energy audits",
        "Kaizen",
        "Eliminate, automate, delegate",
        // Together
        "Delegation",
        "Meeting hygiene",
        "Async communication",
        "Office hours",
        "Interruption windows",
        "Written decisions and docs"
    )
}
