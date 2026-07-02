package com.workoutelite.presentation.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutelite.presentation.common.OnResumed
import com.workoutelite.ui.components.ErrorState
import com.workoutelite.ui.components.LoadingState
import com.workoutelite.ui.theme.WorkoutEliteTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarRoot(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    OnResumed { viewModel.onAction(CalendarAction.OnScreenResumed) }

    CalendarScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> LoadingState(modifier = modifier)
        state.error != null -> ErrorState(
            message = state.error,
            onRetryClick = { onAction(CalendarAction.OnRetryClick) },
            modifier = modifier,
        )
        else -> HistoryContent(
            state = state,
            onAction = onAction,
            modifier = modifier,
        )
    }
}

@Composable
private fun HistoryContent(
    state: CalendarState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = state.activeSessionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PeriodRow(state = state)
        Spacer(modifier = Modifier.height(12.dp))
        StatsGrid(state = state)
        Spacer(modifier = Modifier.height(16.dp))
        MonthCard(state = state, onAction = onAction)
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedContent(
            targetState = state.selectedDay,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
        ) { selectedDay ->
            when {
                selectedDay != null -> DayDetail(day = selectedDay)
                state.totalWorkouts == 0 -> FirstWorkoutHint()
                else -> Text(
                    text = "Tap a highlighted day to see its workouts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PeriodRow(state: CalendarState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PeriodCard(period = state.today, modifier = Modifier.weight(1f))
        PeriodCard(period = state.week, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PeriodCard(
    period: HistoryPeriodUi,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = period.label,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${period.workouts}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = (if (period.workouts == 1) "workout" else "workouts") +
                    " · ${period.minutes} min",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun StatsGrid(state: CalendarState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Rounded.FitnessCenter,
                value = "${state.totalWorkouts}",
                label = if (state.totalWorkouts == 1) "workout" else "workouts",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Rounded.LocalFireDepartment,
                value = "${state.streakDays}",
                label = "day streak",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Rounded.Timer,
                value = "${state.averageMinutes}",
                label = "avg minutes",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                value = state.rollingDifficulty,
                label = "difficulty",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthCard(
    state: CalendarState,
    onAction: (CalendarAction) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAction(CalendarAction.OnPreviousMonthClick) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                    )
                }
                Text(
                    text = state.monthLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = { onAction(CalendarAction.OnNextMonthClick) },
                    enabled = state.canGoToNextMonth,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next month",
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_LABELS.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            state.weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        DayCell(
                            cell = cell,
                            onClick = { date -> onAction(CalendarAction.OnDayClick(date)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarCellUi?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (cell == null) return@Box
        val background = when {
            cell.isCompleted && cell.isSelected -> MaterialTheme.colorScheme.primary
            cell.isCompleted -> MaterialTheme.colorScheme.primaryContainer
            cell.isSelected -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        }
        val contentColor = when {
            cell.isCompleted && cell.isSelected -> MaterialTheme.colorScheme.onPrimary
            cell.isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
            cell.isFuture -> MaterialTheme.colorScheme.outlineVariant
            else -> MaterialTheme.colorScheme.onSurface
        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = cell.isCompleted) { onClick(cell.date) },
            shape = CircleShape,
            color = background,
            contentColor = contentColor,
            border = if (cell.isToday) {
                BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${cell.dayNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (cell.isCompleted) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (cell.workoutCount > 1) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(color = contentColor, shape = CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetail(day: WorkoutDayUi) {
    Column {
        Text(
            text = day.dateLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        day.workouts.forEach { workout ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = workout.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${workout.feedbackLabel} · ${workout.durationMinutes} min",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (workout.exerciseNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = workout.exerciseNames.joinToString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstWorkoutHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.EventAvailable,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No workouts yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Complete your first workout and it will light up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val WEEKDAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Preview
@Composable
private fun CalendarScreenPreview() {
    WorkoutEliteTheme {
        CalendarScreen(
            state = CalendarState(
                isLoading = false,
                monthLabel = "July 2026",
                today = HistoryPeriodUi("Today", workouts = 1, minutes = 14),
                week = HistoryPeriodUi("This week", workouts = 4, minutes = 58),
                totalWorkouts = 8,
                averageMinutes = 14,
                streakDays = 3,
                rollingDifficulty = "3.2",
                weeks = listOf(
                    listOf(null, null, CalendarCellUi("2026-07-01", 1, false, false, false, 1)) +
                        (2..5).map { CalendarCellUi("2026-07-0$it", it, it == 2, it > 2, false, if (it == 2) 2 else 0) },
                ),
                selectedDay = WorkoutDayUi(
                    date = "2026-07-01",
                    dateLabel = "Wednesday, July 1",
                    workouts = listOf(
                        CompletedWorkoutUi("Daily workout", "Just right", 15, listOf("Squat", "Plank", "Burpee")),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
