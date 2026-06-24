package dev.zenn.yotsu.sibbphrase

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import dev.zenn.yotsu.sibbphrase.biometric.presentation.BiometricLockWrapper
import dev.zenn.yotsu.sibbphrase.ui.MainViewModel
import dev.zenn.yotsu.sibbphrase.ui.navigation.SibbPhraseApp
import dev.zenn.yotsu.sibbphrase.ui.theme.SibbPhraseTheme

/**
 * SibbPhrase アプリケーションのメインアクティビティ。
 *
 * アーキテクチャ上の配置: アプリエントリーポイント（ルート）
 * 責務:
 * 1. 他アプリからのテキスト共有（ACTION_SEND）の受信と ViewModel への配送。
 * 2. Jetpack Compose のルートコンテンツ（SibbPhraseApp）のセットアップ。
 * 3. 全画面を保護する `BiometricLockWrapper` によるセキュリティレイヤーの提供。
 *
 * 設計上の特徴:
 * `BiometricLockWrapper` を `SibbPhraseTheme` の直下に配置することで、ナビゲーションを含む
 * アプリの全UIが生体認証による保護対象となるよう設計されている。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** アプリ全体の共通状態（テーマ、共有テキストなど）を管理する ViewModel */
    private val vm: MainViewModel by viewModels()

    /**
     * アクティビティ生成時の初期化処理。
     *
     * 実行フロー:
     * 1. インテント経由で渡された共有テキストの処理。
     * 2. Compose による UI 構成。`BiometricLockWrapper` で `SibbPhraseApp` をラップし、
     *    認証済みの場合のみメインコンテンツが表示されるようにする。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)

        setContent {
            val themeMode by vm.themeMode.collectAsState()

            SibbPhraseTheme(appTheme = themeMode) {
                // ✅ ここで全体をラップするだけ！
                // 認証が必要な場合は内部でロック画面が表示され、成功するとSibbPhraseAppが表示されます
                BiometricLockWrapper {
                    SibbPhraseApp()
                }
            }
        }
    }

    /**
     * 既にアクティビティが起動している状態で新しいインテントを受け取った際の処理。
     * 主に他アプリからの「テキスト共有」が再発生した際に呼び出される。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /**
     * 渡されたインテントの内容を解析し、共有テキストが含まれている場合は ViewModel へ通知する。
     *
     * 呼び出し条件:
     * - `Intent.ACTION_SEND` かつタイプが "text/plain" の場合。
     *
     * @param intent 処理対象のインテント。
     */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND &&
            intent.type == "text/plain"
        ) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            vm.onSharedText(shared)
        }
    }
}
