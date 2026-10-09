package org.example.project.domain.repository

/**
 * Domain repository contract quản lý cài đặt / tùy chọn người dùng và UI state (intro, flags, v.v.).
 */
interface SettingsRepository {
    fun hasSeenIntro(): Boolean
    fun markIntroSeen()
    fun clearAll()
}
