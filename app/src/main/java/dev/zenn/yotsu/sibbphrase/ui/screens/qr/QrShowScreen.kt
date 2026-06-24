@file:OptIn(ExperimentalMaterial3Api::class)
package dev.zenn.yotsu.sibbphrase.ui.screens.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.zenn.yotsu.sibbphrase.R
import dev.zenn.yotsu.sibbphrase.data.qr.QrManager

/**
 * QRコード表示画面（QrShowScreen）のメインComposable。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/screens/qr/）
 * 責務: 保存されている合言葉を元にQRコードを生成・表示し、家族間での合言葉共有を支援する。
 *
 * デザイン上の特徴:
 * 共有の緊急性と安全性のバランスを考慮し、300秒（5分）の有効期限カウントダウンを
 * 視覚的なプログレスバーとともに提示。期限切れ時には直感的な再生成ボタンを提供することで、
 * IT操作に不慣れな家族でも迷わず安全に共有を完了できるよう設計されている。
 *
 * @param navController 画面遷移を制御するコントローラー。
 * @param vm QR生成・表示のロジックを管理する ViewModel。デフォルトで Hilt により注入される。
 */
@Composable
fun QrShowScreen(
    navController: NavController,
    vm: QrViewModel = hiltViewModel()
) {
    // --- ロジック部分（ファイルAから維持） ---
    val state by vm.showState.collectAsState()

    LaunchedEffect(Unit) { vm.generateQr() }

    val themeColor = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.qr_show_title), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        // 説明バナーカード（ファイルBの統一デザインに合わせて追加）
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
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.qr_show_banner_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.qr_show_banner_desc),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 状態に応じた表示（ロジックはファイルAのstateをすべて維持）
        when {
            state.errorMsg != null -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        state.errorMsg!!,
                        modifier = Modifier.padding(20.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            state.isExpired -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.qr_show_expired_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = vm::regenerateQr, // ロジックはファイルAのVM処理を維持
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.qr_show_regenerate), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            state.qrBitmap != null -> {
                // QRコード表示カード（ファイルBスタイルの大きな白背景カード）
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = state.qrBitmap!!.asImageBitmap(),
                                contentDescription = stringResource(R.string.qr_show_desc),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 有効期限プログレスバー（ロジックはファイルAのremainingSecondsを維持）
                        val progress = state.remainingSeconds.toFloat() / QrManager.EXPIRE_SECONDS.toFloat()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progress > 0.3f) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.qr_show_expiry_msg, state.remainingSeconds),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (progress > 0.3f) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.qr_show_warning),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
}
