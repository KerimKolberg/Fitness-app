# KK-Fittracking — Roadmap

A customized Android gym workout tracker inspired by FitNotes, published on Google Play.

## Key decisions

| Decision | Choice | Notes |
|---|---|---|
| Platform | Android only | Distributed via Google Play |
| Language / UI | Kotlin + Jetpack Compose (Material 3) | Native, modern Android toolkit |
| Local database | Room (SQLite) | All data stored on the device |
| Architecture | MVVM + repository layer | UI → ViewModel → Repository → Room |
| Dependency injection | Manual (`AppContainer`) | Simpler than Hilt for one app module; can switch later if the app grows |
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
- [x] Exercise library: built-in exercises + custom exercises, grouped by category (chest, back, legs, shoulders, arms, core, cardio)
- [x] Exercise types: weight × reps, reps only, distance/time (cardio), time only
- [x] Workout logging: add exercises to a day, log sets (weight × reps), edit/delete sets
- [x] Previous-session history shown while logging an exercise
- [x] Calendar view of past workouts
- [x] Per-exercise history screen
- [x] Rest timer with notification/vibration when finished
- [x] Settings: kg/lb, default rest time, theme

### Should-have
- [x] Personal records (PRs) and estimated 1RM, with a marker when a PR is beaten
- [x] Progress graphs per exercise (max weight, estimated 1RM, volume, reps)
- [x] Routines/templates and copying a past workout to today
- [x] Body tracker: bodyweight, body fat %, measurements, with graphs
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
- [ ] Wear OS companion (Galaxy Watch, Pixel Watch): log sets and run the rest timer from the wrist, synced with the phone
- [ ] Health Connect: read daily steps (and optionally heart rate) that Samsung Health, Google Fit, Fitbit and watches already record; write finished workouts back so they show up in those apps
- [ ] Cloud sync + accounts (see "Keeping the door open for cloud sync")

## Data model

Category, Exercise, Workout, WorkoutExercise and Set exist since Phase 1; Routine, RoutineExercise and BodyMeasurement since Phase 2 (database version 2); exercise tempo/perSide and set RPE since Phase 2b (version 3); drop sets and supersets since Phase 2c (version 4) (`app/src/main/java/.../data/db/Entities.kt`); the rest are planned. All tables use `id: UUID`, `createdAt`, `updatedAt`, `deletedAt?`.

- **Category**: name, color, sortOrder
- **Exercise**: name, categoryId, type (WEIGHT_REPS / REPS / DISTANCE_TIME / TIME), notes, isCustom
- **Workout**: date, comment
- **WorkoutExercise**: workoutId, exerciseId, sortOrder
- **Set**: workoutExerciseId, sortOrder, weightKg?, reps?, distanceMeters?, durationSeconds?, comment (isWarmup to come later)
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
- [x] Android project setup (Gradle, Compose, Room, CI build)
- [x] Database + repositories + seed data for built-in exercises
- [x] Exercise library screens
- [x] Workout logging screens
- [x] Calendar + history
- [x] Rest timer (runs while the app process is alive; a foreground service can make it bulletproof later)
- [x] Settings
- [ ] Try it on a real phone and collect fixes
- **Goal:** installable APK, usable for personal training

### Phase 2: Insights
- [x] PRs and estimated 1RM (Epley), with a star on record-setting sets and rep maxes
- [x] Progress graphs (per exercise, plus body measurements)
- [x] Routines, adding a routine to a day, and copying a day's exercises to today
- [x] Body tracker (bodyweight, body fat, 9 circumference measurements)
- [ ] Try it on a real phone and collect fixes
- **Goal:** feature parity with FitNotes' core

### Phase 2b: Exercise variety
Broaden the library beyond gym lifts. Each group needs the right way to log it:

| Group | Examples | How a set is logged |
|---|---|---|
| Mobility | hip CARs, thoracic rotations, ankle mobility | reps, or time; per side |
| Stretching | hamstring, hip flexor, couch stretch | time held; per side |
| Isometrics | wall sit, Copenhagen plank, split-squat hold, mid-thigh pull | time held + optional weight |
| Slow eccentrics (tendons) | Nordic curls, eccentric heel drops, decline squats, Tyler twist | weight × reps + tempo (e.g. 5 s down) |
| Plyometrics | box jumps, broad jumps, pogo hops, depth jumps | reps + optional height/distance |
| Sports | tennis, table tennis, volleyball, padel, badminton, football, basketball | session time + intensity (RPE) + notes |

- [x] New exercise types: "time + weight" for loaded holds, "reps + height/distance" for jumps, "session" for sports
- [x] Optional tempo per exercise (e.g. 5-0-1-0) and "each side" flag
- [x] Session intensity (RPE 1–10) and notes, for sports and conditioning
- [x] New categories: Mobility, Stretching, Isometrics, Tendons & eccentrics, Plyometrics, Sports
- [x] Built-in exercises for each category (50 new)
- [x] Plans (formerly "routines"): one exercise can be in many plans, "Add to plans" on every exercise, plan filter in the exercise picker, starter plans (Push, Pull, Legs, Upper body, Tendon health, Mobility flow)

### Gamification
- [x] XP for sets, workouts, personal records, new exercises and weekly goals; levels
- [x] Weekly workout goal and week streaks
- [x] 14 achievements, including variety ones for mobility, tendons, plyometrics and sports
- [x] Celebrations after saving a set (record, achievement, level up, weekly goal)
- [ ] Ideas for later: challenges (e.g. "30 days of mobility"), yearly summary, share cards

### Phase 2c: Training techniques
- [x] Drop sets: a "Drop set" option when logging, each drop lighter by a set percentage (Settings, default 20%, rounded to 0.5 kg / 1 lb); shown as ↘ under the set they continue
- [x] Supersets and circuits of 2 to 6 exercises on a workout day: pick them in order, log round by round (the app opens the next exercise after each set), rest timer only after the last exercise of a round, ungroup any time
- [ ] Supersets saved inside plans

### Phase 3: Data safety
- [x] Backup to a JSON file anywhere the file picker reaches (Drive, Downloads…), with "last backup" shown in Settings
- [x] Restore with a summary and confirmation; all-or-nothing, so a damaged file changes nothing
- [x] CSV export of workouts and body measurements, in the user's units
- [x] Database migration tests: real v1 and v2 databases upgraded to the current version with no data lost
- **Goal:** safe for other users' data

### Phase 4: Google Play release
- [ ] App name, icon, screenshots, store listing
- [ ] Move UI text into `strings.xml` (needed for translations)
- [ ] Privacy policy (simple, since data stays on device)
- [ ] Signed release build (AAB), Play App Signing
- [ ] Internal testing → closed testing → production
- **Goal:** published app

### Phase 5: Customization
- [ ] Collect missing features from real-world use
- [ ] Prioritize and implement them as v1.x updates
