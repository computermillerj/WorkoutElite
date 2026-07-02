package com.workoutelite

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workoutelite.presentation.calendar.CalendarRoot
import com.workoutelite.presentation.exercises.ExerciseListRoot
import com.workoutelite.presentation.workout.ActiveWorkoutRoot
import com.workoutelite.presentation.workout.TodayWorkoutRoot
import com.workoutelite.ui.theme.WorkoutEliteTheme

@Composable
fun WorkoutEliteApp() {
    WorkoutEliteTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val showBottomBar = currentDestination == null ||
            TopLevelDestination.entries.any { it.matches(currentDestination) }

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
                ) {
                    AppBottomBar(
                        currentDestination = currentDestination,
                        onDestinationClick = { navController.navigateToTopLevel(it) },
                    )
                }
            },
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = TodayRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
            ) {
                composable<TodayRoute> {
                    TodayWorkoutRoot(
                        onNavigateToActiveWorkout = {
                            navController.navigate(ActiveWorkoutRoute) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(paddingValues),
                    )
                }
                composable<CalendarRoute> {
                    CalendarRoot(modifier = Modifier.padding(paddingValues))
                }
                composable<ExercisesRoute> {
                    ExerciseListRoot(modifier = Modifier.padding(paddingValues))
                }
                composable<ActiveWorkoutRoute>(
                    enterTransition = {
                        slideInVertically(tween(260)) { fullHeight -> fullHeight } + fadeIn(tween(260))
                    },
                    exitTransition = {
                        slideOutVertically(tween(220)) { fullHeight -> fullHeight } + fadeOut(tween(220))
                    },
                ) {
                    ActiveWorkoutRoot(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}

private enum class TopLevelDestination(
    val destination: Any,
    val label: String,
    val icon: ImageVector,
    val matches: (NavDestination) -> Boolean,
) {
    TODAY(TodayRoute, "Today", Icons.Rounded.FitnessCenter, { it.hasRoute<TodayRoute>() }),
    CALENDAR(CalendarRoute, "History", Icons.Rounded.CalendarMonth, { it.hasRoute<CalendarRoute>() }),
    EXERCISES(ExercisesRoute, "Exercises", Icons.Rounded.Tune, { it.hasRoute<ExercisesRoute>() }),
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppBottomBar(
    currentDestination: NavDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination?.let(destination.matches) == true,
                onClick = { onDestinationClick(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(text = destination.label) },
            )
        }
    }
}
