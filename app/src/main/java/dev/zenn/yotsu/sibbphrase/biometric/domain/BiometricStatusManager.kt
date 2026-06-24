package dev.zenn.yotsu.sibbphrase.biometric.domain

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

/**
 * 端末の生体認証ハードウェア状態を確認するマネージャー。
 *
 * アーキテクチャ上の配置: 生体認証モジュール（biometric/domain/）
 * 責務: [BiometricManager] をラップし、生体認証センサーの利用可否を
 * アプリ独自の [Status] 型に変換して返す。
 *
 * `BiometricLockViewModel` の `init` ブロック内で DataStore 値が届いた際に呼び出され、
 * 認証ロジックの分岐（ハードウェア非搭載・未登録・利用可能など）の判定に使用される。
 * Hilt により [BiometricModule] を通じて `@Singleton` として提供される。
 *
 * @param context ApplicationContext を内部で保持しメモリリークを防ぐ。
 */
class BiometricStatusManager(context: Context) {

    // ✅ ApplicationContextを保持してリークを防ぐ
    private val biometricManager = BiometricManager.from(context.applicationContext)

    /**
     * 生体認証ハードウェアの状態を表す列挙型。
     *
     * [BiometricLockViewModel] での状態計算や [AppSplashLockScreen] での表示分岐、
     * [SettingsScreen] でのハードウェア利用可否の表示に使用される。
     */
    enum class Status {
        /** 生体認証が利用可能な状態。指紋・顔・PINのいずれかが登録済み。 */
        AVAILABLE,
        /** 生体認証センサーが搭載されていない端末。 */
        NO_HARDWARE,
        /** センサーは搭載されているが、現在利用できない状態（故障・無効化など）。 */
        UNAVAILABLE,
        /** ハードウェアは存在するが、指紋・顔などの生体情報が未登録の状態。 */
        NONE_ENROLLED
    }

    /**
     * 端末の生体認証ハードウェア状態を確認し、[Status] として返す。
     *
     * 確認対象: [Authenticators.BIOMETRIC_STRONG] および [Authenticators.DEVICE_CREDENTIAL]
     * （PIN・パターン・パスワードを含む）の両方を許可条件として評価する。
     *
     * 呼び出しタイミング:
     * - `BiometricLockViewModel.init` ブロック内で DataStore 値が届いた際。
     * - `SettingsScreen` でハードウェア利用可否をUIに反映する際。
     *
     * @return 現在のハードウェア状態を表す [Status]。
     *         [BiometricManager] の返す値がいずれにも該当しない場合は [Status.UNAVAILABLE] を返す。
     */
    fun checkBiometricStatus(): Status {
        val authenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS               -> Status.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE     -> Status.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE  -> Status.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED   -> Status.NONE_ENROLLED
            else -> Status.UNAVAILABLE
        }
    }
}