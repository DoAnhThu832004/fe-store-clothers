package org.example.project.data.repository

import kotlinx.coroutines.withContext
import org.example.project.core.common.DefaultDispatcherProvider
import org.example.project.core.common.DispatcherProvider
import org.example.project.core.common.Resource
import org.example.project.data.local.datasource.SessionLocalDataSource
import org.example.project.data.mapper.toDomain
import org.example.project.data.mapper.toDto
import org.example.project.data.remote.datasource.AuthRemoteDataSource
import org.example.project.data.remote.dto.ForgotPasswordRequestDto
import org.example.project.data.remote.dto.LoginRequestDto
import org.example.project.data.remote.dto.ResetPasswordRequestDto
import org.example.project.domain.model.RegisterTenantParams
import org.example.project.domain.model.RegisterTenantResult
import org.example.project.domain.model.User
import org.example.project.domain.repository.AuthRepository

/**
 * Implementation của [AuthRepository] trong Clean Architecture.
 * Phối hợp giữa [AuthRemoteDataSource] và [SessionLocalDataSource],
 * ánh xạ DTOs sang Domain models và bảo đảm nguyên tắc Single Source of Truth.
 */
class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionDataSource: SessionLocalDataSource,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : AuthRepository {

    override suspend fun login(
        storeCode: String,
        username: String,
        password: String
    ): Resource<User> = withContext(dispatcherProvider.io) {
        val request = LoginRequestDto(
            storeCode = storeCode,
            username = username,
            password = password
        )
        when (val result = remoteDataSource.login(request)) {
            is Resource.Success -> {
                val authData = result.data
                val user = authData.userInfo.toDomain()
                sessionDataSource.saveSession(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    user = user
                )
                Resource.Success(user)
            }
            is Resource.Error -> Resource.Error(result.throwable)
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun registerTenant(params: RegisterTenantParams): Resource<RegisterTenantResult> =
        withContext(dispatcherProvider.io) {
            val requestDto = params.toDto()
            when (val result = remoteDataSource.registerTenant(requestDto)) {
                is Resource.Success -> Resource.Success(result.data.toDomain())
                is Resource.Error -> Resource.Error(result.throwable)
                Resource.Loading -> Resource.Loading
            }
        }

    override suspend fun forgotPassword(email: String, storeCode: String): Resource<String> =
        withContext(dispatcherProvider.io) {
            val request = ForgotPasswordRequestDto(email = email, storeCode = storeCode)
            remoteDataSource.forgotPassword(request)
        }

    override suspend fun resetPassword(
        token: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<Unit> = withContext(dispatcherProvider.io) {
        val request = ResetPasswordRequestDto(
            token = token,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )
        when (val result = remoteDataSource.resetPassword(request)) {
            is Resource.Success -> {
                sessionDataSource.clearSession()
                Resource.Success(Unit)
            }
            is Resource.Error -> Resource.Error(result.throwable)
            Resource.Loading -> Resource.Loading
        }
    }

    override fun isLoggedIn(): Boolean = sessionDataSource.isLoggedIn()

    override fun getUserRoles(): List<String> = sessionDataSource.getUserRoles()

    override fun getCurrentUser(): User? = sessionDataSource.getUser()

    override fun clearSession() = sessionDataSource.clearSession()
}
