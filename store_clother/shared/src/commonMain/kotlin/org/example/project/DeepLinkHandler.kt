package org.example.project

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton bridge để forward Deep Link URL từ iOS Swift → Compose NavHost.
 *
 * ### Cơ chế hoạt động:
 * 1. iOS `ContentView.swift` nhận URL qua `.onOpenURL { url in ... }`
 * 2. Swift gọi `DeepLinkHandler.handleDeepLink(urlString: url.absoluteString)`
 * 3. Hàm này emit URL vào [deepLinkFlow]
 * 4. NavHost (trong `iosMain/MainViewController.kt`) collect [deepLinkFlow]
 *    và navigate đến đúng destination
 *
 * ### Supported URL patterns:
 * - `storeclothes://reset-password?token={token}` — Custom Scheme
 * - `https://yourdomain.com/reset-password?token={token}` — Universal Link
 *
 * @note Đặt ở `commonMain` để cả iosMain và androidMain có thể import.
 *       Trên Android, Deep Link được xử lý trực tiếp bởi Navigation Compose
 *       qua `navDeepLink {}` trong NavHost — không cần dùng handler này.
 */
object DeepLinkHandler {

    private val _deepLinkFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        replay = 1  // replay = 1 để không mất URL nếu NavHost chưa kịp subscribe
    )

    /** Flow emitting deep link URLs khi app được mở qua link */
    val deepLinkFlow: SharedFlow<String> = _deepLinkFlow.asSharedFlow()

    /**
     * Được gọi từ iOS Swift khi nhận URL qua onOpenURL/Universal Link.
     *
     * Tên hàm và parameter phải khớp với Swift call site:
     * `DeepLinkHandlerKt.handleDeepLink(urlString: url.absoluteString)`
     */
    fun handleDeepLink(urlString: String) {
        _deepLinkFlow.tryEmit(urlString)
    }
}
