package com.workoutelite.presentation.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutelite.ui.components.EmptyState
import com.workoutelite.ui.components.ErrorState
import com.workoutelite.ui.components.LoadingState
import com.workoutelite.ui.theme.WorkoutEliteTheme
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExerciseListRoot(
    modifier: Modifier = Modifier,
    viewModel: ExerciseListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ExerciseListScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun ExerciseListScreen(
    state: ExerciseListState,
    onAction: (ExerciseListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Exercises",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tune how often each move shows up",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            WorkoutLengthCard(
                minutes = state.targetDurationMinutes,
                onMinutesChange = { onAction(ExerciseListAction.OnDurationChange(it)) },
                onMinutesCommit = { onAction(ExerciseListAction.OnDurationCommit) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            SearchField(
                query = state.searchQuery,
                onQueryChange = { onAction(ExerciseListAction.OnSearchChange(it)) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        CategoryFilters(state = state, onAction = onAction)
        Spacer(modifier = Modifier.height(4.dp))
        when {
            state.isLoading -> LoadingState()
            state.error != null -> ErrorState(
                message = state.error,
                onRetryClick = { onAction(ExerciseListAction.OnRetryClick) },
            )
            state.exercises.isEmpty() -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "Nothing matches",
                message = "Try a different search or category.",
            )
            else -> ExerciseList(
                exercises = state.exercises,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun WorkoutLengthCard(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    onMinutesCommit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Workout length",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$minutes min",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = minutes.toFloat(),
                onValueChange = { value ->
                    onMinutesChange(((value / 5).roundToInt() * 5).coerceIn(5, 60))
                },
                onValueChangeFinished = onMinutesCommit,
                valueRange = 5f..60f,
                steps = 10,
            )
            Text(
                text = "Used for newly generated workouts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = "Search exercises") },
        leadingIcon = {
            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun CategoryFilters(
    state: ExerciseListState,
    onAction: (ExerciseListAction) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CategoryChip(
                label = "All",
                selected = state.selectedCategory == null,
                onClick = { onAction(ExerciseListAction.OnCategoryClick(null)) },
            )
        }
        items(state.categories, key = { it.key }) { category ->
            CategoryChip(
                label = category.label,
                selected = state.selectedCategory == category.key,
                onClick = { onAction(ExerciseListAction.OnCategoryClick(category.key)) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ExerciseList(
    exercises: List<ExercisePreferenceUi>,
    onAction: (ExerciseListAction) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = exercises,
            key = { it.id },
        ) { exercise ->
            ExercisePreferenceCard(
                exercise = exercise,
                onFrequencyChange = { frequency ->
                    onAction(ExerciseListAction.OnFrequencyChange(exercise.id, frequency))
                },
                onFrequencyCommit = {
                    onAction(ExerciseListAction.OnFrequencyCommit(exercise.id))
                },
            )
        }
    }
}

@Composable
private fun ExercisePreferenceCard(
    exercise: ExercisePreferenceUi,
    onFrequencyChange: (Int) -> Unit,
    onFrequencyCommit: () -> Unit,
) {
    val isMuted = exercise.frequency == 0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isMuted) 0.6f else 1f },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                DifficultyDots(difficulty = exercise.difficulty)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(text = exercise.categoryLabel)
                exercise.equipmentLabel?.let { Tag(text = it) }
                if (exercise.isUnilateral) Tag(text = "Both sides")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = exercise.frequencyLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isMuted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            Slider(
                value = exercise.frequency.toFloat(),
                onValueChange = { onFrequencyChange(it.roundToInt().coerceIn(0, 5)) },
                onValueChangeFinished = onFrequencyCommit,
                valueRange = 0f..5f,
                steps = 4,
            )
        }
    }
}

@Composable
private fun DifficultyDots(difficulty: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "Difficulty",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 3.dp),
        )
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (index < difficulty) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview
@Composable
private fun ExerciseListScreenPreview() {
    WorkoutEliteTheme {
        ExerciseListScreen(
            state = ExerciseListState(
                isLoading = false,
                categories = listOf(
                    CategoryFilterUi("LOWER_BODY", "Lower body"),
                    CategoryFilterUi("CORE", "Core"),
                ),
                selectedCategory = "LOWER_BODY",
                exercises = listOf(
                    ExercisePreferenceUi(
                        id = "squat_bodyweight",
                        name = "Bodyweight Squat",
                        description = "Stand tall, sit hips back and down, then drive through your feet to stand.",
                        categoryKey = "LOWER_BODY",
                        categoryLabel = "Lower body",
                        equipmentLabel = null,
                        difficulty = 2,
                        isUnilateral = false,
                        frequency = 3,
                        frequencyLabel = "Normal",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
