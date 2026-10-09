package org.example.project.datasource

import org.example.project.data.local.datasource.PreferencesLocalDataSource
import org.example.project.local.AppPreferences

/**
 * Android implementation của [PreferencesLocalDataSource] sử dụng [AppPreferences] (SharedPreferences).
 */
class AndroidPreferencesLocalDataSource(
    private val appPreferences: AppPreferences
) : PreferencesLocalDataSource {

    override fun hasSeenIntro(): Boolean = appPreferences.hasSeenIntro()

    override fun markIntroSeen() = appPreferences.markIntroSeen()

    override fun clearAll() = appPreferences.clearAll()
}
