package com.workoutelite.domain.workout

import com.workoutelite.domain.common.Result
import com.workoutelite.domain.exercise.ExerciseWithPreference
import kotlin.math.abs
import kotlin.random.Random

class WorkoutGenerator {
    fun generate(input: WorkoutGenerationInput): Result<DailyWorkout, WorkoutError> {
        val eligible = input.exercises.filter { it.frequency > 0 && it.exercise.isActive }
        if (eligible.isEmpty()) {
            return Result.Error(WorkoutError.NO_ELIGIBLE_EXERCISES)
        }

        // Target a window around the requested duration rather than a point, plus a hard item
        // cap, so generation always terminates even with a tiny eligible pool.
        val minSeconds = input.targetDurationSeconds * 9 / 10
        val maxSeconds = input.targetDurationSeconds * 23 / 20
        val maxItems = (input.targetDurationSeconds / MIN_SECONDS_PER_ITEM).coerceAtLeast(MIN_ITEMS)

        val items = mutableListOf<WorkoutItem>()
        var totalSeconds = 0
        var previousPattern = eligible.first().exercise.movementPattern
        var previousCategory = eligible.first().exercise.category

        while (items.size < maxItems && items.estimatedSeconds() < minSeconds) {
            val pool = eligible
                .filter {
                    it.exercise.movementPattern != previousPattern &&
                        it.exercise.category != previousCategory
                }
                .ifEmpty { eligible.filter { it.exercise.movementPattern != previousPattern } }
                .ifEmpty { eligible }
            val selected = pool.weightedPick(
                random = input.random,
                targetDifficulty = input.targetDifficultyScore,
            )
            val restSeconds = REST_SECONDS
            val item = WorkoutItem(
                exerciseId = selected.exercise.id,
                order = items.size,
                workSeconds = selected.exercise.defaultDurationSeconds,
                restAfterSeconds = restSeconds,
                isUnilateral = selected.exercise.isUnilateral,
            )
            val nextTotal = totalSeconds + item.workSeconds + restSeconds
            if (totalSeconds >= minSeconds && nextTotal > maxSeconds) break

            items += item
            totalSeconds = nextTotal
            previousPattern = selected.exercise.movementPattern
            previousCategory = selected.exercise.category
        }

        val normalizedItems = items.dropLast(1) + items.last().copy(restAfterSeconds = 0)
        val estimatedSeconds = normalizedItems.sumOf { it.workSeconds + it.restAfterSeconds }

        return Result.Success(
            DailyWorkout(
                id = input.workoutId,
                localDate = input.localDate,
                sequence = input.sequence,
                origin = input.origin,
                targetDifficultyScore = input.targetDifficultyScore,
                estimatedDurationSeconds = estimatedSeconds,
                items = normalizedItems,
                createdAtEpochMillis = input.createdAtEpochMillis,
            ),
        )
    }

    private fun List<ExerciseWithPreference>.weightedPick(
        random: Random,
        targetDifficulty: Double,
    ): ExerciseWithPreference {
        val weighted = map { exercise ->
            val difficultyWeight = (6 - abs(exercise.exercise.difficulty - targetDifficulty)).coerceAtLeast(1.0)
            exercise to (exercise.frequency * difficultyWeight).toInt().coerceAtLeast(1)
        }
        var ticket = random.nextInt(weighted.sumOf { it.second })
        for ((exercise, weight) in weighted) {
            ticket -= weight
            if (ticket < 0) return exercise
        }
        return last()
    }

    private fun List<WorkoutItem>.estimatedSeconds(): Int = sumOf { it.workSeconds + it.restAfterSeconds } -
        (lastOrNull()?.restAfterSeconds ?: 0)

    private companion object {
        const val MIN_SECONDS_PER_ITEM = 60
        const val MIN_ITEMS = 4
        const val REST_SECONDS = 15
    }
}

data class WorkoutGenerationInput(
    val workoutId: String,
    val localDate: String,
    val sequence: Int,
    val origin: WorkoutOrigin,
    val targetDifficultyScore: Double,
    val targetDurationSeconds: Int,
    val exercises: List<ExerciseWithPreference>,
    val random: Random,
    val createdAtEpochMillis: Long,
)
