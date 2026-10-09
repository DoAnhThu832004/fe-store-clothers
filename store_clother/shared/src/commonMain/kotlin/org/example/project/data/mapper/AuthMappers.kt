package org.example.project.data.mapper

import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.data.remote.dto.RegisterResponseDto
import org.example.project.data.remote.dto.UserInfoDto
import org.example.project.domain.model.RegisterTenantParams
import org.example.project.domain.model.RegisterTenantResult
import org.example.project.domain.model.User

/**
 * Mappers chuyển đổi giữa Data Layer (DTOs) và Domain Layer (Entities/Models).
 */
fun UserInfoDto.toDomain(): User = User(
    id = this.id,
    username = this.username,
    fullName = this.fullName,
    email = this.email,
    roles = this.roles
)

fun User.toDto(): UserInfoDto = UserInfoDto(
    id = this.id,
    username = this.username,
    fullName = this.fullName,
    email = this.email,
    roles = this.roles
)

fun RegisterTenantParams.toDto(): RegisterRequestDto = RegisterRequestDto(
    storeName = this.storeName,
    storeCode = this.storeCode,
    contactEmail = this.contactEmail,
    contactPhone = this.contactPhone,
    ownerUsername = this.ownerUsername,
    ownerPassword = this.ownerPassword,
    ownerFullName = this.ownerFullName,
    ownerEmail = this.ownerEmail,
    ownerPhone = this.ownerPhone
)

fun RegisterResponseDto.toDomain(): RegisterTenantResult = RegisterTenantResult(
    success = true,
    message = this.message
)
