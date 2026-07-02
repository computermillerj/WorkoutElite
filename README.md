# WorkoutElite

Android-first Kotlin Multiplatform-ready workout app for local-only bodyweight workouts.

## Current implementation chunk

- Android-only Compose app shell.
- KMP-clean `commonMain` domain/data/presentation boundaries.
- Room database scaffold with `exercises`, preferences, workouts, items, active session, difficulty profile, and completions.
- Seeded starter exercise library.
- Daily workout generator with deterministic RNG, duration window, rest metadata, and frequency exclusion.
- Koin dependency injection.
- Today screen showing the generated daily workout summary.
- Active workout timer with pause/resume, skip, quit confirmation, switch-side cues, haptics, keep-screen-on, and Room-backed restore state.
- Completion feedback with Easy/Medium/Hard rolling difficulty updates.
- Calendar/history screen showing completed days, feedback, duration, and exercises used.
- Attributions screen for any future licensed assets.
- Exercise search, category filters, and jump-rope equipment badges.

## Prerequisites

- JDK 21 available at `/usr/lib/jvm/java-21-openjdk-amd64`, or update `gradle.properties`.
- Android SDK installed; local path is in `local.properties`.

## Commands

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:testDebugUnitTest
```

## Architecture

See `docs/ARCHITECTURE_PLAN.md`.

Dependency direction:

```text
presentation -> domain <- data
androidMain -> commonMain via expect/actual seams
```

V1 ships Android only. iOS targets are intentionally not enabled yet, but platform-specific APIs are isolated behind expect/actual or androidMain code so iOS can be added later.

## Next chunks

1. Move from lightweight typed route switching to Navigation Compose if deep links/back stack become necessary.
2. V2 option: add exercise demonstrations if desired; V1 uses exercise name + description only.
3. Add more tests around completion persistence, difficulty updates, and workout balancing.
