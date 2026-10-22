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

internal val timeFocusTechniques = listOf(
    Technique(
        id = "pomodoro",
        title = "Pomodoro Technique",
        aliases = listOf("Pomodoro", "Pomodoro timer"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.CLOCK,
        bestFor = "Tasks with no natural finish line",
        summary = "Work in a short fixed interval, then take a real break. The timer turns an endless task into a series of starts.",
        steps = listOf(
            "Choose one task and one clear target for the interval.",
            "Set a 25-minute timer and work only on that task.",
            "When the timer rings, stop and take a five-minute break away from the screen.",
            "After four intervals, take a longer break of 15 to 30 minutes.",
            "Record what you finished so the next session starts from evidence."
        ),
        daily = "Use Pomodoro when a task is vague or when you are avoiding it. Decide before starting what one interval should produce. During the interval, keep a capture note for distractions instead of following them. If 25 minutes is too short, try 50 work and 10 rest, but keep the principle: one task, a visible interval, a genuine break. Do not use the break to scroll; stand up, drink water or look outside.",
        pitfall = "The timer becomes just another interruption if you check messages at every ring. Keep the break away from the screen.",
        figureCaption = "A timer arc with a 25-minute work and 5-minute rest cycle.",
        appActions = listOf(AppAction.POMODORO)
    ),
    Technique(
        id = "time-blocking",
        title = "Time Blocking, Timeboxing + Calendar-First",
        aliases = listOf("Time blocking", "Timeboxing", "Calendar-first planning", "Parkinson's Law", "Parkinsons Law"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.CALENDAR,
        bestFor = "Making work fit reality instead of hope",
        summary = "Give every commitment a calendar slot, set a fixed start and stop, and put real commitments in before filling the gaps.",
        steps = listOf(
            "Put fixed commitments such as meetings, school runs and health appointments on the calendar first.",
            "Estimate each open task and give it a block, including preparation and travel.",
            "Timebox the block with a start and a stop, not just a start.",
            "Use Parkinson's Law deliberately: shorten the deadline for small tasks when the work tends to expand.",
            "Leave at least 20 percent of the day unscheduled for overflow."
        ),
        daily = "Plan tomorrow for ten minutes at the end of today. Calendar-first means the gym, the school pickup and the meeting are already there; tasks fill the space around them. For a task that keeps expanding, give it a smaller box and a hard stop. Review the plan against the actual day at shutdown, then adjust tomorrow's estimates instead of blaming yourself.",
        pitfall = "An overpacked calendar is fiction. Block less than you think you can do and let the extra space absorb reality.",
        figureCaption = "A day column filled with fixed blocks and gaps.",
        appActions = listOf(AppAction.TIMER, AppAction.TODO)
    ),
    Technique(
        id = "day-theming",
        title = "Day Theming",
        aliases = listOf("Day theming", "Theme days"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.WEEK_THEME,
        bestFor = "Weeks with many kinds of recurring work",
        summary = "Give each weekday a theme so similar work lands together and switching costs fall.",
        steps = listOf(
            "List the recurring kinds of work in your week.",
            "Assign one or two themes to each weekday, for example Monday admin and Tuesday deep work.",
            "Protect the theme from optional meetings.",
            "Move exceptions deliberately rather than redesigning the whole week.",
            "Review after two weeks and keep only the themes that match your actual energy."
        ),
        daily = "Use themes as defaults, not laws. Monday can be admin and planning, Tuesday and Thursday deep work, Wednesday meetings and collaboration, Friday review and learning. Put the theme in the calendar title so colleagues see it. If an urgent request lands, handle it and return to the theme; one exception does not require a new system.",
        pitfall = "Themes become too rigid and force work to wait for the wrong day. Allow a small same-day lane for genuinely urgent items.",
        figureCaption = "Seven day tiles, each with its own theme.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "task-batching",
        title = "Task Batching",
        aliases = listOf("Task batching", "Batching"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.BATCH,
        bestFor = "Email, calls, errands and small admin",
        summary = "Group similar tasks into one session so you pay the setup cost once instead of many times.",
        steps = listOf(
            "List the small tasks you repeat across the week.",
            "Group them by type or context: messages, calls, errands, finance.",
            "Give each group a short recurring session.",
            "Do only that group during the session and keep a capture list for outliers.",
            "Review whether the batch is saving time or delaying something important."
        ),
        daily = "Batch email and messages into two windows, for example late morning and late afternoon, and tell people what response time to expect. Run errands in one trip. Make calls in a single block with numbers ready. Never batch urgent support or a blocking question; those need a fast lane. The aim is fewer context switches, not slower communication.",
        pitfall = "Batching can become an excuse to ignore a message that truly needs a quick answer. Keep a clear service level for urgent channels.",
        figureCaption = "Several similar items gathered into one tray.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "flexible-focus-cycles",
        title = "Flexible Focus Cycles",
        aliases = listOf("Flowtime Technique", "Flowtime", "52/17 method", "52/17", "90-minute ultradian cycles", "Ultradian cycles", "Rule of 52", "Rule of 52 variants and custom intervals"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.CURVE,
        bestFor = "People who fight fixed timers",
        summary = "Match the work interval to your natural focus instead of forcing one ratio. Flowtime, 52/17 and 90-minute cycles are variations of the same idea.",
        steps = listOf(
            "Start a timer when you begin and work until focus fades or a planned limit is reached.",
            "Note how long the focus lasted without judging it.",
            "Take a break proportional to the work: about 5 minutes for 25, 17 for 52, 20 for 90.",
            "If you enter flow, finish the thought and take a slightly longer break afterward.",
            "Experiment for a week and keep the range that leaves you fresh at the end of the day."
        ),
        daily = "Use a 90-minute block for demanding morning work if that matches your energy, then a 52/17 rhythm in the afternoon. With Flowtime, the timer is a record of focus, not a cage; stop when attention genuinely breaks, not when a notification says so. Keep a maximum so hyperfocus does not turn into a skipped meal or a sore back.",
        pitfall = "Without any fixed boundary, flexible cycles drift into endless work. Set minimum and maximum intervals and protect the break.",
        figureCaption = "A work-and-rest curve that adapts to focus.",
        appActions = listOf(AppAction.POMODORO, AppAction.TIMER)
    ),
    Technique(
        id = "time-audit",
        title = "Time Audit",
        aliases = listOf("Time audit", "Time and energy audit", "Time and energy audits"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.AUDIT,
        bestFor = "When the week disappears without visible progress",
        summary = "Track where your hours actually go for one week, then compare the data with your priorities.",
        steps = listOf(
            "Choose a simple tracking method: paper, calendar or a timer app.",
            "Log what you are doing every 30 to 60 minutes, using rough categories.",
            "Group the entries into a handful of categories such as deep work, meetings, admin, care and distraction.",
            "Compare the totals with your stated priorities.",
            "Choose one change for next week and re-audit after a month."
        ),
        daily = "Do not try to track every minute perfectly; a rough log is enough to reveal the pattern. Review the week in 15 minutes and ask where the biggest gap is between intention and reality. The most useful change is usually small: one fewer meeting, one protected morning or one earlier shutdown. Keep the audit temporary so tracking does not become the work.",
        pitfall = "The audit itself can consume the week. Use broad categories and stop after enough data to see a pattern.",
        figureCaption = "A pie chart splitting real hours by category.",
        appActions = listOf(AppAction.INSIGHTS, AppAction.TIMER)
    ),
    Technique(
        id = "buffer-time",
        title = "Buffer Time + Hofstadter's Law",
        aliases = listOf("Buffer time", "Hofstadter's Law", "Hofstadters Law", "Hofstadter's law reminder"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.BUFFER,
        bestFor = "Back-to-back days and optimistic estimates",
        summary = "Things take longer than expected even when you account for that, so leave deliberate gaps and add a buffer to estimates.",
        steps = listOf(
            "Estimate the task as honestly as you can.",
            "Add 25 to 50 percent for the unknown, more for new work.",
            "Place a transition gap between meetings and tasks.",
            "Label the buffer as buffer so it does not get booked by someone else.",
            "Review estimates weekly and adjust the multiplier."
        ),
        daily = "Schedule meetings for 50 minutes instead of 60 and use the ten-minute gap to write notes or breathe. Keep one 30-minute overflow block each afternoon. Do not fill a buffer just because it looks empty; its job is to absorb the task that ran long. When a buffer is used, note what happened so future estimates improve.",
        pitfall = "Unlabelled buffers get booked. Give them a name and treat them as real commitments to recovery.",
        figureCaption = "A timeline with intentional gaps between blocks.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "bookend-routines",
        title = "Bookend Routines",
        aliases = listOf("Shutdown routine", "Morning routine", "Evening routine", "Daily review", "Evening shutdown", "Set up the environment the night before", "Set up environment the night before", "Cold start rituals", "Cold start ritual"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.ROUTINE,
        bestFor = "Starting and ending the day with less friction",
        summary = "Use a short fixed sequence at the start and end of the day to remove decisions, close loops and prepare tomorrow.",
        steps = listOf(
            "Design a three to five step morning routine that fits your real morning.",
            "Design a shutdown routine: capture open tasks, check tomorrow's calendar, tidy the workspace.",
            "Prepare the next day's environment tonight: clothes, food, documents, first task.",
            "Anchor each routine to a reliable cue such as waking up or closing the laptop.",
            "Keep it short enough to survive a bad day."
        ),
        daily = "A morning routine might be water, light, movement and a look at the three priorities. A shutdown routine might be clearing the inbox, writing tomorrow's first action and saying a clear end-of-work phrase. Set out the running shoes or the document the night before so the morning starts with less negotiation. The routine is a boundary, not a performance.",
        pitfall = "Elaborate routines collapse under real life. Keep the minimum version and add steps only when the base is stable.",
        figureCaption = "A sunrise-to-moon sequence of small rituals.",
        appActions = listOf(AppAction.TODO, AppAction.HABITS)
    ),
    Technique(
        id = "chronotype-alignment",
        title = "Chronotype Alignment",
        aliases = listOf("Early bird or night owl alignment", "Early bird", "Night owl", "Chronotype"),
        category = TechniqueCategory.TIME,
        figure = FigureKind.CURVE,
        bestFor = "Matching hard work to your real energy",
        summary = "Put demanding work in your natural peak hours instead of copying someone else's 5 a.m. routine.",
        steps = listOf(
            "For one week, rate your energy every two hours on a simple 1 to 5 scale.",
            "Identify your most alert window and your afternoon dip.",
            "Move one important deep-work task into the peak window.",
            "Move low-energy admin into the dip instead of fighting it.",
            "Protect sleep and light exposure to keep the rhythm stable."
        ),
        daily = "If you are a morning person, put the hardest thinking before meetings. If you are a night owl, stop apologising for it and protect a later deep-work block; do the admin earlier. Negotiate your schedule where you can, and use the dip for email, filing or errands. Chronotype is a tendency, not an identity, but ignoring it costs real focus.",
        pitfall = "A forced schedule can make you feel lazy when the problem is timing. Move the task before questioning your character.",
        figureCaption = "An energy curve with the peak window highlighted.",
        appActions = listOf(AppAction.INSIGHTS)
    ),
    Technique(
        id = "deep-work-blocks",
        title = "Deep Work Blocks",
        aliases = listOf("Deep Work", "Focused Work Blocks", "Focused work block", "Focus block"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.DEEP_BLOCK,
        bestFor = "Work that needs real thinking",
        summary = "Protect a long uninterrupted block for cognitively demanding work, and treat the block as the main event of the day.",
        steps = listOf(
            "Choose one outcome that needs depth, not just activity.",
            "Block two to four hours and tell people you are unavailable.",
            "Remove the phone, close unrelated tabs and keep one document open.",
            "Define the start and end so the block does not leak into the whole day.",
            "Track what you produced and take a real recovery break afterward."
        ),
        daily = "For most people, one deep block before noon is more valuable than a day of fragmented effort. Start with 60 or 90 minutes if four hours feels impossible. Put the block on the calendar and protect it like a meeting with your most important client. Use the time for writing, design, coding, analysis or learning, not for email. The rest of the day can be shallow work without guilt.",
        pitfall = "Deep work is not the same as busy work. If the outcome is vague, the block becomes aimless screen time.",
        figureCaption = "A large protected block surrounded by smaller shallow tasks.",
        appActions = listOf(AppAction.TIMER, AppAction.POMODORO)
    ),
    Technique(
        id = "single-tasking",
        title = "Single-Tasking + Attention Residue",
        aliases = listOf("Single-tasking", "Single tasking", "Attention residue awareness", "Attention residue"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.SINGLE_TASK,
        bestFor = "Multitasking habits and constant switching",
        summary = "Do one thing at a time, and close or note the previous task cleanly before switching so part of your attention does not stay behind.",
        steps = listOf(
            "Choose the one task for the current block.",
            "Close unrelated documents, tabs and messaging apps.",
            "When an interruption arrives, write it on a capture note and return.",
            "Before switching, write where you stopped and what comes next.",
            "Batch the small switches into a later window."
        ),
        daily = "Keep one document and one clear question in front of you. If a thought about another task appears, put it on paper instead of opening the other task. Before a meeting, write one line about where you will resume. Attention residue is the mental lag after a switch; a clean note reduces it. The goal is not perfect focus, but fewer unnecessary transitions.",
        pitfall = "Multitasking feels efficient because it is busy. Measure output, not the number of things you touched.",
        figureCaption = "One highlighted task in a field of inactive ones.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "distraction-firewall",
        title = "Distraction Firewall",
        aliases = listOf("Phone in another room", "Website and app blockers", "Website app blockers", "Digital minimalism"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.SHIELD,
        bestFor = "Phones, apps and websites that pull you away",
        summary = "Put distance and friction between you and the distraction. Willpower is a poor firewall; design is a better one.",
        steps = listOf(
            "List the three distractions that most often break your focus.",
            "Put the phone in another room or a drawer during deep work.",
            "Use a blocker for the websites and apps that are not needed for the task.",
            "Log out, remove the app from the home screen or delete it if it adds no clear value.",
            "Replace the habit with a specific offline alternative such as water, a walk or a paper note."
        ),
        daily = "Before the first deep block, set the phone out of reach and start the blocker. Keep only tools that serve the current task on the desk. Digital minimalism is not total abstinence; it is removing the tools whose cost is higher than their benefit. At the end of the day, notice which apps you opened automatically and add friction to the worst one.",
        pitfall = "Blocking everything at once creates a rebound. Remove or block the few biggest offenders first, then adjust.",
        figureCaption = "A phone behind a protective shield.",
        appActions = listOf(AppAction.TIMER, AppAction.FOCUS)
    ),
    Technique(
        id = "communication-batching",
        title = "Communication Batching",
        aliases = listOf("Notification batching", "Inbox zero", "Inbox Zero", "Email batching"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.BELL,
        bestFor = "Constant messages and email checks",
        summary = "Check messages at set times, process them to a clear next action, and keep a separate lane for genuine urgency.",
        steps = listOf(
            "Turn off non-essential notifications on the phone and desktop.",
            "Choose two or three communication windows each day.",
            "During a window, process each item: reply, capture an action, delegate or archive.",
            "Use a separate urgent channel and tell people what it is for.",
            "End the window with a clear inbox or a clearly parked action list."
        ),
        daily = "Try an 11 a.m. and 4 p.m. email window. Do not open email first thing; spend the first block on the important work. Inbox zero is a processing standard, not a religion: every message should leave the inbox with a decision. If a reply needs more than a few minutes, turn it into a task with a next action instead of leaving it unread.",
        pitfall = "Inbox zero can become a full-time ritual. Timebox the processing window and stop when it ends.",
        figureCaption = "A bell with messages grouped into two check windows.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "distraction-list",
        title = "Distraction List",
        aliases = listOf("Distraction list", "Distraction capture"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.NOTE,
        bestFor = "Intrusive thoughts during focus",
        summary = "Write distracting thoughts on paper and deal with them later instead of following them immediately.",
        steps = listOf(
            "Keep a paper or a single note beside your workspace.",
            "When a thought pulls you away, write one line and return to the task.",
            "Do not organise or act on the list during the focus block.",
            "Review the list at a break or at shutdown.",
            "Do, schedule or delete each item, then start a fresh page."
        ),
        daily = "The list is for both ideas and worries: reply to Sam, check the delivery, look up that book. Writing it down tells the brain the item is safe without letting it hijack the block. At the review, answer the two-minute items and move the rest to Todo. If the same thought keeps returning, it may need a real decision rather than another note.",
        pitfall = "The distraction list becomes another inbox that never gets processed. Review it at least once a day.",
        figureCaption = "A sticky note holding an interrupting thought.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "focus-cues",
        title = "Focus Cues + Soundscape",
        aliases = listOf("Time-restricted environment cues", "Environment cues", "Music or noise strategy", "Music strategy", "Noise strategy"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.FOCUS_CUES,
        bestFor = "Switching into work mode quickly",
        summary = "Use a consistent place, light, object or sound to signal work mode, and choose a sound strategy that supports the task.",
        steps = listOf(
            "Choose one or two cues: a lamp, headphones, a specific playlist or a clean desk.",
            "Use the cue only when you intend to focus.",
            "Match the sound to the task: instrumental music for routine work, silence or white noise for reading.",
            "Keep the cue short and repeatable so it does not become a ritual project.",
            "End the session by turning the cue off, which tells the brain the block is over."
        ),
        daily = "A desk lamp switched on for deep work and off for breaks is a simple, reliable cue. Headphones can signal unavailable to people nearby. Music with lyrics is usually a poor choice for reading or writing; try instrumental, brown noise or silence and notice which one helps you return after a distraction. The best soundscape is the one you stop noticing.",
        pitfall = "Too many cues become noise. Pick one clear signal for focus and one for rest.",
        figureCaption = "A lamp and headphones marking a focused workspace.",
        appActions = listOf(AppAction.FOCUS, AppAction.TIMER)
    ),
    Technique(
        id = "flow-conditions",
        title = "Flow State Conditions",
        aliases = listOf("Flow state conditions", "Flow state", "Flow"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.FLOW,
        bestFor = "Creative and technical work with a clear target",
        summary = "Build the conditions for flow: a clear goal, immediate feedback and a challenge slightly above your current skill.",
        steps = listOf(
            "Define a concrete target for the session.",
            "Remove interruptions and make the feedback loop fast: tests, word count, a visible draft.",
            "Choose a challenge that is hard enough to absorb you but not so hard that you freeze.",
            "Timebox the session so you can stop before exhaustion.",
            "Note which conditions helped and recreate them next time."
        ),
        daily = "For writing, set a word count or a scene goal and track it. For coding, write a failing test first so feedback is immediate. For learning, choose problems just beyond your current level. Flow rarely starts on demand, but it becomes more likely when the goal is clear and the environment is quiet. Do not chase flow as a productivity trophy; build the conditions and let it arrive.",
        pitfall = "Flow cannot be forced by removing all breaks. Stay hydrated, move occasionally and stop before your quality drops.",
        figureCaption = "A challenge-and-skill channel leading into flow.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "monk-mode",
        title = "Monk Mode",
        aliases = listOf("Monk mode"),
        category = TechniqueCategory.FOCUS,
        figure = FigureKind.MONK,
        bestFor = "A short push on one important goal",
        summary = "Set a defined period of strict isolation and focus on one goal, with clear rules for what is paused.",
        steps = listOf(
            "Choose one outcome and a short duration: a few hours a day, a weekend or a week.",
            "Cancel or pause non-essential commitments and tell people in advance.",
            "Write the rules: no social media, one project, defined work hours, meals and movement.",
            "Prepare the environment and supplies before the period starts.",
            "End with a review and a deliberate re-entry plan."
        ),
        daily = "A realistic version is a two-hour monk block every morning, not a dramatic month-long retreat. Use it before a launch, an exam or a difficult piece of work. Isolation without recovery becomes burnout, so schedule food, sleep and a short walk. The exit matters: decide in advance how you will return to messages and normal commitments.",
        pitfall = "Monk mode is not sustainable as a permanent operating mode. Use it as a sprint and plan the recovery.",
        figureCaption = "A single figure inside a protected circle.",
        appActions = listOf(AppAction.TIMER)
    )
)
