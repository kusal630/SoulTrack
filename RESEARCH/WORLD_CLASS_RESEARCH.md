# World-Class Productivity App Research

## Top Apps Analyzed
- Todoist, TickTick, Forest, Toggl, Notion, Structured, Things 3, Fantastical, Amie, Sunsama

---

## 1. Typography

### Todoist
- **Display**: Graphik 600 at 55px, tracking -0.55px
- **Body**: Inter at 475 weight, 12-18px
- **Warm editorial feel** - paper notebook, not SaaS

### Notion
- **Style**: Warm editorial-minimal
- **Canvas**: Paper-white background
- **Ink**: Warm-black (#25221e style)
- **Headlines**: Huge tight headlines

### Recommendation for SoulTrack
- **Primary**: Inter (Regular 400, Medium 500, SemiBold 600, Bold 700)
- **Display/Timer**: Space Grotesk or Outfit (geometric, modern)
- **Type Scale**:
  - Display: 56sp, weight 300, letter-spacing -0.5sp
  - Headline: 32sp, weight 600
  - Title: 24sp, weight 600
  - Body: 16sp, weight 400
  - Label: 12sp, weight 500

---

## 2. Color Palettes

### Todoist
- **Todoist Red**: #e34432 (CTA, active states)
- **Faded Charcoal**: #25221e (primary text, headings)
- **Paper White**: #fefdfc (backgrounds)
- **Soft Gray**: #d7d6d4 (borders, inactive)
- **Subtle Ash**: #6f6c69 (secondary text)
- **Dusty Sage**: #94928f (tertiary)
- **Light Peach**: #fff6f0 (accent backgrounds)

### Notion
- **Ink Strong**: rgba(0,0,0,.95) - headings
- **Ink**: rgba(0,0,0,.898) - body text
- **Ink Muted**: rgba(0,0,0,.54) - secondary
- **Warm Gray**: #615d59 - captions

### Premium App Colors (from Dribbble research)
- **Primary Green**: #10B981 (productive/success)
- **Danger Red**: #EF4444 (wasted/alert)
- **Amber**: #F59E0B (exercise/warning)
- **Blue**: #3B82F6 (learning/info)
- **Purple**: #8B5CF6 (social)
- **Indigo**: #6366F1 (sleep/calm)

---

## 3. Features

### Must-Have (All Top Apps)
1. **Task Management** - Todo/subtask with priorities, due dates, tags
2. **Timer/Tracking** - Real-time timer + manual entry
3. **Calendar View** - Monthly/weekly/daily/agenda
4. **Habit Tracking** - Daily check-in with streaks
5. **Pomodoro Timer** - Focus sessions with breaks
6. **Dark Mode** - Light/Dark/System + AMOLED
7. **Widgets** - Home screen quick actions
8. **Notifications** - Reminders, streaks, deadlines
9. **Statistics/Insights** - Charts, trends, scores
10. **Sync** - Cross-device synchronization

### Premium Features (Differentiators)
1. **Smart Suggestions** - AI-based time recommendations
2. **Focus Mode** - Block distractions, ambient sounds
3. **Weekly Reports** - Beautiful summary cards
4. **Goal Tracking** - Targets with progress visualization
5. **Tags/Labels** - Flexible categorization
6. **Export** - CSV, JSON, image sharing
7. **Themes** - Accent color customization
8. **Gestures** - Swipe to complete, long-press actions

---

## 4. UI/UX Patterns

### Layout
- **Card-based** - Glass morphism for premium feel
- **Bottom navigation** - 4-5 items max
- **FAB** - Quick action for primary task
- **Pull-to-refresh** - Standard gesture

### Animations
- **Spring animations** - Duration 300-500ms, damping 0.6-0.8
- **Fade transitions** - 200-300ms for screen changes
- **Staggered loading** - Cards animate in sequence 50-100ms delay
- **Micro-interactions** - Button press scale 0.95, haptic feedback
- **Progress rings** - Smooth gradient animations

### Cards
- **Glass morphism** - Blur + alpha backgrounds
- **Rounded corners** - 16-24dp radius
- **Subtle shadows** - Elevation 2-4dp
- **Border** - 1px subtle divider

---

## 5. Splash/Onboarding

### Best Practices
- **Splash**: Animated logo (scale + fade), 1-1.5s, gradient background
- **Onboarding**: 3-4 pages, illustrations, skip/next buttons
- **Permission requests**: At relevant moments, not all at once
- **First experience**: Show value immediately, minimal setup

---

## 6. Monetization

### Pricing Tiers (Industry Standard)
| Tier | Price | Features |
|------|-------|----------|
| Free | $0 | Basic tracking, limited features |
| Pro | $3-5/mo | All features, themes, export |
| Premium | $8-12/mo | AI insights, priority support |
| Team | $10-20/user | Collaboration, admin |

### TickTick Model
- Free: Core features
- Premium: $35.99/year ($3/mo) - Calendar, themes, stats

### Toggl Model
- Free: 5 users
- Starter: $9/user/mo
- Premium: $16/user/mo

---

## 7. Specific Recommendations for SoulTrack

### Typography
- Download Inter TTF files (Regular, Medium, SemiBold, Bold)
- Place in `app/src/main/res/font/`
- Create `Type.kt` with Material3 Typography

### Colors
- Use Material You dynamic colors as base
- Add semantic category colors
- Support Light/Dark/AMOLED themes

### Animations
- Timer ring: AnimatedGradientBrush + pulse
- Cards: Staggered entrance animations
- Buttons: Spring press animation (scale 0.95)
- Screens: Shared element transitions

### Features to Add
1. Habit Tracker (streaks, calendar)
2. Goals & Targets (progress bars)
3. Weekly Report (beautiful summary)
4. Themes (Light/Dark/AMOLED/Accent)
5. Notifications (reminders, alerts)
6. Data Export (CSV, JSON, image)
7. Quick Actions (one-tap start)
8. Focus Mode (ambient sounds)

---

## Sources
- Todoist Design System (shadcn.io)
- Notion Design Spec (tastekit.dev)
- TickTick Features & Pricing
- Toggl Track Pricing 2026
- Dribbble Productivity App UI
- Medium UI/UX Case Studies
