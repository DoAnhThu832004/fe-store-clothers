package org.example.project.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Lưu trạng thái UI không nhạy cảm (non-sensitive) của ứng dụng.
 *
 * Tách biệt khỏi [SessionManager] (dùng EncryptedSharedPreferences cho token)
 * vì dữ liệu ở đây không cần mã hoá — áp dụng đúng nguyên tắc:
 * "Don't over-engineer security for non-sensitive data."
 *
 * Hiện quản lý:
 * - [hasSeenIntro]: cờ đánh dấu user đã xem màn hình giới thiệu chưa
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    // ─── Intro / Onboarding ──────────────────────────────────────────────────

    /**
     * Kiểm tra user đã từng xem màn hình giới thiệu (SplashScreen/Intro) chưa.
     *
     * - Lần đầu cài app → trả về `false` → hiển thị Intro
     * - Các lần sau → trả về `true` → bỏ qua Intro
     */
    fun hasSeenIntro(): Boolean =
        prefs.getBoolean(KEY_HAS_SEEN_INTRO, false)

    /**
     * Đánh dấu user đã xem xong Intro.
     * Gọi ngay khi user nhấn bất kỳ nút nào trên SplashScreen.
     */
    fun markIntroSeen() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_INTRO, true).apply()
    }

    // ─── (Mở rộng sau) Reset — dùng khi uninstall/clear data ────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_FILE_NAME  = "store_clother_app_prefs"
        private const val KEY_HAS_SEEN_INTRO = "has_seen_intro"
    }
}
