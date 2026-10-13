# Technical Decisions

## Dependency Versions

| Dependency | Version | Rationale |
|---|---|---|
| Kotlin | 2.0.21 | Stable, Compose compiler compatible |
| AGP | 8.8.0 | Stable, compatible with JDK 21 |
| Compose BOM | 2024.12.01 | Latest stable Compose |
| Room | 2.6.1 | Stable with KSP support |
| WorkManager | 2.10.0 | Latest stable |
| Navigation Compose | 2.8.5 | Latest stable |
| DataStore | 1.1.1 | Latest stable |
| KSP | 2.0.21-1.0.28 | Kotlin compatible |
| Coroutines | 1.9.0 | Latest stable |

## Architecture Decisions

### Single Module
Chosen for F-Droid simplicity and easier maintenance. The app uses a clear package structure within a single module.

### Room Database
Chosen over DataStore for complex relational data (sessions, food entries, etc.). DataStore used only for simple preferences.

### Foreground Service for Timer
Ensures timer survives background and process death. Database timestamps used as source of truth.

### ML: on-device, small, local-only
v1/v2 deferred ML to keep the app F-Droid friendly and avoid large binary
dependencies; heuristic engines provided similar functionality. v3 reopens ML
**only** for the intelligent task recommendation layer: small tabular models
(mood predictor, task-context performance) trained and inferred entirely on
device from the local Room database. No Play Services, no Firebase, no cloud
inference, no large bundled binaries. Heuristics remain the fallback so the
feature works with zero training history. See `MODEL_CARDS.md`.

## Algorithm Decisions

### Growth Score
Weighted component system (0-100) with configurable weights. Transparent calculation with visible components.

### Loop Detection
Cosine similarity between daily routine vectors. Threshold-based with growth/novelty context to reduce false positives.

### Nutrition Quality
Weighted scoring of protein, fiber, added sugar, sodium, and micronutrients. Simple but transparent.

### Sleep Debt
Accumulated deficit over 7 days. Simple and understandable.

## Privacy Decisions

### No Cloud by Default
All data local. Optional features (barcode, online lookup) disabled by default with explicit consent.

### Medical Language
All health indicators use educational, non-diagnostic language. Persistent symptoms always point to healthcare professionals.

### Sensitive Data Opt-In
Weight, calories, body metrics hidden by default. User controls visibility.
