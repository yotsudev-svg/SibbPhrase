package dev.zenn.yotsu.sibbphrase.ui.screens.passphrase

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.crypto.KeystoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 合言葉管理画面（PassphraseScreen）のUI状態。
 *
 * @property hasPassphrase 合言葉が既に設定されているかどうか。
 * @property isEditing 現在合言葉を編集中（入力モード）かどうか。
 */
data class PassphraseUiState(
    val hasPassphrase: Boolean = false,
    val isEditing:     Boolean = false
)

/**
 * 合言葉管理画面（PassphraseScreen）のUI状態および操作を管理するViewModel。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/passphrase/）
 * 責務: 合言葉の登録、変更、リセット状態の管理、および機密情報の安全な保存（KeystoreManager）との中継を行う。
 *
 * @property keystore 安全なストレージへ合言葉を保存・確認するためのマネージャー。
 */
@HiltViewModel
class PassphraseViewModel @Inject constructor(
    private val keystore: KeystoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassphraseUiState())

    /**
     * 合言葉管理画面の現在の状態を保持・公開するStateFlow。
     */
    val uiState: StateFlow<PassphraseUiState> = _uiState.asStateFlow()

    init {
        refreshPassphraseStatus()
    }

    /**
     * 入力された合言葉を安全なストレージに保存する。
     *
     * 呼び出しタイミング:
     * - 合言葉設定画面で「保存」ボタンが押下された際。
     *
     * @param passphrase 入力された合言葉。4文字未満の場合はバリデーションにより中断される。
     */
    fun savePassphrase(passphrase: String) {
        if (passphrase.length < 4) return
        keystore.savePassphrase(passphrase)
        _uiState.update { it.copy(isEditing = false) }
        refreshPassphraseStatus()
    }

    /**
     * 合言葉の入力・変更モードに移行する。
     */
    fun enterEditMode() {
        _uiState.update { it.copy(isEditing = true) }
    }

    /**
     * 保存されている合言葉をすべて消去し、初期状態に戻す。
     *
     * 呼び出しタイミング:
     * - リセット確認ダイアログで「リセット」が選択された際。
     */
    fun resetPassphrase() {
        keystore.clearAll()
        _uiState.update { it.copy(isEditing = false) }
        refreshPassphraseStatus()
    }

    /**
     * 現在の合言葉の保存状況を確認し、UI状態を最新に更新する。
     */
    private fun refreshPassphraseStatus() {
        _uiState.update {
            it.copy(
                hasPassphrase = keystore.hasPassphrase()
            )
        }
    }
}

