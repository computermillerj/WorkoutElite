package com.workoutelite.data.workout

import com.workoutelite.data.database.ExerciseDao
import com.workoutelite.data.database.ExercisePreferenceDao
import com.workoutelite.data.database.ExercisePreferenceEntity
import com.workoutelite.data.seed.seedExercises
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.exercise.ExerciseWithPreference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val preferenceDao: ExercisePreferenceDao,
) : ExerciseRepository {
    private val seedMutex = Mutex()
    private var isSeeded = false

    override suspend fun seedExercisesIfNeeded() {
        if (isSeeded) return
        seedMutex.withLock {
            if (isSeeded) return
            exerciseDao.upsertAll(seedExercises.map { it.toEntity() })
            isSeeded = true
        }
    }

    override suspend fun getExercisesWithPreferences(): List<ExerciseWithPreference> {
        val preferences = preferenceDao.getAll().associateBy { it.exerciseId }
        return exerciseDao.getExercises().map { entity ->
            ExerciseWithPreference(
                exercise = entity.toExercise(),
                frequency = preferences[entity.id]?.frequency ?: DEFAULT_FREQUENCY,
            )
        }
    }

    override suspend fun getActiveExercisesWithPreferences(): List<ExerciseWithPreference> {
        val preferences = preferenceDao.getAll().associateBy { it.exerciseId }
        return exerciseDao.getActiveExercises().map { entity ->
            ExerciseWithPreference(
                exercise = entity.toExercise(),
                frequency = preferences[entity.id]?.frequency ?: DEFAULT_FREQUENCY,
            )
        }
    }

    override suspend fun getExercisesByIds(ids: List<String>) = exerciseDao
        .getExercisesByIds(ids)
        .map { it.toExercise() }

    override suspend fun updateFrequency(exerciseId: String, frequency: Int) {
        preferenceDao.upsert(
            ExercisePreferenceEntity(
                exerciseId = exerciseId,
                frequency = frequency.coerceIn(0, 5),
            ),
        )
    }

    private companion object {
        const val DEFAULT_FREQUENCY = 3
    }
}
