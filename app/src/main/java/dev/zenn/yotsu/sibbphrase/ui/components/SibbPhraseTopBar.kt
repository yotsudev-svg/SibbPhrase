package dev.zenn.yotsu.sibbphrase.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import dev.zenn.yotsu.sibbphrase.ui.theme.SibbPhraseTheme
import dev.zenn.yotsu.sibbphrase.model.AppTheme
import dev.zenn.yotsu.sibbphrase.ui.theme.LocalDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * タブ／画面ごとに背景色とタイトルを変える、見やすい大きめのトップバー。
 * Scaffold の topBar スロットにそのまま差し込んで使う想定の独立コンポーネント。
 * ロジックには一切関与しないので、どの画面からでも安全に呼び出せる。
 *
 * @param title TopBar に表示するタイトル文字列。
 * @param backgroundColor TopBar の背景色。[androidx.compose.ui.graphics.Color.luminance] の値に応じて
 *                        テキスト・アイコン色が白（暗い背景）または黒（明るい背景）に自動切り替えされる。
 * @param onSettingsClick 設定アイコン押下時のコールバック。`null` を渡すとアイコン自体が非表示になる。
 * @param onBackClick 戻るアイコン押下時のコールバック。`null` を渡すとアイコン自体が非表示になる。
 */
@Composable
fun SibbPhraseTopBar(
    title: String,
    backgroundColor: Color,
    onSettingsClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    // 背景の明るさに応じて、文字色を白か黒に自動で切り替える
    val contentColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る",
                        tint = contentColor
                    )
                }
            }

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (onBackClick == null) 8.dp else 0.dp)
            )

            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定",
                        tint = contentColor
                    )
                }
            }
        }
    }
}

/**
 * 画面（ルート）ごとの表示内容をまとめた定義。
 * ライトモード用とダークモード用の背景色を保持する。
 */
data class SibbPhraseTopBarSpec(
    val title: String,
    val lightBackgroundColor: Color,
    val darkBackgroundColor: Color
) {
    /**
     * 現在のテーマに応じたTopBarの背景色を返す。
     *
     * [LocalDarkTheme] から現在のテーマ状態を取得し、ダークモードの場合は [darkBackgroundColor]、
     * ライトモードの場合は [lightBackgroundColor] を返す。
     * `NavGraph.kt` 内の `topBar` スロットで画面ルートに対応する [SibbPhraseTopBarSpec] を
     * 取得した後、このメソッドで背景色を解決して [SibbPhraseTopBar] へ渡す。
     *
     * @param darkTheme 現在のテーマがダークモードかどうか。デフォルトは [LocalDarkTheme.current]。
     * @return 現在のテーマに対応する背景色。
     */
    @Composable
    fun getBackgroundColor(darkTheme: Boolean = LocalDarkTheme.current): Color {
        return if (darkTheme) darkBackgroundColor else lightBackgroundColor
    }
}

/**
 * 各画面（ルート）のTopBar表示仕様をまとめたプリセット定義。
 *
 * アーキテクチャ上の配置: UIコンポーネント層（ui/components/）
 * 責務: 画面ごとのタイトルとライト/ダーク用の背景色を [SibbPhraseTopBarSpec] として定義し、
 * `NavGraph.kt` の `topBar` スロットで `currentRoute` に応じて参照される。
 *
 * 色の設計方針:
 * 各タブの機能的な意味合いに合わせて異なるアクセントカラーを割り当て、
 * ユーザーが現在の画面を直感的に識別できるようにしている。
 * ライトモードとダークモードでそれぞれ可読性を確保した濃度違いの同系色を使用する。
 */
object SibbPhraseTopBarPresets {
    /** 暗号化（変換）画面のTopBar仕様。Blue系カラーで「保護・変換」を表現する。 */
    val Encrypt = SibbPhraseTopBarSpec(
        title = "パスワード変換",
        lightBackgroundColor = Color(0xFF1E88E5), // Blue 600
        darkBackgroundColor  = Color(0xFF0D47A1)  // Blue 900
    )

    /** 復元画面のTopBar仕様。Green系カラーで「解除・復元」を表現する。 */
    val Decrypt = SibbPhraseTopBarSpec(
        title = "パスワード復元",
        lightBackgroundColor = Color(0xFF2E7D32), // Green 700
        darkBackgroundColor  = Color(0xFF1B5E20)  // Green 900
    )

    /** 合言葉管理画面のTopBar仕様。Blue Grey系カラーで「鍵・機密」を表現する。 */
    val Passphrase = SibbPhraseTopBarSpec(
        title = "合言葉",
        lightBackgroundColor = Color(0xFF546E7A), // Blue Grey 600
        darkBackgroundColor  = Color(0xFF263238)  // Blue Grey 900
    )

    /** 設定画面のTopBar仕様。Blue Grey系カラーで統一感を持たせる。 */
    val Settings = SibbPhraseTopBarSpec(
        title = "設定",
        lightBackgroundColor = Color(0xFF607D8B), // Blue Grey 500
        darkBackgroundColor  = Color(0xFF37474F)  // Blue Grey 800
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PreviewSibbPhraseTopBarLight() {
    SibbPhraseTheme(appTheme = AppTheme.LIGHT) {
        Column {
            SibbPhraseTopBar(
                title = SibbPhraseTopBarPresets.Encrypt.title,
                backgroundColor = SibbPhraseTopBarPresets.Encrypt.lightBackgroundColor,
                onSettingsClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            SibbPhraseTopBar(
                title = SibbPhraseTopBarPresets.Decrypt.title,
                backgroundColor = SibbPhraseTopBarPresets.Decrypt.lightBackgroundColor,
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSibbPhraseTopBarDark() {
    SibbPhraseTheme(appTheme = AppTheme.DARK) {
        Column {
            SibbPhraseTopBar(
                title = SibbPhraseTopBarPresets.Encrypt.title,
                backgroundColor = SibbPhraseTopBarPresets.Encrypt.darkBackgroundColor,
                onSettingsClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            SibbPhraseTopBar(
                title = SibbPhraseTopBarPresets.Decrypt.title,
                backgroundColor = SibbPhraseTopBarPresets.Decrypt.darkBackgroundColor,
                onBackClick = {}
            )
        }
    }
}
