package org.example.project.data.repository

import org.example.project.data.local.datasource.PreferencesLocalDataSource
import org.example.project.domain.repository.SettingsRepository

/**
 * Implementation của [SettingsRepository] trong Clean Architecture.
 */
class SettingsRepositoryImpl(
    private val preferencesDataSource: PreferencesLocalDataSource
) : SettingsRepository {

    override fun hasSeenIntro(): Boolean = preferencesDataSource.hasSeenIntro()

    override fun markIntroSeen() = preferencesDataSource.markIntroSeen()

    override fun clearAll() = preferencesDataSource.clearAll()
}
