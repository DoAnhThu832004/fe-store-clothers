package org.example.project.data.remote.dto

/**
 * Request body gửi lên API login.
 */
data class LoginRequestDto(
    val storeCode: String,
    val username: String,
    val password: String
)
