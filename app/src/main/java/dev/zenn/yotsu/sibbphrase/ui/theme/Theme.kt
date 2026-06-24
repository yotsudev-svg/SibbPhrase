package dev.zenn.yotsu.sibbphrase.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.zenn.yotsu.sibbphrase.model.AppTheme

/**
 * 現在のテーマがダークモードかどうかを子Composableへ伝搬するCompositionLocal。
 *
 * [SibbPhraseTheme] 内で `CompositionLocalProvider` によって提供され、
 * `SibbPhraseTopBarSpec.getBackgroundColor()` など、テーマに応じた色の切り替えが必要な
 * 子Composableから `LocalDarkTheme.current` で参照する。
 * デフォルト値は `false`（ライトモード）。
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * SibbPhrase アプリ全体に適用するテーマComposable。
 *
 * アーキテクチャ上の配置: テーマ層（ui/theme/）
 * 責務: [AppTheme] の設定値に基づいてライト/ダークカラースキームを決定し、
 * `MaterialTheme` と [LocalDarkTheme] を子Composableツリー全体へ提供する。
 *
 * カラースキームの優先順位:
 * 1. Android 12（API 31）以上かつ `dynamicColor = true` の場合: 端末のダイナミックカラーを使用。
 * 2. 上記以外: [DarkColorScheme] または [LightColorScheme] の固定カラースキームを使用。
 *
 * [LocalDarkTheme] を `CompositionLocalProvider` で提供することで、
 * MaterialTheme の外部からでも現在のテーマ状態を参照できる。
 *
 * @param appTheme 適用するテーマモード（[AppTheme.SYSTEM] / [AppTheme.LIGHT] / [AppTheme.DARK]）。
 *                 デフォルトは [AppTheme.SYSTEM]（端末設定に連動）。
 * @param dynamicColor Android 12 以上でダイナミックカラーを使用するかどうか。デフォルトは `true`。
 * @param content テーマを適用するComposableコンテンツ。
 */
@Composable
fun SibbPhraseTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT  -> false
        AppTheme.DARK   -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}