package org.example.project.di

import org.example.project.core.di.AppContainer
import org.example.project.core.di.AppContainerHelper
import org.example.project.data.repository.AuthRepositoryImpl
import org.example.project.data.repository.SettingsRepositoryImpl
import org.example.project.datasource.DesktopAuthRemoteDataSource
import org.example.project.datasource.DesktopPreferencesLocalDataSource
import org.example.project.datasource.DesktopSessionLocalDataSource
import org.example.project.domain.repository.AuthRepository
import org.example.project.domain.repository.SettingsRepository
import org.example.project.domain.usecase.CheckAuthStatusUseCase
import org.example.project.domain.usecase.ForgotPasswordUseCase
import org.example.project.domain.usecase.IntroStatusUseCase
import org.example.project.domain.usecase.LoginUseCase
import org.example.project.domain.usecase.RegisterTenantUseCase
import org.example.project.domain.usecase.ResetPasswordUseCase
import org.example.project.local.DesktopAppPreferences
import org.example.project.local.DesktopSessionManager
import org.example.project.network.DesktopAuthApiService
import org.example.project.network.DesktopRetrofitClient

/**
 * Dependency Injection Container cho Desktop App.
 * Cung cấp kết nối Retrofit trực tiếp đến Backend Spring Boot (localhost:8080)
 * và lưu trữ session/preferences trên Desktop.
 */
class DesktopAppContainer : AppContainer {

    private val sessionManager = DesktopSessionManager()
    private val appPreferences = DesktopAppPreferences()

    init {
        DesktopRetrofitClient.init(sessionManager)
    }

    private val authApiService = DesktopRetrofitClient.createService<DesktopAuthApiService>()

    private val sessionLocalDataSource = DesktopSessionLocalDataSource(sessionManager)
    private val preferencesLocalDataSource = DesktopPreferencesLocalDataSource(appPreferences)
    private val authRemoteDataSource = DesktopAuthRemoteDataSource(authApiService)

    override val authRepository: AuthRepository = AuthRepositoryImpl(
        remoteDataSource = authRemoteDataSource,
        sessionDataSource = sessionLocalDataSource
    )

    override val settingsRepository: SettingsRepository = SettingsRepositoryImpl(
        preferencesDataSource = preferencesLocalDataSource
    )

    override val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    override val registerTenantUseCase: RegisterTenantUseCase by lazy {
        RegisterTenantUseCase(authRepository)
    }

    override val forgotPasswordUseCase: ForgotPasswordUseCase by lazy {
        ForgotPasswordUseCase(authRepository)
    }

    override val resetPasswordUseCase: ResetPasswordUseCase by lazy {
        ResetPasswordUseCase(authRepository)
    }

    override val checkAuthStatusUseCase: CheckAuthStatusUseCase by lazy {
        CheckAuthStatusUseCase(authRepository)
    }

    override val introStatusUseCase: IntroStatusUseCase by lazy {
        IntroStatusUseCase(settingsRepository)
    }

    companion object {
        fun init(): AppContainer {
            val container = DesktopAppContainer()
            AppContainerHelper.init(container)
            return container
        }
    }
}
