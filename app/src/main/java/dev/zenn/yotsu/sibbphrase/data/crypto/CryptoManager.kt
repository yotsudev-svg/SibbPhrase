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
 * SibbPhrase 暗号化マネージャー
 *
 * 方式: AES-256-GCM
 *   - パスワード（合言葉）から PBKDF2 で 256bit キーを導出
 *   - 暗号化のたびにランダム IV(12byte) を生成
 *   - 出力形式: Base64( IV(12) + 暗号文 + GCMタグ(16) )
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
        // ソルト: アプリ固定
        private val SALT = "SibbPhraseSalt2024".toByteArray(Charsets.UTF_8)
    }

    fun encrypt(plainText: String, passphrase: String): Result<String> = runCatching {
        val key    = deriveKey(passphrase)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv         = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = iv + cipherText
        Base64.encodeToString(combined, Base64.NO_WRAP)
    }

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
