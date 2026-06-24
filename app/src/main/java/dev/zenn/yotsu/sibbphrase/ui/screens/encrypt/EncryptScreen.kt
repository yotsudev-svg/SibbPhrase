package dev.zenn.yotsu.sibbphrase.ui.screens.encrypt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.zenn.yotsu.sibbphrase.R

/**
 * 暗号化画面（EncryptScreen）のメインComposable。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/encrypt/）
 * 責務: 平文の入力インターフェースを提供し、AES-256-GCMによる暗号化結果の共有・コピーをユーザーに促す。
 *
 * デザイン上の特徴:
 * 子供から高齢者まで幅広い家族層が利用することを想定し、視認性の高い大きなフォント（20sp）、
 * 押しやすいボタンサイズ（高さ64dp）、および操作意図を明確にする説明カードを採用している。
 *
 * @param vm 暗号化画面のロジックを管理する ViewModel。デフォルトで Hilt により注入される。
 */
@Composable
fun EncryptScreen(vm: EncryptViewModel = hiltViewModel()) {
    // --- ロジック部分（ファイルAから維持） ---
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // UI専用のローカルstate（ロジックには影響しない表示制御のみ）
    var showPassword by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)

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
        // 説明カード（子供からお年寄りまで見やすいデザイン）
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
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.encrypt_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.encrypt_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 入力セクション（大きく見やすいフォント）
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.encrypt_input_label),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = state.inputText,
                onValueChange = vm::onInputChanged, // ロジックはファイルAのVMに完全準拠
                placeholder = { Text(stringResource(R.string.encrypt_placeholder), fontSize = 18.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) stringResource(R.string.encrypt_visibility_hide) else stringResource(R.string.encrypt_visibility_show)
                            )
                        }
                        if (state.inputText.isNotEmpty()) {
                            IconButton(
                                onClick = { vm.onInputChanged("") }, // クリアはinput変更ロジックを流用
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.encrypt_clear_desc)
                                )
                            }
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

        // 変換実行ボタン（大きく押しやすいデザイン）
        Button(
            onClick = {
                focusManager.clearFocus()
                vm.encrypt()
            }, // ロジックはファイルAのVMに完全準拠
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.encrypt_button),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 結果表示セクション（ロジックはファイルAのoutputTextを使用）
        if (state.outputText.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.encrypt_result_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        text = state.outputText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // メイン送信ボタン：コピー＆共有
                val chooserTitle = stringResource(R.string.encrypt_share)
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(state.outputText))
                        shareText(context, state.outputText, chooserTitle)
                        vm.onCopied()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isCopied) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (state.isCopied) Icons.Default.ContentCopy else Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isCopied) stringResource(R.string.encrypt_copied) else stringResource(R.string.encrypt_send_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // サブアクション行：共有（他アプリ） / メール / コピー
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { shareText(context, state.outputText, chooserTitle) },
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.encrypt_share), fontSize = 12.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { shareText(context, state.outputText, chooserTitle) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.encrypt_email), fontSize = 12.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, state.outputText)
                            vm.onCopied()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (state.isCopied) Icons.Default.ContentCopy else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (state.isCopied) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isCopied) stringResource(R.string.encrypt_copied) else stringResource(R.string.encrypt_copy),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                TextButton(
                    onClick = vm::clearOutput, // ロジックはファイルAのVMに完全準拠
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.encrypt_clear), fontSize = 15.sp)
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

/**
 * 外部アプリとテキストを共有するための IntentChooser を起動するヘルパー関数。
 *
 * @param ctx コンテキスト
 * @param text 共有する暗号文
 * @param title 共有セレクターに表示するタイトル
 */
private fun shareText(ctx: Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, title))
}
