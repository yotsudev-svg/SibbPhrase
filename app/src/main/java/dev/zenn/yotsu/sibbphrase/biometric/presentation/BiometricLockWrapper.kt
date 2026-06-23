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

@Composable
fun BiometricLockWrapper(
    viewModel: BiometricLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

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