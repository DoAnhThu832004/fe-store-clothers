package org.example.project.domain.validator

/**
 * Kết quả kiểm tra độ mạnh mật khẩu — mỗi tiêu chí là một flag riêng biệt.
 *
 * Được dùng để render **Realtime Password Checklist** trên [ResetPasswordScreen]:
 * mỗi điều kiện đổi màu xanh ngay khi người dùng gõ đạt điều kiện đó.
 *
 * Regex gốc của backend (khớp 100%):
 * `^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!])(?=\S+$).{8,}$`
 */
data class PasswordStrength(
    /** Độ dài >= 8 ký tự */
    val hasMinLength: Boolean = false,
    /** Có ít nhất 1 chữ hoa (A-Z) */
    val hasUppercase: Boolean = false,
    /** Có ít nhất 1 chữ số (0-9) */
    val hasDigit: Boolean = false,
    /** Có ít nhất 1 ký tự đặc biệt trong bộ [@#$%^&+=!] */
    val hasSpecialChar: Boolean = false,
    /** Không chứa khoảng trắng */
    val hasNoWhitespace: Boolean = false
) {
    /** Mật khẩu đạt TẤT CẢ tiêu chí — khớp 100% với Regex backend */
    val isValid: Boolean
        get() = hasMinLength && hasUppercase && hasDigit && hasSpecialChar && hasNoWhitespace
}

/**
 * Validator tập trung cho tầng domain.
 *
 * Không depend vào Android/iOS SDK — thuần Kotlin, testable trong commonTest.
 */
object AuthValidator {

    // Regex mật khẩu khớp 100% với backend Spring Boot
    private val PASSWORD_REGEX = Regex("""^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#${'$'}%^&+=!])(?=\S+${'$'}).{8,}${'$'}""")

    // Regex email cơ bản — kiểm tra format hợp lệ
    private val EMAIL_REGEX = Regex("""^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}${'$'}""")

    /**
     * Kiểm tra format email hợp lệ.
     * @return true nếu email đúng format
     */
    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /**
     * Kiểm tra mật khẩu khớp với Regex của backend.
     * Dùng cho validation cuối cùng trước khi submit.
     */
    fun isValidPassword(password: String): Boolean = PASSWORD_REGEX.matches(password)

    /**
     * Phân tích mật khẩu thành từng tiêu chí riêng lẻ.
     * Dùng để render Realtime Checklist trên UI — cập nhật mỗi lần user gõ 1 ký tự.
     */
    fun analyzePassword(password: String): PasswordStrength = PasswordStrength(
        hasMinLength    = password.length >= 8,
        hasUppercase    = password.any { it.isUpperCase() },
        hasDigit        = password.any { it.isDigit() },
        hasSpecialChar  = password.any { it in "@#\$%^&+=!" },
        hasNoWhitespace = password.none { it.isWhitespace() }
    )

    /**
     * Kiểm tra 2 mật khẩu khớp nhau (dùng cho confirm password field).
     */
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean =
        password == confirmPassword && password.isNotEmpty()
}
