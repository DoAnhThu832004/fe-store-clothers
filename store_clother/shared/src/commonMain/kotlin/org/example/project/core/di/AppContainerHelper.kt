package org.example.project.core.di

/**
 * Singleton holder quản lý [AppContainer] cho toàn bộ ứng dụng.
 * Platform targets (như Android trong MainActivity/Application) có thể gọi `init(customContainer)`
 * để cấu hình các Platform-specific Data Sources (Retrofit, EncryptedSharedPreferences, v.v.).
 */
object AppContainerHelper {

    private var instance: AppContainer? = null

    val current: AppContainer
        get() = instance ?: DefaultAppContainer().also { instance = it }

    fun init(container: AppContainer) {
        instance = container
    }
}
