package org.example.project.di

import android.content.Context
import org.example.project.core.di.AppContainer
import org.example.project.core.di.AppContainerHelper
import org.example.project.data.repository.AuthRepositoryImpl
import org.example.project.data.repository.SettingsRepositoryImpl
import org.example.project.datasource.AndroidAuthRemoteDataSource
import org.example.project.datasource.AndroidPreferencesLocalDataSource
import org.example.project.datasource.AndroidSessionLocalDataSource
import org.example.project.domain.repository.AuthRepository
import org.example.project.domain.repository.SettingsRepository
import org.example.project.domain.usecase.CheckAuthStatusUseCase
import org.example.project.domain.usecase.ForgotPasswordUseCase
import org.example.project.domain.usecase.IntroStatusUseCase
import org.example.project.domain.usecase.LoginUseCase
import org.example.project.domain.usecase.RegisterTenantUseCase
import org.example.project.domain.usecase.ResetPasswordUseCase
import org.example.project.local.AppPreferences
import org.example.project.local.SessionManager
import org.example.project.network.AuthApiService
import org.example.project.network.RetrofitClient

/**
 * Dependency Injection Container cho Android App.
 * Cung cấp các implementation Android (Retrofit, EncryptedSharedPreferences)
 * cho Core [AppContainer] của KMP Clean Architecture.
 */
class AndroidAppContainer(
    context: Context
) : AppContainer {

    private val sessionManager = SessionManager(context)
    private val appPreferences = AppPreferences(context)

    init {
        RetrofitClient.init(sessionManager)
    }

    private val authApiService = RetrofitClient.createService<AuthApiService>()

    private val sessionLocalDataSource = AndroidSessionLocalDataSource(sessionManager)
    private val preferencesLocalDataSource = AndroidPreferencesLocalDataSource(appPreferences)
    private val authRemoteDataSource = AndroidAuthRemoteDataSource(authApiService)

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
        fun init(context: Context): AppContainer {
            val container = AndroidAppContainer(context)
            AppContainerHelper.init(container)
            return container
        }
    }
}
