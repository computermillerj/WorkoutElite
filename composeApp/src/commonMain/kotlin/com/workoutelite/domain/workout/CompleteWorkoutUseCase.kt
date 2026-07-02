package com.workoutelite.domain.workout

import com.workoutelite.domain.common.Result
import com.workoutelite.platform.ClockProvider
import kotlin.coroutines.cancellation.CancellationException

class CompleteWorkoutUseCase(
    private val workoutRepository: WorkoutRepository,
    private val clockProvider: ClockProvider,
) {
    suspend operator fun invoke(
        workout: DailyWorkout,
        feedback: DifficultyFeedback,
        durationSeconds: Int = workout.estimatedDurationSeconds,
    ): Result<Unit, WorkoutError> = try {
        val now = clockProvider.nowEpochMillis()
        workoutRepository.saveCompletedWorkout(
            CompletedWorkout(
                workoutId = workout.id,
                localDate = workout.localDate,
                completedAtEpochMillis = now,
                feedback = feedback,
                durationSeconds = durationSeconds,
            ),
        )
        workoutRepository.updateRollingDifficulty(
            feedback = feedback,
            updatedAtEpochMillis = now,
        )
        Result.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        Result.Error(WorkoutError.DATABASE_UNAVAILABLE)
    }
}
