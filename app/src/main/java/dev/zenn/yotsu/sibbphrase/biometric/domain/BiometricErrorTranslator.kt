package dev.zenn.yotsu.sibbphrase.biometric.domain

import androidx.biometric.BiometricPrompt

object BiometricErrorTranslator {
    fun translate(errorCode: Int, fallbackString: String): String {
        return when (errorCode) {
            BiometricPrompt.ERROR_LOCKOUT ->
                "認証失敗が続いたため、一時的にロックされました。しばらく待つか、スマートフォンのパスコードで解除してください。"

            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                "生体認証が完全にロックされました。スマートフォンのPINまたはパターンで制限を解除してください。"

            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED ->
                "認証がキャンセルされました。"

            BiometricPrompt.ERROR_TIMEOUT ->
                "認証時間が経過しました。もう一度お試しください。"

            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
                "端末に生体情報や暗証番号が登録されていません。端末の設定から登録してください。"

            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                "生体認証機能が現在利用できません。"

            // ✅ 追加：API 29以降。セキュリティパッチ未適用の端末で発生
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED ->
                "セキュリティアップデートが必要なため、生体認証を利用できません。端末のソフトウェアを更新してください。"

            else -> fallbackString.ifBlank { "エラーが発生しました（コード: $errorCode）" }
        }
    }
}