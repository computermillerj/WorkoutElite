package com.workoutelite.data.workout

import com.workoutelite.data.database.UserSettingsDao
import com.workoutelite.data.database.UserSettingsEntity
import com.workoutelite.domain.settings.SettingsRepository
import com.workoutelite.domain.settings.SettingsRepository.Companion.DEFAULT_DURATION_MINUTES
import com.workoutelite.domain.settings.SettingsRepository.Companion.MAX_DURATION_MINUTES
import com.workoutelite.domain.settings.SettingsRepository.Companion.MIN_DURATION_MINUTES

class RoomSettingsRepository(
    private val settingsDao: UserSettingsDao,
) : SettingsRepository {
    override suspend fun getTargetDurationMinutes(): Int = settingsDao
        .getSettings()
        ?.targetDurationMinutes
        ?.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
        ?: DEFAULT_DURATION_MINUTES

    override suspend fun setTargetDurationMinutes(minutes: Int) {
        settingsDao.upsert(
            UserSettingsEntity(
                targetDurationMinutes = minutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES),
            ),
        )
    }
}
