package com.workoutelite.domain.workout

import com.workoutelite.domain.common.Result
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.settings.SettingsRepository
import com.workoutelite.platform.ClockProvider
import com.workoutelite.platform.stableSeed
import com.workoutelite.platform.uuidString
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

class GetOrCreateDailyWorkoutUseCase(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val generator: WorkoutGenerator,
    private val clockProvider: ClockProvider,
) {
    suspend operator fun invoke(): Result<DailyWorkout, WorkoutError> = try {
        exerciseRepository.seedExercisesIfNeeded()
        val localDate = clockProvider.todayString()
        val existing = workoutRepository.getAutoWorkout(localDate)
        if (existing != null) {
            Result.Success(existing)
        } else {
            generateAndSave(localDate)
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        Result.Error(WorkoutError.DATABASE_UNAVAILABLE)
    }

    private suspend fun generateAndSave(localDate: String): Result<DailyWorkout, WorkoutError> {
        val input = WorkoutGenerationInput(
            workoutId = uuidString(),
            localDate = localDate,
            sequence = AUTO_SEQUENCE,
            origin = WorkoutOrigin.AUTO_DAILY,
            targetDifficultyScore = workoutRepository.getRollingDifficultyScore(),
            targetDurationSeconds = settingsRepository.getTargetDurationMinutes() * 60,
            exercises = exerciseRepository.getActiveExercisesWithPreferences(),
            random = Random(stableSeed("$localDate#$AUTO_SEQUENCE")),
            createdAtEpochMillis = clockProvider.nowEpochMillis(),
        )
        return when (val result = generator.generate(input)) {
            is Result.Error -> result
            is Result.Success -> Result.Success(saveOrReadConcurrent(result.data, localDate))
        }
    }

    /**
     * Persists the generated workout; if a concurrent caller won the UNIQUE(localDate, sequence)
     * race, returns the row that caller inserted instead.
     */
    private suspend fun saveOrReadConcurrent(
        workout: DailyWorkout,
        localDate: String,
    ): DailyWorkout = try {
        workoutRepository.saveWorkout(workout)
        workout
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        workoutRepository.getAutoWorkout(localDate) ?: throw exception
    }

    private companion object {
        const val AUTO_SEQUENCE = 0
    }
}
