package dev.zenn.yotsu.sibbphrase.ui.screens.decrypt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.crypto.CryptoManager
import dev.zenn.yotsu.sibbphrase.data.crypto.KeystoreManager
import dev.zenn.yotsu.sibbphrase.data.local.DataStoreManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 復元画面（DecryptScreen）のUI状態。
 *
 * @property inputText ユーザーが入力、または他アプリから共有された暗号文。
 * @property outputText 復号処理の結果として得られた平文。
 * @property isLoading 復号処理中（鍵導出など）かどうかを表すフラグ。
 * @property errorMsg ユーザーに提示するエラーメッセージ（合言葉不一致、形式エラーなど）。
 * @property timerSeconds 平文が自動消去されるまでの残り秒数。
 * @property isCopied クリップボードにコピーされた直後の状態を表すフラグ。
 */
data class DecryptUiState(
    val inputText:    String  = "",
    val outputText:   String  = "",
    val isLoading:    Boolean = false,
    val errorMsg:     String? = null,
    val timerSeconds: Int?    = null,
    val isCopied:     Boolean = false
)

/**
 * 復元画面（DecryptScreen）のUI状態および操作を管理するViewModel。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/decrypt/）
 * 責務: 暗号文の入力、`KeystoreManager` から取得した合言葉を用いた復号、および復元された平文の自動消去タイマーを管理する。
 *
 * @property crypto 復号ロジック（AES-256-GCM）を担当するマネージャー。
 * @property keystore 安全なストレージから合言葉を取得するためのマネージャー。
 * @property dataStore ユーザー設定（復元表示秒数など）を取得するためのマネージャー。
 */
@HiltViewModel
class DecryptViewModel @Inject constructor(
    private val crypto:    CryptoManager,
    private val keystore:  KeystoreManager,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecryptUiState())

    /**
     * 復元画面の現在の状態を保持・公開するStateFlow。
     */
    val uiState: StateFlow<DecryptUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * ユーザーによる入力テキストの変更をUI状態に反映する。
     *
     * @param text 新しい入力文字列（暗号文）。
     */
    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMsg = null) }
    }

    /**
     * 他アプリからの共有（ACTION_SEND）等で渡されたテキストをセットする。
     * 前回の復号結果やタイマーはリセットされる。
     *
     * @param text 共有された暗号文。
     */
    fun setSharedText(text: String) {
        _uiState.update { it.copy(inputText = text, errorMsg = null, outputText = "", timerSeconds = null) }
    }

    /**
     * 入力された暗号文を、保存されている合言葉で復号する。
     *
     * 実行フロー:
     * 1. `KeystoreManager` から合言葉を取得。未設定ならエラーを表示。
     * 2. 入力値のバリデーション（空チェック）。
     * 3. `CryptoManager.decrypt` を呼び出し、AES-256-GCM による復号を非同期で実行。
     * 4. 成功時は平文をセットし、`DataStoreManager` から取得した秒数で自動消去タイマーを開始。
     * 5. 失敗時（AEADBadTagException等）は、合言葉の不一致を示唆する適切な日本語メッセージを表示。
     */
    fun decrypt() {
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
            crypto.decrypt(input, passphrase)
                .onSuccess { plain ->
                    val restoreDisplaySec = dataStore.restoreDisplaySec.first()
                    _uiState.update {
                        it.copy(outputText = plain, isLoading = false,
                            timerSeconds = restoreDisplaySec)
                    }
                    startAutoDeleteTimer(restoreDisplaySec)
                }
                .onFailure { e ->
                    val msg = if (e is javax.crypto.AEADBadTagException)
                        "復元できませんでした。合言葉が正しいか確認してください"
                    else "復元に失敗しました: ${e.message}"
                    _uiState.update { it.copy(isLoading = false, errorMsg = msg) }
                }
        }
    }

    /**
     * 復元された平文がクリップボードにコピーされた際に呼び出される。
     * 一時的に `isCopied` フラグを true にし、2秒後に自動で戻す。
     */
    fun onCopied() {
        _uiState.update { it.copy(isCopied = true) }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(isCopied = false) }
        }
    }

    /**
     * 入力および出力テキスト、タイマー状態をすべて初期化する。
     */
    fun clearOutput() {
        timerJob?.cancel()
        _uiState.update { it.copy(outputText = "", inputText = "", timerSeconds = null) }
    }

    /**
     * 復元された平文を画面から自動消去するためのカウントダウンタイマーを開始する。
     *
     * 設計上の意図:
     * 家族が内容を確認するのに十分な時間を確保するため、デフォルト値は「60秒（1分）」に設定されている。
     * タイマー終了後はセキュリティのために平文とコピー状態を破棄する。
     *
     * @param seconds カウントダウンの開始秒数。
     */
    private fun startAutoDeleteTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(timerSeconds = remaining) }
            }
            _uiState.update { it.copy(outputText = "", timerSeconds = null, isCopied = false) }
        }
    }
}
