package dev.zenn.yotsu.sibbphrase.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.zenn.yotsu.sibbphrase.biometric.presentation.BiometricLockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
    // ✅ BiometricLockViewModel を Hilt から注入
    biometricLockViewModel: BiometricLockViewModel = hiltViewModel()
) {
    val autoDeleteSec by vm.autoDeleteSec.collectAsState()
    val themeMode by vm.themeMode.collectAsState()

    // ✅ 新しい生体認証の状態を取得
    val biometricState by biometricLockViewModel.uiState.collectAsStateWithLifecycle()

    val containerBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    val context = LocalContext.current

    // ❌ 以前の LaunchedEffect(Unit) { vm.checkBiometricAvailability... } は不要なので削除

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "全般設定",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        /* =========================================
           テーマ設定と自動消去タイマーのCardは変更なしのため省略
           （以前のコードのまま残してください）
           ========================================= */

        Text(
            text = "セキュリティ設定",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 生体認証設定
        Card(
            colors = CardDefaults.cardColors(containerColor = containerBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("アプリ起動時にロックを要求", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "指紋・顔認証・端末PINで保護します",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // ✅ ViewModelの isHardwareAvailable で判定
                    if (!biometricState.isHardwareAvailable) {
                        Text(
                            text = "端末に指紋・顔認証、または画面ロック（PIN等）が設定されていないため利用できません",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Switch(
                    // ✅ ハードウェアが利用可能な時だけ操作可能
                    enabled = biometricState.isHardwareAvailable,
                    // ✅ 設定がON、かつハードウェアが利用可能な時だけチェック状態にする
                    checked = biometricState.isBiometricEnabled && biometricState.isHardwareAvailable,
                    // ✅ 新しいViewModelの保存処理を呼び出す
                    onCheckedChange = { biometricLockViewModel.updateBiometricSetting(it) }
                )
            }
        }

        // ライセンス情報（変更なし）
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        TextButton(
            onClick = {
                context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "オープンソースライセンス",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "Version 1.0.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}