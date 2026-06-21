package dev.zenn.yotsu.famipass.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QRコードの生成・解析を担当
 */
@Singleton
class QrManager @Inject constructor() {

    companion object {
        const val QR_PREFIX      = "FAMIPASS::"
        const val EXPIRE_SECONDS = 300L
        private  const val QR_SIZE = 512
    }

    fun generateQrBitmap(passphrase: String): Bitmap {
        val expireAt = System.currentTimeMillis() / 1000 + EXPIRE_SECONDS
        val payload  = "$QR_PREFIX$passphrase::$expireAt"

        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bits  = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)

        return Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565).apply {
            for (x in 0 until QR_SIZE) {
                for (y in 0 until QR_SIZE) {
                    setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    fun parseQrPayload(raw: String): ParseResult {
        if (!raw.startsWith(QR_PREFIX)) return ParseResult.InvalidFormat

        val parts = raw.removePrefix(QR_PREFIX).split("::")
        if (parts.size != 2) return ParseResult.InvalidFormat

        val passphrase = parts[0]
        val expireAt   = parts[1].toLongOrNull() ?: return ParseResult.InvalidFormat
        val nowSec     = System.currentTimeMillis() / 1000

        if (nowSec > expireAt) return ParseResult.Expired

        return ParseResult.Success(passphrase)
    }

    sealed class ParseResult {
        data class Success(val passphrase: String) : ParseResult()
        object Expired       : ParseResult()
        object InvalidFormat : ParseResult()
    }
}
