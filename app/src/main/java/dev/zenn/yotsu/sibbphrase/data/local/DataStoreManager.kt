package dev.zenn.yotsu.sibbphrase.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zenn.yotsu.sibbphrase.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sibbphrase_prefs")

/**
 * アプリ全体の一般設定を管理する DataStore マネージャー。
 *
 * アーキテクチャ上の配置: データ層（data/local/）
 * 責務: オンボーディング状態、自動削除設定、テーマ等のアプリ共通設定を Jetpack DataStore を用いて永続化する。
 *
 * 設計上の注意:
 * 生体認証関連の設定（`BiometricSettingStorage`）とは意図的に DataStore ファイルを分離（"sibbphrase_prefs"）している。
 * これにより、認証ロジックと一般設定の依存関係を疎にし、保守性を高めている。
 * また、同一プロセスで複数インスタンスが生成されないよう、`private val Context.dataStore` を
 * このファイル内に独立して宣言している。これは DataStore の使用上の制約（同一 `name` に対して
 * アプリ内で1つのインスタンスのみを使用すること）を遵守するためである。
 *
 * @property context Hilt から注入されるアプリケーションコンテキスト。DataStore へのアクセスに使用される。
 */
@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_AUTO_DELETE_SEC = intPreferencesKey("auto_delete_sec")
        private val KEY_RESTORE_DISPLAY_SEC = intPreferencesKey("restore_display_sec")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /**
     * オンボーディング（初回起動チュートリアル）が完了したかどうかを通知する Flow。
     * デフォルト値は `false`。
     */
    val isOnboardingDone: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_ONBOARDING_DONE] ?: false }

    /**
     * 暗号文コピー後のクリップボード自動削除までの秒数を通知する Flow。
     * デフォルト値は 60秒。
     */
    val autoDeleteSec: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_AUTO_DELETE_SEC] ?: 60 }

    /**
     * 復元された平文の表示有効期間（秒）を通知する Flow。
     * デフォルト値は 60秒。
     */
    val restoreDisplaySec: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_RESTORE_DISPLAY_SEC] ?: 60 }

    /**
     * アプリのテーマ設定（システム設定連動 / ライト / ダーク）を通知する Flow。
     * デフォルト値は `AppTheme.SYSTEM`。
     */
    val themeMode: Flow<AppTheme> = context.dataStore.data
        .map { prefs ->
            val themeName = prefs[KEY_THEME_MODE] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }
        }

    /**
     * オンボーディング完了フラグを真に更新する。
     * `OnboardingViewModel` からチュートリアル終了時に呼び出される。
     */
    suspend fun markOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    /**
     * クリップボード自動削除までの秒数を更新する。
     * `SettingsScreen`（設定画面）でのユーザー操作により呼び出される。
     *
     * @param sec 設定する秒数
     */
    suspend fun setAutoDeleteSeconds(sec: Int) {
        context.dataStore.edit { it[KEY_AUTO_DELETE_SEC] = sec }
    }

    /**
     * 復元表示の有効期間を更新する。
     * `SettingsScreen`（設定画面）でのユーザー操作により呼び出される。
     *
     * @param sec 設定する秒数
     */
    suspend fun setRestoreDisplaySeconds(sec: Int) {
        context.dataStore.edit { it[KEY_RESTORE_DISPLAY_SEC] = sec }
    }

    /**
     * アプリの表示テーマを更新する。
     * `SettingsScreen`（設定画面）でのユーザー操作により呼び出される。
     *
     * @param theme 適用するテーマモード（SYSTEM / LIGHT / DARK）
     */
    suspend fun setThemeMode(theme: AppTheme) {
        context.dataStore.edit { it[KEY_THEME_MODE] = theme.name }
    }
}

