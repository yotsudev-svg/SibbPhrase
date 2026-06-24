package dev.zenn.yotsu.sibbphrase.data.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ユーザーが設定した「合言葉（passphrase）」を安全に永続化するためのマネージャー。
 *
 * アーキテクチャ上の配置: データ層（data/crypto/）
 * 責務: Android Keystore システムと EncryptedSharedPreferences を組み合わせ、
 * デバイス内のストレージに合言葉を暗号化した状態で保存・取得する。
 *
 * セキュリティ実装:
 * - 鍵管理: Android Keystore による AES-256 鍵生成
 * - ストレージ: `EncryptedSharedPreferences` を使用し、キー（合言葉のラベル）は AES-256 SIV、
 *   値（合言葉そのもの）は AES-256 GCM で二重に保護される。
 *
 * 本クラスで管理される合言葉は、`CryptoManager` が暗号化・復号を行う際のマスターキー導出に使用される。
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    companion object {
        /** 暗号化済みの設定ファイルを識別する名前 */
        private const val PREFS_FILE  = "sibbphrase_secure_prefs"
        /** 合言葉を保存する際のキー名 */
        private const val KEY_PHRASE  = "passphrase"
    }

    /**
     * `EncryptedSharedPreferences` の遅延初期化インスタンス。
     * 内部で Android Keystore を用いたマスターキーの生成または取得を行う。
     *
     * `lazy` で初期化を遅延させているのは、Android Keystore へのアクセスが比較的コストの高い処理であり、
     * `@Singleton` として DI 注入されるタイミング（アプリ起動直後）ではなく、
     * 合言葉へ実際にアクセスする最初のタイミングまで初期化を先送りすることで、
     * 起動時の Main Thread 負荷を軽減するためである。
     */
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

    /**
     * 新しい合言葉を暗号化して保存する。
     *
     * 呼び出しタイミング:
     * - `PassphraseViewModel` を通じた初期設定時、または合言葉の変更時。
     *
     * @param passphrase 保存する合言葉（プレーンテキスト）。内部で AES-256-GCM 暗号化されて保存される。
     */
    fun savePassphrase(passphrase: String) {
        prefs.edit().putString(KEY_PHRASE, passphrase).apply()
    }

    /**
     * 保存されている合言葉を復号して取得する。
     *
     * 呼び出しタイミング:
     * - 暗号化・復号処理の実行直前に `CryptoManager` へ渡す合言葉を取得する際。
     *
     * @return 復号された合言葉。保存されていない場合は null を返す。
     */
    fun getPassphrase(): String? = prefs.getString(KEY_PHRASE, null)

    /**
     * 合言葉が既に保存されているかどうかを確認する。
     *
     * 呼び出しタイミング:
     * - アプリ起動時の未設定状態チェックや、UI上のボタン（「合言葉を登録」か「合言葉を変更」か）の切り替え。
     *
     * @return 保存されている場合は true、未設定の場合は false。
     */
    fun hasPassphrase(): Boolean = getPassphrase() != null

    /**
     * 保存されているすべての機密情報を削除する。
     *
     * 呼び出しタイミング:
     * - `PassphraseViewModel` による設定リセット時。
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
