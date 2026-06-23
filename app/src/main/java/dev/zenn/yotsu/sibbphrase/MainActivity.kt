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


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND &&
            intent.type == "text/plain"
        ) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            vm.onSharedText(shared)
        }
    }
}