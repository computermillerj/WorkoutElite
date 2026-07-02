package com.workoutelite.platform

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ClockProvider {
    fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun todayString(): String = Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
