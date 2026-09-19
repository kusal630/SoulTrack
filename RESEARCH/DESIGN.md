# DESIGN.md — DailyTrack Design System

## Color Palette

### Primary (Brand)
- Primary: #E94560 (Crimson) — CTAs, active states, timer glow
- Primary Container: #C23152 — Cards, elevated surfaces

### Semantic Colors
- Productive/Learning: #2E7D32 (Green)
- Exercise: #E65100 (Orange)
- Wasted: #C62828 (Red)
- Social: #6A1B9A (Purple)
- Sleep: #311B92 (Indigo)
- Recovery: #4CAF50 (Light Green)

### Background System
- Surface: #12121A (Dark mode base)
- Surface Variant: #1A1A25 (Cards)
- Background: #0A0A0F (Deepest)

### Text
- On Surface: #E3E2E6 (Primary text)
- On Surface Variant: #C7C5CA (Secondary text)
- On Primary Container: #FFD9E0

## Typography Scale
- Display: 57sp — Timer digits, hero numbers
- Headline: 32sp — Section headers
- Title Large: 22sp — Card titles
- Title Medium: 16sp — Subsection headers
- Body Large: 16sp — Primary body text
- Body Medium: 14sp — Secondary body text
- Body Small: 12sp — Captions, timestamps
- Label Large: 14sp — Buttons
- Label Medium: 12sp — Chips, tags
- Label Small: 11sp — Badges

## Spacing (4dp grid)
- xs: 4dp
- sm: 8dp
- md: 12dp
- lg: 16dp
- xl: 24dp
- xxl: 32dp
- xxxl: 48dp

## Elevation
- Level 0: 0dp (flat)
- Level 1: 1dp (cards at rest)
- Level 2: 3dp (cards pressed)
- Level 3: 6dp (floating elements)
- Level 4: 8dp (navigation)
- Level 5: 12dp (dialogs)

## Animation Timing
- Micro (press feedback): 100-150ms
- Standard (transitions): 200-300ms
- Slow (page transitions): 400-500ms
- Extended (celebrations): 800-1200ms

## Easing
- Standard: FastOutSlowInEasing
- Decelerate: DecelerateEasing
- Sharp: FastOutLinearInEasing

## Component Patterns

### Timer Circle
- Outer ring: 200dp diameter, 12dp stroke
- Inner content: elapsed time in Display font
- Glow effect: 8dp blur, primary color, pulsing alpha
- Progress arc: gradient stroke (primary to secondary)

### Cards
- Corner radius: 12dp
- Padding: 16dp
- Elevation: Level 1 (rest), Level 2 (pressed)
- Surface tint: primary with 8% alpha

### Buttons
- Height: 56dp (primary), 40dp (secondary)
- Corner radius: 12dp (primary), 20dp (chip)
- Press animation: scale 0.96 with 150ms spring

### Navigation
- Bottom bar height: 80dp
- Icon size: 24dp
- Active indicator: pill shape, primary container color

## Motion Principles
1. Purposeful — every animation serves a function
2. Continuous — elements flow between states
3. Responsive — 100ms touch response
4. Natural — spring physics for organic feel
