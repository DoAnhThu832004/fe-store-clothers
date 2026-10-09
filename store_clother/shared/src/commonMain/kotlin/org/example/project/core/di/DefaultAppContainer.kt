package org.example.project.core.di

import org.example.project.data.local.datasource.InMemoryPreferencesLocalDataSource
import org.example.project.data.local.datasource.InMemorySessionLocalDataSource
import org.example.project.data.remote.datasource.MockAuthRemoteDataSource
import org.example.project.data.repository.AuthRepositoryImpl
import org.example.project.data.repository.SettingsRepositoryImpl
import org.example.project.domain.repository.AuthRepository
import org.example.project.domain.repository.SettingsRepository
import org.example.project.domain.usecase.CheckAuthStatusUseCase
import org.example.project.domain.usecase.ForgotPasswordUseCase
import org.example.project.domain.usecase.IntroStatusUseCase
import org.example.project.domain.usecase.LoginUseCase
import org.example.project.domain.usecase.RegisterTenantUseCase
import org.example.project.domain.usecase.ResetPasswordUseCase

/**
 * Container mặc định cho KMP (Desktop, iOS preview, Web, v.v.).
 * Cung cấp đầy đủ repositories và use cases sẵn sàng hoạt động.
 */
class DefaultAppContainer(
    override val authRepository: AuthRepository = AuthRepositoryImpl(
        remoteDataSource = MockAuthRemoteDataSource(),
        sessionDataSource = InMemorySessionLocalDataSource()
    ),
    override val settingsRepository: SettingsRepository = SettingsRepositoryImpl(
        preferencesDataSource = InMemoryPreferencesLocalDataSource()
    )
) : AppContainer {

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
}
