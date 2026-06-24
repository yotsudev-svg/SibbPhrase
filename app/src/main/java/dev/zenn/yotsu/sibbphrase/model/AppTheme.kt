package dev.zenn.yotsu.sibbphrase.model

/**
 * アプリケーションの表示テーマを表す列挙型（Enum）。
 *
 * アーキテクチャ上の配置: ドメイン/モデル層（model/）
 * 責務: 画面に表示するテーマ（システム連動 / ライト / ダーク）の種類と日本語表示用ラベルを定義する。
 *
 * 設定値は `DataStoreManager` を通じて `sibbphrase_prefs` に文字列として永続化され、
 * `MainActivity` や `SettingsViewModel` から読み出されて UI のテーマ切り替えに利用される。
 *
 * @property label 設定画面（SettingsScreen）などでユーザーに提示される翻訳済みの日本語テキスト。
 */
enum class AppTheme(val label: String) {
    /** システム（端末）の設定に従うデフォルトモード。IT操作に不慣れな家族が迷わないよう標準設定される。 */
    SYSTEM("システムの設定に従う"),
    /** 明るい画面構成のライトテーマ。 */
    LIGHT("ライトテーマ"),
    /** 目に優しい画面構成のダークテーマ。 */
    DARK("ダークテーマ")
}
