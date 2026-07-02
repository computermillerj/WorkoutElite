package com.workoutelite.presentation.workout

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutelite.presentation.common.ObserveAsEvents
import com.workoutelite.presentation.common.OnResumed
import com.workoutelite.ui.components.ErrorState
import com.workoutelite.ui.components.LoadingState
import com.workoutelite.ui.theme.WorkoutEliteTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayWorkoutRoot(
    onNavigateToActiveWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayWorkoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    OnResumed { viewModel.onAction(TodayWorkoutAction.OnScreenResumed) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TodayWorkoutEvent.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
        }
    }

    TodayWorkoutScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun TodayWorkoutScreen(
    state: TodayWorkoutState,
    onAction: (TodayWorkoutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> LoadingState(modifier = modifier)
        state.error != null -> ErrorState(
            message = state.error,
            onRetryClick = { onAction(TodayWorkoutAction.OnRetryClick) },
            modifier = modifier,
        )
        state.workout != null -> TodayContent(
            state = state,
            workout = state.workout,
            onAction = onAction,
            modifier = modifier,
        )
    }
}

@Composable
private fun TodayContent(
    state: TodayWorkoutState,
    workout: WorkoutSummaryUi,
    onAction: (TodayWorkoutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = workout.dateLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            HeroCard(
                state = state,
                workout = workout,
                onAction = onAction,
            )
        }
        item {
            Text(
                text = "The line-up",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        itemsIndexed(workout.exercises) { index, exercise ->
            ExerciseRow(position = index + 1, exercise = exercise)
        }
    }
}

@Composable
private fun HeroCard(
    state: TodayWorkoutState,
    workout: WorkoutSummaryUi,
    onAction: (TodayWorkoutAction) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = when {
                    state.hasActiveSession -> "Workout in progress"
                    state.isAutoCompleted -> "Done for today!"
                    else -> "Your daily workout"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (state.completedCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.completedCount == 1) {
                            "1 workout completed today"
                        } else {
                            "${state.completedCount} workouts completed today"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    icon = Icons.Rounded.Timer,
                    value = "${workout.estimatedMinutes}",
                    label = "minutes",
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    icon = Icons.Rounded.FitnessCenter,
                    value = "${workout.exerciseCount}",
                    label = "exercises",
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    value = workout.difficultyLabel,
                    label = "difficulty",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryActions(state = state, onAction = onAction)
            state.actionError?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PrimaryActions(
    state: TodayWorkoutState,
    onAction: (TodayWorkoutAction) -> Unit,
) {
    val showBonusOnly = state.isAutoCompleted && !state.hasActiveSession
    if (showBonusOnly) {
        OutlinedButton(
            onClick = { onAction(TodayWorkoutAction.OnStartBonusClick) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !state.isStartingWorkout,
        ) {
            ButtonContent(
                isBusy = state.isStartingWorkout,
                icon = Icons.Rounded.PlayArrow,
                text = "Start a bonus workout",
            )
        }
    } else {
        Button(
            onClick = { onAction(TodayWorkoutAction.OnPrimaryClick) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !state.isStartingWorkout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            ButtonContent(
                isBusy = state.isStartingWorkout,
                icon = Icons.Rounded.PlayArrow,
                text = if (state.hasActiveSession) "Resume workout" else "Start workout",
            )
        }
    }
}

@Composable
private fun ButtonContent(
    isBusy: Boolean,
    icon: ImageVector,
    text: String,
) {
    if (isBusy) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
        )
    } else {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ExerciseRow(
    position: Int,
    exercise: WorkoutExercisePreviewUi,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$position",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (exercise.isUnilateral) {
                    Text(
                        text = "Both sides · switch halfway",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${exercise.workSeconds}s",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview
@Composable
private fun TodayWorkoutScreenPreview() {
    WorkoutEliteTheme {
        TodayWorkoutScreen(
            state = TodayWorkoutState(
                isLoading = false,
                workout = WorkoutSummaryUi(
                    dateLabel = "Wednesday, July 2",
                    exerciseCount = 12,
                    estimatedMinutes = 15,
                    difficultyLabel = "3.2",
                    exercises = listOf(
                        WorkoutExercisePreviewUi("Bodyweight Squat", 45, isUnilateral = false),
                        WorkoutExercisePreviewUi("Reverse Lunge", 45, isUnilateral = true),
                        WorkoutExercisePreviewUi("Push-up", 45, isUnilateral = false),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
