package dev.zenn.yotsu.sibbphrase.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.local.DataStoreManager
import dev.zenn.yotsu.sibbphrase.model.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    val autoDeleteSec: StateFlow<Int> = dataStoreManager.autoDeleteSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val themeMode: StateFlow<AppTheme> = dataStoreManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    fun setAutoDeleteSeconds(sec: Int) {
        viewModelScope.launch {
            dataStoreManager.setAutoDeleteSeconds(sec)
        }
    }

    fun setThemeMode(theme: AppTheme) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(theme)
        }
    }
}