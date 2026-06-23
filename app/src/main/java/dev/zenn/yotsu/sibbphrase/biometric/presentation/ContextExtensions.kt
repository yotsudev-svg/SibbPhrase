package dev.zenn.yotsu.sibbphrase.biometric.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * LocalContext.current は ContextWrapper でラップされた Activity を返す場合があるため、
 * 直接 cast ではなく baseContext を再帰的に辿る。
 * Accompanist の実装パターンに準拠。
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}