package com.workoutelite.domain.workout

import com.workoutelite.domain.common.Result
import com.workoutelite.platform.ClockProvider
import kotlin.coroutines.cancellation.CancellationException

class StartWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository,
    private val clockProvider: ClockProvider,
) {
    suspend operator fun invoke(workoutId: String): Result<Unit, WorkoutError> = try {
        workoutRepository.saveActiveSession(
            ActiveWorkoutSession(
                workoutId = workoutId,
                currentItemIndex = 0,
                phase = TimerPhase.WORK,
                elapsedSecondsInPhase = 0,
                isPaused = false,
                updatedAtEpochMillis = clockProvider.nowEpochMillis(),
            ),
        )
        Result.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        Result.Error(WorkoutError.DATABASE_UNAVAILABLE)
    }
}
