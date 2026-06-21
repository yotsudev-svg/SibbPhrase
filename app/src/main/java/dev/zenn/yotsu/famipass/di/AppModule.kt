package dev.zenn.yotsu.famipass.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppModule
 * 現在は @Inject constructor により自動提供されるため、明示的な @Provides は不要。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
}
