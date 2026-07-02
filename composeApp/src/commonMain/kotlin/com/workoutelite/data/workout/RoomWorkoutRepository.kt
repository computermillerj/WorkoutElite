package com.workoutelite.data.workout

import com.workoutelite.data.database.ActiveWorkoutSessionDao
import com.workoutelite.data.database.CompletedWorkoutDao
import com.workoutelite.data.database.DifficultyDao
import com.workoutelite.data.database.DifficultyProfileEntity
import com.workoutelite.data.database.WorkoutDao
import com.workoutelite.domain.workout.ActiveWorkoutSession
import com.workoutelite.domain.workout.CompletedWorkout
import com.workoutelite.domain.workout.DailyWorkout
import com.workoutelite.domain.workout.DifficultyFeedback
import com.workoutelite.domain.workout.WorkoutRepository

class RoomWorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val difficultyDao: DifficultyDao,
    private val completedWorkoutDao: CompletedWorkoutDao,
    private val activeWorkoutSessionDao: ActiveWorkoutSessionDao,
) : WorkoutRepository {
    override suspend fun getAutoWorkout(localDate: String): DailyWorkout? {
        val workout = workoutDao.getAutoWorkout(localDate) ?: return null
        return workout.toWorkout(workoutDao.getItems(workout.id))
    }

    override suspend fun getWorkoutById(id: String): DailyWorkout? {
        val workout = workoutDao.getWorkoutById(id) ?: return null
        return workout.toWorkout(workoutDao.getItems(workout.id))
    }

    override suspend fun getWorkoutsByIds(ids: List<String>): List<DailyWorkout> {
        if (ids.isEmpty()) return emptyList()
        val itemsByWorkout = workoutDao.getItemsForWorkouts(ids).groupBy { it.workoutId }
        return workoutDao.getWorkoutsByIds(ids).map { workout ->
            workout.toWorkout(itemsByWorkout[workout.id].orEmpty())
        }
    }

    override suspend fun getNextSequence(localDate: String): Int =
        workoutDao.getNextSequence(localDate)

    override suspend fun saveWorkout(workout: DailyWorkout) {
        workoutDao.insertWorkoutWithItems(
            workout = workout.toEntity(),
            items = workout.items.map { it.toEntity(workout.id) },
        )
    }

    override suspend fun getRollingDifficultyScore(): Double =
        difficultyDao.getProfile()?.rollingScore ?: INITIAL_DIFFICULTY

    override suspend fun saveCompletedWorkout(completedWorkout: CompletedWorkout) {
        completedWorkoutDao.upsert(completedWorkout.toEntity())
    }

    override suspend fun getCompletedWorkouts(): List<CompletedWorkout> = completedWorkoutDao
        .getCompletedWorkouts()
        .map { it.toCompletedWorkout() }

    override suspend fun getCompletedWorkoutsForDate(localDate: String): List<CompletedWorkout> =
        completedWorkoutDao
            .getCompletedWorkoutsForDate(localDate)
            .map { it.toCompletedWorkout() }

    override suspend fun updateRollingDifficulty(
        feedback: DifficultyFeedback,
        updatedAtEpochMillis: Long,
    ) {
        val current = getRollingDifficultyScore()
        val delta = when (feedback) {
            DifficultyFeedback.EASY -> 0.3
            DifficultyFeedback.MEDIUM -> 0.0
            DifficultyFeedback.HARD -> -0.3
        }
        difficultyDao.upsert(
            DifficultyProfileEntity(
                rollingScore = (current + delta).coerceIn(1.0, 5.0),
                updatedAtEpochMillis = updatedAtEpochMillis,
            ),
        )
    }

    override suspend fun getActiveSession(): ActiveWorkoutSession? =
        activeWorkoutSessionDao.getActiveSession()?.toSession()

    override suspend fun saveActiveSession(session: ActiveWorkoutSession) {
        activeWorkoutSessionDao.upsert(session.toEntity())
    }

    override suspend fun clearActiveSession() {
        activeWorkoutSessionDao.clear()
    }

    private companion object {
        const val INITIAL_DIFFICULTY = 3.0
    }
}
