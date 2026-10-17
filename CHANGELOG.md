# Changelog

## [2.6.0] - 2026-10-17

### Added
- **If-then plans (implementation intentions) on tasks.** When you add a task
  you can now name the cue that should trigger it ("after I sit at my desk")
  and optionally pick a cue hour. The task card shows the plan as
  *"If <cue>, then this."* and the app sends one reminder at the cue hour so you
  start when the moment arrives. This is the best-evidenced way to close the gap
  between intending to do something and actually doing it (Gollwitzer &
  Sheeran, 2006, d = .65; Sheeran et al., 2024).
- **Sleep-aware focus advice.** The focus/focusability estimate now reads last
  night's sleep from your own records: a short night lowers the score and a
  well-rested one raises it, because sleep restriction reliably impairs
  sustained attention (Lowe et al., 2017, g = -0.41).

### Changed
- **Break coach is evidence-based.** Focus-session break reminders now suggest a
  real ~10-minute rest rather than a moment away from the screen, since brief
  breaks restore vigor and cut fatigue while longer breaks help most on
  demanding work (Albulescu et al., 2022).

## [2.5.0] - 2026-10-17

### Fixed
- **Phantom productivity hours are gone.** Phone-usage time was being written
  into the sessions table as if it were work the user had done, which is what
  produced impossible days (the "41h tracked" report). Usage is now read live
  for the Direction card only; it is never stored as tracked time. A database
  migration removes the legacy usage rows from existing installs.
- **Stale sessions can no longer run forever.** A session left active from a
  previous day is closed at its own start on next launch/tick, so a killed
  service can never inflate every following day.
- **No more double timer.** Starting a pomodoro no longer also launches the
  standalone timer service, so there is only ever one timer notification. If a
  plain timer was running, the pomodoro takes it over; `reconcile` never
  auto-starts the timer service for a pomodoro.
- **Rotation no longer reloads the app.** The splash/logo state survives
  configuration changes (`rememberSaveable`), so rotating the phone keeps you
  where you were.
- **Insight actions are real buttons.** The suggested action on each insight is
  now tappable and opens the relevant screen (Tasks, Timer, Habits or Explore)
  instead of being dead text.
- **Growth score is honest.** Consistency counts the days you actually tracked
  in the trailing week rather than resetting to zero on a single missed day, and
  target floors stop a couple of light days from making the score saturate.

### Changed
- **Better quotes, refreshed automatically.** Online quotes are now merged from
  several curated sources and de-duplicated for a larger, higher-quality pool.
- **Smarter timing.** The focus/focusability estimate now weighs the weekday
  rhythm (Monday warm-up, mid-week peak, weekend pace) alongside hour, noise,
  mood and energy.

## [2.4.1] - 2026-10-17

### Fixed
- **Home navigation is now reliable.** Tapping the Home tab from any screen
  (including deep screens opened from a Home card) always returns to Today.
  Navigating to the graph start destination with `saveState`/`restoreState`
  is a no-op in Navigation 2.8.x, which left the user stuck on the deep screen;
  Home now uses the same `navigateToHome()` path as the Back button. In-app back
  arrows and system Back now agree.
- **Pomodoro breaks are no longer killed on start.** The pomodoro service
  stopping its mirrored Timer session used to cascade a stop back into itself,
  ending every break immediately. The stop now explicitly opts out of the
  pomodoro cascade.
- **No more orphaned/duplicated pomodoro rows.** A completed work session is
  closed *before* the break row is inserted, instead of racing it.
- **Honest day ratios.** `trackedRatio`/`productivityRatio` are measured against
  elapsed time, not the full 24 hours, so midday tracking is no longer halved.
- **Inactive sessions with a missing end time** no longer count as a full day.
- **Wellbeing sync** clips sessions to its window and matches overlapping
  sessions, so it cannot write duplicate usage sessions over tracked time.
- **Timer ring** fills against the user's own focus goal instead of a hardcoded
  8 hours, matching the Home ring.
- **History** shows the running session (live duration and timeline) instead of
  a zero-width bar.
- **Pomodoro widget** no longer reads "Session 0/4" while idle and derives
  long/short break from the real state.
- The day coverage/untracked figure now advances every tick even with no
  running session.
- Fixed a lint `NewApi` error (`windowLightNavigationBar` on minSdk 24).

### Removed
- Dead and placeholder-only screens: the unreachable Growth screen plus the
  non-functional Sleep, Exercise, Nutrition, Body and Journal screens. Saved
  Quotes is now reachable from Explore. The app stays focused on tracking and
  improving productive time.

## [2.3.0] - 2026-10-13

### Added
- **AMOLED design system.** Dark mode is now true black (`#000000`) with a
  five-step surface ladder instead of shadows; light mode is a matching warm
  white ladder. One card shape (20dp), one rhythm (16/12/24dp) and one
  typography scale across the app.
- **Component kit** (`ui/components/ScreenScaffold.kt`, `ModernCards.kt`):
  `ScreenList`/`ScreenScaffold` for consistent chrome, `SurfaceCard`,
  `PressableCard`, `ScreenSection`, `SectionTitle`, `StatTile`, `KeyValueRow`,
  `PillChip`, `PrimaryCta`/`TonalCta`/`GhostCta`, `EmptyState`, `Hairline`,
  `IconRow`, plus spacing (`Dimens`) and motion (`Motion`) tokens.
- **Explore hub.** Twelve screens that existed but could never be opened are
  now reachable: Sleep, Exercise, Nutrition, Body, Journal, Goals, Reminders,
  History, Analytics, Focus mode, Achievements and Weekly report — plus
  cross-links from Growth (Body, Goals, History, Analytics, Achievements) and
  Insights (Weekly report). Linked from the Home top bar and Settings.
- Analytics is now a real screen: seven-day time distribution pie, per-day
  productivity bars and a growth-score ring built from actual data, with an
  empty state before any data exists.
- Goals, Reminders, Achievements and Weekly report are wired to real
  Room-backed state in `MainViewModel` instead of being dead composables.

### Changed
- Every screen restyled: `ScreenList`/`ScreenScaffold` headers, `SurfaceCard`
  containers, pill buttons, tinted icon-disc empty states, and no more
  placeholder copy ("charts will appear here").
- Bottom navigation: hairline-topped `surfaceContainer` bar with a 16%-alpha
  pill indicator and a clearer selected/unselected color pair.
- Chart tracks are `surfaceContainerHighest` instead of `Color.Gray`, so they
  read correctly on black.
- Focus mode, onboarding, gamification and the weekly report were redesigned
  to the same tokens as the rest of the app.
- `RESEARCH/DESIGN.md` rewritten to describe the shipped system.

## [2.2.0] - 2026-10-13

### Fixed
- Timer, widget, pomodoro and the notification now read one single source of
  truth (`TimerStateStore`), so the widget no longer freezes or shows a stale
  session after the app process is killed.
- Every stop path (timer screen, widget, notification, pomodoro) sends an
  explicit session id; a stop can no longer race and kill a session that was
  started a moment later.
- The app reconciles its saved timer state with the real session on the
  database at launch: an orphaned service is restarted, a dead one is cleaned
  up, and the widget is repainted.
- Do Not Disturb is enabled only for Productive/Learning sessions or
  deep-focus picks, and is always restored when a session ends or the service
  dies - including when the app is force-stopped.

### Added
- **Intelligent task recommendation layer** (`engine/BrainLayer`): ranks every
  open task into one "Right now" pick plus alternatives, with a one-line human
  reason. Weights urgency, priority, focus fit, personal behaviour, history
  and effort; deadline and high priority are hard weights that can never be
  buried. Works with zero training history.
- **Focusability estimator** (`engine/FocusabilityEngine`): 0-100 score for
  how deep a moment is, from time of day, day of week, break/session length,
  ambient quietness and mood/energy, with reason strings.
- **Personal behaviour profile** (`engine/BehaviorProfile` + `BehaviorProfiler`):
  shrinks toward a neutral prior until enough data exists, so cold start
  behaves correctly instead of randomly.
- Dashboard "Right now" card: focus meter, environment line, primary
  suggestion with reason, two alternatives, and Start / Done actions. Deep
  focus picks turn Do Not Disturb on automatically.
- **Focus & Permissions settings**: one section to grant Do Not Disturb
  access, grant the microphone, and switch recommendations on or off.
- Ambient noise sensing (`service/NoiseMonitor`): an opt-in microphone read
  that produces only a 0-100 quietness number. Audio is never recorded to a
  file, stored or transmitted; it listens only while a focus session runs and
  stops with it.
- Timer foreground service gained `ACTION_DND_CHANGED`, session-id extras and
  auto-Do-Not-Disturb support.

### Changed
- Quote display now rotates through a shuffle pool of the 89 "best" quotes
  (Bhagavad Gita, Stoics, world classics) on app start, without repeats inside
  a process, and pushes the current quote to both widgets.
- Additional Gita verses and world-book quotes added to the offline pool.

## [2.1.0] - 2026-10-09

### Fixed
- Day reports now include sessions that cross midnight, using an overlap query
  instead of requiring a session to start inside the day.
- Time coverage accounts for a still-running session, merges overlapping and
  back-to-back intervals before summing, classifies `REST`/`RECOVERY` as
  recovery, and clamps tracked/productivity ratios to `0..1`.
- Dashboard, Insights, growth score, loop detection, and the weekly report no
  longer double-count Pomodoro and completed to-do minutes, and they clamp
  durations to the selected day instead of counting whole sessions.
- Dashboard pie chart shows sleep, social, and recovery slices instead of
  hiding them; the weekly distribution chart shows the same slices.
- Pomodoro sessions are persisted and restored when the service is restarted,
  so an active focus/break no longer silently stops recording.
- Pomodoro auto-advances to the next phase immediately when the timer expires,
  with a completion notification, instead of waiting for a broadcast that was
  never delivered.
- Do Not Disturb is restored to its previous filter when a session ends.
- Stopping the timer now closes the open session on disk and stops the widget
  from showing a stale elapsed time.
- The timer, widget, and pomodoro screens clamp elapsed time so a bad clock
  reading can no longer produce negative or overflowing durations.
- Growth score is neutral (not red) before any time has been tracked.

### Changed
- The timer and pomodoro screens tick once per second instead of ten times.
- Top-level tabs navigate Home from their back arrow instead of popping the
  navigation stack, and repeated tab jumps no longer stack duplicate entries.
- Task action buttons and insight dismiss buttons use 48dp touch targets.
- The About screen shows the real app version instead of a hardcoded value.

### Added
- Habits is reachable from the home screen and is now wired up to real data.
- "Clear All" on completed tasks asks for confirmation before deleting.
- F-Droid and fastlane metadata updated for `versionCode` 8.

## [0.1.0] - 2024-01-01


### Added
- Core time tracking with active timer and manual logging
- Category system with productivity classification
- Growth score calculation with weighted components
- Routine loop detection using cosine similarity
- Food and drink logging
- Hydration tracking with configurable goals
- Nutrition analysis with macro and micronutrients
- Sleep tracking and debt calculation
- Exercise and movement logging
- Body systems dashboard with indicator cards
- Mood, energy, and stress check-ins
- Journal and life events
- Insight engine with safe, educational warnings
- Analytics and charts
- Settings with privacy controls
- Export/import functionality
- Medical disclaimer throughout the app
- Offline-first architecture
- No cloud, no ads, no tracking
