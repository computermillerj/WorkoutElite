package com.workoutelite.domain.workout

interface WorkoutRepository {
    suspend fun getAutoWorkout(localDate: String): DailyWorkout?
    suspend fun getWorkoutById(id: String): DailyWorkout?
    suspend fun getWorkoutsByIds(ids: List<String>): List<DailyWorkout>
    suspend fun getNextSequence(localDate: String): Int
    suspend fun saveWorkout(workout: DailyWorkout)
    suspend fun getRollingDifficultyScore(): Double
    suspend fun saveCompletedWorkout(completedWorkout: CompletedWorkout)
    suspend fun getCompletedWorkouts(): List<CompletedWorkout>
    suspend fun getCompletedWorkoutsForDate(localDate: String): List<CompletedWorkout>
    suspend fun updateRollingDifficulty(feedback: DifficultyFeedback, updatedAtEpochMillis: Long)
    suspend fun getActiveSession(): ActiveWorkoutSession?
    suspend fun saveActiveSession(session: ActiveWorkoutSession)
    suspend fun clearActiveSession()
}
