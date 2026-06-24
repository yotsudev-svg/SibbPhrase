package dev.zenn.yotsu.sibbphrase.biometric.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_settings")

/**
 * 生体認証のON/OFF設定を永続化するDataStoreリポジトリ。
 *
 * アーキテクチャ上の配置: 生体認証モジュール（biometric/data/）
 * 責務: Jetpack DataStore Preferences を用いて、ユーザーが設定した生体認証の有効/無効状態を
 * デバイスに永続保存・取得する。
 *
 * 設計上の注意:
 * `DataStoreManager`（"sibbphrase_prefs"）とは意図的に DataStore ファイルを分離し、
 * "biometric_settings" という専用ファイルで管理している。
 * これにより、認証ロジックと一般設定の依存関係を疎に保ち、保守性を高めている。
 * また、同一プロセスで複数インスタンスが生成されないよう、`private val Context.dataStore` を
 * このファイル内に独立して宣言している。
 *
 * Hilt により [BiometricModule] を通じて `@Singleton` として提供される。
 *
 * @param context ActivityのContextが渡されても内部でApplicationContextに切り替え、メモリリークを防ぐ。
 */
class BiometricSettingStorage(context: Context) {

    // ✅ ActivityのContextが渡されてもApplicationContextに切り替えてリークを防ぐ
    private val appContext = context.applicationContext

    companion object {
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    /**
     * 生体認証が有効かどうかを通知する Flow。
     *
     * `BiometricLockViewModel` の `init` ブロックで購読され、設定変更のたびに
     * 認証状態の再評価がトリガーされる。
     * デフォルト値は `false`（未設定 = 生体認証が無効な状態）。
     */
    val isBiometricEnabled: Flow<Boolean> = appContext.dataStore.data
        .map { preferences -> preferences[KEY_BIOMETRIC_ENABLED] ?: false }

    /**
     * 生体認証の有効/無効設定を保存する。
     *
     * 呼び出しタイミング:
     * - `SettingsScreen` 上で生体認証スイッチが切り替えられ、
     *   `BiometricLockViewModel.updateBiometricSetting` が呼ばれた際。
     *
     * @param enabled 生体認証を有効にする場合は `true`、無効にする場合は `false`。
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }
}