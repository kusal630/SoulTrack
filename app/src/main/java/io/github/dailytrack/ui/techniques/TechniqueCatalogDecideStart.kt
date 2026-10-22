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

internal val decideTechniques = listOf(
    Technique(
        id = "eisenhower-matrix",
        title = "Eisenhower Matrix",
        aliases = listOf("Urgent-Important Matrix", "Urgent Important Matrix"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.MATRIX,
        bestFor = "When everything feels urgent",
        summary = "Sort every task by urgency and importance, then let the quadrant decide whether you do it now, schedule it, delegate it or delete it.",
        steps = listOf(
            "Draw a 2x2 grid: urgent and important on the axes.",
            "Put every open task into exactly one quadrant, including the small ones.",
            "Do urgent plus important now; schedule important but not urgent.",
            "Delegate or automate urgent but not important; delete neither.",
            "Protect a recurring block for the important, not urgent quadrant."
        ),
        daily = "Run a five-minute pass before opening email. Put today's tasks into the grid, then choose two items for the do-now quadrant and one 60 to 90 minute block for the schedule quadrant. The important, not urgent box is where goals, health and learning live, so treat that block as a real appointment. If the do-now quadrant has more than three items, ask which crisis could have been prevented by a scheduled block last week.",
        pitfall = "Urgency feels like importance, so people spend the day in the do-now box and call it productive. Protect the second quadrant deliberately.",
        figureCaption = "Four quadrants: do now, schedule, delegate, delete.",
        appActions = listOf(AppAction.TODO, AppAction.TIMER)
    ),
    Technique(
        id = "pareto-principle",
        title = "Pareto Principle",
        aliases = listOf("80/20 Rule", "Pareto 80 20", "The 80 20 Principle"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.PARETO,
        bestFor = "Finding leverage in a busy week",
        summary = "A small share of your activities usually creates most of the result. Find that 20 percent and give it more room instead of optimising everything equally.",
        steps = listOf(
            "Track your work for one normal week in rough categories.",
            "List the results each activity produced, not the hours it consumed.",
            "Rank the activities by result and circle the few that carry the most value.",
            "Double the time or quality of that small set.",
            "Reduce, batch or drop the activities that produce little."
        ),
        daily = "Use Pareto as a Friday question, not a daily excuse to ignore maintenance. Ask: which three activities made the other work easier or produced most of the visible value? Schedule those first next week. For a project, look for the single deliverable that would make the rest optional. Keep a short leverage list in the Todo screen so the important twenty percent survives a busy Monday.",
        pitfall = "Pareto is a diagnostic, not permission to skip the small maintenance tasks that keep the system running. Look for the vital few, not the easy few.",
        figureCaption = "A few tall bars create most of the output.",
        appActions = listOf(AppAction.INSIGHTS)
    ),
    Technique(
        id = "three-priority-day",
        title = "Three-Priority Day",
        aliases = listOf("MIT", "Most Important Tasks", "Rule of 3", "Warren Buffett 25/5", "Warren Buffett 25 5", "25/5 Rule"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.THREE_CARDS,
        bestFor = "Days with too many competing tasks",
        summary = "Choose one to three outcomes that would make the day a success and treat everything else as optional. The long-term version prunes goals the same way.",
        steps = listOf(
            "List everything that could be done today.",
            "Choose no more than three outcomes that would make today count.",
            "Write each one as a finish line, not an activity.",
            "Schedule them before reactive work takes the best hours.",
            "For goals, use the Buffett variant: list 25, circle 5, and actively avoid the other 20."
        ),
        daily = "Pick the three at the end of the previous day or in the first five minutes of the morning. Put them at the top of Todo and give the first one your best block. If more than three things feel mandatory, rank them and move the rest to tomorrow or Someday. Review at shutdown: did the day serve the three, or did it merely stay busy?",
        pitfall = "Three is a ceiling, not a target. Choosing five important tasks is the same failure as choosing twenty.",
        figureCaption = "Three numbered cards, with the rest moved aside.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "eat-the-frog",
        title = "Eat the Frog",
        aliases = listOf("Worst-first", "Worst first", "Ugly-first", "Ugly first", "Eat That Frog"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.FROG,
        bestFor = "Avoiding the task you keep postponing",
        summary = "Do the hardest or most valuable task first, while willpower and quiet are still available. Everything after it feels lighter.",
        steps = listOf(
            "Identify the frog: the task you are most likely to postpone and would most regret skipping.",
            "Define the first physical action, not the whole project.",
            "Start before email, messages or news.",
            "Use a timer for the first 25 minutes; continuing is optional, starting is not.",
            "Give yourself a small, pre-decided reward when the frog is done."
        ),
        daily = "Choose tomorrow's frog before you close the laptop. In the morning put the phone in another room, open only the frog, and set a 25-minute timer. Do not check messages until the first session ends. If the frog is genuinely blocked, spend ten minutes removing the block or writing the question you need answered. The point is to make the most valuable task the first thing, not the most unpleasant busywork.",
        pitfall = "People pick the most unpleasant task instead of the most valuable one. The frog must matter, not merely feel bad.",
        figureCaption = "The hard task is moved to the front of the queue.",
        appActions = listOf(AppAction.TIMER, AppAction.TODO)
    ),
    Technique(
        id = "ivy-lee-abcde",
        title = "Ivy Lee Method + ABCDE",
        aliases = listOf("Ivy Lee Method", "ABCDE Method", "ABCDE", "ABCDE prioritisation"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.ORDERED_LIST,
        bestFor = "A simple end-of-day planning ritual",
        summary = "Write a short list the night before, rank it by consequence, and work from the top. The Ivy Lee six-task list and the ABCDE labels are two versions of the same discipline.",
        steps = listOf(
            "At the end of the day, write no more than six unfinished tasks.",
            "Rank them 1 to 6, or label each A to E by consequence.",
            "A items are must-do and carry real consequences; B should be done; C is nice; D can be delegated; E can be eliminated.",
            "The next day start at A1 or number one and stay there until it is done.",
            "Move unfinished items to tomorrow and rebuild the ranking."
        ),
        daily = "Keep the ritual to ten minutes before shutdown. Write tasks as verbs with a clear finish, for example Draft the client brief, not Client brief. In the morning do not renegotiate the list before starting. If an interruption arrives, write it in a capture line and return to the top item. Re-rank only when the facts change, not when the work gets hard.",
        pitfall = "The list turns into a wish list of vague nouns. Use concrete actions and a small number you can genuinely finish.",
        figureCaption = "A short ordered list with the first item highlighted.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "moscow",
        title = "MoSCoW",
        aliases = listOf("MoSCoW Method", "Must Should Could Won't", "Must Should Could Wont"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.ORDERED_LIST,
        bestFor = "Project scope and deadline negotiation",
        summary = "Force scope into four buckets: Must have, Should have, Could have and Will not have this time. The explicit won't list is what protects the deadline.",
        steps = listOf(
            "State the deadline and the available capacity first.",
            "List all candidate requirements without judging them.",
            "Put each into Must, Should, Could or Won't for this cycle.",
            "Check that Must items fit in roughly 60 percent of capacity.",
            "When a new request arrives, ask what moves to Won't to pay for it."
        ),
        daily = "Use MoSCoW on one project at a time, not on every small task. Keep the four buckets in a note or as tags in Todo. At the weekly review, re-read the Won't list; it is normal for an item to move up next cycle, but only by moving something else down. In conversation, say explicitly: this is a Could, not a Must.",
        pitfall = "Everything becomes a Must. If more than about 60 percent is mandatory, the plan is not a plan, it is a wish.",
        figureCaption = "Requirements sorted into a strict priority order.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "rice-value-effort",
        title = "RICE + Value/Effort",
        aliases = listOf("RICE scoring", "RICE", "Value vs Effort matrix", "Value vs Effort", "Value/Effort Matrix"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.MATRIX,
        bestFor = "Ranking a backlog of opportunities",
        summary = "Score ideas by Reach, Impact, Confidence and Effort, or use the quick value-versus-effort grid when a full score would be overkill.",
        steps = listOf(
            "List the candidate ideas or projects.",
            "Score Reach (people helped), Impact (1 to 3), Confidence (percent) and Effort (person-weeks).",
            "Calculate RICE as Reach times Impact times Confidence divided by Effort.",
            "For a fast pass, place each idea on a value versus effort grid.",
            "Do high-value, low-effort items now; schedule high-value, high-effort bets; drop low-value, high-effort work."
        ),
        daily = "Keep this for a weekly backlog session, not every task. Add the scores to the task notes so future-you can see why something was parked. The value versus effort grid is often enough: if you cannot say who benefits and what changes, the value score is a guess. Re-score after two weeks because confidence changes with evidence.",
        pitfall = "The numbers create false precision. Treat RICE as a conversation starter and use ranges when you are unsure.",
        figureCaption = "Opportunities placed by value and effort.",
        appActions = listOf(AppAction.TODO, AppAction.INSIGHTS)
    ),
    Technique(
        id = "decision-lenses",
        title = "Decision Lenses",
        aliases = listOf("Opportunity cost thinking", "Opportunity cost", "10-10-10 rule", "10/10/10 Rule", "Regret minimization", "Regret minimisation"),
        category = TechniqueCategory.DECIDE,
        figure = FigureKind.SCALES,
        bestFor = "Saying yes, saying no and irreversible choices",
        summary = "Run a decision through three lenses: what you give up, how it will feel at three time scales, and which choice you would regret least.",
        steps = listOf(
            "Write the decision in one sentence and the date you must make it.",
            "Opportunity cost: list what you cannot do if you say yes.",
            "10-10-10: how will this feel in 10 minutes, 10 months and 10 years?",
            "Regret minimisation: at 80, which option would you regret not taking?",
            "Choose the smallest reversible next step and set a review date."
        ),
        daily = "Use the lenses only for decisions that are expensive to reverse. For a request, answer with a one-line opportunity cost: I can do this if we move the launch. For a big choice, set a ten-minute timer, write the three lenses, then act. Record the reasoning in a decision journal so you can compare the story with the outcome later.",
        pitfall = "Analysis becomes paralysis. Cap the exercise and prefer a reversible experiment when the evidence is thin.",
        figureCaption = "A balance scale weighing two options.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "five-second-rule",
        title = "5-Second Rule",
        aliases = listOf("Five Second Rule", "5 Second Rule"),
        category = TechniqueCategory.START,
        figure = FigureKind.COUNTDOWN,
        bestFor = "Hesitation before a known first step",
        summary = "Count down 5-4-3-2-1 and move before the brain has time to build a case for delay.",
        steps = listOf(
            "Notice the moment you start negotiating with yourself.",
            "Breathe once and count down clearly: 5, 4, 3, 2, 1.",
            "Physically move toward the task on one.",
            "Shrink the action to the first tiny step.",
            "Repeat for recurring hesitation moments until starting becomes less dramatic."
        ),
        daily = "Use it for starting, not for deciding whether to start. Count down out loud before opening the document, getting out of bed or putting on running shoes. Pair it with a two-minute action so the movement has a target. Do not use it for important decisions that deserve thought; it is a starter motor, not a steering wheel.",
        pitfall = "It cannot replace planning. If you keep hesitating, the task may be unclear, so define the first action first.",
        figureCaption = "A countdown from five to one with a forward arrow.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "two-minute-rule",
        title = "2-Minute Rule",
        aliases = listOf("Two-Minute Rule", "2 Minute Rule", "Two Minute Rule"),
        category = TechniqueCategory.START,
        figure = FigureKind.TWO_MINUTE,
        bestFor = "Small tasks and starting big ones",
        summary = "If a task takes under two minutes, do it now. If it is big, make the first version take two minutes so starting is almost free.",
        steps = listOf(
            "Keep a short list of genuinely two-minute actions.",
            "Do them immediately instead of capturing them for later.",
            "For a large task, define a two-minute entry action: open the file, write one sentence, make one call.",
            "Stop after the two minutes if you want; continuing is a bonus.",
            "Watch the boundary so the rule does not become an excuse to avoid deep work."
        ),
        daily = "Use the rule between blocks for small admin: reply to the one-line message, file the receipt, put the dish away. Use the entry version before deep work; opening the document for two minutes is often enough to get past the friction. Do not let a two-minute task interrupt a protected block unless it is truly urgent; write it down and clear it later.",
        pitfall = "People use it to stay in reactive mode. Cap the small-task sweep and return to the important work.",
        figureCaption = "A two-minute clock beside a small task.",
        appActions = listOf(AppAction.TODO, AppAction.TIMER)
    ),
    Technique(
        id = "implementation-intentions",
        title = "Implementation Intentions",
        aliases = listOf("If-then planning", "If then planning", "Implementation intention"),
        category = TechniqueCategory.START,
        figure = FigureKind.IF_THEN,
        bestFor = "Turning a vague intention into a trigger",
        summary = "Pre-decide the cue and the response: when situation X happens, I will do Y in place Z.",
        steps = listOf(
            "Choose one behaviour you keep failing to start.",
            "Pick a specific, reliable cue: a time, place or end of an existing action.",
            "Write the sentence: When X, I will Y at Z.",
            "Rehearse it mentally once and put the cue in the calendar or a reminder.",
            "Keep only two or three active plans so the cues stay clear."
        ),
        daily = "Attach plans to transitions that already happen: after morning coffee, after stand-up, after closing the laptop. Write them where the cue will be seen, not in a distant notes app. A useful example is: When I sit down with coffee, I will open the deep-work document and write one paragraph. Review weekly and replace any plan whose cue never fires.",
        pitfall = "Vague cues such as when I have time never fire. Use a concrete event and a specific action.",
        figureCaption = "A cue arrow leading to a planned action.",
        appActions = listOf(AppAction.HABITS, AppAction.REMINDERS)
    ),
    Technique(
        id = "temptation-bundling",
        title = "Temptation Bundling",
        aliases = listOf("Temptation bundling"),
        category = TechniqueCategory.START,
        figure = FigureKind.LINK,
        bestFor = "Tasks that are necessary but dull",
        summary = "Pair the task you need to do with something you enjoy, and make the enjoyment available only during that task.",
        steps = listOf(
            "Choose a necessary task with low intrinsic appeal.",
            "Choose a reward that does not compete for the same attention, such as a favourite podcast during chores.",
            "Make a rule: the reward happens only while doing the task.",
            "Set up the pairing before you start.",
            "Review whether the reward helps or distracts, and adjust."
        ),
        daily = "Use bundling for maintenance work: a favourite show only while folding laundry, a specific playlist only for admin, a good coffee only at the desk during the report. Keep the reward low-attention and short. Do not bundle deep work with anything that pulls focus; save the pairing for tasks that are boring rather than cognitively demanding.",
        pitfall = "A distracting reward defeats the purpose. Choose something that makes the task pleasant without stealing your attention.",
        figureCaption = "Two linked cards: task plus reward.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "tiny-start",
        title = "Tiny Start",
        aliases = listOf("Swiss Cheese Method", "Swiss cheese method", "Just 5 minutes", "Just 5 Minutes", "Five minute start"),
        category = TechniqueCategory.START,
        figure = FigureKind.BIG_TO_SMALL,
        bestFor = "Dreaded tasks and blank-page resistance",
        summary = "Shrink the start to five minutes or poke small holes in the task. Momentum is easier to find than motivation.",
        steps = listOf(
            "Pick the task you are avoiding and name the smallest useful hole.",
            "Set a timer for five minutes and work only on that hole.",
            "Stop at five minutes if the resistance is still high.",
            "Write down the next hole before you leave.",
            "Repeat on another day, or continue while the momentum lasts."
        ),
        daily = "Use the first five minutes of a dreaded task before checking messages. The hole might be opening the file and writing the heading, finding one source or making one outline. A five-minute session is a success even if it does not finish the task, because it creates a visible starting point. Stack several holes across the week rather than waiting for a heroic block.",
        pitfall = "Holes that never connect become busywork. Each small session should move the same deliverable forward.",
        figureCaption = "A large block broken into small workable pieces.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "task-chunking",
        title = "Task Chunking",
        aliases = listOf("Eat the elephant", "Eat the Elephant", "One bite at a time", "Eat the elephant one bite at a time"),
        category = TechniqueCategory.START,
        figure = FigureKind.BIG_TO_SMALL,
        bestFor = "Large goals that feel overwhelming",
        summary = "Break a large task into the smallest next physical actions and schedule one bite at a time.",
        steps = listOf(
            "Write the final outcome in one sentence.",
            "List the major deliverables or milestones.",
            "Split each deliverable until the next action is small enough to start in under five minutes.",
            "Give every action a verb and a visible finish line.",
            "Put only the next action on today's list; keep the rest in the project note."
        ),
        daily = "When a project appears on your list, do not carry the whole project into the day. Open it, choose one bite, and put that action on Todo. For long goals, schedule a recurring 30-minute bite so progress happens even when the week is messy. At the end of each session, write the next action so tomorrow starts without re-planning.",
        pitfall = "Over-planning is a form of procrastination. Stop splitting once the next action is clear and start it.",
        figureCaption = "One large goal separated into daily bites.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "commitment-devices",
        title = "Commitment Devices",
        aliases = listOf("Commitment device", "Commitment devices", "Stakes", "Public promise", "Public commitment"),
        category = TechniqueCategory.START,
        figure = FigureKind.LOCK,
        bestFor = "Behaviours you keep renegotiating",
        summary = "Make quitting costly or visible so the future version of you has less room to negotiate.",
        steps = listOf(
            "Define the exact behaviour and the time window.",
            "Choose a proportionate stake: money, a public promise or a locked app.",
            "Make it visible to someone who will notice.",
            "Remove the escape route before the hard moment arrives.",
            "Review after the window and remove the device when the habit is stable."
        ),
        daily = "Keep stakes small and relevant: send a friend a message before the workout and owe them a coffee if you skip, or schedule a website blocker for the work block. A public commitment can be a single sentence to a colleague, not a dramatic announcement. The device should carry the decision when motivation dips, not replace the skill of planning.",
        pitfall = "Shame-based stakes can make one miss feel like a catastrophe. Use a cost you can pay without punishing yourself.",
        figureCaption = "A padlock holding a commitment in place.",
        appActions = listOf(AppAction.TIMER, AppAction.HABITS)
    ),
    Technique(
        id = "body-doubling",
        title = "Body Doubling",
        aliases = listOf("Body doubling", "Co-working"),
        category = TechniqueCategory.START,
        figure = FigureKind.PEOPLE,
        bestFor = "Starting when you are alone and stuck",
        summary = "Work alongside another person, in person or on a silent call, so the social presence supplies the starting structure.",
        steps = listOf(
            "Choose one task and one short session.",
            "Invite a friend, colleague or online co-working room.",
            "Agree on the rules: silent work, camera optional, check-in at the end.",
            "Work for 25 minutes without conversation.",
            "Share what you finished and decide whether to repeat."
        ),
        daily = "Use a 25-minute silent call before a task you keep avoiding, or go to a library or cafe where other people are working. Tell the other person exactly what you intend to finish so the session has a target. Body doubling is especially useful for admin, writing and cleaning; it is not a replacement for focused skill practice.",
        pitfall = "The session turns into chatting or helping each other. Set the silent rule before the timer starts.",
        figureCaption = "Two people working side by side.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "zeigarnik-start",
        title = "Zeigarnik Start",
        aliases = listOf("Zeigarnik effect trick", "Zeigarnik effect", "Zeigarnik trick"),
        category = TechniqueCategory.START,
        figure = FigureKind.NAG,
        bestFor = "Getting back into a task the next day",
        summary = "Start before you stop and leave a clear unfinished edge, because open tasks stay active in memory and pull you back.",
        steps = listOf(
            "Work for two minutes on the task you want to return to.",
            "Stop mid-step rather than at a polished finish.",
            "Write the exact next action where you will see it.",
            "Leave the file or workspace in a state that makes returning easy.",
            "Schedule the return and start there next time."
        ),
        daily = "End a writing session mid-sentence or a coding session with a failing test and a note about the fix. Do not close every tab and erase the context; capture the next step in one line. The goal is a useful open loop, not a pile of unfinished work. Pair it with a shutdown note so the pull is focused rather than stressful.",
        pitfall = "Open loops can become anxiety if they are not captured. Always write the next action and a return time.",
        figureCaption = "An unfinished progress ring that keeps pulling you back.",
        appActions = listOf(AppAction.TODO, AppAction.TIMER)
    ),
    Technique(
        id = "procrastination-journal",
        title = "Procrastination Journal",
        aliases = listOf("Procrastination journaling", "Procrastination journal"),
        category = TechniqueCategory.START,
        figure = FigureKind.NOTE,
        bestFor = "When you keep avoiding a specific task",
        summary = "Write down what you are avoiding and why, then turn the insight into one tiny next action.",
        steps = listOf(
            "Set a five-minute timer when you notice avoidance.",
            "Write the task you are avoiding in plain words.",
            "Answer three prompts: what am I afraid of, what am I confused about, what am I missing?",
            "Name the smallest next action that addresses the real obstacle.",
            "Do that action for two minutes before closing the journal."
        ),
        daily = "Keep a page or note open during work. When you reach for a distraction, write one line about what happened just before it. At the end of the week, look for patterns: unclear next steps, fear of judgment, missing information or low energy. Change the system for the most common pattern rather than relying on more self-criticism.",
        pitfall = "Journaling becomes another way to avoid the task. Cap it at five minutes and always end with an action.",
        figureCaption = "A notebook with three reflection prompts.",
        appActions = listOf(AppAction.TODO)
    )
)
