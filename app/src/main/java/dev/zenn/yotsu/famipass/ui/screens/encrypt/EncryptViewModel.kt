package dev.zenn.yotsu.famipass.ui.screens.encrypt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.famipass.data.crypto.CryptoManager
import dev.zenn.yotsu.famipass.data.crypto.KeystoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EncryptUiState(
    val inputText:   String  = "",
    val outputText:  String  = "",
    val isLoading:   Boolean = false,
    val errorMsg:    String? = null
)

@HiltViewModel
class EncryptViewModel @Inject constructor(
    private val crypto:   CryptoManager,
    private val keystore: KeystoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncryptUiState())
    val uiState: StateFlow<EncryptUiState> = _uiState.asStateFlow()

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMsg = null) }
    }

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

    fun clearOutput() {
        _uiState.update { it.copy(outputText = "", inputText = "") }
    }
}
