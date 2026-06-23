package dev.zenn.yotsu.sibbphrase.biometric.presentation

data class BiometricLockState(
    // ✅ 追加：DataStore の初回読み込み完了を示すフラグ
    //    false の間は UI 側でローディング扱いにし、誤った状態表示を防ぐ
    val isInitialized: Boolean = false,
    val isUnlocked: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    // ✅ デフォルトを false に変更
    //    DataStore 読み込み前に「ハードウェアあり」と判定されるのを防ぐ
    val isHardwareAvailable: Boolean = false,
    val isEnrollmentRequired: Boolean = false,
    val errorMessage: String? = null,
    val isLockout: Boolean = false,
    val attemptCount: Int = 0
)