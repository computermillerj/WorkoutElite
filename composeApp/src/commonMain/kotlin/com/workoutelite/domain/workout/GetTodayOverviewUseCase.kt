package com.workoutelite.domain.workout

import com.workoutelite.domain.common.Result
import com.workoutelite.domain.common.map
import kotlin.coroutines.cancellation.CancellationException

data class TodayOverview(
    val workout: DailyWorkout,
    val completions: List<CompletedWorkout>,
    val activeSession: ActiveWorkoutSession?,
) {
    val isAutoWorkoutCompleted: Boolean
        get() = completions.any { it.workoutId == workout.id }
}

class GetTodayOverviewUseCase(
    private val getOrCreateDailyWorkout: GetOrCreateDailyWorkoutUseCase,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(): Result<TodayOverview, WorkoutError> = try {
        getOrCreateDailyWorkout().map { workout ->
            TodayOverview(
                workout = workout,
                completions = workoutRepository.getCompletedWorkoutsForDate(workout.localDate),
                activeSession = workoutRepository.getActiveSession(),
            )
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        Result.Error(WorkoutError.DATABASE_UNAVAILABLE)
    }
}
