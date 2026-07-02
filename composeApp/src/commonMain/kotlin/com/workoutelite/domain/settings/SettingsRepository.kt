package com.workoutelite.domain.settings

interface SettingsRepository {
    suspend fun getTargetDurationMinutes(): Int
    suspend fun setTargetDurationMinutes(minutes: Int)

    companion object {
        const val DEFAULT_DURATION_MINUTES = 15
        const val MIN_DURATION_MINUTES = 5
        const val MAX_DURATION_MINUTES = 60
    }
}
