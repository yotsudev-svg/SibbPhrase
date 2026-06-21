package dev.zenn.yotsu.sibbphrase.ui.screens.passphrase

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.crypto.KeystoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PassphraseUiState(
    val hasPassphrase: Boolean = false,
    val isEditing:     Boolean = false
)

@HiltViewModel
class PassphraseViewModel @Inject constructor(
    private val keystore: KeystoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassphraseUiState())
    val uiState: StateFlow<PassphraseUiState> = _uiState.asStateFlow()

    init {
        refreshPassphraseStatus()
    }

    fun savePassphrase(passphrase: String) {
        if (passphrase.length < 4) return
        keystore.savePassphrase(passphrase)
        _uiState.update { it.copy(isEditing = false) }
        refreshPassphraseStatus()
    }

    fun enterEditMode() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun resetPassphrase() {
        keystore.clearAll()
        _uiState.update { it.copy(isEditing = false) }
        refreshPassphraseStatus()
    }

    private fun refreshPassphraseStatus() {
        _uiState.update {
            it.copy(
                hasPassphrase = keystore.hasPassphrase()
            )
        }
    }
}
