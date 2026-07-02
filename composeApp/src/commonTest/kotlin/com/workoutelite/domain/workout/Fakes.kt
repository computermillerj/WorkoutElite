package com.workoutelite.domain.workout

import com.workoutelite.domain.exercise.Exercise
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.exercise.ExerciseWithPreference
import com.workoutelite.domain.settings.SettingsRepository

class FakeWorkoutRepository(
    var rollingScore: Double = 3.0,
) : WorkoutRepository {
    val savedWorkouts = mutableListOf<DailyWorkout>()
    val completions = mutableListOf<CompletedWorkout>()
    var activeSession: ActiveWorkoutSession? = null
    var saveWorkoutCalls = 0

    override suspend fun getAutoWorkout(localDate: String): DailyWorkout? =
        savedWorkouts.firstOrNull { it.localDate == localDate && it.sequence == 0 }

    override suspend fun getWorkoutById(id: String): DailyWorkout? =
        savedWorkouts.firstOrNull { it.id == id }

    override suspend fun getWorkoutsByIds(ids: List<String>): List<DailyWorkout> =
        savedWorkouts.filter { it.id in ids }

    override suspend fun getNextSequence(localDate: String): Int =
        (savedWorkouts.filter { it.localDate == localDate }.maxOfOrNull { it.sequence } ?: -1) + 1

    override suspend fun saveWorkout(workout: DailyWorkout) {
        check(
            savedWorkouts.none {
                it.localDate == workout.localDate && it.sequence == workout.sequence
            },
        ) { "UNIQUE(localDate, sequence) violated" }
        saveWorkoutCalls++
        savedWorkouts += workout
    }

    override suspend fun getRollingDifficultyScore(): Double = rollingScore

    override suspend fun saveCompletedWorkout(completedWorkout: CompletedWorkout) {
        completions += completedWorkout
    }

    override suspend fun getCompletedWorkouts(): List<CompletedWorkout> = completions

    override suspend fun getCompletedWorkoutsForDate(localDate: String): List<CompletedWorkout> =
        completions.filter { it.localDate == localDate }

    override suspend fun updateRollingDifficulty(
        feedback: DifficultyFeedback,
        updatedAtEpochMillis: Long,
    ) {
        val delta = when (feedback) {
            DifficultyFeedback.EASY -> 0.3
            DifficultyFeedback.MEDIUM -> 0.0
            DifficultyFeedback.HARD -> -0.3
        }
        rollingScore = (rollingScore + delta).coerceIn(1.0, 5.0)
    }

    override suspend fun getActiveSession(): ActiveWorkoutSession? = activeSession

    override suspend fun saveActiveSession(session: ActiveWorkoutSession) {
        activeSession = session
    }

    override suspend fun clearActiveSession() {
        activeSession = null
    }
}

class FakeSettingsRepository(
    var targetDurationMinutes: Int = 15,
) : SettingsRepository {
    override suspend fun getTargetDurationMinutes(): Int = targetDurationMinutes

    override suspend fun setTargetDurationMinutes(minutes: Int) {
        targetDurationMinutes = minutes
    }
}

class FakeExerciseRepository(
    private val exercises: List<ExerciseWithPreference>,
) : ExerciseRepository {
    override suspend fun seedExercisesIfNeeded() = Unit

    override suspend fun getExercisesWithPreferences(): List<ExerciseWithPreference> = exercises

    override suspend fun getActiveExercisesWithPreferences(): List<ExerciseWithPreference> =
        exercises.filter { it.exercise.isActive }

    override suspend fun getExercisesByIds(ids: List<String>): List<Exercise> =
        exercises.map { it.exercise }.filter { it.id in ids }

    override suspend fun updateFrequency(exerciseId: String, frequency: Int) = Unit
}
