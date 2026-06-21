package dev.zenn.yotsu.famipass.data.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 合言葉を Android Keystore + EncryptedSharedPreferences で保存する
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    companion object {
        private const val PREFS_FILE  = "famipass_secure_prefs"
        private const val KEY_PHRASE  = "passphrase"
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            ctx,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePassphrase(passphrase: String) {
        prefs.edit().putString(KEY_PHRASE, passphrase).apply()
    }

    fun getPassphrase(): String? = prefs.getString(KEY_PHRASE, null)

    fun hasPassphrase(): Boolean = getPassphrase() != null

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
