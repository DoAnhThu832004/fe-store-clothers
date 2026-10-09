package org.example.project.data.local.datasource

/**
 * Interface cho Local Data Source lưu trữ UI preferences và settings không nhạy cảm.
 */
interface PreferencesLocalDataSource {

    fun hasSeenIntro(): Boolean

    fun markIntroSeen()

    fun clearAll()
}
