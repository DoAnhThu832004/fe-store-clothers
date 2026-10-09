package org.example.project.domain.model

/**
 * Domain model kết quả đăng ký tenant.
 */
data class RegisterTenantResult(
    val success: Boolean,
    val message: String
)
