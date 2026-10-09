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

    private val STORE_CODE_REGEX = Regex("""^[a-z0-9]+(-[a-z0-9]+)*$""")
    private val USERNAME_REGEX = Regex("""^[a-z0-9_]+$""")
    private val PHONE_REGEX = Regex("""^(\+84|0)[0-9]{9}$""")

    /**
     * Kiểm tra format email hợp lệ.
     * @return true nếu email đúng format
     */
    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /**
     * Kiểm tra mã cửa hàng hợp lệ (3-100 ký tự, chữ thường, số, dấu gạch ngang).
     */
    fun isValidStoreCode(storeCode: String): Boolean {
        val trimmed = storeCode.trim()
        return trimmed.length in 3..100 && STORE_CODE_REGEX.matches(trimmed)
    }

    /**
     * Kiểm tra username hợp lệ (4-50 ký tự, chữ thường, số, gạch dưới).
     */
    fun isValidUsername(username: String): Boolean {
        val trimmed = username.trim()
        return trimmed.length in 4..50 && USERNAME_REGEX.matches(trimmed)
    }

    /**
     * Kiểm tra số điện thoại (đầu 0 hoặc +84, tổng 10 số).
     */
    fun isValidPhone(phone: String): Boolean = PHONE_REGEX.matches(phone.trim())

    /**
     * Kiểm tra độ dài mật khẩu tối thiểu 8 ký tự.
     */
    fun isValidPasswordLength(password: String): Boolean = password.length >= 8

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
