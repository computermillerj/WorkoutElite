package com.workoutelite.domain.workout

import com.workoutelite.domain.common.Result
import com.workoutelite.domain.exercise.EquipmentRequirement
import com.workoutelite.domain.exercise.Exercise
import com.workoutelite.domain.exercise.ExerciseCategory
import com.workoutelite.domain.exercise.ExerciseWithPreference
import com.workoutelite.domain.exercise.MovementPattern
import com.workoutelite.platform.ClockProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DailyWorkoutUseCasesTest {
    @Test
    fun `auto workout is generated once and returned unchanged afterwards`() = runTest {
        val workoutRepository = FakeWorkoutRepository()
        val useCase = getOrCreateUseCase(workoutRepository)

        val first = assertIs<Result.Success<DailyWorkout>>(useCase())
        val second = assertIs<Result.Success<DailyWorkout>>(useCase())

        assertEquals(first.data, second.data)
        assertEquals(1, workoutRepository.saveWorkoutCalls)
        assertEquals(0, first.data.sequence)
        assertEquals(WorkoutOrigin.AUTO_DAILY, first.data.origin)
    }

    @Test
    fun `on-demand workout takes the next sequence and differs from the auto workout`() = runTest {
        val workoutRepository = FakeWorkoutRepository()
        val auto = assertIs<Result.Success<DailyWorkout>>(getOrCreateUseCase(workoutRepository)())

        val onDemand = CreateOnDemandWorkoutUseCase(
            exerciseRepository = exerciseRepository(),
            workoutRepository = workoutRepository,
            settingsRepository = FakeSettingsRepository(),
            generator = WorkoutGenerator(),
            clockProvider = ClockProvider(),
        )
        val bonus = assertIs<Result.Success<DailyWorkout>>(onDemand())

        assertEquals(1, bonus.data.sequence)
        assertEquals(WorkoutOrigin.ON_DEMAND, bonus.data.origin)
        assertEquals(auto.data.localDate, bonus.data.localDate)
        assertNotEquals(
            auto.data.items.map { it.exerciseId },
            bonus.data.items.map { it.exerciseId },
        )
        assertEquals(2, workoutRepository.savedWorkouts.size)
    }

    @Test
    fun `no eligible exercises yields an error and persists nothing`() = runTest {
        val workoutRepository = FakeWorkoutRepository()
        val useCase = GetOrCreateDailyWorkoutUseCase(
            exerciseRepository = FakeExerciseRepository(
                exercises = sampleExercises().map { ExerciseWithPreference(it, frequency = 0) },
            ),
            workoutRepository = workoutRepository,
            settingsRepository = FakeSettingsRepository(),
            generator = WorkoutGenerator(),
            clockProvider = ClockProvider(),
        )

        val result = useCase()

        assertEquals(Result.Error(WorkoutError.NO_ELIGIBLE_EXERCISES), result)
        assertTrue(workoutRepository.savedWorkouts.isEmpty())
    }

    @Test
    fun `starting a session persists a fresh unpaused session for the workout`() = runTest {
        val workoutRepository = FakeWorkoutRepository()
        val useCase = StartWorkoutSessionUseCase(
            workoutRepository = workoutRepository,
            clockProvider = ClockProvider(),
        )

        useCase("workout-1")

        val session = workoutRepository.activeSession
        assertEquals("workout-1", session?.workoutId)
        assertEquals(0, session?.currentItemIndex)
        assertEquals(TimerPhase.WORK, session?.phase)
        assertEquals(0, session?.elapsedSecondsInPhase)
        assertEquals(false, session?.isPaused)
    }

    @Test
    fun `generated workout duration follows the configured target`() = runTest {
        val workoutRepository = FakeWorkoutRepository()
        val useCase = GetOrCreateDailyWorkoutUseCase(
            exerciseRepository = exerciseRepository(),
            workoutRepository = workoutRepository,
            settingsRepository = FakeSettingsRepository(targetDurationMinutes = 30),
            generator = WorkoutGenerator(),
            clockProvider = ClockProvider(),
        )

        val result = assertIs<Result.Success<DailyWorkout>>(useCase())

        assertTrue(
            result.data.estimatedDurationSeconds in (30 * 60 * 9 / 10)..(30 * 60 * 23 / 20),
            "expected ~30 min, got ${result.data.estimatedDurationSeconds}s",
        )
    }

    private fun getOrCreateUseCase(workoutRepository: FakeWorkoutRepository) =
        GetOrCreateDailyWorkoutUseCase(
            exerciseRepository = exerciseRepository(),
            workoutRepository = workoutRepository,
            settingsRepository = FakeSettingsRepository(),
            generator = WorkoutGenerator(),
            clockProvider = ClockProvider(),
        )

    private fun exerciseRepository() = FakeExerciseRepository(
        exercises = sampleExercises().map { ExerciseWithPreference(it, frequency = 3) },
    )

    private fun sampleExercises() = List(10) { index ->
        Exercise(
            id = "exercise_$index",
            name = "Exercise $index",
            description = "Description $index",
            category = ExerciseCategory.entries[index % ExerciseCategory.entries.size],
            movementPattern = MovementPattern.entries[index % MovementPattern.entries.size],
            equipment = EquipmentRequirement.NONE,
            difficulty = index % 5 + 1,
            defaultDurationSeconds = 45,
            isUnilateral = false,
            demoAssetPath = null,
            assetSourceUrl = null,
            assetLicense = null,
            assetAttribution = null,
        )
    }
}
