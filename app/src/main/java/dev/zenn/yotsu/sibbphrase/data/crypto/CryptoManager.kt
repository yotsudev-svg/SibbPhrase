package dev.zenn.yotsu.sibbphrase.data.crypto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SibbPhrase 暗号化マネージャー。
 *
 * アーキテクチャ上の配置: データ層（data/crypto/）
 * 責務: ユーザーが入力した「合言葉（passphrase）」を用いたテキストの暗号化および復号を担当。
 *
 * 方式: AES-256-GCM
 *   - 鍵導出: PBKDF2WithHmacSHA256 (100,000イテレーション)
 *   - 暗号化のたびにランダム IV (12byte) を生成
 *   - 出力形式: Base64( IV(12) + 暗号文 + GCMタグ(16) )
 *
 * 本クラスは Hilt により @Singleton として提供され、EncryptViewModel や DecryptViewModel 等の
 * UI層コンポーネントから利用される。
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val ALGORITHM   = "AES/GCM/NoPadding"
        private const val KEY_ALGO    = "PBKDF2WithHmacSHA256"
        private const val KEY_LEN     = 256          // bit
        private const val ITER_COUNT  = 100_000      // NIST推奨
        private const val GCM_TAG_LEN = 128          // bit
        private const val IV_LEN      = 12           // byte

        /**
         * PBKDF2 用のソルト。
         *
         * 設計意図:
         * 通常、ソルトはランダム生成して暗号文に同梱するのが一般的だが、本アプリでは
         * 「異なるデバイス間でも同一の合言葉を設定すれば復号できる（秘密鍵共有方式）」
         * という要件を満たすため、アプリ固定のソルト（SibbPhraseSalt2024）を使用している。
         */
        private val SALT = "SibbPhraseSalt2024".toByteArray(Charsets.UTF_8)
    }

    /**
     * 平文を指定された合言葉で AES-256-GCM 暗号化する。
     *
     * 実行フロー:
     * 1. [passphrase] から PBKDF2 を用いて 256bit キーを導出
     * 2. 暗号化ごとにランダムな IV (12byte) を生成
     * 3. 暗号化を実行し、IV と暗号文（GCMタグ含む）を連結
     * 4. 最終的なデータを Base64 文字列として返す
     *
     * @param plainText 暗号化対象の文字列
     * @param passphrase 鍵導出に使用する合言葉
     * @return 暗号化に成功した場合は Base64 エンコード済みの暗号文（IV + 暗号文 + GCMタグの連結）。
     *         失敗時は [Result.failure]。失敗原因は実装上のバグ（不正なキー長・アルゴリズム名の誤り等）に起因する
     *         [java.security.InvalidKeyException] や [javax.crypto.NoSuchPaddingException] などが想定されるが、
     *         正常な実行環境では発生しない。
     */
    fun encrypt(plainText: String, passphrase: String): Result<String> = runCatching {
        val key    = deriveKey(passphrase)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv         = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = iv + cipherText
        Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Base64 エンコードされた暗号文を指定された合言葉で復号する。
     *
     * 実行フロー:
     * 1. Base64 文字列をデコードし、先頭 12byte から IV を抽出
     * 2. [passphrase] から PBKDF2 を用いて同一の 256bit キーを導出
     * 3. GCMParameterSpec を用いて復号を実行
     *
     * @param encoded Base64 エンコードされた暗号文 (IV + CipherText)
     * @param passphrase 鍵導出に使用する合言葉
     * @return 復号に成功した場合は平文。失敗時（合言葉の不一致、データ破損等）は Result.failure
     * @throws IllegalArgumentException データサイズが IV 長に満たない場合にスロー
     */
    fun decrypt(encoded: String, passphrase: String): Result<String> = runCatching {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_LEN) { "データが短すぎます" }

        val iv         = combined.copyOfRange(0, IV_LEN)
        val cipherText = combined.copyOfRange(IV_LEN, combined.size)

        val key    = deriveKey(passphrase)
        val spec   = GCMParameterSpec(GCM_TAG_LEN, iv)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    /**
     * パスワード（合言葉）から PBKDF2WithHmacSHA256 を用いて AES キーを導出する。
     *
     * 設計意図:
     * 総当たり攻撃に対する耐性を高めるため、NIST推奨の 100,000 回のイテレーションを実行する。
     * 導出後、メモリ安全性のために PBEKeySpec のクリア処理を行う。
     *
     * @param passphrase 入力された合言葉
     * @return 導出された 256bit の SecretKeySpec
     */
    private fun deriveKey(passphrase: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_ALGO)
        val spec    = PBEKeySpec(
            passphrase.toCharArray(),
            SALT,
            ITER_COUNT,
            KEY_LEN
        )
        val raw = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }
}
