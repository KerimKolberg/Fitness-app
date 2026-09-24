# Fitness App — Roadmap

A customized Android gym workout tracker inspired by FitNotes, published on Google Play.

## Key decisions

| Decision | Choice | Notes |
|---|---|---|
| Platform | Android only | Distributed via Google Play |
| Language / UI | Kotlin + Jetpack Compose (Material 3) | Native, modern Android toolkit |
| Local database | Room (SQLite) | All data stored on the device |
| Architecture | MVVM + repository layer | UI → ViewModel → Repository → Room |
| Dependency injection | Hilt | |
| Connectivity | Offline-only for v1 | Designed so cloud sync can be added later (see below) |
| Units | kg / lb, user-selectable | Store weights in kg internally, convert for display |

### Keeping the door open for cloud sync

v1 is fully offline, but the data layer follows these rules so sync/accounts can be added later without a painful migration:

1. **UUID primary keys** (not auto-increment integers), so records created on different devices never collide.
2. **`createdAt` / `updatedAt` timestamps** on every synced table, for change tracking and conflict resolution.
3. **Soft deletes** via a nullable `deletedAt` column, so deletions can be propagated to other devices.
4. **Repository layer**: screens never access Room directly. A remote data source can later be added behind the repositories.
5. **Room migrations** are written for every schema change (no destructive migrations once released).

## Feature list

### Must-have (MVP)
- [ ] Exercise library: built-in exercises + custom exercises, grouped by category (chest, back, legs, shoulders, arms, core, cardio)
- [ ] Exercise types: weight × reps, reps only, distance/time (cardio), time only
- [ ] Workout logging: add exercises to a day, log sets (weight × reps), edit/delete sets
- [ ] Previous-session history shown while logging an exercise
- [ ] Calendar view of past workouts
- [ ] Per-exercise history screen
- [ ] Rest timer with notification/vibration when finished
- [ ] Settings: kg/lb, default rest time, theme

### Should-have
- [ ] Personal records (PRs) and estimated 1RM, with a marker when a PR is beaten
- [ ] Progress graphs per exercise (max weight, estimated 1RM, volume, reps)
- [ ] Routines/templates and copying a past workout to today
- [ ] Body tracker: bodyweight, body fat %, measurements, with graphs
- [ ] Backup/restore to a local file / Google Drive (via the Android file picker)
- [ ] CSV export
- [ ] Set and workout comments
- [ ] Plate calculator

### Nice-to-have / custom (Phase 5)
- [ ] Features found missing after real use, to be added here
- [ ] Supersets / circuits
- [ ] RPE / RIR per set
- [ ] Progression suggestions
- [ ] Home-screen widget
- [ ] Wear OS companion
- [ ] Cloud sync + accounts (see "Keeping the door open for cloud sync")

## Data model (initial draft)

All tables use `id: UUID`, `createdAt`, `updatedAt`, `deletedAt?`.

- **Category**: name, color, sortOrder
- **Exercise**: name, categoryId, type (WEIGHT_REPS / REPS / DISTANCE_TIME / TIME), notes, isCustom
- **Workout**: date, comment
- **WorkoutExercise**: workoutId, exerciseId, sortOrder
- **Set**: workoutExerciseId, sortOrder, weightKg?, reps?, distanceM?, durationSec?, isWarmup, comment
- **Routine**: name, notes
- **RoutineExercise**: routineId, exerciseId, sortOrder, targetSets?, targetReps?
- **BodyMeasurement**: date, type (WEIGHT / BODY_FAT / custom), value, unit

## Phases

### Phase 0: Planning
- [x] Choose platform and stack
- [x] Define feature list and priorities
- [x] Draft data model
- [ ] Sketch main screens (Today/log, Exercise picker, Exercise log, Calendar, History, Settings)

### Phase 1: MVP
- [ ] Android project setup (Gradle, Compose, Room, Hilt, CI build)
- [ ] Database + repositories + seed data for built-in exercises
- [ ] Exercise library screens
- [ ] Workout logging screens
- [ ] Calendar + history
- [ ] Rest timer
- [ ] Settings
- **Goal:** installable APK, usable for personal training

### Phase 2: Insights
- [ ] PRs and estimated 1RM
- [ ] Progress graphs
- [ ] Routines/templates
- [ ] Body tracker
- **Goal:** feature parity with FitNotes' core

### Phase 3: Data safety
- [ ] Backup/restore
- [ ] CSV export
- [ ] Database migration tests
- **Goal:** safe for other users' data

### Phase 4: Google Play release
- [ ] App name, icon, screenshots, store listing
- [ ] Privacy policy (simple, since data stays on device)
- [ ] Signed release build (AAB), Play App Signing
- [ ] Internal testing → closed testing → production
- **Goal:** published app

### Phase 5: Customization
- [ ] Collect missing features from real-world use
- [ ] Prioritize and implement them as v1.x updates
