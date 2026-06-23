package dev.zenn.yotsu.sibbphrase.biometric.domain

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

class BiometricStatusManager(context: Context) {

    // ✅ ApplicationContextを保持してリークを防ぐ
    private val biometricManager = BiometricManager.from(context.applicationContext)

    enum class Status {
        AVAILABLE,      // 利用可能
        NO_HARDWARE,    // センサー非搭載
        UNAVAILABLE,    // 現在利用不可（故障、無効化など）
        NONE_ENROLLED   // ハードはあるが、指紋/顔が未登録
    }

    fun checkBiometricStatus(): Status {
        val authenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS          -> Status.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Status.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Status.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED  -> Status.NONE_ENROLLED
            else -> Status.UNAVAILABLE
        }
    }
}