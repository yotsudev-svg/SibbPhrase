package dev.zenn.yotsu.sibbphrase.biometric.presentation

/**
 * アプリ全体の生体認証ロック状態を表すUI状態クラス。
 *
 * アーキテクチャ上の配置: 生体認証モジュール（biometric/presentation/）
 * 責務: [BiometricLockViewModel] が管理・更新し、[BiometricLockWrapper] および
 * [AppSplashLockScreen] がこの状態を購読してUIの表示切り替えに使用する。
 *
 * 初期値はすべて「未初期化・ロック状態」に設定されており、DataStore の初回読み込みが完了する前に
 * 誤った状態でUIが表示されることを防いでいる。
 */
data class BiometricLockState(
    /**
     * DataStore の初回読み込みが完了したかどうかを示すフラグ。
     *
     * `false` の間は [BiometricLockWrapper] 側でローディングインジケーターを表示し、
     * 初期化前に誤ったロック状態が描画されることを防ぐ。
     * DataStore から最初の値が届いた時点で `true` に更新される。
     */
    val isInitialized: Boolean = false,

    /**
     * アプリがアンロック（認証済み）状態かどうかを示すフラグ。
     *
     * `true` の場合は [BiometricLockWrapper] の `content`（メインUI）が表示され、
     * `false` の場合は [AppSplashLockScreen] が表示される。
     * 認証成功時に `true`、バックグラウンド移行時に `false` へ更新される。
     */
    val isUnlocked: Boolean = false,

    /**
     * 生体認証がユーザー設定でONになっているかどうかを示すフラグ。
     *
     * [BiometricSettingStorage] から取得した DataStore 値を反映する。
     * `false` の場合、ロック機能は無効化されアプリは常にアンロック状態となる。
     */
    val isBiometricEnabled: Boolean = false,

    /**
     * 端末の生体認証ハードウェアが利用可能かどうかを示すフラグ。
     *
     * デフォルトを `false` に設定することで、DataStore 読み込み前に
     * 「ハードウェアあり」と誤判定されることを防ぐ。
     * [BiometricStatusManager.Status.AVAILABLE] または [BiometricStatusManager.Status.NONE_ENROLLED]
     * の場合に `true` となる。
     */
    val isHardwareAvailable: Boolean = false,

    /**
     * ハードウェアは存在するが生体情報（指紋・顔）が未登録の状態かどうかを示すフラグ。
     *
     * `true` の場合、[AppSplashLockScreen] は再試行ボタンではなく
     * 端末設定への誘導ボタン（生体情報登録画面へのリンク）を表示する。
     */
    val isEnrollmentRequired: Boolean = false,

    /**
     * ユーザーに提示するエラーメッセージ。
     *
     * 認証エラー発生時に [BiometricErrorTranslator] で変換された日本語メッセージが格納される。
     * 認証成功時やバックグラウンド移行時には `null` にリセットされる。
     */
    val errorMessage: String? = null,

    /**
     * 連続認証失敗によるシステムロックアウト状態かどうかを示すフラグ。
     *
     * `true` の場合、[AppSplashLockScreen] は再試行ボタンを非表示にし、
     * PIN による解除を案内するテキストのみを表示する。
     * [BiometricPrompt.ERROR_LOCKOUT] または [BiometricPrompt.ERROR_LOCKOUT_PERMANENT]
     * 発生時に `true` となる。
     */
    val isLockout: Boolean = false,

    /**
     * 連続認証失敗の回数。
     *
     * [BiometricLockViewModel.onAuthAttemptFailed] が呼ばれるたびにインクリメントされ、
     * 認証成功時に 0 にリセットされる。[AppSplashLockScreen] 上でロックアウト前の
     * 失敗回数表示に使用される。
     */
    val attemptCount: Int = 0
)