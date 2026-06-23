package dev.zenn.yotsu.sibbphrase.biometric.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.zenn.yotsu.sibbphrase.R

@Composable
fun AppSplashLockScreen(
    uiState: BiometricLockState,
    onRetryClick: () -> Unit
) {
    val context = LocalContext.current

    // ✅ 生体情報未登録時に設定画面へ遷移するランチャー
    //    戻ってきたタイミングで ViewModel の DataStore 監視が自動的に状態を再評価する
    val enrollLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* 登録後の状態更新は DataStore の Flow が自動検知 */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                // ✅ contentDescription を日本語に統一（TalkBack 対応）
                contentDescription = stringResource(R.string.lock_screen_content_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.lock_screen_protected),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.attemptCount > 0 && !uiState.isLockout) {
                Text(
                    text = stringResource(R.string.lock_screen_fail_count, uiState.attemptCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isLockout)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            when {
                // ロックアウト中：PIN でのロック解除を案内
                uiState.isLockout -> {
                    Text(
                        text = stringResource(R.string.lock_screen_lockout_guide),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // ✅ 生体情報未登録：設定画面へ誘導
                //    API 30 以上は ACTION_BIOMETRIC_ENROLL で直接登録画面へ
                //    API 29 以下はセキュリティ設定画面へフォールバック
                uiState.isEnrollmentRequired -> {
                    Text(
                        text = stringResource(R.string.lock_screen_enroll_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                    putExtra(
                                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                    )
                                }
                                enrollLauncher.launch(intent)
                            } else {
                                // API 29以下：セキュリティ設定画面へフォールバック
                                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                            }
                        } catch (e: ActivityNotFoundException) {
                            // 一部メーカーで Intent が未実装の場合のフォールバック
                            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                        }
                    }) {
                        Text(stringResource(R.string.lock_screen_enroll_button))
                    }
                }

                // 通常：再試行ボタン
                else -> {
                    Button(
                        onClick = onRetryClick,
                        enabled = uiState.isHardwareAvailable
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.lock_screen_retry))
                    }
                }
            }
        }
    }
}