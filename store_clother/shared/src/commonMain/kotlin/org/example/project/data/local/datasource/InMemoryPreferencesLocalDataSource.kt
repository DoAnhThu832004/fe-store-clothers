package org.example.project.data.local.datasource

/**
 * Implementation lưu trữ Preferences trong bộ nhớ.
 * Dùng làm fallback mặc định cho KMP Desktop, iOS preview, Web và Unit Tests.
 */
class InMemoryPreferencesLocalDataSource : PreferencesLocalDataSource {

    private var seenIntro: Boolean = false

    override fun hasSeenIntro(): Boolean = seenIntro

    override fun markIntroSeen() {
        seenIntro = true
    }

    override fun clearAll() {
        seenIntro = false
    }
}
