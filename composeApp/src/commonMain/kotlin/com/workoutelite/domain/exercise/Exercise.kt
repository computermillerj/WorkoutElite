package com.workoutelite.domain.exercise

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val category: ExerciseCategory,
    val movementPattern: MovementPattern,
    val equipment: EquipmentRequirement,
    val difficulty: Int,
    val defaultDurationSeconds: Int,
    val isUnilateral: Boolean,
    val demoAssetPath: String?,
    val assetSourceUrl: String?,
    val assetLicense: String?,
    val assetAttribution: String?,
    val isActive: Boolean = true,
)

enum class ExerciseCategory {
    LOWER_BODY,
    UPPER_BODY,
    CORE,
    CARDIO,
    MOBILITY,
    FULL_BODY,
}

enum class MovementPattern {
    SQUAT,
    LUNGE,
    PUSH,
    HINGE,
    CORE,
    JUMP,
    CARDIO,
    MOBILITY,
}

enum class EquipmentRequirement {
    NONE,
    JUMP_ROPE,
}

data class ExerciseWithPreference(
    val exercise: Exercise,
    val frequency: Int,
)
