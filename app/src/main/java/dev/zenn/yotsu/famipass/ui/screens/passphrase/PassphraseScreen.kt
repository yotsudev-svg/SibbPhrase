package dev.zenn.yotsu.famipass.ui.screens.passphrase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.zenn.yotsu.famipass.ui.navigation.Screen

@Composable
fun PassphraseScreen(
    nav: NavController?  = null,
    vm:  PassphraseViewModel = hiltViewModel()
) {
    // --- ロジック部分 ---
    val state by vm.uiState.collectAsState()

    var newPass     by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var savedMsg    by remember { mutableStateOf(false) }

    var showChangeDialog by remember { mutableStateOf(false) }
    var showResetDialog  by remember { mutableStateOf(false) }

    val passphraseColor = MaterialTheme.colorScheme.secondary
    val containerBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)

    // ダイアログ：変更確認
    if (showChangeDialog) {
        AlertDialog(
            onDismissRequest = { showChangeDialog = false },
            title   = { Text("合言葉を変更しますか？") },
            text    = { Text("現在の合言葉は上書きされます。") },
            confirmButton = {
                TextButton(onClick = {
                    showChangeDialog = false
                    newPass = ""
                    vm.enterEditMode()
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeDialog = false }) { Text("キャンセル") }
            }
        )
    }

    // ダイアログ：リセット確認
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title   = { Text("合言葉をリセットしますか？") },
            text    = { Text("設定した合言葉が削除され、初期状態に戻ります。") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    newPass = ""
                    vm.resetPassphrase()
                }) { Text("OK", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("キャンセル") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 説明バナーカード
        Card(
            colors = CardDefaults.cardColors(containerColor = containerBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = passphraseColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "家族の秘密の合言葉を登録",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "暗号を変換・復元するために使う家族だけの「秘密の合言葉」です。同じ合言葉を設定した家族間でのみパスワードを復元できます。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 合言葉 設定/変更セクション
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isEnabled = !state.hasPassphrase || state.isEditing

            Text(
                text = if (isEnabled) "合言葉を設定する" else "設定済みの合言葉",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (state.hasPassphrase && !state.isEditing) {
                Text(
                    text = "✅ 合言葉が設定済みです",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedTextField(
                value = if (isEnabled) newPass else "••••••••",
                onValueChange = { newPass = it; errorMsg = null },
                enabled = isEnabled,
                placeholder = { Text("例: yamadake2026 などの文字", fontSize = 18.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                visualTransformation = if (showPass || !isEnabled) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    if (isEnabled) {
                        IconButton(
                            onClick = { showPass = !showPass },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPass) "合言葉を隠す" else "合言葉を表示"
                            )
                        }
                    }
                }
            )

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (savedMsg) {
                Text("✅ 保存しました", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            if (isEnabled) {
                Button(
                    onClick = {
                        when {
                            newPass.length < 4     -> errorMsg = "合言葉は4文字以上にしてください"
                            else -> {
                                vm.savePassphrase(newPass)
                                newPass     = ""
                                savedMsg    = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = passphraseColor)
                ) {
                    Text("保存する", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showChangeDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = passphraseColor)
                    ) {
                        Text("変更する", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("リセット", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // QR連携セクション（ロジックはファイルAのnav.navigateを維持）
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { nav?.navigate(Screen.QrShow.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = passphraseColor)
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "QRコードで共有する（親機）",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { nav?.navigate(Screen.QrScan.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "家族のQRコードを読み取る（子機）",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
