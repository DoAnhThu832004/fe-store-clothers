package org.example.project.datasource

import org.example.project.data.local.datasource.PreferencesLocalDataSource
import org.example.project.local.DesktopAppPreferences

/**
 * Desktop implementation của [PreferencesLocalDataSource] sử dụng [DesktopAppPreferences].
 */
class DesktopPreferencesLocalDataSource(
    private val appPreferences: DesktopAppPreferences
) : PreferencesLocalDataSource {

    override fun hasSeenIntro(): Boolean = appPreferences.hasSeenIntro()

    override fun markIntroSeen() = appPreferences.markIntroSeen()

    override fun clearAll() = appPreferences.clearAll()
}
