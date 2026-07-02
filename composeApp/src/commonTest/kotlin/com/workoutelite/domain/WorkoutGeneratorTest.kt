package com.workoutelite.domain

import com.workoutelite.domain.common.Result
import com.workoutelite.domain.exercise.EquipmentRequirement
import com.workoutelite.domain.exercise.Exercise
import com.workoutelite.domain.exercise.ExerciseCategory
import com.workoutelite.domain.exercise.ExerciseWithPreference
import com.workoutelite.domain.exercise.MovementPattern
import com.workoutelite.domain.workout.WorkoutError
import com.workoutelite.domain.workout.WorkoutGenerationInput
import com.workoutelite.domain.workout.WorkoutGenerator
import com.workoutelite.domain.workout.WorkoutOrigin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkoutGeneratorTest {
    private val generator = WorkoutGenerator()

    @Test
    fun `returns error when no exercises are eligible`() {
        val result = generator.generate(input(exercises = emptyList()))

        assertEquals(Result.Error(WorkoutError.NO_ELIGIBLE_EXERCISES), result)
    }

    @Test
    fun `generates workout inside duration window`() {
        val result = generator.generate(input())

        val workout = assertIs<Result.Success<*>>(result).data as com.workoutelite.domain.workout.DailyWorkout
        assertTrue(workout.estimatedDurationSeconds in 810..1035)
        assertTrue(workout.items.isNotEmpty())
        assertEquals(0, workout.items.last().restAfterSeconds)
    }

    @Test
    fun `short target duration yields a proportionally short workout`() {
        val result = generator.generate(input(targetDurationSeconds = 300))

        val workout = assertIs<Result.Success<*>>(result).data as com.workoutelite.domain.workout.DailyWorkout
        assertTrue(
            workout.estimatedDurationSeconds in 270..345,
            "expected ~5 min, got ${workout.estimatedDurationSeconds}s",
        )
    }

    @Test
    fun `tiny eligible pool still terminates and respects the item cap`() {
        val result = generator.generate(
            input(
                exercises = listOf(
                    ExerciseWithPreference(sampleExercise("only"), frequency = 1),
                ),
            ),
        )

        val workout = assertIs<Result.Success<*>>(result).data as com.workoutelite.domain.workout.DailyWorkout
        assertTrue(workout.items.isNotEmpty())
        assertTrue(workout.items.size <= 15)
    }

    @Test
    fun `higher frequency exercises are selected more often`() {
        // Same movement pattern for both, so the diversity rule relaxes to the full pool and
        // the weighted pick (not forced alternation) decides every slot.
        val exercises = listOf(
            ExerciseWithPreference(sampleExercise("rare", pattern = MovementPattern.CORE), frequency = 1),
            ExerciseWithPreference(sampleExercise("daily", pattern = MovementPattern.CORE), frequency = 5),
        )
        val picks = (0 until 50).flatMap { seed ->
            val result = generator.generate(input(exercises = exercises, random = Random(seed)))
            val workout = assertIs<Result.Success<*>>(result).data as com.workoutelite.domain.workout.DailyWorkout
            workout.items.map { it.exerciseId }
        }

        val dailyCount = picks.count { it == "daily" }
        val rareCount = picks.count { it == "rare" }
        assertTrue(
            dailyCount > rareCount,
            "expected 'daily' ($dailyCount) to be picked more often than 'rare' ($rareCount)",
        )
    }

    @Test
    fun `unilateral exercises carry the flag for the halfway alert`() {
        val result = generator.generate(
            input(
                exercises = listOf(
                    ExerciseWithPreference(sampleExercise("lunge", isUnilateral = true), frequency = 3),
                ),
            ),
        )

        val workout = assertIs<Result.Success<*>>(result).data as com.workoutelite.domain.workout.DailyWorkout
        assertTrue(workout.items.all { it.isUnilateral })
    }

    @Test
    fun `frequency zero exercises are excluded`() {
        val result = generator.generate(
            input(
                exercises = listOf(
                    sampleExercise("excluded") to 0,
                    sampleExercise("included") to 3,
                ).map { (exercise, frequency) -> ExerciseWithPreference(exercise, frequency) },
            ),
        )

        val workout = assertIs<Result.Success<*>>(result).data as com.workoutelite.domain.workout.DailyWorkout
        assertTrue(workout.items.all { it.exerciseId == "included" })
    }

    private fun input(
        exercises: List<ExerciseWithPreference> = List(8) { index ->
            ExerciseWithPreference(sampleExercise("exercise_$index"), frequency = 3)
        },
        random: Random = Random(1),
        targetDurationSeconds: Int = 900,
    ) = WorkoutGenerationInput(
        workoutId = "workout",
        localDate = "2026-07-02",
        sequence = 0,
        origin = WorkoutOrigin.AUTO_DAILY,
        targetDifficultyScore = 3.0,
        targetDurationSeconds = targetDurationSeconds,
        exercises = exercises,
        random = random,
        createdAtEpochMillis = 0,
    )

    private fun sampleExercise(
        id: String,
        isUnilateral: Boolean = false,
        pattern: MovementPattern = MovementPattern.entries[id.length % MovementPattern.entries.size],
    ) = Exercise(
        id = id,
        name = id,
        description = id,
        category = ExerciseCategory.FULL_BODY,
        movementPattern = pattern,
        equipment = EquipmentRequirement.NONE,
        difficulty = 3,
        defaultDurationSeconds = 45,
        isUnilateral = isUnilateral,
        demoAssetPath = null,
        assetSourceUrl = null,
        assetLicense = null,
        assetAttribution = null,
    )
}
