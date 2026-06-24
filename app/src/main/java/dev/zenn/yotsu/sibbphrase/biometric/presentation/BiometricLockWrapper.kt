package dev.zenn.yotsu.sibbphrase.biometric.presentation

import android.view.WindowManager
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.BiometricPrompt
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel // ✅ 正しいパッケージ
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zenn.yotsu.sibbphrase.biometric.domain.BiometricErrorTranslator

/**
 * アプリ全体を生体認証で保護するComposableラッパー。
 *
 * アーキテクチャ上の配置: 生体認証モジュール（biometric/presentation/）
 * 責務: [BiometricLockViewModel] と連携し、アプリのライフサイクルに応じた
 * 自動ロック・認証トリガー・スクリーンショット防止（FLAG_SECURE）を制御する。
 *
 * [MainActivity] の `setContent` 直下で `SibbPhraseApp` をこのComposableでラップすることで、
 * ナビゲーションを含むアプリの全UIが生体認証による保護対象となる。
 *
 * 表示の切り替え:
 * - `isInitialized` が `false` の間: [CircularProgressIndicator] によるローディングを表示。
 * - `isUnlocked` が `true` の場合: `content`（メインUI）を表示。
 * - `isUnlocked` が `false` の場合: [AppSplashLockScreen] を表示。
 *
 * ライフサイクル連動（[DisposableEffect] + [LifecycleEventObserver]）:
 * - [Lifecycle.Event.ON_STOP]: [BiometricLockViewModel.onAppBackground] を呼び出し、
 *   アプリを即座にロック状態へ遷移させる。
 * - [Lifecycle.Event.ON_START]: [BiometricLockViewModel.triggerAuthenticationIfNeeded] を呼び出し、
 *   必要に応じて認証ダイアログを再表示する。
 *
 * FLAG_SECURE 制御:
 * [SideEffect] 内で `isUnlocked` の状態に応じて [WindowManager.LayoutParams.FLAG_SECURE] を付け外しし、
 * ロック中のスクリーンショット・画面録画を防止する。
 * FLAG_SECURE の制御は [SideEffect] と [DisposableEffect] の2箇所で管理しており、
 * [SideEffect] が再コンポーズのたびに `isUnlocked` に応じてフラグを動的に付け外しするのに対し、
 * `onDispose` を持つ [DisposableEffect(Unit)] はナビゲーション等によってこの Composable が
 * ツリーから取り除かれた際にもフラグを確実に解除するフェイルセーフとして機能する。
 *
 * 認証イベントの収集:
 * `LaunchedEffect(Unit)` 内で [BiometricLockViewModel.triggerAuthEvent] を収集し、
 * イベント受信時に `launcher.launch(authRequest)` で認証ダイアログを表示する。
 * `Channel.CONFLATED` の採用により、Composable の初期化タイミングとの競合によるイベント消失を防いでいる。
 *
 * @param viewModel 生体認証ロック状態を管理するViewModel。デフォルトで Hilt により注入される。
 * @param content 認証成功後に表示するメインUIのComposableコンテンツ。
 */
@Composable
fun BiometricLockWrapper(
    viewModel: BiometricLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ フォールバックに DeviceCredential（PIN/パターン/パスワード）を指定することで、
    //    生体情報未登録端末でも認証を完了できるようにし、
    //    生体認証のロックアウト時にもデバイス認証で突破できるようにしている。
    val authRequest = remember {
        AuthenticationRequest.biometricRequest(
            title = "アプリの保護",
            AuthenticationRequest.Biometric.Fallback.DeviceCredential
        ) {
            setSubtitle("指紋や顔、またはPINを使用してロックを解除します")
        }
    }

    val launcher = rememberAuthenticationLauncher(
        resultCallback = remember {
            object : AuthenticationResultCallback {
                override fun onAuthResult(result: AuthenticationResult) {
                    when (result) {
                        is AuthenticationResult.Success -> viewModel.onAuthSuccess()
                        is AuthenticationResult.Error -> {
                            val isLockout =
                                result.errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                                        result.errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
                            val message = BiometricErrorTranslator.translate(
                                result.errorCode,
                                result.errString?.toString().orEmpty()
                            )
                            viewModel.onAuthError(message, isLockout)
                        }
                        is AuthenticationResult.CustomFallbackSelected -> {
                            // Android Baklava(36.1)以降の複数カスタムフォールバック選択時
                        }
                    }
                }

                override fun onAuthAttemptFailed() {
                    viewModel.onAuthAttemptFailed()
                }
            }
        }
    )

    // ✅ Channel.receiveAsFlow() により、subscriber 不在時のイベントも確実に1回届く
    //    LaunchedEffect(uiState.isInitialized) による2重トリガーは不要になったため削除
    LaunchedEffect(Unit) {
        viewModel.triggerAuthEvent.collect {
            launcher.launch(authRequest)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP  -> viewModel.onAppBackground()
                Lifecycle.Event.ON_START -> viewModel.triggerAuthenticationIfNeeded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SideEffect {
        if (!uiState.isUnlocked) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // ✅ ライフサイクル監視の DisposableEffect(lifecycleOwner) とは独立して定義することで、
    //    ナビゲーション等によって BiometricLockWrapper が Composable ツリーから取り除かれた際にも
    //    FLAG_SECURE が確実に解除されるフェイルセーフとして機能させている。
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    when {
        !uiState.isInitialized -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.isUnlocked -> content()
        else -> AppSplashLockScreen(
            uiState = uiState,
            onRetryClick = { viewModel.triggerAuthentication() }
        )
    }
}