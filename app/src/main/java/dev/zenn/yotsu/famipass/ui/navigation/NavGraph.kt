package dev.zenn.yotsu.famipass.ui.navigation

import android.widget.Toast
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
import dev.zenn.yotsu.famipass.ui.MainViewModel
import dev.zenn.yotsu.famipass.ui.SharedNavigationEvent
import dev.zenn.yotsu.famipass.ui.components.FamiPassTopBar
import dev.zenn.yotsu.famipass.ui.components.FamiPassTopBarPresets
import dev.zenn.yotsu.famipass.ui.components.WithCameraPermission
import dev.zenn.yotsu.famipass.ui.screens.decrypt.DecryptScreen
import dev.zenn.yotsu.famipass.ui.screens.decrypt.DecryptViewModel
import dev.zenn.yotsu.famipass.ui.screens.encrypt.EncryptScreen
import dev.zenn.yotsu.famipass.ui.screens.onboarding.OnboardingScreen
import dev.zenn.yotsu.famipass.ui.screens.onboarding.OnboardingViewModel
import dev.zenn.yotsu.famipass.ui.screens.qr.QrScanScreen
import dev.zenn.yotsu.famipass.ui.screens.qr.QrShowScreen
import dev.zenn.yotsu.famipass.ui.screens.settings.SettingsScreen
import dev.zenn.yotsu.famipass.ui.screens.passphrase.PassphraseScreen
import dev.zenn.yotsu.famipass.ui.screens.passphrase.PassphraseViewModel

@Composable
fun FamiPassApp(
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

    // ロジックは既存のshowBottomBarと同一条件（トップバーもこの3画面でのみ表示）
    val showBottomBar = currentRoute in listOf(
        Screen.Encrypt.route, Screen.Decrypt.route, Screen.Passphrase.route
    )

    // 現在のルートに応じたトップバーの見た目（タイトル・背景色）を決定するだけの追加ロジック
    val topBarSpec = when (currentRoute) {
        Screen.Encrypt.route    -> FamiPassTopBarPresets.Encrypt
        Screen.Decrypt.route    -> FamiPassTopBarPresets.Decrypt
        Screen.Passphrase.route -> FamiPassTopBarPresets.Passphrase
        Screen.Settings.route   -> FamiPassTopBarPresets.Settings
        else -> null
    }

    Scaffold(
        topBar = {
            topBarSpec?.let { spec ->
                FamiPassTopBar(
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
                
                // 共有されたテキストがあればセットする
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
            composable(Screen.QrScan.route)   {
                WithCameraPermission {
                    QrScanScreen(navController = nav, onSuccess = { nav.popBackStack() })
                }
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}