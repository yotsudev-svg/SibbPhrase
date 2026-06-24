package dev.zenn.yotsu.sibbphrase.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zenn.yotsu.sibbphrase.data.crypto.KeystoreManager
import dev.zenn.yotsu.sibbphrase.data.local.DataStoreManager
import dev.zenn.yotsu.sibbphrase.model.AppTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 他アプリからのテキスト共有に応じた画面遷移イベントを表すシールドクラス。
 *
 * [MainActivity.handleShareIntent] で `Intent.ACTION_SEND` を受信した際に
 * [MainViewModel.onSharedText] を通じて発火され、`NavGraph.kt` 内の
 * `LaunchedEffect` でナビゲーションに反映される。
 */
sealed class SharedNavigationEvent {
    /**
     * 復元（Decrypt）画面へ遷移し、受け取ったテキストを入力欄にセットするイベント。
     * 合言葉が設定済みの場合に発火される。
     *
     * @param text 他アプリから共有された暗号文（トリム済み）。
     */
    data class GoToDecrypt(val text: String) : SharedNavigationEvent()

    /**
     * 合言葉（Passphrase）画面へ遷移し、設定を促すメッセージを渡すイベント。
     * 合言葉が未設定の場合に発火される。
     *
     * @param message 画面遷移先に渡す案内メッセージ。
     */
    data class GoToPassphrase(val message: String) : SharedNavigationEvent()
}

/**
 * アプリ全体の共通状態と、他アプリからのテキスト共有ハンドリングを管理するViewModel。
 *
 * アーキテクチャ上の配置: プレゼンテーション層（ui/）
 * 責務:
 * 1. `Intent.ACTION_SEND` で共有されたテキストを受け取り、合言葉の設定状況に応じて
 *    適切な画面へ遷移させる [SharedNavigationEvent] を発火する。
 * 2. アプリ全体のテーマ設定（[AppTheme]）を [DataStoreManager] から取得し、
 *    [MainActivity] の `SibbPhraseTheme` へ提供する。
 *
 * @property keystore 合言葉の設定状況確認に使用するマネージャー。
 * @property dataStoreManager テーマ設定の取得に使用するデータ層のリポジトリ。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val keystore: KeystoreManager,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SharedNavigationEvent>(extraBufferCapacity = 1)

    /**
     * 他アプリからのテキスト共有に応じた画面遷移イベントを通知する SharedFlow。
     *
     * `NavGraph.kt` 内の `LaunchedEffect(Unit)` で収集され、受信したイベントの種別に応じて
     * [Screen.Decrypt] または [Screen.Passphrase] へのナビゲーションが実行される。
     * `extraBufferCapacity = 1` により、サブスクライバー不在時でも直近の1件をバッファリングする。
     */
    val navigationEvent = _navigationEvent.asSharedFlow()

    /**
     * アプリ全体の表示テーマ（[AppTheme.SYSTEM] / [AppTheme.LIGHT] / [AppTheme.DARK]）を
     * 保持・公開するStateFlow。
     *
     * [MainActivity] の `setContent` 内で `collectAsState()` により購読され、
     * `SibbPhraseTheme(appTheme = themeMode)` へ渡される。
     * デフォルト値は [AppTheme.SYSTEM]（端末設定に連動）。
     */
    val themeMode: StateFlow<AppTheme> = dataStoreManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    /**
     * 他アプリから共有されたテキストを受け取り、適切な画面遷移イベントを発火する。
     *
     * 実行フロー:
     * 1. [KeystoreManager.hasPassphrase] で合言葉の設定状況を確認する。
     * 2. 設定済みの場合: テキストをトリムして [SharedNavigationEvent.GoToDecrypt] を発火し、
     *    復元画面へ遷移させる。
     * 3. 未設定の場合: [SharedNavigationEvent.GoToPassphrase] を発火し、
     *    合言葉設定を促すメッセージとともに合言葉管理画面へ遷移させる。
     *
     * 呼び出しタイミング:
     * - [MainActivity.handleShareIntent] で `Intent.ACTION_SEND` を受信した際。
     *
     * @param text 他アプリから共有された生のテキスト文字列。
     */
    fun onSharedText(text: String) {
        viewModelScope.launch {
            if (keystore.hasPassphrase()) {
                _navigationEvent.emit(SharedNavigationEvent.GoToDecrypt(text.trim()))
            } else {
                _navigationEvent.emit(SharedNavigationEvent.GoToPassphrase("まずは家族共通の合言葉を設定してください"))
            }
        }
    }
}