# Soul Track

**Offline-first, privacy-first life tracking Android app — built around the ring.**

Soul Track is a **time tracker first**: every second you track feeds the day
ring, the growth score, the routine/comfort-zone detection and the insights.
It is built to answer one question — where did my time go, and how do I get
more of it back? Every number the app shows is computed from your own records.
Nothing is stored in the cloud, there are no accounts, no ads and no tracking.

## The rings

The app icon is the product: the **inner ring** is today's task progress (with
a needle that snaps when the last task lands), the **outer ring** is seven
status marks — one per day — that only change at midnight, with today left dim
and marked by a white dot. The same `SoulRings` component drives the Today
screen, History and the widgets, in a pure-black AMOLED theme with a mint →
aqua → sky gradient and Manrope typography. See [RESEARCH/DESIGN.md](RESEARCH/DESIGN.md).

## Features

- **Time Tracking**: Active timer with foreground service, manual logging, session management
- **Productivity Analysis**: Track productive, wasted, neutral, learning, and exercise time
- **Growth Score**: Transparent daily score from weighted components
- **Routine Loop Detection**: Detect repetitive patterns using cosine similarity
- **Insights Center**: Personalized warnings and suggestions
- **Our own targets and calibration**: set productive/learning minutes in Settings, or let the app calibrate from your trailing fortnight
- **Export/Import**: JSON and CSV backup/restore
- **Settings**: Profile, targets, privacy controls
- **Soul Rings**: inner ring = today's tasks, outer ring = 7 day statuses
- **Day Classification**: every day ends as Done / Tasks not met / Not
  productive / Wasting time, with thresholds you can tune in Settings
- **Themes**: AMOLED black, soft dark, light, system, plus dynamic colour on
  Android 12+
- **Focus sessions**: aqua work ring, sky break ring, keep-screen-on, optional
  Do Not Disturb
- **Task switch detection**: when a session jumps to another task within five
  minutes, the jump is counted, the dropped task stays in view, and Insights
  shows the resumption delay plus focus after a jump vs the rest of the day
  (the "hallucination" index, 0-100)
- **Intelligent task recommendation**: the Dashboard picks the next task from
  your own history, the current focus level, mood and energy — and always
  prefers the task already in flight over starting a new context
- **Your own targets**: productive/learning/exercise minutes, sleep hours,
  water and pomodoro goals are set in Settings, and anything you leave empty
  is calibrated from your own trailing fortnight

### Planned
- **Persistent Task Reminders**: an ongoing notification that keeps your due
  task visible and re-reminds until it's done or snoozed

## Privacy

- All data stays on your device
- No cloud accounts
- No telemetry or analytics
- No ads
- No proprietary dependencies
- Sensitive health tracking is opt-in and toggleable at any time
- Medical disclaimer displayed throughout

## Medical Disclaimer

DailyTrack provides **educational insights and lifestyle indicators only**. It is **not medical advice**. If you have persistent symptoms, please consult a healthcare professional.

## Building

### Prerequisites

- JDK 17+
- Android SDK (compileSdk 35)
- Gradle 8.11.1 (included via wrapper)

### Build Commands

```bash
# Build debug APK
./gradlew assembleFdroidDebug

# Build release APK (signed if signing.gradle / keystore.properties present)
./gradlew assembleFdroidRelease

# Run unit tests (flavor-qualified; plain testDebugUnitTest is ambiguous)
./gradlew testFdroidDebugUnitTest

# Run lint checks
./gradlew lintFdroidDebug
```

## F-Droid Submission

**Important**: Before publishing to F-Droid, replace the placeholder `applicationId` in `app/build.gradle.kts` with a unique reverse-domain applicationId that you control.

The current applicationId `io.github.dailytrack` is a placeholder.

## License

Apache License 2.0

## Third-Party Notices

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for dependency licenses.

## ☕ Support the Developer

If Soul Track helps you, consider buying me a coffee!

<a href="https://buymeacoffee.com/kusal630" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 50px !important; width: 180px !important;"></a>
