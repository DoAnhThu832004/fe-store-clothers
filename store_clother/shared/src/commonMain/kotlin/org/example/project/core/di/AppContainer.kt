package org.example.project.core.di

import org.example.project.domain.repository.AuthRepository
import org.example.project.domain.repository.SettingsRepository
import org.example.project.domain.usecase.CheckAuthStatusUseCase
import org.example.project.domain.usecase.ForgotPasswordUseCase
import org.example.project.domain.usecase.IntroStatusUseCase
import org.example.project.domain.usecase.LoginUseCase
import org.example.project.domain.usecase.RegisterTenantUseCase
import org.example.project.domain.usecase.ResetPasswordUseCase

/**
 * Dependency Injection Container Interface cho ứng dụng KMP.
 * Cung cấp toàn bộ Repositories và Use Cases của Domain Layer.
 */
interface AppContainer {
    val authRepository: AuthRepository
    val settingsRepository: SettingsRepository

    val loginUseCase: LoginUseCase
    val registerTenantUseCase: RegisterTenantUseCase
    val forgotPasswordUseCase: ForgotPasswordUseCase
    val resetPasswordUseCase: ResetPasswordUseCase
    val checkAuthStatusUseCase: CheckAuthStatusUseCase
    val introStatusUseCase: IntroStatusUseCase
}
