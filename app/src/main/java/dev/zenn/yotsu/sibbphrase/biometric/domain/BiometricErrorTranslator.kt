package dev.zenn.yotsu.sibbphrase.biometric.domain

import androidx.biometric.BiometricPrompt

/**
 * [BiometricPrompt] が返すエラーコードを日本語のユーザーメッセージに変換するユーティリティ。
 *
 * アーキテクチャ上の配置: 生体認証モジュール（biometric/domain/）
 * 責務: 認証失敗・エラー時のシステムエラーコードを、IT操作に不慣れな家族でも理解できる
 * 日本語メッセージへ変換する。変換された文字列は [BiometricLockViewModel.onAuthError] へ渡され、
 * [AppSplashLockScreen] 上でユーザーに表示される。
 *
 * ステートレスなユーティリティのため `object` として定義し、インスタンスを生成しない。
 */
object BiometricErrorTranslator {

    /**
     * [BiometricPrompt] のエラーコードを日本語のユーザー向けメッセージに変換する。
     *
     * 呼び出しタイミング:
     * - `BiometricLockWrapper` 内の `AuthenticationResultCallback.onAuthResult` で
     *   [AuthenticationResult.Error] が返された際。
     *
     * 設計上の注意:
     * - [BiometricPrompt.ERROR_USER_CANCELED]・[BiometricPrompt.ERROR_NEGATIVE_BUTTON]・
     *   [BiometricPrompt.ERROR_CANCELED] は原因は異なるが、いずれもユーザー操作によるキャンセルと
     *   みなして同一メッセージを返す。
     * - [BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED] は、セキュリティパッチが未適用の
     *   端末でのみ発生するコード。専用の更新案内メッセージを提供する。
     * - [fallbackString] が空白の場合はエラーコード付きのデフォルトメッセージを返す。
     *
     * @param errorCode [BiometricPrompt] が返すエラーコード定数（例: [BiometricPrompt.ERROR_LOCKOUT]）。
     * @param fallbackString エラーコードに対応する定義がない場合に使用するフォールバック文字列。
     *                       空白の場合は「エラーが発生しました（コード: X）」形式に置き換えられる。
     * @return ユーザーに提示する翻訳済みの日本語エラーメッセージ。
     */
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

            // ✅ セキュリティパッチ未適用の端末で発生するコード
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED ->
                "セキュリティアップデートが必要なため、生体認証を利用できません。端末のソフトウェアを更新してください。"

            else -> fallbackString.ifBlank { "エラーが発生しました（コード: $errorCode）" }
        }
    }
}