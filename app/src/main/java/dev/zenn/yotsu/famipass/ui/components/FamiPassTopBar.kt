package dev.zenn.yotsu.famipass.ui.components

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import dev.zenn.yotsu.famipass.ui.theme.FamipassTheme
import dev.zenn.yotsu.famipass.model.AppTheme
import dev.zenn.yotsu.famipass.ui.theme.LocalDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * タブ／画面ごとに背景色とタイトルを変える、見やすい大きめのトップバー。
 * Scaffold の topBar スロットにそのまま差し込んで使う想定の独立コンポーネント。
 * ロジックには一切関与しないので、どの画面からでも安全に呼び出せる。
 */
@Composable
fun FamiPassTopBar(
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
data class FamiPassTopBarSpec(
    val title: String,
    val lightBackgroundColor: Color,
    val darkBackgroundColor: Color
) {
    /**
     * 現在のテーマに応じた背景色を返す。
     */
    @Composable
    fun getBackgroundColor(darkTheme: Boolean = LocalDarkTheme.current): Color {
        return if (darkTheme) darkBackgroundColor else lightBackgroundColor
    }
}

object FamiPassTopBarPresets {
    val Encrypt = FamiPassTopBarSpec(
        title = "パスワード変換",
        lightBackgroundColor = Color(0xFF1E88E5), // Blue 600
        darkBackgroundColor = Color(0xFF0D47A1)  // Blue 900
    )
    val Decrypt = FamiPassTopBarSpec(
        title = "パスワード復元",
        lightBackgroundColor = Color(0xFF2E7D32), // Green 700
        darkBackgroundColor = Color(0xFF1B5E20)  // Green 900
    )
    val Passphrase = FamiPassTopBarSpec(
        title = "合言葉",
        lightBackgroundColor = Color(0xFF546E7A), // Blue Grey 600
        darkBackgroundColor = Color(0xFF263238)  // Blue Grey 900
    )
    val Settings = FamiPassTopBarSpec(
        title = "設定",
        lightBackgroundColor = Color(0xFF607D8B), // Blue Grey 500
        darkBackgroundColor = Color(0xFF37474F)  // Blue Grey 800
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PreviewFamiPassTopBarLight() {
    FamipassTheme(appTheme = AppTheme.LIGHT) {
        Column {
            FamiPassTopBar(
                title = FamiPassTopBarPresets.Encrypt.title,
                backgroundColor = FamiPassTopBarPresets.Encrypt.lightBackgroundColor,
                onSettingsClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            FamiPassTopBar(
                title = FamiPassTopBarPresets.Decrypt.title,
                backgroundColor = FamiPassTopBarPresets.Decrypt.lightBackgroundColor,
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewFamiPassTopBarDark() {
    FamipassTheme(appTheme = AppTheme.DARK) {
        Column {
            FamiPassTopBar(
                title = FamiPassTopBarPresets.Encrypt.title,
                backgroundColor = FamiPassTopBarPresets.Encrypt.darkBackgroundColor,
                onSettingsClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            FamiPassTopBar(
                title = FamiPassTopBarPresets.Decrypt.title,
                backgroundColor = FamiPassTopBarPresets.Decrypt.darkBackgroundColor,
                onBackClick = {}
            )
        }
    }
}
