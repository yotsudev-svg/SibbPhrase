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
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.zenn.yotsu.sibbphrase.model.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel()
) {
    val autoDeleteSec by vm.autoDeleteSec.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
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

        // テーマ設定
        Card(
            colors = CardDefaults.cardColors(containerColor = containerBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("テーマ設定", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppTheme.entries.forEachIndexed { index, theme ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AppTheme.entries.size),
                            onClick = { vm.setThemeMode(theme) },
                            selected = themeMode == theme,
                            label = { 
                                Text(
                                    text = when(theme) {
                                        AppTheme.SYSTEM -> "システム"
                                        AppTheme.LIGHT -> "ライト"
                                        AppTheme.DARK -> "ダーク"
                                    },
                                    fontSize = 13.sp
                                ) 
                            }
                        )
                    }
                }
            }
        }

        // 自動消去タイマー設定（PassphraseScreenから移設）
        Card(
            colors = CardDefaults.cardColors(containerColor = containerBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("自動消去タイマー", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "復元後 $autoDeleteSec 秒で自動消去",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { sec ->
                        FilterChip(
                            selected = autoDeleteSec == sec,
                            onClick  = { vm.setAutoDeleteSeconds(sec) },
                            label    = { Text("${sec}秒", fontSize = 15.sp) }
                        )
                    }
                }
                Text(
                    text = "※復元されたパスワードが表示された後、指定した秒数が経過すると画面から自動的に消去されます。セキュリティ向上のための機能です。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // ライセンス情報（将来的な拡張例）
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
