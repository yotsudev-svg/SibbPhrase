package dev.zenn.yotsu.sibbphrase.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QRコードの生成・解析を担当するマネージャー。
 *
 * アーキテクチャ上の配置: データ層（data/qr/）
 * 責務: 合言葉共有のためのQRコード（Bitmap）生成、およびスキャンされたペイロードのパース・有効期限チェックを行う。
 *
 * ペイロード形式: `SIBBPHRASE::<合言葉>::<有効期限UNIX秒>`
 *
 * 本クラスは Hilt により @Singleton として提供され、`QrViewModel` を通じて UI層から利用される。
 */
@Singleton
class QrManager @Inject constructor() {

    companion object {
        /** QRペイロードの識別接頭辞 */
        const val QR_PREFIX      = "SIBBPHRASE::"
        /** QRコードの有効期間（秒）。短すぎず、かつセキュリティを担保できる5分（300秒）に設定。 */
        const val EXPIRE_SECONDS = 300L
        /** 生成されるQRコードのサイズ（px） */
        private  const val QR_SIZE = 512
        /** ペイロード内の各要素を区切るセパレータ */
        private  const val SEPARATOR = "::"
    }

    /**
     * 合言葉と有効期限を含むQRコードのBitmapを生成する。
     *
     * 呼び出しタイミング:
     * - `QrShowScreen` 表示時、または有効期限切れによる再生成時。
     *
     * @param passphrase QRに埋め込む合言葉
     * @return 512x512 px のQRコードBitmap
     */
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

    /**
     * スキャンされたQRコードの生の文字列を解析し、合言葉を抽出する。
     *
     * 呼び出しタイミング:
     * - `QrScanScreen` でカメラがQRコードを検出し、その内容を解析する際。
     *
     * 設計上の重要な工夫:
     * 合言葉自体にセパレータ（"::"）が含まれている可能性を考慮し、[lastIndexOf] を使用して
     * ペイロードの末尾にある「有効期限（数字のみ）」との境界を特定している。これにより、
     * 前方の合言葉部分に何度 "::" が出現しても安全に分割・復元が可能。
     *
     * @param raw QRコードから読み取られた生のペイロード文字列
     * @return 解析結果を表す [ParseResult]
     */
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

    /**
     * QR解析の結果を表すシールダークラス。
     */
    sealed class ParseResult {
        /** 解析成功。抽出された [passphrase] を保持する。 */
        data class Success(val passphrase: String) : ParseResult()
        /** 有効期限（300秒）が切れている状態。 */
        object Expired       : ParseResult()
        /** SibbPhraseの形式ではない、またはデータが欠落している状態。 */
        object InvalidFormat : ParseResult()
    }
}
