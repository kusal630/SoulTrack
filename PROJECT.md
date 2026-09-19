# Project

Idea: DailyTrack is an offline-first, privacy-first life tracking Android app. The goal is to make it the best-in-class time tracker with world-class UI, animations, and user experience.

Platform: Android (Kotlin, Jetpack Compose, Material 3)

# Features (v2 — Must Build for Best-in-Class)

## 1. Timer UI Overhaul (Critical)
- Circular animated timer with pulsing glow effect when running
- Smooth start/stop animations with spring physics
- Category color bleed into entire screen background
- Elapsed time with monospace font and subtle animation on digit change
- Quick-action chips for common categories
- Swipeable session history mini-timeline below timer
- Haptic feedback on start/stop

## 2. Pomodoro Screen Premium Redesign
- Full-screen immersive experience during active session
- Circular progress ring with gradient stroke and animated glow
- Ambient background that shifts color as time progresses
- Session counter with animated ring segments
- Break screen with calming animation (breathing circle)
- Long break after 4 consecutive sessions
- Sound toggle with visual indicator

## 3. Quote System Anti-Repetition
- Smart rotation: never repeat within 24h, track shown quotes
- Context-aware: morning/afternoon/evening quotes
- Category-based quotes tied to activity type
- Animated quote transitions (fade + slide)
- User can add custom quotes
- Widget: quote changes on tap with smooth crossfade

## 4. Dashboard Visual Hierarchy
- Hero section with animated growth score ring
- Time breakdown with animated bar chart
- Streak counter with fire animation
- Quick stats row with animated counters
- Today timeline view (horizontal scroll)

## 5. Design System
- 4dp grid spacing system
- Typography scale: Display to Label
- Semantic color system (productive=green, wasted=red, exercise=orange)
- Animation standards: micro 150ms, standard 300ms, slow 500ms
- Motion patterns: shared element transitions

## 6. Polish and Micro-interactions
- Button press animations (scale)
- Card hover states with elevation
- Loading skeletons instead of spinners
- Empty states with illustrations
- Success celebrations

# Done criteria (loop stops when ALL true)
- Timer screen has premium circular design with animations
- Pomodoro screen is immersive with ambient color shifts
- Quote system never repeats within 24h and is context-aware
- Dashboard has animated visual hierarchy
- DESIGN.md system applied to every screen
- Build passes with no critical bugs
- All screens feel premium and cohesive

# Loop instruction
Each run: read RESEARCH/*.md, pick highest-priority work, build it.
