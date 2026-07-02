# TODO

## Done

- Month-grid calendar UI with completed-day highlighting, day-detail cards, total/streak stats.
- Type-safe Compose Navigation (`navigation-compose` + `@Serializable` routes) with per-screen
  ViewModel scoping and back handling.
- On-demand "bonus" workouts (next `sequence` per day, seed folded with the sequence).
- Monotonic-clock interval timer with background-pause, restore-paused after process death,
  and a get-ready countdown.

## Upcoming chunks

- V2 option: revisit exercise demonstrations if desired.
  - Possible approaches: licensed video/WebP assets, Lottie/Rive, or procedural mannequin animations.
  - V1 intentionally uses exercise name + description only.
- Replace the deprecated `BackHandler` with `NavigationEventHandler` once it stabilizes in
  Compose Multiplatform.
- Consider a versioned seed (instead of once-per-process upsert) when the exercise library
  starts shipping updates.
