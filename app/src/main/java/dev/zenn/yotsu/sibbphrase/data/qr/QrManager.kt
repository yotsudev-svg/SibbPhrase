package dev.zenn.yotsu.sibbphrase.data.qr

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
        const val QR_PREFIX      = "SIBBPHRASE::"
        const val EXPIRE_SECONDS = 300L
        private  const val QR_SIZE = 512
        private  const val SEPARATOR = "::"
    }

    fun generateQrBitmap(passphrase: String): Bitmap {
        val expireAt = System.currentTimeMillis() / 1000 + EXPIRE_SECONDS
        val payload  = "$QR_PREFIX$passphrase$SEPARATOR$expireAt"

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

        val body = raw.removePrefix(QR_PREFIX)

        // 修正: 合言葉に "::" が含まれていても壊れないよう、
        // 末尾のexpireAt（数値）との区切りとして「最後の "::"」を使う。
        // （expireAtは常に数字のみのため、最後の区切りで分割すれば
        //   先頭側の合言葉に "::" が何個含まれていても正しく復元できる）
        val sepIndex = body.lastIndexOf(SEPARATOR)
        if (sepIndex == -1) return ParseResult.InvalidFormat

        val passphrase = body.substring(0, sepIndex)
        val expireAtStr = body.substring(sepIndex + SEPARATOR.length)

        if (passphrase.isEmpty()) return ParseResult.InvalidFormat
        val expireAt = expireAtStr.toLongOrNull() ?: return ParseResult.InvalidFormat

        val nowSec = System.currentTimeMillis() / 1000
        if (nowSec > expireAt) return ParseResult.Expired

        return ParseResult.Success(passphrase)
    }

    sealed class ParseResult {
        data class Success(val passphrase: String) : ParseResult()
        object Expired       : ParseResult()
        object InvalidFormat : ParseResult()
    }
}