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

internal val habitsLearningTechniques = listOf(
    Technique(
        id = "one-percent-rule",
        title = "1% Rule",
        aliases = listOf("1% Rule", "One percent rule", "1 percent rule"),
        category = TechniqueCategory.HABITS,
        figure = FigureKind.STAIRS,
        bestFor = "Skills and habits that need patience",
        summary = "Improve a little every day and let the gains compound. The daily step is small on purpose; the direction matters more than the size.",
        steps = listOf(
            "Choose one skill or behaviour you want to improve over months.",
            "Define what a one percent improvement looks like today: one page, one rep, one minute.",
            "Do the small action at a consistent time.",
            "Track that you showed up rather than judging the size of the gain.",
            "Review monthly and increase the step only when the habit is stable."
        ),
        daily = "Make the daily step small enough that a bad day cannot erase it: one page of reading, one minute of practice, one set of an exercise. Attach it to an existing routine so it does not need a new slot. The compounding is real only if the action is repeatable; a heroic session followed by a week off is not one percent a day. Look at the monthly trend, not the daily fluctuation.",
        pitfall = "People chase dramatic progress and quit when the daily gain feels invisible. Measure consistency, then let the results lag.",
        figureCaption = "Small upward steps that compound over time.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "habit-stacking",
        title = "Habit Stacking",
        aliases = listOf("Habit stacking", "Habit stack"),
        category = TechniqueCategory.HABITS,
        figure = FigureKind.STACK,
        bestFor = "Adding a new habit to a stable routine",
        summary = "Attach the new habit to an existing one using the formula: after current habit, I will do new habit.",
        steps = listOf(
            "Choose a current habit that happens reliably every day.",
            "Write the link: After I do X, I will do Y.",
            "Make the new habit tiny enough to finish in under two minutes.",
            "Rehearse the transition once and put a visual cue where the anchor happens.",
            "Track it for two weeks before adding another link."
        ),
        daily = "After I pour my morning coffee, I will write one sentence in my journal. After I brush my teeth, I will floss one tooth. The anchor must be stable; a habit that only happens three times a week is a weak foundation. Keep the new action smaller than you think it should be. Once the link feels automatic, you can grow the action without changing the cue.",
        pitfall = "A shaky anchor makes a shaky stack. Choose a habit that already happens without reminders.",
        figureCaption = "Blocks stacked on top of an existing routine.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "never-miss-twice",
        title = "Never Miss Twice",
        aliases = listOf("Never miss twice", "Two-day rule", "Minimum version", "Two-day rule and minimum version"),
        category = TechniqueCategory.HABITS,
        figure = FigureKind.SPRINT,
        bestFor = "Recovering from a missed habit without quitting",
        summary = "One missed day is normal; two in a row starts a new pattern. Keep a tiny minimum version for hard days.",
        steps = listOf(
            "Define the full version and a minimum version of the habit.",
            "Track the days you show up.",
            "If you miss once, do the minimum version the next day no matter what.",
            "Do not try to repay the missed day with a double session.",
            "Review the trigger that caused the miss and adjust it."
        ),
        daily = "The minimum version should take under two minutes: one page, one stretch, one sentence. It keeps the identity and the routine alive when the full version is impossible. If you miss twice, do not shame yourself; make the next action even smaller and put it in the calendar. Consistency is built by returning quickly, not by never failing.",
        pitfall = "The minimum version can slowly become the only version. Use it for recovery, not as a permanent hiding place.",
        figureCaption = "A habit loop with one missed day and an immediate return.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "chain-tracking",
        title = "Don't Break the Chain + Tracking",
        aliases = listOf("Don't break the chain", "Dont break the chain", "Break the chain", "Habit tracking", "Habit tracker"),
        category = TechniqueCategory.HABITS,
        figure = FigureKind.CHECKLIST,
        bestFor = "Building visible consistency",
        summary = "Mark every day you show up and use the visible chain as gentle motivation, with a minimum version for hard days.",
        steps = listOf(
            "Choose a habit and a simple way to mark it.",
            "Put the tracker where you will see it daily.",
            "Mark the day immediately after the action.",
            "If the chain breaks, use the never-miss-twice rule and restart.",
            "Review the chain weekly; celebrate consistency, not perfection."
        ),
        daily = "A paper calendar with an X is often more satisfying than a hidden app, but the app works if you open it daily. Track only a few habits; a wall of empty boxes creates guilt rather than motivation. The chain is feedback, not a punishment. If you miss two days, restart with the minimum version and keep the old chain visible as evidence that you can do it.",
        pitfall = "Streak anxiety can make one miss feel like total failure. The streak is a tool; the habit is the goal.",
        figureCaption = "A calendar with a visible chain of completed days.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "atomic-laws-environment",
        title = "Atomic Habits Four Laws + Environment Design",
        aliases = listOf("Atomic habits four laws", "Atomic Habits", "Four laws of behavior change", "Environment design"),
        category = TechniqueCategory.HABITS,
        figure = FigureKind.STACK,
        bestFor = "Making good habits easier and bad habits harder",
        summary = "Make the habit obvious, attractive, easy and satisfying; design the environment so the cues for good habits are in sight and the cues for bad ones are hidden.",
        steps = listOf(
            "Make the cue obvious: put the water, book or gym bag where you will see it.",
            "Make the habit attractive: pair it with something you enjoy or join people who do it.",
            "Make it easy: reduce the friction and shrink the first step.",
            "Make it satisfying: add an immediate small reward.",
            "Invert the laws for habits you want to stop: hide the cue, make it difficult and unsatisfying."
        ),
        daily = "Change one part of the environment this week: put the phone in a drawer during work, leave the running shoes by the door, move the snacks out of sight. You do not need to redesign the whole house. A good environment makes the desired action the path of least resistance. Review your spaces monthly and ask which cue is quietly working against you.",
        pitfall = "Relying on motivation ignores the environment. If the cue is invisible, the habit will be inconsistent.",
        figureCaption = "Cues arranged around a habit loop.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "keystone-identity",
        title = "Keystone + Identity Habits",
        aliases = listOf("Keystone habits", "Identity-based habits", "Identity based habits", "Keystone habit"),
        category = TechniqueCategory.HABITS,
        figure = FigureKind.MIRROR,
        bestFor = "Habits that change how you see yourself",
        summary = "Focus on a habit that triggers others, and act from the identity you want rather than chasing a single outcome.",
        steps = listOf(
            "Choose the identity you want to strengthen: a healthy person, a writer, a reliable colleague.",
            "Find the smallest daily action that would be evidence for that identity.",
            "Pick a keystone habit such as sleep, exercise or planning that improves other areas.",
            "Track the evidence, not just the result.",
            "Tell yourself the identity statement before the action."
        ),
        daily = "Before the workout, say: I am someone who trains today. Before writing, say: I am a person who writes. The statement is not magic; it changes which action feels consistent. Keystone habits such as regular sleep or a daily walk often improve diet, mood and focus without direct effort. Choose one keystone and one identity action, not a whole new personality.",
        pitfall = "An identity without evidence becomes a slogan. Keep the daily action small enough to provide real proof.",
        figureCaption = "A mirror reflecting the person you are becoming.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "rewards-accountability",
        title = "Rewards + Accountability",
        aliases = listOf("Reward systems", "Reward system", "Rewards", "Accountability partner", "Accountability"),
        category = TechniqueCategory.HABITS,
        figure = FigureKind.TROPHY,
        bestFor = "Keeping motivation and follow-through",
        summary = "Celebrate small wins immediately, and give someone else permission to check whether you did what you said.",
        steps = listOf(
            "Choose a small, immediate reward for the habit.",
            "Make the reward available only after the action.",
            "Tell one person the specific commitment and the check-in time.",
            "Share progress briefly at the check-in, including misses.",
            "Adjust the reward or the accountability rhythm if it stops helping."
        ),
        daily = "A reward can be a coffee, a walk or a checkmark; it does not need to be expensive. For accountability, a weekly message to a friend is often enough: here is what I planned, here is what I did. Choose someone who will ask a real question rather than politely nodding. The goal is support, not surveillance.",
        pitfall = "Rewards that undermine the habit, such as junk food after every workout, replace one problem with another. Keep the reward small and aligned.",
        figureCaption = "A trophy and a check-in message.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "active-recall",
        title = "Active Recall + Pretesting",
        aliases = listOf("Active Recall", "Active recall", "Retrieval practice", "Retrieval practice and practice tests", "Practice tests", "Pretesting"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.OPEN_BOOK,
        bestFor = "Remembering what you study",
        summary = "Close the notes and retrieve what you remember, or try questions before you learn, so the brain practices the skill it needs at test time.",
        steps = listOf(
            "Read or study the material once.",
            "Close the source and write everything you remember on a blank page.",
            "Check the gaps and correct them.",
            "Turn the gaps into questions or flashcards.",
            "Before the next session, answer those questions from memory."
        ),
        daily = "Use a ten-minute blank-page recall at the end of a study session. Before reading a chapter, spend two minutes guessing the key questions it will answer; being wrong primes the brain to notice the answer. Retrieval feels harder than rereading, which is why it works. Schedule recall rather than hoping it happens while highlighting.",
        pitfall = "Recognition is not recall. Looking at a highlighted page and feeling familiar does not mean you can produce the idea.",
        figureCaption = "A closed book with a blank recall page.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "explain-out-loud",
        title = "Explain It Out Loud",
        aliases = listOf("Feynman Technique", "Feynman", "Teach someone else", "Teach someone", "Rubber duck debugging", "Rubber duck"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.SPEECH,
        bestFor = "Finding the gaps in your understanding",
        summary = "Explain the idea in simple language to a novice, a colleague or a rubber duck; the places where you stumble are the places to study.",
        steps = listOf(
            "Choose the concept and write its name at the top of a page.",
            "Explain it in plain words as if to someone who does not know it.",
            "Notice where you reach for jargon or lose the thread.",
            "Return to the source for those gaps, then explain again.",
            "Simplify until a beginner could follow the main idea."
        ),
        daily = "Use the Feynman technique after reading something important: explain it in five sentences without looking. For code, explain the problem aloud to a rubber duck before changing anything; the act of narrating often reveals the faulty assumption. Offering to teach a concept to a colleague is one of the fastest ways to find out whether you really know it.",
        pitfall = "Repeating jargon is not explaining. If you cannot replace a technical term with a plain example, the gap is still there.",
        figureCaption = "A speech bubble explaining an idea simply.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "spaced-repetition",
        title = "Spaced Repetition",
        aliases = listOf("Spaced repetition", "Spaced repetition system", "SRS"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.SPACED,
        bestFor = "Long-term memory of facts and vocabulary",
        summary = "Review material at increasing intervals so each review happens just as the memory starts to fade.",
        steps = listOf(
            "Turn key facts into short question-and-answer cards.",
            "Review the cards and rate how easily you recalled each one.",
            "Schedule the next review at a longer interval for easy cards and a shorter one for hard cards.",
            "Keep sessions short and daily.",
            "Retire cards only when the knowledge is genuinely automatic."
        ),
        daily = "Ten minutes of spaced review in the morning is enough for most personal decks. Add cards from what you read, but keep them atomic: one fact per card. A typical sequence is one day, three days, one week, two weeks, one month, but the exact schedule matters less than consistent retrieval. If a deck grows faster than you can review, stop adding and prune.",
        pitfall = "Too many cards creates review debt. Make fewer, better cards and delete the ones that no longer serve you.",
        figureCaption = "Review intervals expanding over time.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "interleaving",
        title = "Interleaving",
        aliases = listOf("Interleaving", "Interleaved practice"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.WEAVE,
        bestFor = "Learning to choose the right method",
        summary = "Mix related topics or problem types in one session instead of practising one type in a long block.",
        steps = listOf(
            "Choose two or three related topics or problem types.",
            "Alternate between them within a session.",
            "Before each problem, decide which approach fits rather than following a cue.",
            "Review the mistakes that come from choosing the wrong method.",
            "Keep the mix related enough that comparison is useful."
        ),
        daily = "For maths or coding, mix problem types after you have learned the basics. For languages, mix vocabulary, listening and speaking rather than spending a whole hour on one drill. Interleaving feels harder and produces more errors during practice, which is part of why it improves transfer. Do not interleave completely unrelated subjects in the same short session.",
        pitfall = "Interleaving too early, before the basics are understood, creates confusion. Learn the pieces, then mix them.",
        figureCaption = "Two strands of practice woven together.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "sq3r-cornell",
        title = "SQ3R + Cornell Notes",
        aliases = listOf("SQ3R", "Cornell notes", "Cornell Notes", "Cornell note taking"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.DOC,
        bestFor = "Reading textbooks and taking useful notes",
        summary = "Survey, question, read, recite and review; structure notes with cues, notes and a summary so they support recall.",
        steps = listOf(
            "Survey the chapter: headings, diagrams, summary and questions.",
            "Turn the headings into questions you want to answer.",
            "Read actively and take notes on the right side of a Cornell page.",
            "Recite the answers aloud without looking, then write cues in the left column.",
            "Add a short summary at the bottom and review it later."
        ),
        daily = "Use SQ3R for a chapter you actually need to learn, not every article you open. The question step is what turns passive reading into a search. Use the Cornell cue column to write quiz questions, then cover the notes and answer them. A five-minute review of the summary the next day is more valuable than a second highlighter pass.",
        pitfall = "Copying the textbook word for word is not note-taking. Use your own words and leave the page with questions.",
        figureCaption = "A Cornell page with cues, notes and a summary.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "mnemonics-palace",
        title = "Mnemonics + Memory Palace",
        aliases = listOf("Mnemonics", "Memory palace", "Method of loci"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.PALACE,
        bestFor = "Remembering lists, terms and sequences",
        summary = "Turn abstract information into vivid images and place them along a familiar route so the mind can retrieve them in order.",
        steps = listOf(
            "Choose a familiar route with distinct locations, such as your home.",
            "Turn each item into a vivid, exaggerated image.",
            "Place one image at each location in order.",
            "Walk the route mentally and retrieve the images.",
            "Refresh the route with spaced review and replace weak images."
        ),
        daily = "Use a memory palace for a speech, a shopping list or a set of terms you need in sequence. The images should be unusual, funny or sensory; boring images do not stick. Walk the route once before an event. For long-term knowledge, combine the palace with spaced retrieval rather than relying on a single dramatic memorisation session.",
        pitfall = "A palace without review fades like any other memory. Use it as an encoding tool, not a replacement for retrieval.",
        figureCaption = "A familiar building with items placed along a route.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "chunking-elaboration",
        title = "Chunking, Elaboration + Layered Learning",
        aliases = listOf("Chunking", "Elaboration", "Learn in layers", "Learning in layers", "Overview first", "Learn in layers overview first detail later"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.STACK,
        bestFor = "Understanding complex material",
        summary = "Group information into meaningful units, connect it to what you already know, and get the big picture before drilling into detail.",
        steps = listOf(
            "Skim the material for its structure and main idea first.",
            "Group related facts into three to five meaningful chunks.",
            "Connect each chunk to something you already understand.",
            "Explain the relationships between chunks in your own words.",
            "Drill into the details only after the overview is clear."
        ),
        daily = "Before reading a dense chapter, look at the headings and write a one-paragraph overview. When you meet a list, group it into a few categories rather than memorising items in isolation. Ask how the new idea relates to an old one; elaboration is the act of building those links. If you get lost in detail, zoom out and rebuild the map.",
        pitfall = "Chunks that are too large become vague. Keep each chunk small enough to explain in one sentence.",
        figureCaption = "A big picture broken into meaningful chunks.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "dual-coding",
        title = "Dual Coding",
        aliases = listOf("Dual coding", "Dual Coding Theory"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.DUAL,
        bestFor = "Understanding processes and relationships",
        summary = "Combine words with diagrams or sketches so the same idea is encoded through two channels and the relationships become visible.",
        steps = listOf(
            "Read or listen to the explanation.",
            "Draw the process, structure or relationship from memory.",
            "Label the drawing with short words, not paragraphs.",
            "Explain the drawing aloud and check it against the source.",
            "Redraw it from memory later to test the model."
        ),
        daily = "When you learn a process, draw the boxes and arrows instead of only writing bullet points. For a concept, sketch a simple diagram and label the parts. The drawing does not need to be artistic; it needs to show relationships. Pair the sketch with a short verbal explanation, then test yourself by redrawing without looking.",
        pitfall = "Decorative images do not count. The picture must carry the explanation, not just make the page look nice.",
        figureCaption = "Words and a diagram describing the same idea.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "deliberate-practice",
        title = "Deliberate Practice",
        aliases = listOf("Deliberate practice", "Deliberate practise"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.TARGET,
        bestFor = "Getting better at a specific skill",
        summary = "Practise the weakest part of a skill on purpose, with immediate feedback and a clear target, rather than repeating what you already do well.",
        steps = listOf(
            "Define the skill and the specific sub-skill you want to improve.",
            "Find your current weak point, ideally with a teacher or recording.",
            "Design a short drill that isolates that weakness.",
            "Get feedback immediately and adjust on the next repetition.",
            "Track the drill and increase difficulty only when it becomes reliable."
        ),
        daily = "Spend 20 to 30 minutes on a narrow drill rather than an hour of general practice. For speaking, record and review one segment. For coding, solve a problem type that exposes your weakness. For music, slow the difficult bar down. Deliberate practice is uncomfortable by design; if it feels easy and fluent, you may be performing rather than improving.",
        pitfall = "Mindless repetition strengthens existing habits, including bad ones. You need a target, feedback and adjustment.",
        figureCaption = "A target with an arrow focused on the weak spot.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "project-learning",
        title = "Project-Based Learning",
        aliases = listOf("Learning by projects", "Project-based learning", "Project based learning"),
        category = TechniqueCategory.LEARNING,
        figure = FigureKind.BUILD,
        bestFor = "Turning knowledge into usable skill",
        summary = "Learn by building something real, pulling in the theory just in time and shipping a small result.",
        steps = listOf(
            "Choose a small project with a visible output.",
            "Define the scope and the first deliverable.",
            "Learn the minimum needed to build the next piece, then build it.",
            "Get feedback from a real user or a test.",
            "Write down what you learned and choose the next project."
        ),
        daily = "Replace one tutorial session with a small build. If you are learning a language, write a short letter; if you are learning data, analyse a real dataset; if you are learning design, redesign one screen. Keep the scope small enough to finish in a week. The project creates a reason to remember the theory and exposes the gaps that reading alone hides.",
        pitfall = "Tutorial hell feels productive. If you have not built anything without a guide, the learning is incomplete.",
        figureCaption = "A small real build assembled from learning blocks.",
        appActions = listOf(AppAction.TODO)
    )
)
