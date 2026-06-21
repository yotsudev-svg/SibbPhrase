package dev.zenn.yotsu.famipass.ui.screens.decrypt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.famipass.data.crypto.CryptoManager
import dev.zenn.yotsu.famipass.data.crypto.KeystoreManager
import dev.zenn.yotsu.famipass.data.local.DataStoreManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DecryptUiState(
    val inputText:    String  = "",
    val outputText:   String  = "",
    val isLoading:    Boolean = false,
    val errorMsg:     String? = null,
    val timerSeconds: Int?    = null,
    val isCopied:     Boolean = false
)

@HiltViewModel
class DecryptViewModel @Inject constructor(
    private val crypto:    CryptoManager,
    private val keystore:  KeystoreManager,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecryptUiState())
    val uiState: StateFlow<DecryptUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMsg = null) }
    }

    fun setSharedText(text: String) {
        _uiState.update { it.copy(inputText = text, errorMsg = null, outputText = "", timerSeconds = null) }
    }

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
                    val autoDeleteSec = dataStore.autoDeleteSec.first()
                    _uiState.update {
                        it.copy(outputText = plain, isLoading = false,
                            timerSeconds = autoDeleteSec)
                    }
                    startAutoDeleteTimer(autoDeleteSec)
                }
                .onFailure { e ->
                    val msg = if (e is javax.crypto.AEADBadTagException)
                        "復元できませんでした。合言葉が正しいか確認してください"
                    else "復元に失敗しました: ${e.message}"
                    _uiState.update { it.copy(isLoading = false, errorMsg = msg) }
                }
        }
    }

    fun onCopied() {
        _uiState.update { it.copy(isCopied = true) }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(isCopied = false) }
        }
    }

    fun clearOutput() {
        timerJob?.cancel()
        _uiState.update { it.copy(outputText = "", inputText = "", timerSeconds = null) }
    }

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
