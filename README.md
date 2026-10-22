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

- **Today**: one day ring, a live tracking chip, one primary focus action, and
  the three tasks that matter today.
- **Focus**: work/break cycles, a start ritual, break coach, optional Do Not
  Disturb, and one-tap access to the manual timer for work you forgot to track.
- **Tasks**: a clean priority-sorted list, optional daily planner, quick add,
  and logging for work already done.
- **Insights**: a daily growth score, this week's seven status bars, and one
  active suggestion. No chart wall.
- **More**: techniques, habits, history, reminders and saved quotes live here,
  out of the daily path.
- **Time tracking**: active timer with foreground service, manual logging and
  session history.
- **Day classification**: every day ends as Done / Tasks not met / Not
  productive / Wasting time, with thresholds you can tune in Settings.
- **Private device sync**: pair a phone and tablet with a QR code and sync
  directly over your LAN or VPN, with authenticated AES-GCM encryption and no
  cloud account.
- **Settings**: targets, focus guardrails, privacy controls and themes.
- **Soul Rings**: inner ring = today's tasks, outer ring = 7 day statuses.
- **Themes**: AMOLED black, soft dark, light, system, plus dynamic colour on
  Android 12+.

## A calmer surface

The app intentionally keeps just four destinations: **Today, Focus, Tasks and
Insights**. Research on feature overload and dashboard attention led to three
rules:

1. **One primary action per screen.** Today asks you to start or continue a
   focus block; everything else is secondary.
2. **Progressive disclosure.** Techniques, habits, history, reminders and
   quotes sit under More, while advanced planner and guardrail settings stay
   in Settings.
3. **Few, high-signal numbers.** Insights shows a score, a week pattern and one
   suggestion instead of a wall of overlapping charts.

The result is a shorter path from opening the app to starting meaningful work.

### Planned
- **Persistent Task Reminders**: an ongoing notification that keeps your due
  task visible and re-reminds until it's done or snoozed

## Privacy

- All data stays local unless you explicitly pair another device
- Optional device sync is direct and end-to-end encrypted; no cloud relay
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
- Gradle 9.5.0 (included via wrapper)

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
