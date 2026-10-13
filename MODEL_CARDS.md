# Model Cards

## Current Status

No trained ML models are shipped. The recommendation layer ships as
**deterministic, on-device scoring** (`engine/BrainLayer`, `engine/FocusabilityEngine`,
`engine/BehaviorProfile`) with the same inputs and outputs the future models
will use, so nothing has to be re-designed when a learned model replaces a
heuristic. All signals below are live in v2.2.0.

## Rules for every model we add

1. On-device only (no cloud inference, no telemetry)
2. Permissive open-source license (Apache 2.0 / MIT), F-Droid compatible
3. Documented model card here, with training data, inputs, limits and failure modes
4. Resettable and retrainable from Settings
5. Shown as a **suggestion with a reason**, never as a command
6. Not used for medical diagnosis, not face recognition, not biometric identification
7. **Must work out of the box** — cold-start behaviour with zero history has to
   be correct and useful, not random

## Signals (implemented)

### 1. Mood signal
- **Task:** predict the user's mood/energy for a given moment, from their own
  check-in history.
- **Inputs:** hour of day, day of week, recent sleep and recovery minutes,
  prior task mix that day, recent mood/energy/stress readings.
- **Target:** `MoodCheckInEntity.mood` / `.energy` / `.stress` (1–5).
- **Implemented as:** the latest `MoodCheckInEntity` in the last 24 hours
  (mood, energy) is read straight into `FocusContext`; no model is trained.
- **Planned replacement:** small on-device regressor trained incrementally.
- **Limits:** absent check-in → mood is simply `null` and the scorer ignores
  that axis. Degradation is graceful, individual-only, never shared.
- **Failure mode:** low confidence → recommender leans on priority +
  focusability only.

### 2. Task Context Performance Model
- **Task:** for each task, learn when it actually gets done well.
- **Inputs:** task id, category, priority, hour of day, day of week, recent
  completion history, elapsed pomodoro success.
- **Target:** completion within the recommended window / time-to-complete.
- **Implemented as:** `BehaviorProfiler.build()` over a 45-day window →
  `BehaviorProfile` with per-hour completion affinity shrunk toward 0.5 based
  on sample size, an overall completion rate and the typical working hours.
- **Failure mode:** zero history → `BehaviorProfile.coldStart()` (flat priors),
  so cold start is neutral rather than random. Unseen task → category prior;
  unseen category → global prior.

### 3. Focusability / Environment Estimator
- **Task:** estimate how focusable the current moment is (0–100).
- **Inputs:** optional ambient-noise estimate (only if mic permission granted
  and explicitly enabled), time of day, day of week, time since last break,
  minutes into the running session, mood/energy.
- **Implemented as:** `FocusabilityEngine.evaluate()` — deterministic,
  additive, clamped to 0–100, returns `HIGH`/`MEDIUM`/`LOW` plus reason
  strings. Circadian curve drives the score; break length, noise, mood and
  energy adjust it.
- **Failure mode:** no microphone permission or the toggle is off → quietness
  is `null` and the feature falls back to the pure time-based heuristic.
  Every band still returns at least one reason string.

## Recommended-Task Decision Layer (not itself a model)
Combines the three signals plus `TodoEntity.priority` and deadline proximity
into a ranked "do this now" suggestion with a one-line reason.

**Implemented as:** `BrainLayer.recommend(BrainInputs)`. Score =
`urgency*0.30 + priority*0.25 + focusFit*0.20 + behavior*0.10 + history*0.10 +
effort*0.05`. Hard overrides: a deadline inside 24h can never score below its
urgency value, and a high-priority task due inside 24h is pinned at ≥95, so
the ranking can never bury it. Output is `Recommendation(task, score, reason,
deepFocus)` plus up to two alternatives, rendered by
`ui/brain/RecommendationCard`.

Covered by `BrainLayerTest` (urgency/priority dominance, focus band fitting,
cold-start behaviour, reason presence) and scored against a naive
"sort by priority" baseline; must beat it.

## Governance

- Runs entirely on device; no network, no telemetry.
- Every recommendation carries a human-readable reason; it is a suggestion,
  never a command.
- Switchable off in Settings → Focus & Permissions.
- Not used for medical diagnosis.

## Previously Planned (still optional)

### Activity Suggestion
- Suggest likely category based on time and history
- User confirms suggestion
- Model: Simple classification

### Routine Anomaly Detection
- Detect unusual deviations from normal routine
- Show as suggestion, not fact
- Model: Statistical anomaly detection

### Food Photo Classification
- Only with permissively licensed model
- Result is suggestion, not guarantee
- Model: ImageNet-trained classifier (if bundled)
