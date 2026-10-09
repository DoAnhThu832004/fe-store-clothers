package org.example.project.domain.model

/**
 * Domain entity đại diện cho người dùng đã đăng nhập trong hệ thống.
 * Không phụ thuộc vào DTO hay bất kỳ framework nào.
 */
data class User(
    val id: Long = 0L,
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val roles: List<String> = emptyList()
)
