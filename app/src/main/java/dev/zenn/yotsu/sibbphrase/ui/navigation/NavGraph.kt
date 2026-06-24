package dev.zenn.yotsu.sibbphrase.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.zenn.yotsu.sibbphrase.ui.MainViewModel
import dev.zenn.yotsu.sibbphrase.ui.SharedNavigationEvent
import dev.zenn.yotsu.sibbphrase.ui.components.SibbPhraseTopBar
import dev.zenn.yotsu.sibbphrase.ui.components.SibbPhraseTopBarPresets
// 修正: WithCameraPermissionは廃止（QrScanScreen側に権限管理を一本化したため import削除）
import dev.zenn.yotsu.sibbphrase.ui.screens.decrypt.DecryptScreen
import dev.zenn.yotsu.sibbphrase.ui.screens.decrypt.DecryptViewModel
import dev.zenn.yotsu.sibbphrase.ui.screens.encrypt.EncryptScreen
import dev.zenn.yotsu.sibbphrase.ui.screens.onboarding.OnboardingScreen
import dev.zenn.yotsu.sibbphrase.ui.screens.onboarding.OnboardingViewModel
import dev.zenn.yotsu.sibbphrase.ui.screens.qr.QrScanScreen
import dev.zenn.yotsu.sibbphrase.ui.screens.qr.QrShowScreen
import dev.zenn.yotsu.sibbphrase.ui.screens.settings.SettingsScreen
import dev.zenn.yotsu.sibbphrase.ui.screens.passphrase.PassphraseScreen

/**
 * SibbPhrase アプリのルートComposableおよびナビゲーショングラフ定義。
 *
 * アーキテクチャ上の配置: ナビゲーション層（ui/navigation/）
 * 責務:
 * 1. [NavHost] によるアプリ全体の画面遷移グラフを定義する。
 * 2. [Scaffold] に [SibbPhraseTopBar]（カスタムTopBar）とBottomNavigationBarを組み込み、
 *    現在のルートに応じて表示内容を切り替える。
 * 3. [MainViewModel] の [SharedNavigationEvent] を購読し、他アプリからのテキスト共有に応じた
 *    ナビゲーションイベントを処理する。
 * 4. [OnboardingViewModel] のオンボーディング完了フラグに基づき、スタート画面を動的に決定する。
 *
 * BottomNavigationBarに表示される画面: [Screen.Encrypt] / [Screen.Decrypt] / [Screen.Passphrase]
 * TopBarが表示される画面: 上記3画面 + [Screen.Settings]
 * TopBarが表示されない画面: [Screen.Onboarding] / [Screen.QrShow] / [Screen.QrScan]
 *
 * @param mainVm 他アプリからのテキスト共有イベントとテーマ管理を担うViewModel。
 * @param onboardingVm オンボーディング完了フラグの管理を担うViewModel。
 */
@Composable
fun SibbPhraseApp(
    mainVm: MainViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel()
) {
    val nav              = rememberNavController()
    val currentEntry     by nav.currentBackStackEntryAsState()
    val currentRoute     = currentEntry?.destination?.route
    val isOnboardingDone by onboardingVm.isOnboardingDone.collectAsState()
    val context          = LocalContext.current

    if (isOnboardingDone == null) return

    val startDestination = remember {
        if (isOnboardingDone == true) Screen.Encrypt.route
        else Screen.Onboarding.route
    }

    // 共有インテント（ACTION_SEND）からの遷移イベントをハンドリング
    LaunchedEffect(Unit) {
        mainVm.navigationEvent.collect { event ->
            when (event) {
                is SharedNavigationEvent.GoToDecrypt -> {
                    nav.navigate(Screen.Decrypt.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                }
                is SharedNavigationEvent.GoToPassphrase -> {
                    nav.navigate(Screen.Passphrase.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                }
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Encrypt.route, Screen.Decrypt.route, Screen.Passphrase.route
    )

    val topBarSpec = when (currentRoute) {
        Screen.Encrypt.route    -> SibbPhraseTopBarPresets.Encrypt
        Screen.Decrypt.route    -> SibbPhraseTopBarPresets.Decrypt
        Screen.Passphrase.route -> SibbPhraseTopBarPresets.Passphrase
        Screen.Settings.route   -> SibbPhraseTopBarPresets.Settings
        else -> null
    }

    Scaffold(
        topBar = {
            topBarSpec?.let { spec ->
                SibbPhraseTopBar(
                    title = spec.title,
                    backgroundColor = spec.getBackgroundColor(),
                    onSettingsClick = if (currentRoute != Screen.Settings.route && showBottomBar) {
                        { nav.navigate(Screen.Settings.route) }
                    } else null,
                    onBackClick = if (currentRoute == Screen.Settings.route) {
                        { nav.popBackStack() }
                    } else null
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    listOf(Screen.Encrypt, Screen.Decrypt, Screen.Passphrase).forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick  = {
                                nav.navigate(screen.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = {
                                when (screen) {
                                    Screen.Encrypt    -> Icon(Icons.Outlined.Lock,     contentDescription = screen.label)
                                    Screen.Decrypt    -> Icon(Icons.Outlined.LockOpen, contentDescription = screen.label)
                                    Screen.Passphrase -> Icon(Icons.Outlined.Key,      contentDescription = screen.label)
                                    else              -> {}
                                }
                            },
                            label = { Text(screen.label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = nav,
            startDestination = startDestination,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        onboardingVm.markOnboardingDone()
                        nav.navigate(Screen.Passphrase.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Encrypt.route)  { EncryptScreen() }
            composable(Screen.Decrypt.route)  {
                val decryptVm: DecryptViewModel = hiltViewModel()

                LaunchedEffect(Unit) {
                    mainVm.navigationEvent.collect { event ->
                        if (event is SharedNavigationEvent.GoToDecrypt) {
                            decryptVm.setSharedText(event.text)
                        }
                    }
                }

                DecryptScreen(vm = decryptVm)
            }
            composable(Screen.Passphrase.route) { PassphraseScreen(nav = nav) }
            composable(Screen.QrShow.route)   { QrShowScreen(navController = nav) }
            // 修正: WithCameraPermissionラッパーを除去。
            // 権限・ハードウェアチェックはQrScanScreen内に一本化。
            composable(Screen.QrScan.route)   {
                QrScanScreen(navController = nav, onSuccess = { nav.popBackStack() })
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}