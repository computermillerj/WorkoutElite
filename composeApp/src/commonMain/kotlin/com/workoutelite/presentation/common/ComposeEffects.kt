package com.workoutelite.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveAsEvents(
    events: Flow<T>,
    onEvent: (T) -> Unit,
) {
    LaunchedEffect(events) {
        events.collect { event -> onEvent(event) }
    }
}

/** Invokes [onResumed] every time the host lifecycle reaches RESUMED, including the first time. */
@Composable
fun OnResumed(onResumed: () -> Unit) {
    OnLifecycleEvent(targetEvent = Lifecycle.Event.ON_RESUME, onEvent = onResumed)
}

/** Invokes [onStopped] when the host lifecycle stops, e.g. the app moves to the background. */
@Composable
fun OnStopped(onStopped: () -> Unit) {
    OnLifecycleEvent(targetEvent = Lifecycle.Event.ON_STOP, onEvent = onStopped)
}

@Composable
private fun OnLifecycleEvent(
    targetEvent: Lifecycle.Event,
    onEvent: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    DisposableEffect(lifecycleOwner, targetEvent) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == targetEvent) currentOnEvent()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
