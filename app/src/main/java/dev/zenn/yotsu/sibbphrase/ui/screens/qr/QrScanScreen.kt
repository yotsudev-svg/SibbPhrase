package dev.zenn.yotsu.sibbphrase.ui.screens.qr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.navigation.NavController
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.zenn.yotsu.sibbphrase.R
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/** Composeのcontextからアクティビティを辿って取得するヘルパー */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalGetImage::class, ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    navController: NavController,
    onSuccess: () -> Unit,
    vm: QrViewModel = hiltViewModel()
) {
    // --- ロジック部分（ファイルAから維持） ---
    val state          by vm.scanState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context        = LocalContext.current
    val activity       = remember(context) { context.findActivity() }

    LaunchedEffect(state.successMsg) {
        if (state.successMsg != null) {
            delay(1500)
            onSuccess()
        }
    }

    // --- カメラハードウェア有無チェック ---
    val hasCameraHardware = remember(context) {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val ids = manager?.cameraIdList
            ids != null && ids.isNotEmpty() && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } catch (e: Exception) {
            false
        }
    }

    // --- カメラ権限管理（NavGraph側のWithCameraPermissionは廃止し、ここに一本化） ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    // 修正: 「今後表示しない」判定のため、要求を一度でも行ったかを記録する
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(hasCameraHardware) {
        if (hasCameraHardware && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 修正: 「今後表示しない」が選択された状態の判定。
    // ・権限がまだ無い
    // ・一度はリクエストを行った（permissionRequested）
    // ・かつ shouldShowRequestPermissionRationale が false
    //   （初回未リクエストの場合もfalseになるため、permissionRequestedと組み合わせて判定する）
    val isPermanentlyDenied = !hasCameraPermission && permissionRequested && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    var manualKey by remember { mutableStateOf("") }

    // --- カメラリソース（Provider/Executor）をComposable全体で保持し、離脱時に解放する ---
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                // バインド前に離脱した場合などは無視
            }
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // 成功表示
            state.successMsg != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(stringResource(R.string.qr_scan_success_icon), fontSize = 56.sp)
                            Text(
                                state.successMsg!!,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // カメラが使えない場合：手動入力フォールバック
            !hasCameraHardware -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "カメラ無効",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.qr_scan_camera_unavailable),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.qr_scan_camera_unavailable_desc),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = manualKey,
                                onValueChange = { manualKey = it },
                                placeholder = { Text(stringResource(R.string.qr_scan_manual_placeholder), fontSize = 16.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                            )
                            Button(
                                onClick = {
                                    if (manualKey.trim().isNotEmpty()) {
                                        vm.applyManualPassphrase(manualKey.trim())
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.qr_scan_manual_submit), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    state.errorMsg?.let { msg ->
                        Spacer(Modifier.height(12.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            // 権限が許可されていない場合
            !hasCameraPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "カメラ無効",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isPermanentlyDenied) stringResource(R.string.qr_scan_permission_denied) else stringResource(R.string.qr_scan_permission_required),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isPermanentlyDenied)
                            stringResource(R.string.qr_scan_permission_denied_desc)
                        else
                            stringResource(R.string.qr_scan_permission_required_desc),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(20.dp))

                    if (isPermanentlyDenied) {
                        // 修正: 「今後表示しない」状態では再リクエストしても無反応のため、
                        // アプリ詳細設定画面を直接開く導線を用意する
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.qr_scan_open_settings), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.qr_scan_grant_permission), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 通常のスキャン画面
            else -> {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)

                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                            val scanner = BarcodeScanning.getClient(options)

                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { ia ->
                                    ia.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val img = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.process(img)
                                                .addOnSuccessListener { barcodes ->
                                                    barcodes.firstOrNull()?.rawValue?.let { raw ->
                                                        vm.onQrScanned(raw)
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // スキャン枠ガイド
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(24.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.75f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.qr_scan_guide),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // エラーメッセージ
                state.errorMsg?.let { msg ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                msg,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}