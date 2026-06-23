package dev.zenn.yotsu.sibbphrase.ui.screens.qr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.crypto.KeystoreManager
import dev.zenn.yotsu.sibbphrase.data.qr.QrManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrShowUiState(
    val qrBitmap:        Bitmap?  = null,
    val remainingSeconds: Int     = QrManager.EXPIRE_SECONDS.toInt(),
    val isExpired:        Boolean = false,
    val errorMsg:         String? = null
)

data class QrScanUiState(
    val isScanning:  Boolean = true,
    val successMsg:  String? = null,
    val errorMsg:    String? = null
)

@HiltViewModel
class QrViewModel @Inject constructor(
    private val keystore: KeystoreManager,
    private val qrManager: QrManager
) : ViewModel() {

    private val _showState = MutableStateFlow(QrShowUiState())
    val showState: StateFlow<QrShowUiState> = _showState.asStateFlow()

    private val _scanState = MutableStateFlow(QrScanUiState())
    val scanState: StateFlow<QrScanUiState> = _scanState.asStateFlow()

    private var countdownJob: Job? = null

    fun generateQr() {
        val passphrase = keystore.getPassphrase() ?: run {
            _showState.update { it.copy(errorMsg = "先に合言葉を設定してください（設定タブ）") }
            return
        }

        val bitmap = qrManager.generateQrBitmap(passphrase)
        _showState.update {
            it.copy(
                qrBitmap         = bitmap,
                remainingSeconds = QrManager.EXPIRE_SECONDS.toInt(),
                isExpired        = false,
                errorMsg         = null
            )
        }
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = QrManager.EXPIRE_SECONDS.toInt()
            while (remaining > 0) {
                delay(1000)
                remaining--
                _showState.update { it.copy(remainingSeconds = remaining) }
            }
            _showState.update { it.copy(qrBitmap = null, isExpired = true) }
        }
    }

    fun regenerateQr() {
        _showState.update { it.copy(isExpired = false) }
        generateQr()
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    fun onQrScanned(rawValue: String) {
        if (!_scanState.value.isScanning) return
        _scanState.update { it.copy(isScanning = false) }

        when (val result = qrManager.parseQrPayload(rawValue)) {
            is QrManager.ParseResult.Success -> {
                keystore.savePassphrase(result.passphrase)
                _scanState.update { it.copy(successMsg = "✅ 設定完了！家族と同じ合言葉が設定されました") }
            }
            is QrManager.ParseResult.Expired -> {
                _scanState.update {
                    it.copy(
                        isScanning = true,
                        errorMsg   = "QRコードの有効期限が切れています。\n親機で再生成してください"
                    )
                }
            }
            is QrManager.ParseResult.InvalidFormat -> {
                _scanState.update {
                    it.copy(
                        isScanning = true,
                        errorMsg   = "SibbPhraseのQRコードではありません"
                    )
                }
            }
        }
    }

    /**
     * カメラが使えない端末向けの手動入力フォールバック。
     * QRペイロード形式（SIBBPHRASE::合言葉::有効期限）ではなく、
     * 入力された合言葉をそのまま保存する。
     */
    fun applyManualPassphrase(passphrase: String) {
        if (!_scanState.value.isScanning) return

        if (passphrase.isBlank()) {
            _scanState.update { it.copy(errorMsg = "合言葉を入力してください") }
            return
        }

        _scanState.update { it.copy(isScanning = false) }
        keystore.savePassphrase(passphrase)
        _scanState.update {
            it.copy(successMsg = "✅ 設定完了！家族と同じ合言葉が設定されました")
        }
    }

    fun resetScan() {
        _scanState.update { QrScanUiState() }
    }
}