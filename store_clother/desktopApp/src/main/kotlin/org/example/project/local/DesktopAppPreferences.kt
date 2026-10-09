package org.example.project.local

import java.util.prefs.Preferences

/**
 * Quản lý UI Preferences trên Desktop (hasSeenIntro, v.v.).
 */
class DesktopAppPreferences {

    private val prefs: Preferences = Preferences.userNodeForPackage(DesktopAppPreferences::class.java)

    companion object {
        private const val KEY_INTRO_SEEN = "intro_seen"
    }

    fun hasSeenIntro(): Boolean = prefs.getBoolean(KEY_INTRO_SEEN, false)

    fun markIntroSeen() {
        prefs.putBoolean(KEY_INTRO_SEEN, true)
        prefs.flush()
    }

    fun clearAll() {
        prefs.clear()
        prefs.flush()
    }
}
