package com.workoutelite.presentation.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutelite.domain.workout.DifficultyFeedback
import com.workoutelite.platform.KeepScreenOn
import com.workoutelite.presentation.common.ObserveAsEvents
import com.workoutelite.presentation.common.OnStopped
import com.workoutelite.ui.components.LoadingState
import com.workoutelite.ui.theme.WorkoutEliteTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ActiveWorkoutRoot(
    onNavigateBack: () -> Unit,
    viewModel: ActiveWorkoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KeepScreenOn(active = !state.isComplete && state.error == null)
    OnStopped { viewModel.onAction(ActiveWorkoutAction.OnBackgrounded) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ActiveWorkoutEvent.NavigateBack -> onNavigateBack()
        }
    }

    // BackHandler is deprecated in favor of NavigationEventHandler, but it remains the only
    // dependency-free way to intercept back in common code on this Compose version.
    @Suppress("DEPRECATION")
    BackHandler {
        when {
            state.error != null -> onNavigateBack()
            state.isComplete -> viewModel.onAction(ActiveWorkoutAction.OnSkipFeedbackClick)
            else -> viewModel.onAction(ActiveWorkoutAction.OnQuitClick)
        }
    }

    ActiveWorkoutScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutState,
    onAction: (ActiveWorkoutAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            state.isComplete -> MaterialTheme.colorScheme.surface
            state.phase == WorkoutPhaseUi.REST -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 400),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when {
            state.isLoading -> LoadingState()
            state.error != null -> NoWorkoutContent(
                message = state.error,
                onBackClick = onNavigateBack,
            )
            state.isComplete -> CompleteContent(state = state, onAction = onAction)
            else -> TimerContent(state = state, onAction = onAction)
        }
        if (state.showQuitConfirmation) {
            QuitConfirmationDialog(onAction = onAction)
        }
    }
}

@Composable
private fun TimerContent(
    state: ActiveWorkoutState,
    onAction: (ActiveWorkoutAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WorkoutProgressHeader(state = state)
        Spacer(modifier = Modifier.height(24.dp))
        PhaseBadge(phase = state.phase, isPaused = state.isPaused)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = state.exerciseName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (state.exerciseDescription.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.exerciseDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        CountdownRing(state = state)
        Spacer(modifier = Modifier.height(12.dp))
        SwitchSidesBanner(visible = state.showSwitchSides)
        Spacer(modifier = Modifier.weight(1f))
        state.nextExerciseName?.let { next ->
            Text(
                text = "Up next: $next",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Controls(state = state, onAction = onAction)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WorkoutProgressHeader(state: ActiveWorkoutState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Exercise ${state.itemPosition} of ${state.itemCount}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (state.itemPosition - 1f) / state.itemCount.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun PhaseBadge(
    phase: WorkoutPhaseUi,
    isPaused: Boolean,
) {
    val (label, container, content) = when {
        isPaused -> Triple(
            "PAUSED",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        phase == WorkoutPhaseUi.PREPARE -> Triple(
            "GET READY",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        phase == WorkoutPhaseUi.WORK -> Triple(
            "WORK",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        else -> Triple(
            "REST",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CountdownRing(state: ActiveWorkoutState) {
    // The timer state ticks once per second; a linear tween of exactly one second between
    // targets keeps the ring in continuous motion instead of stepping and pausing.
    val targetProgress = state.remainingSeconds / state.phaseTotalSeconds.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
    )
    val ringColor = when (state.phase) {
        WorkoutPhaseUi.PREPARE -> MaterialTheme.colorScheme.tertiary
        WorkoutPhaseUi.WORK -> MaterialTheme.colorScheme.primary
        WorkoutPhaseUi.REST -> MaterialTheme.colorScheme.secondary
    }

    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(240.dp),
            color = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 12.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.remainingSeconds.toTimerText(),
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "of ${state.phaseTotalSeconds.toTimerText()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchSidesBanner(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                text = "Switch sides",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Controls(
    state: ActiveWorkoutState,
    onAction: (ActiveWorkoutAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        OutlinedIconButton(
            onClick = { onAction(ActiveWorkoutAction.OnQuitClick) },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Quit workout",
            )
        }
        FilledIconButton(
            onClick = { onAction(ActiveWorkoutAction.OnPauseResumeClick) },
            modifier = Modifier.size(80.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            AnimatedContent(
                targetState = state.isPaused,
                transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
            ) { isPaused ->
                Icon(
                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        OutlinedIconButton(
            onClick = { onAction(ActiveWorkoutAction.OnSkipClick) },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Skip",
            )
        }
    }
}

@Composable
private fun CompleteContent(
    state: ActiveWorkoutState,
    onAction: (ActiveWorkoutAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Celebration,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Workout complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "About ${state.completedMinutes} min of movement. Nice work.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "How did it feel?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeedbackButton(
            icon = Icons.Rounded.SentimentVerySatisfied,
            title = "Easy",
            subtitle = "Push me harder next time",
            enabled = !state.isSavingFeedback,
            onClick = { onAction(ActiveWorkoutAction.OnFeedbackClick(DifficultyFeedback.EASY)) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeedbackButton(
            icon = Icons.Rounded.SentimentSatisfied,
            title = "Just right",
            subtitle = "Keep it at this level",
            enabled = !state.isSavingFeedback,
            onClick = { onAction(ActiveWorkoutAction.OnFeedbackClick(DifficultyFeedback.MEDIUM)) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeedbackButton(
            icon = Icons.Rounded.SentimentDissatisfied,
            title = "Hard",
            subtitle = "Ease off a little",
            enabled = !state.isSavingFeedback,
            onClick = { onAction(ActiveWorkoutAction.OnFeedbackClick(DifficultyFeedback.HARD)) },
        )
        state.feedbackError?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { onAction(ActiveWorkoutAction.OnSkipFeedbackClick) },
            enabled = !state.isSavingFeedback,
        ) {
            Text(text = "Skip")
        }
    }
}

@Composable
private fun FeedbackButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoWorkoutContent(
    message: String,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBackClick) {
            Text(text = "Back to Today")
        }
    }
}

@Composable
private fun QuitConfirmationDialog(onAction: (ActiveWorkoutAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(ActiveWorkoutAction.OnDismissQuitClick) },
        title = { Text(text = "Quit workout?") },
        text = { Text(text = "This workout will not be marked complete. You can start it again later today.") },
        confirmButton = {
            Button(onClick = { onAction(ActiveWorkoutAction.OnConfirmQuitClick) }) {
                Text(text = "Quit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onAction(ActiveWorkoutAction.OnDismissQuitClick) }) {
                Text(text = "Keep going")
            }
        },
    )
}

private fun Int.toTimerText(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Preview
@Composable
private fun ActiveWorkoutScreenPreview() {
    WorkoutEliteTheme {
        ActiveWorkoutScreen(
            state = ActiveWorkoutState(
                isLoading = false,
                phase = WorkoutPhaseUi.WORK,
                exerciseName = "Reverse Lunge",
                exerciseDescription = "Step backward, lower both knees, then push through the front foot.",
                remainingSeconds = 27,
                phaseTotalSeconds = 45,
                itemPosition = 3,
                itemCount = 12,
                nextExerciseName = "Plank",
                showSwitchSides = true,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun WorkoutCompleteScreenPreview() {
    WorkoutEliteTheme {
        ActiveWorkoutScreen(
            state = ActiveWorkoutState(
                isLoading = false,
                isComplete = true,
                completedMinutes = 15,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
