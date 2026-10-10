# Changelog

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
