# Fitness App

A gym workout tracker for Android, inspired by FitNotes. Works fully offline: all data stays on your phone.

See [ROADMAP.md](ROADMAP.md) for the plan and progress.

## What it does today (Phase 1)

- **Workout log**: one screen per day, with previous/next day arrows and a calendar
- **Exercise library**: about 70 built-in exercises in 8 categories, search, and your own custom exercises
- **Logging sets**: weight × reps, reps only, distance and time, or time only. Tap a set to edit or delete it
- **Last time** hint and a full **history** per exercise
- **Rest timer** that starts when you save a set, with vibration and a notification
- **Settings**: kg/lb (km/mi), default rest time, light/dark theme

## Try it on your phone

Every push builds a debug APK on GitHub:

1. Open the repository's **Actions** tab and pick the latest green **Android build** run.
2. Download the **fitness-app-debug-apk** artifact and unzip it.
3. Copy the `.apk` to your phone and open it (allow "Install unknown apps" when asked).

## Build it yourself

Open the project in Android Studio, or from a terminal (JDK 17+ and the Android SDK are required):

```sh
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit and database tests
```

## Project layout

```
app/src/main/java/com/kerimkolberg/fitnessapp/
  model/      plain data types, units, and formatting
  data/       repositories, built-in exercises, settings
  data/db/    Room entities, DAOs, and the database
  timer/      rest timer and its alarm
  ui/         Compose screens, one package per screen, plus navigation
```

Stack: Kotlin, Jetpack Compose (Material 3), Room, DataStore, Navigation Compose.
The database is designed so cloud sync can be added later without migrating data (see the roadmap).
