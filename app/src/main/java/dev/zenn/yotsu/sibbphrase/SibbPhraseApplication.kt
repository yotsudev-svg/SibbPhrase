package dev.zenn.yotsu.sibbphrase

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * SibbPhrase アプリケーションのエントリーポイントとなる Application クラス。
 *
 * アーキテクチャ上の配置: アプリルート
 * 責務: `@HiltAndroidApp` アノテーションにより Hilt の依存注入コンポーネントを初期化する。
 * アプリ起動時に最初にインスタンス化され、アプリケーション全体のライフサイクルを通じて
 * Hilt の DI グラフが維持される起点となる。
 *
 * AndroidManifest.xml の `android:name` にこのクラスを指定することで有効化される。
 */
@HiltAndroidApp
class SibbPhraseApplication : Application()