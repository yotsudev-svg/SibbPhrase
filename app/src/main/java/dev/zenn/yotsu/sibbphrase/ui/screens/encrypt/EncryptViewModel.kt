package dev.zenn.yotsu.sibbphrase.ui.screens.encrypt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.crypto.CryptoManager
import dev.zenn.yotsu.sibbphrase.data.crypto.KeystoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 暗号化画面（EncryptScreen）のUI状態。
 *
 * @property inputText ユーザーが入力した平文。
 * @property outputText 暗号化処理の結果として生成された暗号文（Base64形式）。
 * @property isLoading 暗号化処理中（鍵導出など）かどうかを表すフラグ。
 * @property errorMsg ユーザーに提示するエラーメッセージ（合言葉未設定や入力不足など）。
 * @property isCopied クリップボードにコピーされた直後の状態を表すフラグ。
 */
data class EncryptUiState(
    val inputText:   String  = "",
    val outputText:  String  = "",
    val isLoading:   Boolean = false,
    val errorMsg:    String? = null,
    val isCopied:    Boolean = false
)

/**
 * 暗号化画面（EncryptScreen）のUI状態および操作を管理するViewModel。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/encrypt/）
 * 責務: ユーザー入力されたテキストを、`KeystoreManager` から取得した合言葉を用いて
 * `CryptoManager` で暗号化し、その結果をUIに反映する。
 *
 * @property crypto 暗号化ロジック（AES-256-GCM）を担当するマネージャー。
 * @property keystore 安全なストレージから合言葉を取得するためのマネージャー。
 */
@HiltViewModel
class EncryptViewModel @Inject constructor(
    private val crypto:   CryptoManager,
    private val keystore: KeystoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncryptUiState())

    /**
     * 暗号化画面の現在の状態を保持・公開するStateFlow。
     */
    val uiState: StateFlow<EncryptUiState> = _uiState.asStateFlow()

    /**
     * ユーザーによる入力テキストの変更をUI状態に反映する。
     *
     * @param text 新しい入力文字列。
     */
    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMsg = null) }
    }

    /**
     * 入力された平文を、保存されている合言葉で暗号化する。
     *
     * 実行フロー:
     * 1. `KeystoreManager` から合言葉を取得。未設定ならエラーを表示。
     * 2. 入力値のバリデーション（空チェック）。
     * 3. `CryptoManager.encrypt` を呼び出し、PBKDF2による鍵導出とAES-GCM暗号化を非同期で実行。
     * 4. 結果（成功時はBase64暗号文、失敗時はエラー）をUI状態に反映。
     */
    fun encrypt() {
        val passphrase = keystore.getPassphrase() ?: run {
            _uiState.update { it.copy(errorMsg = "先に合言葉を設定してください") }
            return
        }
        val input = _uiState.value.inputText.trim()
        if (input.isEmpty()) {
            _uiState.update { it.copy(errorMsg = "文字を入力してください") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMsg = null) }
        viewModelScope.launch {
            crypto.encrypt(input, passphrase)
                .onSuccess { encoded ->
                    _uiState.update { it.copy(outputText = encoded, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMsg = "変換に失敗しました: ${e.message}")
                    }
                }
        }
    }

    /**
     * 暗号文がクリップボードにコピーされた際に呼び出される。
     *
     * 一時的に `isCopied` フラグを true にし、2秒後に自動で戻すことで
     * UI上の「コピー完了」通知（スナックバーやアイコン変更）を制御する。
     */
    fun onCopied() {
        _uiState.update { it.copy(isCopied = true) }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(isCopied = false) }
        }
    }

    /**
     * 入力および出力テキスト、状態フラグをすべて初期化する。
     * 画面遷移や「クリア」ボタン押下時に呼び出される。
     */
    fun clearOutput() {
        _uiState.update { it.copy(outputText = "", inputText = "", isCopied = false) }
    }
}
