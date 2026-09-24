package org.example.project.data.remote.dto

data class RegisterRequestDto(
    val storeName: String,
    val storeCode: String,
    val contactEmail: String,
    val contactPhone: String,
    val ownerUsername: String,
    val ownerPassword: String,
    val ownerFullName: String,
    val ownerEmail: String,
    val ownerPhone: String
)
data class RegisterResponseDto(
    val storeName: String,
    val storeCode: String,
    val ownerUsername: String,
    val message: String
)
data class ErrorResponse(
    val success: Boolean,
    val error: ErrorDetail?,
    val timestamp: String?
)

data class ErrorDetail(
    val code: String,
    val message: String
)
