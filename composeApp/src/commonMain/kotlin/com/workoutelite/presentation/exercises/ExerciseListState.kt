package com.workoutelite.presentation.exercises

import androidx.compose.runtime.Stable

@Stable
data class ExerciseListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val exercises: List<ExercisePreferenceUi> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val categories: List<CategoryFilterUi> = emptyList(),
    val targetDurationMinutes: Int = 15,
)

data class CategoryFilterUi(
    val key: String,
    val label: String,
)

data class ExercisePreferenceUi(
    val id: String,
    val name: String,
    val description: String,
    val categoryKey: String,
    val categoryLabel: String,
    val equipmentLabel: String?,
    val difficulty: Int,
    val isUnilateral: Boolean,
    val frequency: Int,
    val frequencyLabel: String,
)

sealed interface ExerciseListAction {
    data class OnFrequencyChange(
        val exerciseId: String,
        val frequency: Int,
    ) : ExerciseListAction

    data class OnFrequencyCommit(val exerciseId: String) : ExerciseListAction
    data class OnDurationChange(val minutes: Int) : ExerciseListAction
    data object OnDurationCommit : ExerciseListAction
    data class OnSearchChange(val query: String) : ExerciseListAction
    data class OnCategoryClick(val categoryKey: String?) : ExerciseListAction
    data object OnRetryClick : ExerciseListAction
}

fun frequencyLabel(frequency: Int): String = when (frequency.coerceIn(0, 5)) {
    0 -> "Never"
    1 -> "Rare"
    2 -> "Sometimes"
    3 -> "Normal"
    4 -> "Often"
    else -> "Daily"
}
