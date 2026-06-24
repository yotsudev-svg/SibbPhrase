package dev.zenn.yotsu.sibbphrase.biometric.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.zenn.yotsu.sibbphrase.biometric.data.BiometricSettingStorage
import dev.zenn.yotsu.sibbphrase.biometric.domain.BiometricStatusManager
import javax.inject.Singleton

/**
 * 生体認証モジュール（biometric/）の依存関係を提供するHiltモジュール。
 *
 * アーキテクチャ上の配置: 依存注入層（biometric/di/）
 * 責務: [BiometricSettingStorage] と [BiometricStatusManager] を
 * アプリケーションスコープの Singleton として生成・提供する。
 *
 * どちらのクラスも Context を必要とするコンストラクタを持つため、
 * `@Inject constructor` での自動注入は行わず、本モジュールで `@Provides` メソッドを明示的に定義し、
 * `@ApplicationContext` アノテーションで正しいコンテキストを注入している。
 */
@Module
@InstallIn(SingletonComponent::class)
object BiometricModule {

    /**
     * [BiometricSettingStorage] の Singleton インスタンスを提供する。
     *
     * ApplicationContext を直接渡すことで、[BiometricSettingStorage] 内での
     * `context.applicationContext` への再変換が冗長にならないようにしている。
     *
     * @param context Hilt が提供するアプリケーションスコープのContext。
     * @return [BiometricSettingStorage] の Singleton インスタンス。
     */
    @Provides
    @Singleton
    fun provideBiometricSettingStorage(
        @ApplicationContext context: Context
    ): BiometricSettingStorage {
        return BiometricSettingStorage(context)
    }

    /**
     * [BiometricStatusManager] の Singleton インスタンスを提供する。
     *
     * ApplicationContext を渡すことで、[BiometricStatusManager] 内の
     * `BiometricManager.from(context.applicationContext)` が適切なContextで動作するようにしている。
     *
     * @param context Hilt が提供するアプリケーションスコープのContext。
     * @return [BiometricStatusManager] の Singleton インスタンス。
     */
    @Provides
    @Singleton
    fun provideBiometricStatusManager(
        @ApplicationContext context: Context
    ): BiometricStatusManager {
        return BiometricStatusManager(context)
    }
}