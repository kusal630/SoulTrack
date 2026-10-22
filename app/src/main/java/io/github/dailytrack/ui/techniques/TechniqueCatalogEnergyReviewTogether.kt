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

internal val energyReviewTogetherTechniques = listOf(
    Technique(
        id = "energy-management",
        title = "Energy Management",
        aliases = listOf("Energy management", "Managing energy"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.BATTERY,
        bestFor = "Matching work to how you actually feel",
        summary = "Schedule demanding work when your energy is high and save low-energy tasks for the dip. Time is fixed; energy is not.",
        steps = listOf(
            "Track your energy for a week at a few fixed times.",
            "List your tasks by the energy they require.",
            "Put the highest-energy work in your best window.",
            "Move admin and routine tasks to the low-energy window.",
            "Protect sleep, food and movement because they set the baseline."
        ),
        daily = "Before planning the day, ask what kind of energy the work needs. Writing a strategy or learning something hard belongs in the peak; filing, email and tidying can fill the dip. If a meeting lands in your best window, protect one earlier slot for the important work. Energy management is not an excuse to avoid hard tasks; it is a way to give them a fair chance.",
        pitfall = "A perfect calendar on an empty tank produces nothing. Treat recovery as part of the plan, not a reward for finishing.",
        figureCaption = "A battery indicating high and low energy windows.",
        appActions = listOf(AppAction.INSIGHTS)
    ),
    Technique(
        id = "sleep-priority",
        title = "Sleep Priority + Consolidation",
        aliases = listOf("Sleep priority", "Sleep consolidation", "Sleep", "Study before sleep"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.MOON,
        bestFor = "Focus, memory and emotional stability",
        summary = "Protect a consistent sleep schedule and review important material before bed so memory consolidation works for you.",
        steps = listOf(
            "Choose a consistent wake time, including weekends where possible.",
            "Create a 30-minute wind-down away from bright screens.",
            "Keep the room dark, cool and quiet.",
            "Review the day's key learning briefly before sleep rather than cramming new material.",
            "Aim for seven to nine hours and protect the schedule like a meeting."
        ),
        daily = "The most reliable lever is a consistent wake time, because it anchors the whole rhythm. Dim the lights and stop work at a fixed hour; a shutdown routine helps. If you must study in the evening, review familiar material rather than starting something completely new. A short nap can help, but it does not replace nighttime sleep. Caffeine after early afternoon is a common hidden cause of poor sleep.",
        pitfall = "Sacrificing sleep to get more done usually costs more focus and memory the next day than the extra hour gained.",
        figureCaption = "A moon and a brain locking in the day's learning.",
        appActions = listOf(AppAction.INSIGHTS)
    ),
    Technique(
        id = "movement-breaks",
        title = "Movement Breaks + Walking",
        aliases = listOf("Exercise breaks", "Exercise break", "Walking", "Exercise", "Walking meetings"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.WALK,
        bestFor = "Afternoon slumps and creative blocks",
        summary = "Use short bursts of movement and walking to restore energy, loosen attention and generate ideas.",
        steps = listOf(
            "Set a reminder to stand every 60 to 90 minutes.",
            "Take a five-minute walk away from the desk.",
            "Get outside when possible; light and distance help.",
            "Use a walking meeting or call for low-screen conversations.",
            "Return with one sentence about what you noticed or decided."
        ),
        daily = "Walk after lunch instead of scrolling. Take the stairs for a short break. If a problem is stuck, walk for ten minutes without trying to solve it and see what surfaces. Movement does not need to be a workout to count; the goal is to interrupt long sitting and restore circulation. Keep the break short enough that it does not become a second work session.",
        pitfall = "Using exercise as punishment for eating or sitting turns recovery into another stressor. Move to feel better, not to earn rest.",
        figureCaption = "A walking figure with a small step trail.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "hydration-nutrition",
        title = "Hydration + Nutrition Rhythm",
        aliases = listOf("Hydration", "Nutrition rhythm", "Hydration and nutrition rhythm", "Nutrition"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.WATER,
        bestFor = "Avoiding energy crashes during work",
        summary = "Drink water regularly and eat in a steady rhythm so blood sugar and hydration do not quietly wreck your focus.",
        steps = listOf(
            "Keep a water bottle visible and drink at natural transitions.",
            "Eat meals at roughly consistent times.",
            "Build meals around protein, fibre and slow carbohydrates.",
            "Plan a small snack for the afternoon dip instead of improvising.",
            "Notice which foods or caffeine timing leave you shaky or foggy."
        ),
        daily = "Put a glass of water on the desk before you start work. Drink at the start of each focus block and at meals. A lunch that is mostly refined carbohydrates often produces an afternoon crash; a balanced meal is a more reliable base. Keep caffeine before early afternoon if sleep is fragile. This is not a diet plan, just a stable rhythm that supports attention.",
        pitfall = "Caffeine can mask dehydration and hunger. Check water and food before adding another coffee.",
        figureCaption = "A water droplet and a steady meal rhythm.",
        appActions = listOf(AppAction.EXPLORE)
    ),
    Technique(
        id = "strategic-breaks",
        title = "Strategic Breaks + Naps",
        aliases = listOf("Strategic breaks", "Breaks", "Non-sleep deep rest", "NSDR", "Short naps", "Naps"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.PAUSE,
        bestFor = "Restoring focus before exhaustion",
        summary = "Take a real break before you are depleted, and use a short nap or non-sleep deep rest when you need a deeper reset.",
        steps = listOf(
            "Schedule a break before the point of exhaustion, not after.",
            "Leave the screen and change posture or location.",
            "Choose a short walk, stretch, water or quiet breathing.",
            "If napping, keep it under about 25 minutes and set an alarm.",
            "Return with a single clear task, not a full inbox."
        ),
        daily = "A five-minute break every hour is often more effective than a long break at the end of the day. For a deeper reset, try 10 to 20 minutes of NSDR or a short nap before 3 p.m. Do not spend the break on social media; that often leaves you more scattered. Set a timer so rest does not turn into avoidance.",
        pitfall = "Long or late naps can make sleep harder at night. Keep naps short and early if you use them.",
        figureCaption = "A pause symbol with a short recovery arc.",
        appActions = listOf(AppAction.TIMER)
    ),
    Technique(
        id = "mindfulness-breathing",
        title = "Mindfulness + Breathing",
        aliases = listOf("Mindfulness", "Breathing", "Mindfulness and breathing", "Breathwork"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.BREATH,
        bestFor = "Switching out of stress before work",
        summary = "Use a short breathing or awareness practice to settle the nervous system and return attention to the present task.",
        steps = listOf(
            "Take one minute before a focus block or a difficult conversation.",
            "Breathe in for about four counts and out for about six, without forcing it.",
            "Notice the body, the breath and one sound.",
            "When attention wanders, return without judging the wandering.",
            "Keep the practice short and repeatable rather than aiming for a perfect meditation."
        ),
        daily = "Three slow breaths before opening a difficult document can be enough. A longer practice of ten minutes in the morning can set the tone for the day. The goal is not to empty the mind; it is to notice when it has been captured and come back. Use it at transitions, after a stressful message and before sleep.",
        pitfall = "Expecting a blank mind leads to frustration. The return of attention is the practice, not a failure.",
        figureCaption = "Concentric breathing rings expanding and settling.",
        appActions = listOf(AppAction.EXPLORE)
    ),
    Technique(
        id = "digital-sabbath",
        title = "Digital Sabbath",
        aliases = listOf("Digital sabbath", "Digital detox"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.UNPLUG,
        bestFor = "Resetting attention and sleep",
        summary = "Take a regular block of time off screens and work apps so attention can recover and real life gets some unshared space.",
        steps = listOf(
            "Choose a recurring window, such as one evening a week or Sunday morning.",
            "Tell the people who need to reach you and set an emergency channel.",
            "Prepare offline activities and charge devices away from the bedroom.",
            "Turn off work notifications for the window.",
            "Review how it felt and adjust the length."
        ),
        daily = "Start with one hour rather than a whole weekend. Put the phone in a drawer, close the laptop and do something physical or social. The point is not moral purity; it is to break the automatic reach for the screen and let attention reset. If a full sabbath feels impossible, protect a smaller daily window such as the first hour after waking.",
        pitfall = "A strict detox can become another performance. Choose a sustainable window and keep the emergency channel clear.",
        figureCaption = "A plug pulled out of the wall.",
        appActions = listOf(AppAction.EXPLORE)
    ),
    Technique(
        id = "boundaries",
        title = "Boundaries + Saying No",
        aliases = listOf("Saying no", "Saying no / boundaries", "Boundaries", "Protecting time"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.FENCE,
        bestFor = "Protecting focus from low-value requests",
        summary = "Say no to low-value requests clearly and early, and offer a specific alternative when you genuinely want to help.",
        steps = listOf(
            "Know your top priorities for the week before a request arrives.",
            "Decide whether the request serves those priorities or only the asker's urgency.",
            "Say no briefly and honestly, without a long justification.",
            "Offer a smaller yes or a later time if the relationship matters.",
            "Protect the boundary by keeping the new commitment out of your calendar."
        ),
        daily = "Use a simple script: I cannot take this on this week, but I can review a draft on Friday. For meetings, ask for an agenda or decline if there is none. For messages, set office hours and tell people when you will respond. A boundary is not a wall; it is a clear edge that lets you make a real yes to the work that matters.",
        pitfall = "Saying no to everything becomes isolation. The skill is a clear no and a deliberate yes, not permanent refusal.",
        figureCaption = "A clean fence marking a protected boundary.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "self-compassion-growth",
        title = "Self-Compassion + Growth Mindset",
        aliases = listOf("Self-compassion", "Self compassion", "Growth mindset", "Growth Mindset"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.HEART,
        bestFor = "Recovering from setbacks without quitting",
        summary = "Treat a setback as information rather than a verdict, and treat skill as something that improves with effort and feedback.",
        steps = listOf(
            "Notice the self-critical story after a miss.",
            "Name the feeling and ask what you would say to a friend in the same situation.",
            "Separate the behaviour from your identity: I missed today, not I am a failure.",
            "Choose one small next step that would improve the situation.",
            "Record the lesson and try again with a changed approach."
        ),
        daily = "After a missed habit or a bad day, spend one minute being kind and one minute being practical. Self-compassion is not letting yourself off the hook; it removes the shame that often leads to more avoidance. Replace not good at this with not good at this yet, then name the specific practice that would move you forward. The combination of kindness and a next step is what makes recovery sustainable.",
        pitfall = "Self-compassion without action becomes avoidance. Pair the kindness with one concrete change.",
        figureCaption = "A heart with a small upward step.",
        appActions = listOf(AppAction.EXPLORE)
    ),
    Technique(
        id = "mental-contrasting",
        title = "Mental Contrasting + Visualization",
        aliases = listOf("Visualization", "Visualisation", "Mental contrasting", "WOOP", "Wish Outcome Obstacle Plan"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.IF_THEN,
        bestFor = "Turning a wished-for outcome into a plan",
        summary = "Picture the outcome you want, then picture the obstacle that could stop you, and connect the two with an if-then plan.",
        steps = listOf(
            "Wish: name the outcome clearly.",
            "Outcome: imagine the best realistic result and how it feels.",
            "Obstacle: identify the main internal or external obstacle.",
            "Plan: write, if obstacle happens, then I will do this response.",
            "Rehearse the plan and review it after the first attempt."
        ),
        daily = "Use WOOP for one goal at a time, in five minutes. The obstacle should be specific enough to recognise when it appears: when I feel the urge to check my phone after lunch. The if-then plan should be an action you can actually take in that moment. Visualisation alone can create false confidence; the contrast with the obstacle is what turns the fantasy into preparation.",
        pitfall = "Spending all the time on the positive outcome creates relaxed inaction. Always include the obstacle and the plan.",
        figureCaption = "A wish and an obstacle linked by an if-then plan.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "progress-principle",
        title = "Progress Principle",
        aliases = listOf("Progress principle", "Small wins"),
        category = TechniqueCategory.ENERGY,
        figure = FigureKind.STAIRS,
        bestFor = "Keeping motivation on long projects",
        summary = "Small visible progress is one of the strongest motivators, so make wins visible and record them regularly.",
        steps = listOf(
            "Break the goal into visible milestones.",
            "Track the smallest unit of progress, not only the final result.",
            "Keep a done list alongside the task list.",
            "Share a small win with someone who will notice.",
            "Review the progress weekly and adjust the next step."
        ),
        daily = "At the end of the day, write three things you moved forward, even if they are small. Keep a done column or a simple log. On a long project, put the milestones on the calendar so progress is visible during the messy middle. The point is not to celebrate busyness; it is to notice genuine movement, which makes the next step easier to choose.",
        pitfall = "Only celebrating major milestones makes long projects feel stagnant. Track the small wins that actually happen.",
        figureCaption = "A staircase of small visible wins.",
        appActions = listOf(AppAction.INSIGHTS)
    ),
    Technique(
        id = "weekly-review",
        title = "Weekly Review",
        aliases = listOf("Weekly review", "Projects list + weekly review", "Projects list and weekly review"),
        category = TechniqueCategory.REVIEW,
        figure = FigureKind.REVIEW_ZOOM,
        bestFor = "Starting the week with a trusted plan",
        summary = "Once a week, clear the inbox, review every project, check the calendar and choose the next week's priorities.",
        steps = listOf(
            "Schedule 30 to 60 minutes at the same time each week.",
            "Empty the capture inbox and process loose notes.",
            "Review the calendar for the past and coming weeks.",
            "Check every active project for a next action and move stalled items.",
            "Choose the three priorities for the coming week and write them down."
        ),
        daily = "A Friday afternoon or Sunday evening review works for most people; pick one and protect it. Use a checklist so you do not forget the boring steps. The goal is trust: after the review, you should believe the list contains what matters and nothing is silently lost. If the review keeps getting skipped, shorten it to 20 minutes rather than dropping it.",
        pitfall = "The weekly review is the first thing to disappear in a busy week, which is exactly when it is most useful. Protect a minimum version.",
        figureCaption = "A calendar with a magnifying glass and a checked plan.",
        appActions = listOf(AppAction.TODO, AppAction.INSIGHTS)
    ),
    Technique(
        id = "monthly-quarterly-review",
        title = "Monthly + Quarterly Reviews",
        aliases = listOf("Monthly and quarterly reviews", "Monthly review", "Quarterly review"),
        category = TechniqueCategory.REVIEW,
        figure = FigureKind.REVIEW_ZOOM,
        bestFor = "Zooming out from the weekly grind",
        summary = "Step back from tasks and ask whether the goals, projects and habits still deserve their place.",
        steps = listOf(
            "Schedule a monthly review and a longer quarterly review.",
            "Review progress against goals and key metrics.",
            "List what is working, what is stuck and what has changed.",
            "Decide what to continue, change or stop.",
            "Set the next month or quarter's priorities."
        ),
        daily = "Keep the monthly review to 30 minutes and the quarterly to 90. Use the same questions each time so you can compare. Look at the calendar and the done list, not only your memory. A review that produces no decision is just a report; end with one or two changes to the next period. This is also the right time to retire goals that no longer matter.",
        pitfall = "A quarterly review without a decision changes nothing. Finish by choosing what to stop, start and continue.",
        figureCaption = "A calendar zooming out to reveal a larger plan.",
        appActions = listOf(AppAction.INSIGHTS)
    ),
    Technique(
        id = "start-stop-continue",
        title = "Start / Stop / Continue",
        aliases = listOf("Start/Stop/Continue", "Start Stop Continue", "Start stop continue"),
        category = TechniqueCategory.REVIEW,
        figure = FigureKind.LIGHTS,
        bestFor = "A quick, honest team or personal retrospective",
        summary = "Review a period by deciding what to begin, what to quit and what to keep doing, then turn the short list into real actions.",
        steps = listOf(
            "List the activities and habits from the period.",
            "Put each one in Start, Stop or Continue.",
            "Choose one item from each column to act on.",
            "Give the action a date and an owner.",
            "Review the result at the next retro."
        ),
        daily = "Use it at the end of a week, a sprint or a project. Keep the list short enough to act on. Start might be a fifteen-minute planning block; Stop might be checking email before the first focus session; Continue might be the daily walk that is working. The value is in the follow-through, so choose fewer items and make them observable.",
        pitfall = "A long list of good intentions with no owner or date is not a retrospective. Limit each column and commit.",
        figureCaption = "Three traffic-light panels for start, stop and continue.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "kaizen",
        title = "Kaizen",
        aliases = listOf("Kaizen", "Continuous improvement"),
        category = TechniqueCategory.REVIEW,
        figure = FigureKind.ARROW_STEPS,
        bestFor = "Steady improvement without a big overhaul",
        summary = "Improve one small part of the system regularly and standardise the improvement so it sticks.",
        steps = listOf(
            "Notice a recurring friction in your day.",
            "Choose one small change that addresses it.",
            "Test the change for a week.",
            "Keep it if it helps, discard it if it does not.",
            "Write the improved process down so it becomes the new default."
        ),
        daily = "Ask once a week: what is one small thing that would make next week easier? It might be moving a charger, creating a template or changing the order of a routine. Small changes are less exciting than a full redesign, but they are more likely to survive. Standardise the ones that work, otherwise you will drift back to the old way.",
        pitfall = "Kaizen is not an excuse for endless tinkering. Make a change, test it and move on.",
        figureCaption = "Small incremental arrows moving upward.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "eliminate-automate-delegate",
        title = "Eliminate, Automate, Delegate",
        aliases = listOf("Eliminate automate delegate", "Eliminate, Automate, Delegate", "EAD"),
        category = TechniqueCategory.REVIEW,
        figure = FigureKind.TRASH,
        bestFor = "A task list that has grown too heavy",
        summary = "Before optimising a task, ask whether it can be removed, automated or handed to someone else.",
        steps = listOf(
            "List the recurring tasks that consume time.",
            "For each task, ask first: can it be eliminated entirely?",
            "If not, ask whether a tool or template can automate it.",
            "If not, ask whether someone else can do it with a clear brief.",
            "Only then optimise the task you must keep."
        ),
        daily = "Run this on one category at a time, such as reporting, scheduling or household admin. Eliminating is the cheapest improvement, but people jump to automation too early and end up maintaining a complicated machine. Delegation requires a clear outcome and enough context. Keep a short list of tasks you have removed; it is evidence that the system is getting lighter.",
        pitfall = "Automating a task that should not exist wastes more time. Always try eliminate first.",
        figureCaption = "Three actions: remove, automate and hand off.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "delegation",
        title = "Delegation",
        aliases = listOf("Delegating", "Handing off"),
        category = TechniqueCategory.TOGETHER,
        figure = FigureKind.HANDOFF,
        bestFor = "Freeing time without losing quality",
        summary = "Hand a task to the right person with a clear outcome, the context they need and enough authority to finish it.",
        steps = listOf(
            "Choose a task that someone else can do or learn to do.",
            "Define the outcome, deadline and standard of quality.",
            "Explain why it matters and what decisions they can make alone.",
            "Share the relevant context and a way to ask questions.",
            "Check in at agreed points and review the result, not every step."
        ),
        daily = "Start with one recurring task rather than a whole project. Write a short brief: outcome, deadline, constraints, and what done looks like. Agree on the level of authority: decide, recommend or ask. A check-in is not micromanagement if the interval is agreed in advance. When the work returns, review the result and give specific feedback so the next handoff is easier.",
        pitfall = "Dumping a task without context is not delegation. The other person needs a clear outcome and a way to succeed.",
        figureCaption = "A baton passing from one person to another.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "meeting-hygiene",
        title = "Meeting Hygiene",
        aliases = listOf("Meeting hygiene", "Better meetings"),
        category = TechniqueCategory.TOGETHER,
        figure = FigureKind.MEETING,
        bestFor = "Calendars full of low-value meetings",
        summary = "Every meeting needs a purpose, an agenda, the right people and a clear action at the end, or it should be a message.",
        steps = listOf(
            "Ask whether the meeting is necessary or could be async.",
            "Write the purpose and agenda before sending the invite.",
            "Invite only the people who need to decide or contribute.",
            "Start and end on time; default to 25 or 50 minutes.",
            "End with owners, actions and dates, and share notes."
        ),
        daily = "Before accepting a meeting, ask what decision or output it produces. If there is no agenda, ask for one or suggest a written update. In the meeting, name the decision to be made and park side topics. End two minutes early to capture actions. A meeting that could have been a message is a tax on everyone in the room.",
        pitfall = "Meetings become status updates. If no decision or collaboration is needed, use a written update instead.",
        figureCaption = "A clock with a short agenda and action list.",
        appActions = listOf(AppAction.TODO)
    ),
    Technique(
        id = "async-office-hours",
        title = "Async Communication + Office Hours",
        aliases = listOf("Async communication", "Asynchronous communication", "Office hours", "Interruption windows"),
        category = TechniqueCategory.TOGETHER,
        figure = FigureKind.PLANE,
        bestFor = "Reducing live interruptions",
        summary = "Move non-urgent communication to written async messages and set specific windows when you are available live.",
        steps = listOf(
            "Choose an async channel for updates, questions and decisions.",
            "Write enough context for the other person to respond without a meeting.",
            "Set a response-time expectation for each channel.",
            "Publish office hours for live questions.",
            "Batch live conversations into those windows and protect the rest."
        ),
        daily = "Use a written update at the start of the day and a short summary at the end. For questions, include what you have tried and what you need. Set office hours such as 2 to 3 p.m. on Tuesday and Thursday, and tell people that urgent issues should use the agreed urgent channel. Async is not slower; it is clearer and less interruptive when the writing is good.",
        pitfall = "Async messages can become vague and endless. Include context, a clear ask and a deadline.",
        figureCaption = "A paper plane carrying a written message.",
        appActions = listOf(AppAction.EXPLORE)
    ),
    Technique(
        id = "written-decisions",
        title = "Written Decisions + Docs",
        aliases = listOf("Written decisions", "Written decisions and docs", "Decision docs", "Decision records"),
        category = TechniqueCategory.TOGETHER,
        figure = FigureKind.DOC,
        bestFor = "Avoiding repeated discussions and lost context",
        summary = "Write the context, options, decision and owner in one short document so the team can stop re-litigating and start acting.",
        steps = listOf(
            "Write the problem and the decision to be made in one paragraph.",
            "List the viable options and the main trade-off of each.",
            "Record the decision, the reasoning and the owner.",
            "Add the date and a review date if the decision is reversible.",
            "Share the link where the work happens and update it when things change."
        ),
        daily = "Keep decision docs to one page. A good doc answers: what is the problem, what did we choose, why, who owns it and when will we revisit it? Link it in the project or task. The goal is not bureaucracy; it is to save the team from having the same conversation three times because the reasoning lived only in someone's memory.",
        pitfall = "Documents nobody reads are waste. Keep them short, link them at the point of work and write only decisions that matter.",
        figureCaption = "A short document with a decision and a check mark.",
        appActions = listOf(AppAction.TODO)
    )
)
