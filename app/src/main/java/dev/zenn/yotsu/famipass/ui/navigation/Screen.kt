package dev.zenn.yotsu.famipass.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Onboarding : Screen("onboarding", "はじめに")
    object Encrypt    : Screen("encrypt",    "変換")
    object Decrypt    : Screen("decrypt",    "復元")
    object Passphrase : Screen("passphrase", "合言葉")
    object QrShow     : Screen("qr_show",    "QR表示")
    object QrScan     : Screen("qr_scan",    "QRスキャン")
    object Settings   : Screen("settings",   "設定")
}
