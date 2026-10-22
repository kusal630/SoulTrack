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

internal val systemsClarityTechniques = listOf(
    Technique(
        id = "gtd",
        title = "Getting Things Done",
        aliases = listOf("GTD", "Getting Things Done", "Next action", "Next action for every project", "Someday/Maybe list", "Someday Maybe", "Waiting-for list", "Waiting for", "Projects list"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.CHECKLIST,
        bestFor = "A busy head and too many open loops",
        summary = "Capture everything, clarify what each item means, organise the next actions, review the system weekly and engage with a trusted list.",
        steps = listOf(
            "Capture every task, idea and commitment into one trusted inbox.",
            "Clarify each item: is it actionable, a project, a reference or trash?",
            "Define the very next physical action for anything actionable.",
            "Organise actions by context or project, and keep Someday/Maybe and Waiting-for lists.",
            "Do a weekly review so the system stays trustworthy, then work from it."
        ),
        daily = "Keep the inbox within reach all day and empty it daily. If a task takes under two minutes, do it now; otherwise put the next action on the right list. Every project needs a visible next action, or it will silently stall. Track things you have delegated on a Waiting-for list. The weekly review is the maintenance that makes the whole system worth using.",
        pitfall = "Collecting without clarifying creates a bigger pile of stress. The review and next-action step are not optional.",
        figureCaption = "Five steps from capture to engagement.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "kanban-wip",
        title = "Kanban + WIP Limits",
        aliases = listOf("Kanban", "Personal Kanban", "WIP limits", "Personal Kanban / WIP limits"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.KANBAN,
        bestFor = "Too many things in progress at once",
        summary = "Make work visible in columns and cap how many items can be in progress so finishing beats starting.",
        steps = listOf(
            "Create three columns: To do, Doing and Done.",
            "Put every active task or project on a card.",
            "Set a work-in-progress limit for Doing, often one or two items.",
            "Move a card only when it is genuinely finished or blocked.",
            "Review the board weekly and remove cards that no longer matter."
        ),
        daily = "Keep the board where you will see it, either in the app or on paper. When Doing is full, do not start another card; finish, unblock or explicitly park one. A blocked card is not progress, so write what it is waiting for. Limit columns to three until the habit is stable; more columns create more places for work to hide.",
        pitfall = "Too many columns and no WIP limit turns Kanban into another task list. Start simple and enforce the cap.",
        figureCaption = "Three columns with a limited number of cards in Doing.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "bullet-journal",
        title = "Bullet Journal",
        aliases = listOf("Bullet Journal", "BuJo", "Rapid logging"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.BULLET,
        bestFor = "People who think better on paper",
        summary = "Use rapid logging with bullets for tasks, events and notes, then migrate what still matters so the journal stays alive.",
        steps = listOf(
            "Choose one notebook and create a daily log.",
            "Use a dot for a task, a circle for an event and a dash for a note.",
            "Mark tasks done, migrated or dropped at the end of the day.",
            "Keep a monthly log for dates and a future log for later commitments.",
            "Migrate unfinished tasks deliberately instead of copying everything."
        ),
        daily = "Spend five minutes in the morning writing the day and five minutes at night marking what happened. Migration is the key: if a task moves forward three times, either schedule it, break it down or drop it. Keep the journal functional; fancy layouts are optional and often become procrastination. The notebook is a tool for thinking, not an art project.",
        pitfall = "The system becomes a craft project. A messy journal that you use beats a beautiful one you avoid.",
        figureCaption = "A daily log with task bullets and a migration arrow.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "sprints-retrospectives",
        title = "Sprints + Retrospectives",
        aliases = listOf("Scrum", "Sprints", "Sprint", "Agile retrospectives", "Agile retrospective", "Retrospective"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.SPRINT,
        bestFor = "Projects that need a steady cadence",
        summary = "Work in short fixed cycles with a plan and a look-back, so you adjust the process instead of only pushing the work.",
        steps = listOf(
            "Choose a one- or two-week sprint and a small set of outcomes.",
            "Plan the sprint from a prioritised backlog.",
            "Check progress briefly each day and remove blockers.",
            "At the end, review what was actually delivered.",
            "Run a short retrospective and choose one process change for the next sprint."
        ),
        daily = "A personal sprint can be one week with three outcomes. Keep a short daily check: what moved, what is blocked, what is next. At the end, spend 20 minutes on the retrospective: what helped, what hurt, what will we change? Pick one change, not five. The rhythm matters more than the ceremony, and a sprint without a review is just a deadline.",
        pitfall = "Sprints without retrospectives repeat the same problems. Protect the look-back even when the sprint went badly.",
        figureCaption = "A repeating plan-do-review loop.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "smart-okrs",
        title = "SMART Goals + OKRs",
        aliases = listOf("SMART goals", "SMART", "OKRs", "OKR", "Objectives and Key Results"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.TARGET,
        bestFor = "Turning vague ambitions into measurable direction",
        summary = "Write specific, measurable goals, and at a larger scale pair one objective with a few measurable key results.",
        steps = listOf(
            "Write the outcome you want, not the activity you will perform.",
            "Make it Specific, Measurable, Achievable, Relevant and Time-bound.",
            "For an OKR, choose one inspiring Objective and three Key Results with baselines and targets.",
            "Review progress weekly and update the numbers.",
            "At the end of the period, score the result and set the next objective."
        ),
        daily = "Keep the current objective visible on the dashboard or in a note. A weekly check is enough for most personal OKRs; daily tasks should be the work that moves the key results. If a key result has no baseline, the first task is to measure it. Avoid turning every task into an OKR; the system is for direction, not for adding ceremony to a grocery list.",
        pitfall = "Key results become a task list. They should describe an outcome, such as reduce support response time to four hours, not publish three blog posts.",
        figureCaption = "A target with one objective and three measured rings.",
        appActions = listOf(AppAction.INSIGHTS, AppAction.TODO)
    ),
    Technique(
        id = "backward-planning",
        title = "Backward Planning + Work Breakdown",
        aliases = listOf("Backward planning", "Work breakdown structure", "WBS", "Critical path thinking", "Critical path"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.BACKWARD,
        bestFor = "Deadlines, launches and event planning",
        summary = "Start from the deadline, work backward through the deliverables, and find the sequence that determines the finish date.",
        steps = listOf(
            "Write the final deadline and what must be true on that date.",
            "Break the project into deliverables using a work breakdown structure.",
            "List dependencies and put the deliverables in reverse order from the deadline.",
            "Find the longest dependent chain; that is your critical path.",
            "Schedule the critical path first and add buffer around it."
        ),
        daily = "Use this for one substantial project at a time. Put the reverse milestones in the calendar and treat the earliest critical-path task as the priority. If a task is not on the critical path, its delay may still matter, but it should not crowd out the chain that determines the date. Review the plan weekly and update it when a dependency changes.",
        pitfall = "Planning every tiny step in advance becomes fiction. Break work down to deliverables, then detail only the next few actions.",
        figureCaption = "Milestones planned backward from a deadline.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "mvp-thinking",
        title = "MVP Thinking",
        aliases = listOf("Minimum viable product thinking", "Minimum viable product", "MVP"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.MVP,
        bestFor = "Side projects and first versions",
        summary = "Ship the smallest version that delivers real value, then improve it with feedback instead of polishing in private.",
        steps = listOf(
            "Define the core job the product or deliverable must do.",
            "List the features or parts, then separate must-haves from nice-to-haves.",
            "Cut everything that is not needed to deliver the core job.",
            "Build and test the smallest useful version with a real user.",
            "Use the feedback to decide the next smallest improvement."
        ),
        daily = "For a work deliverable, ask what the smallest useful version is that someone could actually use today. For a side project, set a one-week build limit and show it to one person before adding more. MVP does not mean low quality; it means narrow scope with a solid core. Keep a parking lot for the features you deliberately postponed.",
        pitfall = "An MVP is not an excuse for a broken experience. The core job must work well enough to learn from.",
        figureCaption = "A small solid version beside a larger outlined plan.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "pre-mortem",
        title = "Pre-Mortem",
        aliases = listOf("Pre-mortem", "Premortem", "Pre mortem"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.CRACK,
        bestFor = "Before a project with real stakes",
        summary = "Imagine the project has already failed, write the reasons, and add safeguards before those reasons happen.",
        steps = listOf(
            "Set the scene: it is three months from now and the project failed.",
            "Write every plausible reason for the failure without filtering.",
            "Rank the reasons by likelihood and impact.",
            "Choose the top three and add a concrete prevention or detection step.",
            "Assign an owner and a review date for each safeguard."
        ),
        daily = "Run a ten-minute pre-mortem before a launch, a big presentation or a new habit plan. Include the boring failures: no time, unclear owner, missing data, a dependency arriving late. The point is not to worry, but to make the failure cheaper. Put the top safeguards into the project or calendar so they are real actions, not a list of fears.",
        pitfall = "The exercise turns into a fear spiral. Keep every risk paired with a specific mitigation.",
        figureCaption = "A target with a crack and a repair mark.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "post-mortem",
        title = "Post-Mortem / After-Action Review",
        aliases = listOf("Post-mortem", "Post mortem", "After-action review", "After action review", "AAR"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.MAGNIFY,
        bestFor = "Learning from a finished project",
        summary = "After the work ends, ask what worked, what did not and what will change next time, without assigning blame.",
        steps = listOf(
            "Schedule a short review while the memory is fresh.",
            "Agree the facts and the intended outcome first.",
            "List what worked and what did not, using the system rather than people as the unit of analysis.",
            "Find the root cause of the biggest miss.",
            "Choose one or two changes and write them into the next plan."
        ),
        daily = "Use a 15-minute review after a sprint, presentation, trip or difficult week. Keep it blameless: the useful question is what made the right action hard, not who was at fault. Store the notes where the next project can find them. A review that produces five changes nobody implements is worse than one change that becomes standard practice.",
        pitfall = "Blame shuts down honesty. Focus on the process, the information and the constraints.",
        figureCaption = "A magnifying glass over a completed checklist.",
        appActions = listOf(AppAction.INSIGHTS)
    ),
    Technique(
        id = "checklists-sops",
        title = "Checklists + SOPs",
        aliases = listOf("Checklists", "Templates and SOPs", "Templates", "SOPs", "Standard operating procedures"),
        category = TechniqueCategory.SYSTEMS,
        figure = FigureKind.CHECKLIST,
        bestFor = "Repeated and high-stakes work",
        summary = "Write the reliable steps once, then reuse them so you do not rely on memory during a busy or stressful moment.",
        steps = listOf(
            "Choose a task you repeat or that is expensive to get wrong.",
            "Write the steps in the order you actually do them.",
            "Keep only the steps that prevent a real error.",
            "Test the checklist on a live run and refine it.",
            "Store it where the work starts and update it after every surprise."
        ),
        daily = "Create a shutdown checklist, a travel checklist, a release checklist or a template for recurring reports. Use the checklist before you feel you need it; high-stakes moments are exactly when memory fails. A good checklist is short enough to finish and specific enough to catch a mistake. When you find a new failure, add one step and remove a step that no longer earns its place.",
        pitfall = "Checklists grow into unread documents. Prune them and keep them focused on errors that matter.",
        figureCaption = "A short checklist with completed items.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "capture-inbox",
        title = "Capture Inbox / Brain Dump",
        aliases = listOf("Brain dump", "Inbox capture", "Mind dump"),
        category = TechniqueCategory.CLARITY,
        figure = FigureKind.INBOX,
        bestFor = "A head full of unfinished thoughts",
        summary = "Keep one trusted place for every incoming task, idea and worry, and empty it on a regular rhythm.",
        steps = listOf(
            "Choose one inbox: a notes app, a paper notebook or a single Todo list.",
            "Spend ten minutes dumping everything that is on your mind.",
            "Do not organise during the dump; just get it out.",
            "Process the inbox daily or weekly into actions, projects, calendar entries or trash.",
            "Close the loop by using the same inbox every time."
        ),
        daily = "Do a brain dump at the start of the week or whenever you feel scattered. Keep a capture shortcut on your phone so an idea does not have to be held in memory. The dump is not the system; it is the doorway. If the inbox grows for days, schedule a 15-minute processing session and move each item to a clear next action or delete it.",
        pitfall = "Multiple inboxes defeat the purpose. Pick one and redirect everything else into it.",
        figureCaption = "A funnel collecting scattered notes into one tray.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "para",
        title = "Second Brain (PARA)",
        aliases = listOf("Second brain", "Second Brain", "PARA", "PARA method"),
        category = TechniqueCategory.CLARITY,
        figure = FigureKind.PARA,
        bestFor = "Organising notes by action instead of topic",
        summary = "Organise your notes into Projects, Areas, Resources and Archive so the system reflects what you are actually doing.",
        steps = listOf(
            "Create four top-level folders or spaces: Projects, Areas, Resources and Archive.",
            "Put notes for active, time-bound outcomes in Projects.",
            "Put ongoing responsibilities such as health or finances in Areas.",
            "Put reference material you may need later in Resources.",
            "Move completed projects and stale notes to Archive during a review."
        ),
        daily = "When you create a note, ask which project or area it serves. If it serves neither and is not useful reference, do not file it. At the weekly review, move finished projects to Archive and check that every active project has a next action. PARA is a filing system for action, not a library for collecting everything you read.",
        pitfall = "Filing becomes the work. If a note will never support a project, area or decision, let it go.",
        figureCaption = "Four folders labelled Projects, Areas, Resources and Archive.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "zettelkasten",
        title = "Zettelkasten",
        aliases = listOf("Zettelkasten", "Zettelkasten notes", "Atomic notes"),
        category = TechniqueCategory.CLARITY,
        figure = FigureKind.LINKS,
        bestFor = "Building ideas that compound over time",
        summary = "Write small atomic notes in your own words and link them, so your reading turns into a network of reusable ideas.",
        steps = listOf(
            "Write one idea per note, using your own words.",
            "Give the note a clear title that states the idea.",
            "Link it to at least one existing note and explain the connection.",
            "Keep an index note for entry points into important themes.",
            "Review and write from the notes instead of only collecting more."
        ),
        daily = "After reading or a meeting, write three to five small notes rather than highlighting. The value comes from linking: this idea supports, contradicts or extends that one. Use the notes to draft an article, a decision or a plan; that is how the network pays you back. A note you never use is not wasted, but a note you cannot find is.",
        pitfall = "The collector's fallacy: saving quotes feels like learning. Write in your own words and make links.",
        figureCaption = "Three linked notes forming a small idea network.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "daily-pages",
        title = "Daily Pages",
        aliases = listOf("Daily notes", "Journaling", "Daily journal", "Morning pages", "Morning Pages"),
        category = TechniqueCategory.CLARITY,
        figure = FigureKind.NOTE,
        bestFor = "Clearing mental noise before work",
        summary = "Write a short daily page to empty the head, notice patterns and start the day with fewer competing thoughts.",
        steps = listOf(
            "Choose a time and a small fixed format.",
            "Write freely for five to ten minutes without editing.",
            "Use prompts if needed: what matters today, what is worrying me, what did I learn yesterday?",
            "End with one priority or one question for the day.",
            "Review the week for repeating themes."
        ),
        daily = "Morning pages can be three pages of free writing or a five-minute brain dump; the length matters less than the consistency. Use the evening for three lines: what went well, what did not, what is next. Keep it private and messy. If writing becomes another task to optimise, shrink it to one sentence until the habit is easy again.",
        pitfall = "Perfectionism turns journaling into a performance. Write badly on purpose and keep it private.",
        figureCaption = "An open notebook with a short daily entry.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "mind-mapping",
        title = "Mind Mapping",
        aliases = listOf("Mind mapping", "Mind map"),
        category = TechniqueCategory.CLARITY,
        figure = FigureKind.MINDMAP,
        bestFor = "Thinking through a messy topic or plan",
        summary = "Draw ideas as branches from a central question so relationships become visible and new connections appear.",
        steps = listOf(
            "Write the central question in the middle of a page.",
            "Add main branches for the big themes.",
            "Add smaller branches with keywords, not long sentences.",
            "Use colour or symbols to mark decisions, risks and actions.",
            "Turn the action branches into tasks or calendar blocks."
        ),
        daily = "Use a mind map for a project kickoff, a difficult decision or notes from a talk. Give it ten minutes and stop; the map is a thinking tool, not a work of art. Afterward, choose the three most important branches and translate them into concrete next actions. If the map has no actions, it is probably note-taking rather than planning.",
        pitfall = "A beautiful map with no decisions is decoration. Always end by choosing what to do.",
        figureCaption = "A central idea branching into related themes.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "gratitude-reflection",
        title = "Gratitude + Reflection Prompts",
        aliases = listOf("Gratitude and reflection prompts", "Gratitude", "Reflection prompts", "Evening review"),
        category = TechniqueCategory.CLARITY,
        figure = FigureKind.HEART,
        bestFor = "Ending the day without carrying the noise",
        summary = "Use a short evening review to notice what went well, what was hard and what you want to carry into tomorrow.",
        steps = listOf(
            "Choose two or three prompts you will actually answer.",
            "Write three specific good things from the day.",
            "Name one difficulty and one thing you learned from it.",
            "Write tomorrow's most important priority.",
            "Keep the review under five minutes."
        ),
        daily = "Try: What went well? What was hard? What is one thing I will do differently? What matters tomorrow? Be specific; a good thing is a person, a moment or a small win, not a generic positive statement. The reflection is not about forcing a good mood; it is about giving the day a clear ending so it does not keep running in your head.",
        pitfall = "Gratitude can become toxic positivity if it ignores real problems. Name the difficulty too, then choose one next step.",
        figureCaption = "A heart-shaped note with simple reflection prompts.",
        appActions = listOf(AppAction.HABITS)
    ),
    Technique(
        id = "decision-journal",
        title = "Decision Journal",
        aliases = listOf("Decision journal", "Decision journaling"),
        category = TechniqueCategory.CLARITY,
        figure = FigureKind.SCALES,
        bestFor = "Learning from important choices",
        summary = "Record the reasoning behind a decision before you know the outcome, then compare your prediction with what happened.",
        steps = listOf(
            "When a significant decision comes up, write the context and the options.",
            "Record what you expect to happen, your confidence and the main risk.",
            "Choose the decision and set a review date.",
            "At the review, compare the outcome with the prediction.",
            "Look for patterns in your assumptions and adjust your process."
        ),
        daily = "Keep it for decisions that are expensive or repeatable, not for what to eat. A single paragraph is enough: context, options, expectation, confidence, review date. The journal protects you from hindsight bias, because you can see what you actually believed at the time. Review a few entries monthly and look for recurring overconfidence or avoided risks.",
        pitfall = "Recording only successes teaches nothing. Include the decisions that went badly and the ones you avoided.",
        figureCaption = "A balance scale beside a decision note.",
        appActions = listOf(AppAction.TODO)
    )
)
