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

@HiltViewModel
class BiometricLockViewModel @Inject constructor(
    private val settingStorage: BiometricSettingStorage,
    private val statusManager: BiometricStatusManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiometricLockState())
    val uiState: StateFlow<BiometricLockState> = _uiState.asStateFlow()

    // ✅ SharedFlow → Channel.CONFLATED に変更
    //    Channel はサブスクライバー不在でもバッファにイベントを保持し、
    //    接続時に確実に1回届ける。CONFLATED で同時に積まれても最新1件のみ保持。
    private val _authTriggerChannel = Channel<Unit>(Channel.CONFLATED)
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

    fun triggerAuthentication() {
        _authTriggerChannel.trySend(Unit)
    }

    fun triggerAuthenticationIfNeeded() {
        val state = _uiState.value
        if (!state.isUnlocked && state.isBiometricEnabled && state.isHardwareAvailable && !state.isLockout) {
            triggerAuthentication()
        }
    }

    fun onAppBackground() {
        val state = _uiState.value
        if (state.isBiometricEnabled && state.isUnlocked) {
            _uiState.update {
                it.copy(isUnlocked = false, errorMessage = null)
            }
        }
    }

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

    fun onAuthError(errorMsg: String, isLockoutError: Boolean) {
        _uiState.update {
            it.copy(errorMessage = errorMsg, isLockout = isLockoutError)
        }
    }

    fun onAuthAttemptFailed() {
        _uiState.update {
            it.copy(
                attemptCount = it.attemptCount + 1,
                errorMessage = "生体情報が一致しませんでした。再度お試しください。"
            )
        }
    }

    fun updateBiometricSetting(enabled: Boolean) {
        viewModelScope.launch {
            settingStorage.setBiometricEnabled(enabled)
        }
    }

    // ✅ ViewModel 破棄時に Channel を閉じてリソースを解放
    override fun onCleared() {
        super.onCleared()
        _authTriggerChannel.close()
    }
}