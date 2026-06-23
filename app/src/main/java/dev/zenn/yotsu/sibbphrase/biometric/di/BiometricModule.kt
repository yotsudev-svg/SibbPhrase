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

@Module
@InstallIn(SingletonComponent::class)
object BiometricModule {

    @Provides
    @Singleton
    fun provideBiometricSettingStorage(
        @ApplicationContext context: Context
    ): BiometricSettingStorage {
        return BiometricSettingStorage(context)
    }

    @Provides
    @Singleton
    fun provideBiometricStatusManager(
        @ApplicationContext context: Context
    ): BiometricStatusManager {
        return BiometricStatusManager(context)
    }
}