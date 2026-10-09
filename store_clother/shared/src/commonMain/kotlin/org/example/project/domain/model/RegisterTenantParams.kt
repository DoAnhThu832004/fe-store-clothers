package org.example.project.domain.model

/**
 * Domain model chứa dữ liệu đăng ký cửa hàng (tenant).
 */
data class RegisterTenantParams(
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
