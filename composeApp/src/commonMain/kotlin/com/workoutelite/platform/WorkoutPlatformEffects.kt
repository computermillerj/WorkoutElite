package com.workoutelite.platform

import androidx.compose.runtime.Composable

@Composable
expect fun KeepScreenOn(active: Boolean)

interface WorkoutCuePlayer {
    fun playIntervalCue()
    fun playHalfwayCue()
    fun playCompleteCue()
}
