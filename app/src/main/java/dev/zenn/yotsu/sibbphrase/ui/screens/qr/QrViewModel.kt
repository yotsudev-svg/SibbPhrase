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

/**
 * QR表示画面（QrShowScreen）のUI状態。
 *
 * @property qrBitmap 生成されたQRコードのBitmap。
 * @property remainingSeconds 有効期限までの残り秒数。デフォルトは QrManager.EXPIRE_SECONDS (300秒)。
 * @property isExpired 有効期限が切れたかどうか。
 * @property errorMsg 合言葉未設定時などのエラーメッセージ。
 */
data class QrShowUiState(
    val qrBitmap:        Bitmap?  = null,
    val remainingSeconds: Int     = QrManager.EXPIRE_SECONDS.toInt(),
    val isExpired:        Boolean = false,
    val errorMsg:         String? = null
)

/**
 * QRスキャン画面（QrScanScreen）のUI状態。
 *
 * @property isScanning 現在カメラでスキャン中かどうか。
 * @property successMsg 解析・保存成功時のメッセージ。
 * @property errorMsg 期限切れ、形式不正、またはバリデーションエラー時のメッセージ。
 */
data class QrScanUiState(
    val isScanning:  Boolean = true,
    val successMsg:  String? = null,
    val errorMsg:    String? = null
)

/**
 * QRコードの生成（表示）およびスキャン（解析）のUI状態を管理するViewModel。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/qr/）
 * 責務: 家族間で合言葉を共有するためのQRコード生成と、スキャン結果の解析・保存を制御する。
 *
 * @property keystore 合言葉の取得・保存を行うためのマネージャー。
 * @property qrManager QRコードの生成・解析ロジックを担当するマネージャー。
 */
@HiltViewModel
class QrViewModel @Inject constructor(
    private val keystore: KeystoreManager,
    private val qrManager: QrManager
) : ViewModel() {

    private val _showState = MutableStateFlow(QrShowUiState())

    /**
     * QR表示画面の現在の状態を保持・公開するStateFlow。
     */
    val showState: StateFlow<QrShowUiState> = _showState.asStateFlow()

    private val _scanState = MutableStateFlow(QrScanUiState())

    /**
     * QRスキャン画面の現在の状態を保持・公開するStateFlow。
     */
    val scanState: StateFlow<QrScanUiState> = _scanState.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * 保存されている合言葉を元にQRコードを生成し、有効期限のカウントダウンを開始する。
     *
     * 呼び出しタイミング:
     * - `QrShowScreen` が表示された際。
     */
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

    /**
     * QRコードの有効期限（300秒）のカウントダウン処理。
     * 0秒になるとQRコードの破棄と期限切れ状態への遷移を行う。
     */
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

    /**
     * 期限切れになったQRコードを新しく再生成する。
     */
    fun regenerateQr() {
        _showState.update { it.copy(isExpired = false) }
        generateQr()
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    /**
     * カメラがQRコードを検出し、その内容（rawValue）を解析・反映する。
     *
     * 実行フロー:
     * 1. スキャン中の場合のみ処理。
     * 2. `QrManager.parseQrPayload` で合言葉を抽出（lastIndexOfを用いたセパレータ分離）。
     * 3. 成功時は `KeystoreManager` に保存。期限切れや不正形式時はエラーを表示。
     *
     * @param rawValue スキャンされたQRコードの生の文字列。
     */
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
     *
     * QRペイロード形式（SIBBPHRASE::合言葉::有効期限）ではなく、
     * 入力された合言葉をそのまま保存する。
     *
     * @param passphrase ユーザーが手動入力した合言葉。
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

    /**
     * スキャン状態をリセットし、再スキャン可能な状態にする。
     */
    fun resetScan() {
        _scanState.update { QrScanUiState() }
    }
}
