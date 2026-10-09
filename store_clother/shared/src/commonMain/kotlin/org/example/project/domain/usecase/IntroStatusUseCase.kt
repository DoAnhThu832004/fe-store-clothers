package org.example.project.domain.usecase

import org.example.project.domain.repository.SettingsRepository

/**
 * UseCase kiểm tra và cập nhật cờ đã xem màn hình Intro/Onboarding.
 */
class IntroStatusUseCase(
    private val settingsRepository: SettingsRepository
) {
    fun hasSeenIntro(): Boolean = settingsRepository.hasSeenIntro()

    fun markIntroSeen() = settingsRepository.markIntroSeen()
}
