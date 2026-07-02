package com.workoutelite.domain.workout

import com.workoutelite.platform.ClockProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CompleteWorkoutUseCaseTest {
    @Test
    fun `easy feedback saves completion and increases difficulty`() = runTest {
        val repository = FakeWorkoutRepository(rollingScore = 3.0)
        val useCase = CompleteWorkoutUseCase(
            workoutRepository = repository,
            clockProvider = ClockProvider(),
        )

        useCase(workout(), DifficultyFeedback.EASY)

        assertEquals(DifficultyFeedback.EASY, repository.completions.single().feedback)
        assertEquals(3.3, repository.rollingScore)
    }

    @Test
    fun `hard feedback clamps difficulty at minimum`() = runTest {
        val repository = FakeWorkoutRepository(rollingScore = 1.1)
        val useCase = CompleteWorkoutUseCase(
            workoutRepository = repository,
            clockProvider = ClockProvider(),
        )

        useCase(workout(), DifficultyFeedback.HARD)

        assertEquals(1.0, repository.rollingScore)
    }

    @Test
    fun `completion inherits the workout's local date, not today's`() = runTest {
        val repository = FakeWorkoutRepository()
        val useCase = CompleteWorkoutUseCase(
            workoutRepository = repository,
            clockProvider = ClockProvider(),
        )

        useCase(workout(localDate = "1999-12-31"), DifficultyFeedback.MEDIUM)

        assertEquals("1999-12-31", repository.completions.single().localDate)
    }

    @Test
    fun `actual duration is recorded when provided`() = runTest {
        val repository = FakeWorkoutRepository()
        val useCase = CompleteWorkoutUseCase(
            workoutRepository = repository,
            clockProvider = ClockProvider(),
        )

        useCase(workout(), DifficultyFeedback.MEDIUM, durationSeconds = 480)

        assertEquals(480, repository.completions.single().durationSeconds)
    }

    private fun workout(localDate: String = "2026-07-02") = DailyWorkout(
        id = "workout",
        localDate = localDate,
        sequence = 0,
        origin = WorkoutOrigin.AUTO_DAILY,
        targetDifficultyScore = 3.0,
        estimatedDurationSeconds = 900,
        items = emptyList(),
        createdAtEpochMillis = 1L,
    )
}
