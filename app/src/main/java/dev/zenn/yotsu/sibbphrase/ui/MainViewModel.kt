package dev.zenn.yotsu.sibbphrase.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.crypto.KeystoreManager
import dev.zenn.yotsu.sibbphrase.data.local.DataStoreManager
import dev.zenn.yotsu.sibbphrase.model.AppTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SharedNavigationEvent {
    data class GoToDecrypt(val text: String) : SharedNavigationEvent()
    data class GoToPassphrase(val message: String) : SharedNavigationEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val keystore: KeystoreManager,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SharedNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent = _navigationEvent.asSharedFlow()

    val themeMode: StateFlow<AppTheme> = dataStoreManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)


    fun onSharedText(text: String) {
        viewModelScope.launch {
            if (keystore.hasPassphrase()) {
                _navigationEvent.emit(SharedNavigationEvent.GoToDecrypt(text.trim()))
            } else {
                _navigationEvent.emit(SharedNavigationEvent.GoToPassphrase("まずは家族共通の合言葉を設定してください"))
            }
        }
    }
}
