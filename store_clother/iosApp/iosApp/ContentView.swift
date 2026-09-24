import UIKit
import SwiftUI
import Shared

/**
 * ComposeView — bọc Compose Multiplatform ViewController trong SwiftUI.
 *
 * MainViewController() được tạo bởi KMP shared module.
 * Để forward Deep Link URL vào NavHost, chúng ta dùng một static callback
 * mà MainViewController sẽ expose từ phía Kotlin.
 */
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            // ── Deep Link Handler (Universal Link & Custom Scheme) ──────────
            // onOpenURL được iOS gọi khi user tap link trong email.
            // URL được forward sang Kotlin/Compose NavHost thông qua
            // DeepLinkHandlerKt.handleDeepLink() — hàm này được define
            // trong commonMain/iosMain và nhận URL string để navigate.
            .onOpenURL { url in
                DeepLinkHandlerKt.handleDeepLink(urlString: url.absoluteString)
            }
    }
}