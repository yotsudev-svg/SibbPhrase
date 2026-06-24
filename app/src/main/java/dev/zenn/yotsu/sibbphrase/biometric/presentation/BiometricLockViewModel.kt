package dev.zenn.yotsu.sibbphrase.biometric.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.biometric.data.BiometricSettingStorage
import dev.zenn.yotsu.sibbphrase.biometric.domain.BiometricStatusManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * アプリ全体の生体認証ロック状態を管理するViewModel。
 *
 * 生体認証モジュール（biometric/）の中心的役割を果たし、全画面をラップする `BiometricLockWrapper` と連携して
 * アプリのロック・アンロック、バックグラウンド移行時の自動ロック、認証トリガーの単方向通知（UDF）を制御する。
 *
 * 設計上の重要な特徴:
 * 1. **生体認証の初期化ロジック**: 初回DataStore読み込み時のみロック状態（`isUnlocked`）を計算し、
 *    設定変更時は現在のロック状態を維持する（アプリ使用中の不要な再ロックを防止）。
 * 2. **Channel.CONFLATED の採用**: 認証トリガーの通知に `Channel.CONFLATED` を使用。
 *    これにより、UI側（Composable）の初期化タイミングと競合してイベントが消失するのを防ぎ、確実に1回届ける。
 * 3. **DataStoreの分離**: `BiometricSettingStorage` は専用のDataStore（"biometric_settings"）を使用し、
 *    アプリ全体の通常設定（`DataStoreManager`）とは別ファイルで一元管理される。
 *
 * @property settingStorage 生体認証のON/OFF設定を永続化するDataStoreリポジトリ。
 * @property statusManager 端末の生体認証ハードウェア状態（利用可能、未登録など）を判定するマネージャー。
 */
@HiltViewModel
class BiometricLockViewModel @Inject constructor(
    private val settingStorage: BiometricSettingStorage,
    private val statusManager: BiometricStatusManager
) : ViewModel() {

    /**
     * 生体認証ロックのUI状態を表すStateFlow。
     *
     * `BiometricLockWrapper` などのUI層がこれを購読し、ロック画面（`AppSplashLockScreen`）を
     * 表示するか、本来のメインコンテンツを表示するかを判定する。
     */
    private val _uiState = MutableStateFlow(BiometricLockState())
    val uiState: StateFlow<BiometricLockState> = _uiState.asStateFlow()

    // ✅ SharedFlow → Channel.CONFLATED に変更
    //    Channel はサブスクライバー不在でもバッファにイベントを保持し、
    //    接続時に確実に1回届ける。CONFLATED で同時に積まれても最新1件のみ保持。
    /**
     * UI層（`BiometricLockWrapper` 内のLaunchedEffectなど）に対して、
     * `BiometricPrompt` の起動（認証ダイアログ表示）を促すイベントを通知するChannel。
     */
    private val _authTriggerChannel = Channel<Unit>(Channel.CONFLATED)

    /**
     * `BiometricPrompt` の認証をトリガーする単方向イベントのFlow。
     * UI層（Composable）がこのFlowを安全に収集し、イベント受信時に認証ダイアログを表示する。
     */
    val triggerAuthEvent: Flow<Unit> = _authTriggerChannel.receiveAsFlow()

    init {
        settingStorage.isBiometricEnabled
            .onEach { isEnabled ->
                val status = statusManager.checkBiometricStatus()
                val isHwAvailable =
                    status == BiometricStatusManager.Status.AVAILABLE ||
                            status == BiometricStatusManager.Status.NONE_ENROLLED

                // ✅ update 前に初期化済みかどうかをキャプチャ
                val wasInitialized = _uiState.value.isInitialized

                _uiState.update { current ->
                    // ✅ 初回ロード時のみ isUnlocked を計算で決定する
                    //    初期化後（設定変更時）は:
                    //      - 無効化 or ハードウェア不可 → アンロック
                    //      - 有効化 → 現在のロック状態を維持（すでにアプリ使用中のため再ロックしない）
                    val newIsUnlocked = if (!current.isInitialized) {
                        !isEnabled || !isHwAvailable
                    } else {
                        if (!isEnabled || !isHwAvailable) true else current.isUnlocked
                    }

                    current.copy(
                        isInitialized = true,
                        isBiometricEnabled = isEnabled,
                        isUnlocked = newIsUnlocked,
                        isHardwareAvailable = isHwAvailable,
                        isEnrollmentRequired = status == BiometricStatusManager.Status.NONE_ENROLLED
                    )
                }

                // ✅ 初回ロード時かつロック状態の場合のみ認証トリガー
                //    wasInitialized=true（設定変更）のときはトリガーしない
                val currentState = _uiState.value
                if (!wasInitialized &&
                    isEnabled &&
                    status == BiometricStatusManager.Status.AVAILABLE &&
                    !currentState.isUnlocked
                ) {
                    triggerAuthentication()
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 認証ダイアログの表示トリガーを即座に発行する。
     *
     * `_authTriggerChannel`（Channel.CONFLATED）に `Unit` を送信し、UI層に通知する。
     */
    fun triggerAuthentication() {
        _authTriggerChannel.trySend(Unit)
    }

    /**
     * 必要に応じて認証をトリガーする。
     *
     * アプリが未ロック状態、かつ生体認証が有効でハードウェアが利用可能、かつロックアウト（連続失敗による制限）
     * されていない場合にのみ、`triggerAuthentication` を呼び出す。
     * 主に `BiometricLockWrapper` がフォアグラウンド復帰（ON_START）を検知したタイミングなどで呼び出される。
     */
    fun triggerAuthenticationIfNeeded() {
        val state = _uiState.value
        if (!state.isUnlocked && state.isBiometricEnabled && state.isHardwareAvailable && !state.isLockout) {
            triggerAuthentication()
        }
    }

    /**
     * アプリがバックグラウンドに移行した（LifecycleのON_STOPなど）タイミングで呼び出される。
     *
     * 生体認証が有効かつアンロック状態である場合、セキュリティのために即座にアプリを再ロック（`isUnlocked = false`）し、
     * エラーメッセージをクリアする。
     * この状態変化を受けて、[BiometricLockWrapper] 内の [SideEffect] が
     * [android.view.WindowManager.LayoutParams.FLAG_SECURE] を自動的に有効化し、
     * スクリーンショット・画面録画を防止する。再ロック自体はこのメソッドの責務であり、
     * FLAG_SECURE の付け外しは [BiometricLockWrapper] 側の責務として明確に分離されている。
     */
    fun onAppBackground() {
        val state = _uiState.value
        if (state.isBiometricEnabled && state.isUnlocked) {
            _uiState.update {
                it.copy(isUnlocked = false, errorMessage = null)
            }
        }
    }

    /**
     * 生体認証が正常に成功したタイミングで呼び出される。
     *
     * 状態をアンロック（`isUnlocked = true`）に更新し、エラーメッセージのクリア、
     * ロックアウトフラグの解除、連続失敗カウント（`attemptCount`）の初期化を行う。
     */
    fun onAuthSuccess() {
        _uiState.update {
            it.copy(
                isUnlocked = true,
                errorMessage = null,
                isLockout = false,
                attemptCount = 0
            )
        }
    }

    /**
     * 生体認証中に修復不可能なエラー（キャンセル、ハードウェアエラー、ロックアウトなど）が発生した際に呼び出される。
     *
     * エラーメッセージをUI状態にセットし、ロックアウトエラー（`isLockoutError = true`）である場合は
     * 制限状態として記録する。
     *
     * @param errorMsg ユーザーに提示する翻訳済みの日本語エラーメッセージ（`BiometricErrorTranslator` で変換されたもの）。
     * @param isLockoutError 連続失敗によりシステムからロックアウトされた（`BIOMETRIC_ERROR_LOCKOUT(_PERMANENT)`）かどうか。
     */
    fun onAuthError(errorMsg: String, isLockoutError: Boolean) {
        _uiState.update {
            it.copy(errorMessage = errorMsg, isLockout = isLockoutError)
        }
    }

    /**
     * 生体認証の認証試行が1回失敗した（指紋の不一致など、ダイアログが閉じない軽微な失敗）タイミングで呼び出される。
     *
     * 連続失敗カウント（`attemptCount`）をインクリメントし、即時提示用の日本語メッセージをセットする。
     */
    fun onAuthAttemptFailed() {
        _uiState.update {
            it.copy(
                attemptCount = it.attemptCount + 1,
                errorMessage = "生体情報が一致しませんでした。再度お試しください。"
            )
        }
    }

    /**
     * 設定画面（`SettingsScreen`）などで生体認証の有効/無効設定が変更された際に呼び出される。
     *
     * `settingStorage` を通じて DataStore 内の設定値を非同期に更新する。
     * 更新後は `init` ブロック内の Flow を経由して、`wasInitialized = true` のルートで
     * 状態が安全に再計算される。
     *
     * @param enabled 生体認証を有効にする場合は `true`、無効にする場合は `false`。
     */
    fun updateBiometricSetting(enabled: Boolean) {
        viewModelScope.launch {
            settingStorage.setBiometricEnabled(enabled)
        }
    }

    /**
     * ViewModel破棄時のクリーンアップ処理。
     *
     * `_authTriggerChannel` を閉じ、コルーチンのチャンネルリソースを完全に解放する。
     */
    override fun onCleared() {
        super.onCleared()
        _authTriggerChannel.close()
    }
}