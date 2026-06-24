package dev.zenn.yotsu.sibbphrase.ui.screens.decrypt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.zenn.yotsu.sibbphrase.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 復元画面（DecryptScreen）のメインComposable。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/decrypt/）
 * 責務: 暗号文の入力インターフェースを提供し、復号結果と自動消去タイマーをユーザーに提示する。
 *
 * デザイン上の特徴:
 * IT操作に不慣れな家族が迷わないよう、大きな入力フィールド、押しやすい復元ボタン、
 * そして「60秒」のゆとりを持った自動消去タイマー表示（Timelapseアイコン付き）を採用している。
 *
 * @param vm 復元画面のロジックを管理する ViewModel。デフォルトで Hilt により注入される。
 */
@Composable
fun DecryptScreen(vm: DecryptViewModel = hiltViewModel()) {
    // --- ロジック部分（ファイルAから維持） ---
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Green configuration theme（ファイルBのデザインを反映）
    val restorePrimaryGreen = Color(0xFF2E7D32)
    val cardBg = Color(0xFFE8F5E9)
    val successTextColor = Color(0xFF1B5E20)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // タイトルバナー
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = restorePrimaryGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.decrypt_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = successTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.decrypt_description),
                        fontSize = 14.sp,
                        color = successTextColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 入力フィールドセクション（貼り付けボタン付き）
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.decrypt_input_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // クリップボードから貼り付け（ロジックはvm.onInputChangedを流用）
                TextButton(
                    onClick = {
                        val clipText = clipboardManager.getText()?.text ?: ""
                        if (clipText.isNotEmpty()) {
                            vm.onInputChanged(clipText)
                        }
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.decrypt_paste), fontSize = 14.sp)
                }
            }

            OutlinedTextField(
                value = state.inputText,
                onValueChange = vm::onInputChanged, // ロジックはファイルAのVMに完全準拠
                placeholder = { Text(stringResource(R.string.decrypt_placeholder), fontSize = 18.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (state.inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { vm.onInputChanged("") }, // クリアはinput変更ロジックを流用
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.decrypt_clear)
                            )
                        }
                    }
                }
            )
        }

        // エラーメッセージ（ロジックはファイルAのerrorMsgを使用）
        state.errorMsg?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 復元実行ボタン（大きく押しやすいデザイン）
        Button(
            onClick = {
                focusManager.clearFocus()
                vm.decrypt()
            }, // ロジックはファイルAのVMに完全準拠
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = restorePrimaryGreen)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.decrypt_button),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 結果表示セクション（ロジックはファイルAのoutputText/isCopied/timerSecondsを使用）
        AnimatedVisibility(
            visible = state.outputText.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.decrypt_result_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 復元結果の大きな表示
                        Text(
                            text = state.outputText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = successTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        // 自動消去タイマー表示（ロジックはファイルAのtimerSecondsを使用）
                        state.timerSeconds?.let { sec ->
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timelapse,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.decrypt_timer_msg, sec),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // コピーボタン（ロジックはファイルAのcopyToClipboard + vm.onCopied()を流用）
                            Button(
                                onClick = {
                                    copyToClipboard(context, state.outputText)
                                    vm.onCopied()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isCopied) MaterialTheme.colorScheme.secondary
                                    else successTextColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isCopied) stringResource(R.string.decrypt_copied) else stringResource(R.string.decrypt_copy),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 今すぐ消去ボタン（ロジックはファイルAのclearOutputを使用）
                            OutlinedButton(
                                onClick = vm::clearOutput,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.decrypt_clear_now), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * テキストをクリップボードにコピーするヘルパー関数。
 *
 * @param ctx コンテキスト
 * @param text コピー対象の文字列
 */
private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("SibbPhrase", text))
}
