# DESIGN.md — Soul Track Design System

The UI is the ring logo, extended. Pure-black AMOLED canvas, one mint → aqua →
sky gradient, Manrope everywhere, and a single component — `SoulRings` — that
carries the product idea: **today is the inner ring, the week is the outer
ring.**

## Principles

1. **True black canvas.** Dark background and surface are `#000000`; on OLED
   those pixels are off. Depth comes from tonal steps and 1dp hairlines, never
   from shadows.
2. **The rings do the talking.** Progress is drawn as arcs, not bars, wherever
   a day or a week is the subject. Bars are for distributions.
3. **Colour is never the only signal.** Every day status is icon + word + hue.
   WCAG contrast is checked against the black canvas.
4. **8dp rhythm.** Grid `8dp` (half step `4dp` allowed), card gap `16dp`,
   section gap `24dp`, screen inset `16dp`.
5. **One primary action per screen.** A full-width pill `PrimaryCta` with the
   brand gradient; everything else is tonal, outlined or ghost.
6. **Stillness respects stillness.** All motion is driven by
   `Settings.Global.ANIMATOR_DURATION_SCALE` — reduce-motion turns the spring
   animations into instant state changes.

## Color Palette

### Brand (the logo gradient)
| Token | Value | Use |
|---|---|---|
| `Mint` | `#7CF5C4` | primary, CTA start, active accents, needle hub |
| `Aqua` | `#4FD1E8` | secondary, focus ring, CTA middle |
| `Sky` | `#5B9DFF` | tertiary, break ring, CTA end, week trend |
| `OnMint` | `#00201A` | content on mint/aqua fills |

`brandColorAt(t)` lerps `Mint → Aqua → Sky` for any `t` in 0…1 (used by the
sweep gradients, the trend line and the CTA brush).

### AMOLED surface ladder (default, dynamic colour OFF)
| Token | Value |
|---|---|
| `background` / `surface` | `#000000` |
| `surfaceContainerLow` | `#0A0A0F` |
| `surfaceContainer` | `#12121A` — cards |
| `surfaceContainerHigh` | `#1B1B24` |
| `surfaceContainerHighest` | `#24242F` — chart tracks, empty arcs |
| `outline` / hairlines | `#33333F` / white at 8% |

Text: `onSurface` `#ECECF2`, `onSurfaceVariant` `#A3A3B2`, disabled `#6B6B7A`.

Alternates: **Soft dark** background `#0E0E13`; **Light** mirrors the same
structure on `#FCFCFF`. Theme mode and dynamic colour are persisted by
`ThemeSettings` (Settings → Appearance).

### Day status palette (`DayStatusPalette`, `LocalDayStatusColors`)
| Status | Colour | Icon |
|---|---|---|
| Done (all tasks) | `#5EF2B4` | check circle |
| Tasks not met | `#FFB84D` | repeat |
| Not productive | `#B39DFF` | timer |
| Wasting time | `#FF5D6C` | fire |
| No tasks assigned | `#2A2A33` | assignment |
| In progress (today) | `#2A2A33` + white dot | hourglass |

Classification order (`engine/DayClassification.kt`), early return first:
no tasks → `NO_TASKS`; distraction > 25% of tracked → `WASTING_TIME`;
productive < 40% of planned focus → `NOT_PRODUCTIVE`; completion < 100% →
`TASKS_NOT_MET`; else `DONE`. Both thresholds live in
`DayClassificationThresholds` and are retuned from Settings → Day ring at
runtime (defaults are exactly what the unit tests assert).

### Semantic / category colours (unchanged semantics)
Productive `#5EF2B4` · Learning `#3B82F6` · Exercise `#F59E0B` ·
Wasted `#FF5D6C` · Social `#8B5CF6` · Sleep `#6366F1` · Recovery `#34D399` ·
Neutral `#6B7280`.

## Typography (Manrope — `SoulFontFamily`)

Weights: Regular 400, Medium 500, SemiBold 600, Bold 700
(`res/font/manrope_*.ttf`). `InterFontFamily` is a deprecated alias.

| Style | Size / weight | Use |
|---|---|---|
| `displayLarge` | 56 / Bold, tabular | hero numbers |
| `TimerDigits` | 56 / SemiBold, tabular figures | timer countdown |
| `TimerDigitsSmall` | 16 / SemiBold, tabular | tracker chips, rows |
| `headlineSmall` | 22 / SemiBold | screen titles |
| `titleLarge` | 22 / SemiBold | card hero titles |
| `titleMedium` | 16 / SemiBold | section titles |
| `titleSmall` | 14 / SemiBold | list titles, buttons |
| `bodyLarge` | 16 / Regular | primary body |
| `bodyMedium` | 14 / Regular | secondary body |
| `bodySmall` | 12 / Regular | captions |
| `labelMedium` / `labelSmall` | 12 / 11 Medium | chips, badges |

Numbers that change in place always use `tabularFigures()` or a `TimerDigits`
style so digits never jitter.

## Spacing (8dp grid) — `Dimens`
`Grid` 8 · `ScreenPadding` 16 · `SectionGap` 24 · `CardPadding` 16 ·
`CardGap` 16 · `TitleGap` 8 · `MinTouchTarget` 48 · `ButtonHeight` 56 ·
`BottomBarHeight` 80 · `HeroRing` 260 · `MiniRing` 72

## Shapes — `SoulTrackShapes` / `SoulShapes`
`extraSmall` 8 · `small` 12 · `medium` 16 · `large` **24 (cards)** ·
`extraLarge` 28; plus `SoulShapes.Pill` (50%), `SoulShapes.Sheet` (28dp top).

## Motion — `Motion`
micro 150ms · standard 260ms · slow 420ms. Rings use
`spring(dampingRatio = MediumBouncy, stiffness = MediumLow)` and a stiff
spring under reduce-motion. Press feedback = scale 0.975.

## `SoulRings` — the core component (`ui/components/SoulRings.kt`)

512-unit drawing grid: outer radius 150 / stroke 26, inner radius 92 /
stroke 14, week segments inset 8° with round caps, needle 70 units + hub,
glow = an 18dp blurred pass of the same strokes under the crisp ones.

```kotlin
SoulRings(
    dayProgress = 0f..1f,          // inner ring: today's task completion
    weekStatuses = listOf<DayStatus?>(...), // outer ring, Mon..Sun, null = future
    todayIndex = index,            // today's segment stays dim + white dot
    showNeedle = true,
    animate = true,
    onDayCompleted = { ... },      // fires a haptic tick when progress hits 1
    contentDescription = "Today: 3 of 5 tasks done."
)
```

Supporting pieces:
- `MiniSoulRing(progress, status, isToday)` — 44–72dp ring for lists, week
  strips and empty states.
- `WeekStatuses.kt` — `weekStatusesFor`, `dayProgressFor`, `daySnapshotFor`,
  `weekStartFor`, `WeekDayNames`, `WeekDayShortNames`: pure functions that
  turn the existing `TimeCoverage` weekly report + `TodoEntity` list into the
  numbers the rings need. No data-layer changes.

### Logo / icon / splash
`res/drawable/ic_launcher_foreground.xml` is the same geometry (R_OUT 29.5,
R_IN 18) with 42 paths: status-coloured week segments, dim today segment with
a white dot, gradient inner arc and needle. Monochrome variant for themed
icons; splash is `Theme.DailyTrack.Splash` + `core-splashscreen`, and
`MainActivity` installs it with `ThemeSettings.load(this)` before setContent.

## Component kit

### `ScreenScaffold.kt`
`ScreenList(title, subtitle, navController, onBack, compact, actions, fab, content)`
— collapsing large title over black, 16dp inset, bottom padding applied.
`ScreenScaffold(...)` for custom content, `InlineHeader(...)` for headers
inside content.

### `ModernCards.kt`
`SurfaceCard` (24dp, tonal, hairline, optional `onClick`) · `PressableCard` ·
`ScreenSection` · `SectionTitle` · `StatTile` · `KeyValueRow` · `PillChip` ·
`PrimaryCta` (brand gradient `primary → secondary → tertiary`, content on
`onPrimary`, disabled = surface tonal) · `TonalCta` · `GhostCta` ·
`EmptyState` (icon disc → title → message → action, every empty list) ·
`Hairline` · `IconRow` · `CompletedSessionRow` pattern.

### Also
`PieChart`, `PieChartLegend`, `ProgressRing` (`Charts.kt`) — tracks are
`surfaceContainerHighest`, never `Color.Gray`; `MoodSheet`, `UpNextCard`
(`ui/brain/`); `WeekStatusBars`, `StackedTimeBar` (Insights).

## Screens

- **Today** (`DashboardScreen`) — `TodayHero` (260dp `SoulRings` + counts +
  status legend), tracker chips with tabular timers, `UpNextCard`
  (gradient border, reason chips, FocusBadge, Start / Not now), today's task
  rows with swipe-to-complete, one `PrimaryCta`, comfort zone, time
  breakdown, History/Insights shortcuts.
- **Tasks** (`TodoScreen`) — progress overview built on `MiniSoulRing`,
  priority chips carry an icon **and** a weight, swipe-left completes and
  springs back.
- **Focus** (`PomodoroScreen`) — aqua work ring, sky break ring, `TimerDigits`,
  big pill start/stop, keep-screen-on while running, optional Do Not Disturb
  via `FocusDndManager`.
- **Insights** (`InsightsScreen`) — growth score in a mint→aqua→sky arc with a
  trend delta, seven status bars for the week, concentric comfort-zone rings,
  stacked time bar (wasted `#FF5D6C`), gradient productivity trend.
- **History** (`HistoryScreen`) — seven `MiniSoulRing`s, tap a day → verdict
  card (icon + word + reason), 24h second-by-second timeline bar, that day's
  sessions.
- **Settings** (`SettingsScreen`) — Appearance (AMOLED / Soft dark / Light /
  System, dynamic colour on Android 12+), Day ring thresholds, focus
  permissions, targets, data.

## Navigation
Bottom bar: Home, Timer, Tasks, Focus, Insights — 80dp, `surfaceContainer`
with a hairline, pill indicator in `primary` at 16% alpha, 24dp icons.
Everything secondary hangs off the **Explore** hub.

## Patterns

### Ring geometry
Inner ring = today's completion, needle points at progress, fading tail.
Outer ring = seven week segments, recorded at midnight, today dim + white dot.
Glow = blurred duplicate under the crisp strokes.

### Buttons
Primary pill 56dp gradient; secondary tonal; tertiary outlined with
`outlineVariant`. Press = scale 0.975, 150ms.

### Status communication
`icon + label + colour`, in that order of priority. Legends are spelled out
("All tasks done", "Tasks not met", …) rather than shown as colour dots alone.

### Empty states
Icon disc (64dp, tint at 14%) → title → description → optional action.
Never blank space.

## Motion principles
1. Purposeful — every animation serves a function.
2. Continuous — elements flow between states.
3. Responsive — 150ms touch response.
4. Natural — spring physics, and none at all when the system asks for less
   motion.
