# UI Sweep Brief — modern AMOLED Material 3

Project: `/home/kusal/Documents/projects_opencode/TIme-tracker`
Module: `app`, package `io.github.dailytrack`, Kotlin + Jetpack Compose + Material 3.

This brief is the single source of truth for the current design system. Read it
before touching any screen.

## 1. What already exists (use it, do not reinvent it)

### Theme — `ui/theme/Theme.kt`
- Dark mode is **true-black AMOLED**: `background`/`surface` = `#000000`.
  Never write `Color.Black` or `#000000` yourself for a surface — always
  `MaterialTheme.colorScheme.background`.
- Layered surfaces replace shadows. In order of depth:
  `surfaceContainerLowest` (#000) → `surfaceContainerLow` (#0A0A0C) →
  `surfaceContainer` (#101013) → `surfaceContainerHigh` (#17171B) →
  `surfaceContainerHighest` (#1F1F24).
- `surfaceVariant` (#17171C) for recessed fields.
- `outlineVariant` for hairlines, `onSurfaceVariant` for secondary text.
- Brand: `primary` = crimson `#E94560` dark / `#C23152` light.
  Accent: `secondary` indigo, `tertiary` mint.
- Shapes (`MaterialTheme.shapes`): `extraSmall` 6dp, `small` 10dp,
  `medium` 16dp, `large` 20dp, `extraLarge` 28dp. **Cards use `large` (20dp).**
- Category colors (unchanged): `ProductiveColor`, `WastedColor`,
  `ExerciseColor`, `LearningColor`, `SocialColor`, `SleepColor`,
  `RecoveryColor`, `NeutralColor`, plus `*Gradient` lists.
- Gradients for hero/CTA: `GradientStart/Mid/End`, `ProductiveGradient`, etc.

### Spacing — `ui/theme/Dimens.kt`
`Dimens.ScreenPadding` 16dp, `SectionGap` 24dp, `CardPadding` 16dp,
`CardGap` 12dp, `TitleGap` 8dp, `MinTouchTarget` 48dp, `ButtonHeight` 52dp.
`Motion.Micro/Standard/Slow` = 150/260/420 ms.

### Component kit — `ui/components/`
`ScreenScaffold.kt`:
```kotlin
ScreenScaffold(
    title: String, subtitle: String? = null,
    navController: NavController? = null,   // gives you the back arrow
    onBack: (() -> Unit)? = null,
    compact: Boolean = false,               // true = 64dp bar, false = collapsing large bar
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
)

ScreenList(...same params..., content: LazyListScope.() -> Unit)
// ScreenList already applies: fillMaxSize + padding(padding) +
// horizontal 16dp + Arrangement.spacedBy(12.dp) + vertical contentPadding 8dp.
```
Prefer `ScreenList` for anything that scrolls.

`ModernCards.kt`:
| Composable | Use for |
|---|---|
| `SurfaceCard(modifier, shape, containerColor, border, contentPadding, onClick, content)` | every block of content; 20dp radius, tonal fill, 1dp hairline, no elevation |
| `PressableCard(shape, containerColor, onClick, content)` | tappable card with 0.975 press scale |
| `ScreenSection(title, subtitle, actionLabel, onAction, leadingIcon, tint, content)` | titled card with optional header action |
| `SectionTitle(title, subtitle, trailing)` | heading above a group of cards (no container) |
| `StatTile(label, value, icon, tint, caption, progress)` | metric block (icon disc, big number, label, optional progress bar) |
| `KeyValueRow(label, value, valueColor)` | label … value line inside a section |
| `PillChip(label, selected, onClick, tint)` | filter / selector chip |
| `PrimaryCta(text, onClick, enabled, icon, gradient)` | full-width pill primary button, 52dp |
| `TonalCta(text, onClick, icon, enabled)` | full-width pill tonal button |
| `GhostCta(text, onClick, icon)` | outlined pill button |
| `EmptyState(title, message, icon, tint, actionLabel, onAction)` | **every** empty list |
| `Hairline()` | 1dp separator that reads on pure black |
| `IconRow(icon, title, subtitle, tint, trailing)` | icon + 2 lines row |

Also available from `CommonComponents.kt`: `TimeBreakdownPieChart`, `LegendItem`,
`CategoryDropdown`, `NotesField`, `SectionCard`, `SectionHeader`, `MedicalDisclaimerCard`.

## 2. Rules

1. **Never hand-roll a `TopAppBar`/`LargeTopAppBar`/`Scaffold` header.** Use
   `ScreenList` or `ScreenScaffold`. Exception: screens that own an immersive
   custom chrome (Timer, Pomodoro, Onboarding, Focus mode) — restyle those in
   place but keep the same structure.
2. **No shadows, no `elevation`, no `Color.LightGray`, no `Color.White`
   surfaces.** Depth = surface tonal step + 1dp `outlineVariant` hairline.
3. **No hardcoded hex for surfaces or text.** Only semantic
   `MaterialTheme.colorScheme.*` tokens, or the named category colors.
4. **Cards:** `MaterialTheme.shapes.large` (20dp) radius, `Dimens.CardPadding`
   (16dp) inner padding, `surfaceContainer` fill.
5. **Typography:** always `MaterialTheme.typography.*`. Raw `fontSize` only for
   hero numerals (timer digits, score rings). Sentence case headings.
6. **Every list needs an `EmptyState`** with icon + title + description +
   action. No blank screens, no "..." placeholders.
7. **Touch targets ≥ 48dp** for icon-only actions (`IconButton` already is).
8. **Buttons:** `PrimaryCta` for the one primary action per screen,
   `TonalCta`/`GhostCta` for the rest. Full-width pill shape.
9. **Charts:** never `Color.Gray` tracks. Use
   `MaterialTheme.colorScheme.surfaceContainerHighest` as track, category
   colors as fill, and always print the number next to the label.
10. **Do not change any public composable signature** — `ui/Navigation.kt`,
    `ui/viewmodel/MainViewModel.kt` and other screens call them.
11. **Do not edit files outside your assigned list.** If you need something
    from the shared kit that does not exist, define a private composable
    *inside your own file* using the same tokens.
12. **Presentation only.** Do not change data/business logic. You may wire a
    button to an existing callback that is currently `() -> {}` only if the
    callback already exists in the signature.
13. Keep the Apache 2.0 header at the top of every file you rewrite.

## 3. The look, in one paragraph

Pure-black canvas, content sitting on barely-there charcoal cards with 20dp
corners and a hairline edge, crimson accents used sparingly for the one action
that matters, generous 16/12dp rhythm, large sentence-case titles with a quiet
grey subtitle, pill-shaped buttons and chips, and a friendly icon-disc empty
state where data is missing. Light mode gets the same structure on warm white.

## 4. Verify

`./gradlew compileFdroidDebugKotlin 2>&1 | grep -E '^e: '` must be empty for
your files when you finish. Other agents may be editing other files at the same
time, so **filter compiler output to only the paths you own** and ignore
everything else.
