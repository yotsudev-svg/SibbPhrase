package dev.zenn.yotsu.sibbphrase.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.local.DataStoreManager
import dev.zenn.yotsu.sibbphrase.model.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 設定画面（SettingsScreen）のUI状態および操作を管理するViewModel。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/settings/）
 * 責務: テーマ設定や、クリップボード自動削除秒数、復元表示秒数といったアプリ共通設定のUI状態を公開し、
 * ユーザーの変更操作を `DataStoreManager` へ中継・永続化する。
 *
 * @property dataStoreManager アプリの一般設定を永続化・管理するデータ層のリポジトリコンポーネント。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    /**
     * 暗号文コピー後のクリップボード自動削除までの秒数を保持・公開するStateFlow。
     *
     * 設計上の意図:
     * IT操作に不慣れな家族が焦らずに操作できるよう、デフォルト値は安心の「60秒（1分）」に設定されている。
     * 画面が非表示の間（サブスクライバー数が0になってから5秒間）はキャッシュを保持し、不要な再読込を抑止する。
     */
    val autoDeleteSec: StateFlow<Int> = dataStoreManager.autoDeleteSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    /**
     * 復元された平文の画面表示有効期間（秒）を保持・公開するStateFlow。
     *
     * 設計上の意図:
     * コピー同様、家族が内容を確認するのに十分な時間を確保するため、デフォルト値は安心の「60秒（1分）」に設定されている。
     */
    val restoreDisplaySec: StateFlow<Int> = dataStoreManager.restoreDisplaySec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    /**
     * アプリ全体の表示テーマ（SYSTEM / LIGHT / DARK）を保持・公開するStateFlow。
     *
     * デフォルト値はシステムのテーマ設定に連動する `AppTheme.SYSTEM`。
     */
    val themeMode: StateFlow<AppTheme> = dataStoreManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    /**
     * クリップボード自動削除までの秒数を変更し、永続化を要求する。
     *
     * 呼び出しタイミング:
     * - `SettingsScreen` 上でユーザーが削除秒数の設定（ドロップダウン等）を変更した際。
     *
     * @param sec 新しく設定する秒数
     */
    fun setAutoDeleteSeconds(sec: Int) {
        viewModelScope.launch {
            dataStoreManager.setAutoDeleteSeconds(sec)
        }
    }

    /**
     * 復元表示の有効期間（秒）を変更し、永続化を要求する。
     *
     * 呼び出しタイミング:
     * - `SettingsScreen` 上でユーザーが表示秒数の設定（ドロップダウン等）を変更した際。
     *
     * @param sec 新しく設定する秒数
     */
    fun setRestoreDisplaySeconds(sec: Int) {
        viewModelScope.launch {
            dataStoreManager.setRestoreDisplaySeconds(sec)
        }
    }

    /**
     * アプリ全体の表示テーマを変更し、永続化を要求する。
     *
     * 呼び出しタイミング:
     * - `SettingsScreen` 上でユーザーがテーマ選択（ライト・ダーク・システム連動）を切り替えた際。
     *
     * @param theme 適用する新しいテーマモード（SYSTEM / LIGHT / DARK）
     */
    fun setThemeMode(theme: AppTheme) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(theme)
        }
    }
}
