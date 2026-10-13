# Project

Idea: DailyTrack is an offline-first, privacy-first life tracking Android app. The goal is to make it the best-in-class time tracker with world-class UI, animations, and user experience.

Platform: Android (Kotlin, Jetpack Compose, Material 3)

# Features (v2 — Must Build for Best-in-Class)

Status key: **done** / **in progress** / **not started**. See `RESEARCH/DESIGN.md` for the shipped design system and `RESEARCH/UI_SWEEP_BRIEF.md` for the rules every screen follows.

## 1. Timer UI Overhaul (Critical) — **done**
- Circular animated timer with pulsing glow effect when running
- Smooth start/stop animations with spring physics
- Category color bleed into entire screen background
- Elapsed time with monospace font and subtle animation on digit change
- Quick-action chips for common categories
- Swipeable session history mini-timeline below timer
- Haptic feedback on start/stop

## 2. Pomodoro Screen Premium Redesign — **done**
- Full-screen immersive experience during active session
- Circular progress ring with gradient stroke and animated glow
- Ambient background that shifts color as time progresses
- Session counter with animated ring segments
- Break screen with calming animation (breathing circle)
- Long break after 4 consecutive sessions
- Sound toggle with visual indicator

## 3. Quote System Anti-Repetition — **done**
- Smart rotation: never repeat within 24h, track shown quotes
- Context-aware: morning/afternoon/evening quotes
- Category-based quotes tied to activity type
- Animated quote transitions (fade + slide)
- User can add custom quotes
- Widget: quote changes on tap with smooth crossfade

## 4. Dashboard Visual Hierarchy — **done**
- Hero section with animated growth score ring
- Time breakdown with animated bar chart
- Streak counter with fire animation
- Quick stats row with animated counters
- Today timeline view (horizontal scroll)

## 5. Design System — **done** — AMOLED Material 3, applied to every screen
- 4dp grid spacing system
- Typography scale: Display to Label
- Semantic color system (productive=green, wasted=red, exercise=orange)
- Animation standards: micro 150ms, standard 300ms, slow 500ms
- Motion patterns: shared element transitions

## 6. Polish and Micro-interactions — **in progress**
- Button press animations (scale)
- Card hover states with elevation
- Loading skeletons instead of spinners
- Empty states with illustrations
- Success celebrations

## 7. Intelligent Task Recommendation Layer (v3 headline feature) — BUILT (v2.2.0)

The app decides **which task you should do right now** and surfaces it before you have to think.

**Where it lives:** `engine/BrainLayer.kt` (scoring),
`engine/FocusabilityEngine.kt` (0–100 moment score), `engine/BehaviorProfile.kt` +
`engine/BehaviorProfiler.kt` (per-hour behaviour with cold-start shrinkage),
`service/NoiseMonitor.kt` (opt-in quietness), `ui/brain/RecommendationCard.kt`
(Dashboard "Right now" card), `service/FocusSettings.kt` + Settings →
Focus & Permissions (DND access, mic permission, on/off). Tests:
`app/src/test/java/io/github/dailytrack/engine/{BrainLayer,FocusabilityEngine,BehaviorProfile}Test.kt`
and `service/NoiseMonitorTest.kt`.

**Still open:** predicted mood (only the latest check-in is read today), the
persistent task reminder notification, and a learned calibration layer.

### Inputs
- **Focus level + environment.** Sense how focusable the moment is (ambient
  noise estimate from the mic where permitted, time of day, time since last
  break, current session/pomodoro state, time on screen) and map it to a
  0–100 focusability score.
- **Predicted mood.** Train an on-device model from the user's own mood
  check-ins (`MoodCheckInEntity`: mood, energy, stress) keyed on time of day,
  day of week, recent sleep/recovery, and their task history. Goal: predict how
  the user will feel with near-perfect accuracy for that individual.
- **Task performance history.** For each task, learn when it actually gets
  done — completion rate, time-to-complete, and pomodoro success by hour and
  by context.
- **Priority.** Never let the model bury a high-priority task: `priority` from
  `TodoEntity` (and deadline proximity) acts as a hard weight, not a hint.

### Output
- A ranked recommendation: *"Right now, do X"* plus 2–3 alternatives.
- Short, human reason for the pick ("quiet hour + you finish writing tasks
  best before noon").
- If focusability is low → recommend **low-focus-required** tasks (inbox
  triage, errands, easy admin).
- If focusability is high and mood/energy is good → recommend
  **high-focus-required** tasks (deep work, learning, the overdue high-priority
  item).
- Explanations are shown as suggestions, never as commands or diagnosis.

### Privacy
- Everything trains and infers on-device. No cloud inference, no telemetry.
- Model is opt-in, retrainable, and resettable from Settings.

## 8. Persistent Task Reminders — **not started**
- Persistent (ongoing, non-dismissable-while-due) notification that keeps the
  recommended/current task visible.
- Re-reminds on a smart cadence until the task is done or explicitly snoozed.
- Snooze / done / start-actions directly from the notification.
- Quiet hours and per-task reminder overrides.
- Built on the existing `ReminderEntity` + WorkManager scheduling so F-Droid
  builds need no Play Services.

# Implementation Plan (current)
1. Fix bugs and UX issues — **done** (single timer source of truth, stop-race
   fix, state reconcile, Do Not Disturb restore)
2. Take UX/UI to best-in-class experience — **design system shipped**;
   remaining: on-device visual QA of all 23 screens, micro-interactions and
   loading skeletons
3. Add the intelligent decision layer **and** the persistent notification
   feature that keeps reminding the user about their tasks
   - intelligent decision layer — **done** (out-of-the-box, tested)
   - persistent notification reminders — **not started**

**Acceptance for step 3:** the intelligent layer must work **out of the box,
almost perfectly** — i.e. correct and useful with zero training history (pure
cold-start), then keep improving as data accumulates. It must be covered by
unit tests over synthetic fixtures before it ships.

# Done criteria (loop stops when ALL true)
- [x] Timer screen has premium circular design with animations
- [x] Pomodoro screen is immersive with ambient color shifts
- [x] Quote system never repeats within 24h and is context-aware
- [x] Dashboard has animated visual hierarchy
- [x] DESIGN.md system applied to every screen (AMOLED, 20dp cards,
      `ScreenList`/`ScreenScaffold` chrome, pill buttons, empty states)
- [x] Build passes with no critical bugs (`assembleFdroidDebug`, unit tests,
      `lintFdroidDebug` all green)
- [x] All screens feel premium and cohesive
- [ ] Every screen reachable — Explore hub + Growth/Insights cross-links done;
      verify on device
- [x] Intelligent layer recommends the right task for low-focus and high-focus
      moments, weights task priority correctly, and is never worse than a naive
      "sort by priority" baseline
- [x] Intelligent layer passes unit tests from a **cold start with no history**
- [ ] Persistent notification keeps a due task visible and re-reminds until
      resolved

# Loop instruction
Each run: read RESEARCH/*.md, pick highest-priority work, build it.
