package dev.zenn.yotsu.sibbphrase.ui.screens.passphrase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.zenn.yotsu.sibbphrase.R
import dev.zenn.yotsu.sibbphrase.ui.navigation.Screen

/**
 * 合言葉管理画面（PassphraseScreen）のメインComposable。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/passphrase/）
 * 責務: 合言葉の登録・変更・リセット、および家族間共有のためのQR表示・スキャン画面への遷移を管理する。
 *
 * デザイン上の特徴:
 * 家族で統一された秘密鍵（合言葉）を扱うためのハブとなる画面であり、
 * セキュリティ上の重要性を伝える説明バナー、視認性の高い入力フィールド、
 * 誤操作を防ぐための確認ダイアログを採用している。
 *
 * @param nav 画面遷移を制御する NavController。QR表示/スキャン画面への遷移に使用される。
 * @param vm 合言葉管理のロジックを管理する ViewModel。デフォルトで Hilt により注入される。
 */
@Composable
fun PassphraseScreen(
    nav: NavController?  = null,
    vm:  PassphraseViewModel = hiltViewModel()
) {
    // --- ロジック部分 ---
    val state by vm.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

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
            title   = { Text(stringResource(R.string.passphrase_dialog_change_title)) },
            text    = { Text(stringResource(R.string.passphrase_dialog_change_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showChangeDialog = false
                    newPass = ""
                    vm.enterEditMode()
                }) { Text(stringResource(R.string.passphrase_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showChangeDialog = false }) { Text(stringResource(R.string.passphrase_cancel)) }
            }
        )
    }

    // ダイアログ：リセット確認
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title   = { Text(stringResource(R.string.passphrase_dialog_reset_title)) },
            text    = { Text(stringResource(R.string.passphrase_dialog_reset_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    newPass = ""
                    vm.resetPassphrase()
                }) { Text(stringResource(R.string.passphrase_ok), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.passphrase_cancel)) }
            }
        )
    }

    val errorShortMsg = stringResource(R.string.passphrase_error_too_short)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
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
                        text = stringResource(R.string.passphrase_banner_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.passphrase_banner_desc),
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
                text = if (isEnabled) stringResource(R.string.passphrase_input_label_new) else stringResource(R.string.passphrase_input_label_saved),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (state.hasPassphrase && !state.isEditing) {
                Text(
                    text = stringResource(R.string.passphrase_status_set),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedTextField(
                value = if (isEnabled) newPass else "••••••••",
                onValueChange = { newPass = it; errorMsg = null },
                enabled = isEnabled,
                placeholder = { Text(stringResource(R.string.passphrase_placeholder), fontSize = 18.sp) },
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
                                contentDescription = if (showPass) stringResource(R.string.passphrase_visibility_hide) else stringResource(R.string.passphrase_visibility_show)
                            )
                        }
                    }
                }
            )

            errorMsg?.let {
                Text(errorShortMsg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (savedMsg) {
                Text(stringResource(R.string.passphrase_save_success), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            if (isEnabled) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        when {
                            newPass.length < 4     -> errorMsg = errorShortMsg
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
                    Text(stringResource(R.string.passphrase_save_button), fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.passphrase_change_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.passphrase_reset_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    text = stringResource(R.string.passphrase_qr_share),
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
                    text = stringResource(R.string.passphrase_qr_scan),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

