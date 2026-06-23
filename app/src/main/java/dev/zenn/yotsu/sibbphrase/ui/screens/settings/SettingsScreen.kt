package dev.zenn.yotsu.sibbphrase.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.zenn.yotsu.sibbphrase.biometric.presentation.BiometricLockViewModel
import dev.zenn.yotsu.sibbphrase.model.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
    biometricLockViewModel: BiometricLockViewModel = hiltViewModel()
) {
    val autoDeleteSec by vm.autoDeleteSec.collectAsState()
    val themeMode by vm.themeMode.collectAsState() // ViewModelから現在のテーマを取得
    val biometricState by biometricLockViewModel.uiState.collectAsStateWithLifecycle()

    val containerBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    val context = LocalContext.current

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

        // ✅ 追加：テーマ設定カード
        Card(
            colors = CardDefaults.cardColors(containerColor = containerBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("アプリのテーマ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // AppThemeの列挙型（SYSTEM, LIGHT, DARK）に合わせて選択肢をループ生成
                AppTheme.entries.forEach { theme ->
                    val label = when (theme) {
                        AppTheme.SYSTEM -> "システムの設定に従う"
                        AppTheme.LIGHT -> "ライトテーマ"
                        AppTheme.DARK -> "ダークテーマ"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setThemeMode(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeMode == theme),
                            onClick = { vm.setThemeMode(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, fontSize = 14.sp)
                    }
                }
            }
        }

        // 自動消去設定カード（既存のコードをそのまま配置）
        Card(
            colors = CardDefaults.cardColors(containerColor = containerBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "クリップボード自動消去時間", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "消去するまでの秒数", fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (autoDeleteSec > 5) vm.setAutoDeleteSeconds(autoDeleteSec - 5) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-", fontSize = 20.sp)
                        }
                        Text(
                            text = "${autoDeleteSec}秒",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.widthIn(min = 40.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalIconButton(
                            onClick = { if (autoDeleteSec < 120) vm.setAutoDeleteSeconds(autoDeleteSec + 5) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+", fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        Text(
            text = "セキュリティ設定",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 生体認証設定カード
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("アプリ起動時にロックを要求", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "指紋・顔認証・端末PINで保護します",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    enabled = biometricState.isHardwareAvailable,
                    checked = biometricState.isBiometricEnabled && biometricState.isHardwareAvailable,
                    onCheckedChange = { biometricLockViewModel.updateBiometricSetting(it) }
                )
            }
        }

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