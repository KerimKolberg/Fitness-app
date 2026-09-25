package com.kkfittracking.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** One logged set with what the game needs to know about its exercise. */
data class LoggedSet(
    val id: String,
    val date: LocalDate,
    val exerciseId: String,
    val categoryId: String,
    val type: ExerciseType,
    /** How the exercise trains, for the variety achievements. */
    val style: TrainingStyle,
    val values: SetValues,
)

/** Achievements are computed from the workout log, so they need no storage of their own. */
enum class Achievement(val emoji: String, val title: String, val description: String, val target: Int) {
    FIRST_WORKOUT("👟", "First step", "Log your first workout", 1),
    WORKOUTS_10("💪", "Regular", "Log 10 workouts", 10),
    WORKOUTS_50("🏋️", "Dedicated", "Log 50 workouts", 50),
    WORKOUTS_100("🏛️", "Centurion", "Log 100 workouts", 100),
    FIRST_RECORD("⭐", "Personal best", "Set your first personal record", 1),
    RECORDS_25("🏆", "Record breaker", "Set 25 personal records", 25),
    STREAK_4("🔥", "On a roll", "Reach your weekly goal 4 weeks in a row", 4),
    STREAK_12("🌋", "Unstoppable", "Reach your weekly goal 12 weeks in a row", 12),
    TON_DAY("🦍", "Heavy day", "Lift 10,000 kg (weight × reps) in one day", 10_000),
    EXPLORER("🧭", "Explorer", "Train exercises from 6 different sections", 6),
    MOBILITY_10("🧘", "Supple", "Do mobility or stretching on 10 days", 10),
    TENDONS_10("🛡️", "Bulletproof", "Do isometrics or slow eccentrics on 10 days", 10),
    PLYOMETRICS_10("🦘", "Springy", "Do plyometrics on 10 days", 10),
    SPORTS_10("🎾", "Game on", "Play sports on 10 days", 10),
    HIIT_10("⚡", "Interval hero", "Do HIIT or intervals on 10 days", 10),
}

data class AchievementStatus(
    val achievement: Achievement,
    /** How far along it is, capped at the target. */
    val progress: Int,
    val unlockedOn: LocalDate?,
) {
    val isUnlocked: Boolean get() = unlockedOn != null
}

data class GameStats(
    val xp: Int = 0,
    val level: Int = 1,
    /** XP earned inside the current level. */
    val xpIntoLevel: Int = 0,
    /** XP the current level takes in total. */
    val xpForLevel: Int = xpToNextLevel(1),
    val weeklyGoal: Int = 3,
    val workoutsThisWeek: Int = 0,
    /** Weeks in a row the goal was reached, counting this week once it is reached. */
    val weekStreak: Int = 0,
    val totalWorkouts: Int = 0,
    val totalRecords: Int = 0,
    val achievements: List<AchievementStatus> = Achievement.entries.map { AchievementStatus(it, 0, null) },
)

/** How XP is earned. */
object XpRules {
    const val PER_SET = 10
    const val PER_WORKOUT = 25
    const val PER_RECORD = 50
    const val PER_NEW_EXERCISE = 15
    const val PER_WEEKLY_GOAL = 100
}

/** XP needed to go from [level] to the next one: 100, 150, 200, ... */
fun xpToNextLevel(level: Int): Int = 100 + 50 * (level - 1)

/** Works out the level for a total XP. Returns (level, xpIntoLevel, xpForLevel). */
fun levelFor(xp: Int): Triple<Int, Int, Int> {
    var level = 1
    var remaining = xp
    while (remaining >= xpToNextLevel(level)) {
        remaining -= xpToNextLevel(level)
        level++
    }
    return Triple(level, remaining, xpToNextLevel(level))
}

private fun weekOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Replays the whole log day by day to work out XP, streaks and when each achievement was unlocked. */
fun computeGameStats(sets: List<LoggedSet>, weeklyGoal: Int, today: LocalDate): GameStats {
    val goal = weeklyGoal.coerceAtLeast(1)
    val recordIds = sets.groupBy { it.exerciseId }.flatMap { (_, exerciseSets) ->
        val sessions = exerciseSets.groupBy { it.date }.map { (date, daySets) ->
            HistorySession(date, daySets.map { SetEntry(it.id, it.values) })
        }
        personalRecordSetIds(sessions, exerciseSets.first().type)
    }.toSet()

    var xp = 0
    var workouts = 0
    var records = 0
    val seenExercises = mutableSetOf<String>()
    val seenCategories = mutableSetOf<String>()
    var mobilityDays = 0
    var tendonDays = 0
    var plyometricDays = 0
    var sportDays = 0
    var hiitDays = 0
    var heaviestDay = 0.0
    val workoutsPerWeek = mutableMapOf<LocalDate, Int>()
    val goalWeeks = mutableSetOf<LocalDate>()
    var bestStreak = 0
    val unlocked = mutableMapOf<Achievement, LocalDate>()

    fun streakEndingAt(week: LocalDate): Int {
        var count = 0
        var cursor = week
        while (cursor in goalWeeks) {
            count++
            cursor = cursor.minusWeeks(1)
        }
        return count
    }

    fun progressOf(achievement: Achievement): Int = when (achievement) {
        Achievement.FIRST_WORKOUT, Achievement.WORKOUTS_10, Achievement.WORKOUTS_50, Achievement.WORKOUTS_100 -> workouts
        Achievement.FIRST_RECORD, Achievement.RECORDS_25 -> records
        Achievement.STREAK_4, Achievement.STREAK_12 -> bestStreak
        Achievement.TON_DAY -> heaviestDay.toInt()
        Achievement.EXPLORER -> seenCategories.size
        Achievement.MOBILITY_10 -> mobilityDays
        Achievement.TENDONS_10 -> tendonDays
        Achievement.PLYOMETRICS_10 -> plyometricDays
        Achievement.SPORTS_10 -> sportDays
        Achievement.HIIT_10 -> hiitDays
    }

    sets.groupBy { it.date }.toSortedMap().forEach { (date, daySets) ->
        workouts++
        val dayRecords = daySets.count { it.id in recordIds }
        records += dayRecords
        val newExercises = daySets.map { it.exerciseId }.distinct().count { seenExercises.add(it) }
        xp += XpRules.PER_WORKOUT + XpRules.PER_SET * daySets.size + XpRules.PER_RECORD * dayRecords +
            XpRules.PER_NEW_EXERCISE * newExercises

        val styles = daySets.map { it.style }.toSet()
        seenCategories += daySets.map { it.categoryId }
        if (TrainingStyle.MOBILITY in styles || TrainingStyle.STRETCHING in styles) mobilityDays++
        if (TrainingStyle.ISOMETRIC in styles || TrainingStyle.ECCENTRIC in styles) tendonDays++
        if (TrainingStyle.PLYOMETRIC in styles) plyometricDays++
        if (TrainingStyle.SPORT in styles) sportDays++
        if (TrainingStyle.HIIT in styles) hiitDays++
        heaviestDay = maxOf(heaviestDay, daySets.sumOf { (it.values.weightKg ?: 0.0) * (it.values.reps ?: 0) })

        val week = weekOf(date)
        val count = (workoutsPerWeek[week] ?: 0) + 1
        workoutsPerWeek[week] = count
        if (count == goal) {
            goalWeeks += week
            xp += XpRules.PER_WEEKLY_GOAL
            bestStreak = maxOf(bestStreak, streakEndingAt(week))
        }

        Achievement.entries.forEach { achievement ->
            if (achievement !in unlocked && progressOf(achievement) >= achievement.target) unlocked[achievement] = date
        }
    }

    val thisWeek = weekOf(today)
    val currentStreak = if (thisWeek in goalWeeks) streakEndingAt(thisWeek) else streakEndingAt(thisWeek.minusWeeks(1))
    val (level, xpIntoLevel, xpForLevel) = levelFor(xp)
    return GameStats(
        xp = xp,
        level = level,
        xpIntoLevel = xpIntoLevel,
        xpForLevel = xpForLevel,
        weeklyGoal = goal,
        workoutsThisWeek = workoutsPerWeek[thisWeek] ?: 0,
        weekStreak = currentStreak,
        totalWorkouts = workouts,
        totalRecords = records,
        achievements = Achievement.entries.map {
            AchievementStatus(it, progressOf(it).coerceAtMost(it.target), unlocked[it])
        },
    )
}

/** What changed between two snapshots, for celebrating right after a set is saved. */
fun celebrationsBetween(before: GameStats, after: GameStats): List<String> {
    val messages = mutableListOf<String>()
    if (after.level > before.level) messages += "Level ${after.level} reached!"
    val alreadyUnlocked = before.achievements.filter { it.isUnlocked }.map { it.achievement }.toSet()
    after.achievements.filter { it.isUnlocked && it.achievement !in alreadyUnlocked }.forEach {
        messages += "${it.achievement.emoji} Achievement unlocked: ${it.achievement.title}"
    }
    if (after.weekStreak > before.weekStreak && after.workoutsThisWeek == after.weeklyGoal) {
        messages += "🔥 Weekly goal reached: ${after.weekStreak}-week streak"
    }
    return messages
}
