package dev.zenn.yotsu.sibbphrase.ui.navigation

/**
 * アプリ内のナビゲーション画面を定義するシールドクラス。
 *
 * アーキテクチャ上の配置: ナビゲーション層（ui/navigation/）
 * 責務: NavHost に登録される各画面のルート文字列とボトムナビゲーション用ラベルを一元管理する。
 * `NavGraph.kt` での `composable(Screen.Xxx.route)` の指定や、
 * BottomNavigationItem の選択状態判定（`currentRoute == screen.route`）に使用される。
 *
 * @property route NavHost のルート識別子として使用する文字列。
 * @property label ボトムナビゲーションバーのアイテムラベルとして表示される日本語テキスト。
 */
sealed class Screen(val route: String, val label: String) {
    /** 初回起動時のみ表示されるオンボーディング（チュートリアル）画面。 */
    object Onboarding : Screen("onboarding", "はじめに")

    /** 平文をAES-256-GCMで暗号化する「パスワード変換」タブ画面。 */
    object Encrypt    : Screen("encrypt",    "変換")

    /** 暗号文を復号する「パスワード復元」タブ画面。 */
    object Decrypt    : Screen("decrypt",    "復元")

    /** 合言葉の登録・変更・リセットを行う「合言葉」タブ画面。 */
    object Passphrase : Screen("passphrase", "合言葉")

    /** 合言葉をQRコードで表示し家族間共有を行うサブ画面。[Passphrase] からのみ遷移する。 */
    object QrShow     : Screen("qr_show",    "QR表示")

    /** CameraX + ML Kit でQRコードをスキャンし合言葉を取り込むサブ画面。[Passphrase] からのみ遷移する。 */
    object QrScan     : Screen("qr_scan",    "QRスキャン")

    /** テーマ・自動消去秒数・生体認証などのアプリ設定画面。 */
    object Settings   : Screen("settings",   "設定")
}