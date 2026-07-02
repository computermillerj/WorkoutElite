package com.workoutelite.domain.workout

import com.workoutelite.domain.common.Result
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.settings.SettingsRepository
import com.workoutelite.platform.ClockProvider
import com.workoutelite.platform.stableSeed
import com.workoutelite.platform.uuidString
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

/**
 * Generates and persists an extra same-day workout at the next free sequence. Seeding the RNG
 * with the sequence keeps every bonus workout different from the day's earlier ones.
 */
class CreateOnDemandWorkoutUseCase(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val generator: WorkoutGenerator,
    private val clockProvider: ClockProvider,
) {
    suspend operator fun invoke(): Result<DailyWorkout, WorkoutError> = try {
        exerciseRepository.seedExercisesIfNeeded()
        val localDate = clockProvider.todayString()
        val exercises = exerciseRepository.getActiveExercisesWithPreferences()
        val targetDurationSeconds = settingsRepository.getTargetDurationMinutes() * 60
        var lastError: Exception? = null

        repeat(MAX_SEQUENCE_ATTEMPTS) {
            val sequence = workoutRepository.getNextSequence(localDate)
            val input = WorkoutGenerationInput(
                workoutId = uuidString(),
                localDate = localDate,
                sequence = sequence,
                origin = WorkoutOrigin.ON_DEMAND,
                targetDifficultyScore = workoutRepository.getRollingDifficultyScore(),
                targetDurationSeconds = targetDurationSeconds,
                exercises = exercises,
                random = Random(stableSeed("$localDate#$sequence")),
                createdAtEpochMillis = clockProvider.nowEpochMillis(),
            )
            when (val result = generator.generate(input)) {
                is Result.Error -> return result
                is Result.Success -> try {
                    workoutRepository.saveWorkout(result.data)
                    return Result.Success(result.data)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    lastError = exception
                }
            }
        }
        throw lastError ?: IllegalStateException("Could not allocate a workout sequence")
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        Result.Error(WorkoutError.DATABASE_UNAVAILABLE)
    }

    private companion object {
        const val MAX_SEQUENCE_ATTEMPTS = 3
    }
}
