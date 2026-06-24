package dev.zenn.yotsu.sibbphrase.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * アプリケーション全体の依存関係を提供する Hilt モジュール。
 *
 * アーキテクチャ上の配置: 依存注入層（di/）
 * 責務: アプリ全体の共通コンポーネント（Singleton）の生成・提供を管理する。
 *
 * 現在の実装では、主要なマネージャークラス（`CryptoManager`, `DataStoreManager` など）が
 * `@Inject constructor` を用いたコンストラクタ注入に対応しているため、
 * 本モジュール内での明示的な `@Provides` メソッドによる定義は最小限となっている。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
}
