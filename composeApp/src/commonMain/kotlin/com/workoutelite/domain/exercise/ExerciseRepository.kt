package com.workoutelite.domain.exercise

interface ExerciseRepository {
    suspend fun seedExercisesIfNeeded()
    suspend fun getExercisesWithPreferences(): List<ExerciseWithPreference>
    suspend fun getActiveExercisesWithPreferences(): List<ExerciseWithPreference>
    suspend fun getExercisesByIds(ids: List<String>): List<Exercise>
    suspend fun updateFrequency(exerciseId: String, frequency: Int)
}
