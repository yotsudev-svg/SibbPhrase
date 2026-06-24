package dev.zenn.yotsu.sibbphrase.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.local.DataStoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * オンボーディング画面（OnboardingScreen）のUI状態および操作を管理するViewModel。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/onboarding/）
 * 責務: 初回起動時のチュートリアル表示状態を管理し、完了フラグを `DataStoreManager` へ保存する。
 *
 * @property dataStoreManager アプリの一般設定（オンボーディング完了フラグを含む）を管理するデータ層のリポジトリ。
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    /**
     * オンボーディング（初回起動チュートリアル）が完了したかどうかを通知する StateFlow。
     *
     * 状態の変化:
     * - `null`: DataStore からの状態取得中（初期値）。
     * - `false`: 未完了。チュートリアル画面を表示する。
     * - `true`: 完了済み。メインコンテンツ（暗号化/復号画面など）へ遷移する。
     *
     * 画面が非表示の間（サブスクライバー数が0になってから5秒間）はキャッシュを保持し、不要な再読込を抑止する。
     */
    val isOnboardingDone: StateFlow<Boolean?> = dataStoreManager.isOnboardingDone
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * オンボーディングが完了したことを永続化層（DataStore）に記録する。
     *
     * 呼び出しタイミング:
     * - チュートリアルの最終ページで「はじめる」等のボタンが押下された際。
     */
    fun markOnboardingDone() {
        viewModelScope.launch {
            dataStoreManager.markOnboardingDone()
        }
    }
}
