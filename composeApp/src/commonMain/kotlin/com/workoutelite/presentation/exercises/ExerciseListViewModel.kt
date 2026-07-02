package com.workoutelite.presentation.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutelite.domain.exercise.EquipmentRequirement
import com.workoutelite.domain.exercise.ExerciseCategory
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.exercise.ExerciseWithPreference
import com.workoutelite.domain.settings.SettingsRepository
import com.workoutelite.domain.settings.SettingsRepository.Companion.MAX_DURATION_MINUTES
import com.workoutelite.domain.settings.SettingsRepository.Companion.MIN_DURATION_MINUTES
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExerciseListViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ExerciseListState(
            searchQuery = savedStateHandle[KEY_SEARCH] ?: "",
            selectedCategory = savedStateHandle[KEY_CATEGORY],
        ),
    )
    val state = _state.asStateFlow()

    private var allExercises = emptyList<ExercisePreferenceUi>()

    init {
        loadExercises()
    }

    fun onAction(action: ExerciseListAction) {
        when (action) {
            is ExerciseListAction.OnFrequencyChange -> updateFrequency(
                exerciseId = action.exerciseId,
                frequency = action.frequency,
            )
            is ExerciseListAction.OnFrequencyCommit -> commitFrequency(action.exerciseId)
            is ExerciseListAction.OnDurationChange -> _state.update {
                it.copy(
                    targetDurationMinutes = action.minutes
                        .coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES),
                )
            }
            ExerciseListAction.OnDurationCommit -> commitDuration()
            is ExerciseListAction.OnSearchChange -> updateSearch(action.query)
            is ExerciseListAction.OnCategoryClick -> updateCategory(action.categoryKey)
            ExerciseListAction.OnRetryClick -> loadExercises()
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                exerciseRepository.seedExercisesIfNeeded()
                allExercises = exerciseRepository
                    .getExercisesWithPreferences()
                    .map { exercise -> exercise.toUi() }
                val targetDurationMinutes = settingsRepository.getTargetDurationMinutes()
                _state.update {
                    it.copy(
                        isLoading = false,
                        exercises = filteredExercises(),
                        categories = ExerciseCategory.entries
                            .filter { category -> allExercises.any { it.categoryKey == category.name } }
                            .map { CategoryFilterUi(it.name, it.toLabel()) },
                        targetDurationMinutes = targetDurationMinutes,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Exercise settings are unavailable. Please try again.",
                    )
                }
            }
        }
    }

    private fun updateFrequency(exerciseId: String, frequency: Int) {
        val safeFrequency = frequency.coerceIn(0, 5)
        allExercises = allExercises.map { exercise ->
            if (exercise.id == exerciseId) {
                exercise.copy(
                    frequency = safeFrequency,
                    frequencyLabel = frequencyLabel(safeFrequency),
                )
            } else {
                exercise
            }
        }
        _state.update { it.copy(exercises = filteredExercises()) }
    }

    private fun commitFrequency(exerciseId: String) {
        val frequency = allExercises.firstOrNull { it.id == exerciseId }?.frequency ?: return
        viewModelScope.launch {
            runCatching { exerciseRepository.updateFrequency(exerciseId, frequency) }
        }
    }

    private fun commitDuration() {
        val minutes = _state.value.targetDurationMinutes
        viewModelScope.launch {
            runCatching { settingsRepository.setTargetDurationMinutes(minutes) }
        }
    }

    private fun updateSearch(query: String) {
        savedStateHandle[KEY_SEARCH] = query
        _state.update { it.copy(searchQuery = query, exercises = filteredExercises(query = query)) }
    }

    private fun updateCategory(categoryKey: String?) {
        savedStateHandle[KEY_CATEGORY] = categoryKey
        _state.update {
            it.copy(
                selectedCategory = categoryKey,
                exercises = filteredExercises(categoryKey = categoryKey),
            )
        }
    }

    private fun filteredExercises(
        query: String = _state.value.searchQuery,
        categoryKey: String? = _state.value.selectedCategory,
    ): List<ExercisePreferenceUi> = allExercises.filter { exercise ->
        val matchesQuery = query.isBlank() ||
            exercise.name.contains(query, ignoreCase = true) ||
            exercise.description.contains(query, ignoreCase = true)
        val matchesCategory = categoryKey == null || exercise.categoryKey == categoryKey
        matchesQuery && matchesCategory
    }

    private fun ExerciseWithPreference.toUi() = ExercisePreferenceUi(
        id = exercise.id,
        name = exercise.name,
        description = exercise.description,
        categoryKey = exercise.category.name,
        categoryLabel = exercise.category.toLabel(),
        equipmentLabel = when (exercise.equipment) {
            EquipmentRequirement.NONE -> null
            EquipmentRequirement.JUMP_ROPE -> "Jump rope"
        },
        difficulty = exercise.difficulty,
        isUnilateral = exercise.isUnilateral,
        frequency = frequency,
        frequencyLabel = frequencyLabel(frequency),
    )

    private fun ExerciseCategory.toLabel(): String = name
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.titlecase() }

    private companion object {
        const val KEY_SEARCH = "searchQuery"
        const val KEY_CATEGORY = "selectedCategory"
    }
}
