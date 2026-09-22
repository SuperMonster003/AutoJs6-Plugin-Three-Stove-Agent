<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>登録済みスクリプトの選択と画面の段階的な操作により AutoJs6 上で自然言語のタスクを実行</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語

******

現在の README.md は以下の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### はじめに

******

AI Agent は自然言語の目標を, AutoJs6 が動作する Android デバイス上の実際の操作に変えます. ユーザーがエージェント用に登録したスクリプトを選んでパラメーターを補い実行するか, アクセシビリティのノードツリーを通じて画面を観察し, 観察, 判断, 操作, 検証の循環で段階的に操作します. 目標を達成するか, 確認が必要になるか, 予算を使い切るまで続けます. [AutoJs6 ディスカッション #577](https://github.com/SuperMonster003/AutoJs6/discussions/577) への回答です.

このプラグインは AutoJs6 プラグインであると同時に単独のアプリでもあります. スクリプトからは AutoJs6 の `ai.agent` API を通じて, ユーザーからは独自のタスク画面, AutoJs6 のドロワー, フローティングボール, システムの共有シート, アプリのショートカット, 音声入力を通じて利用します. モデル呼び出しとデバイス操作は常に Binder 経由で AutoJs6 に委ねられます. ホストはプラグインにモデルブローカー (3-Stone AI などホストが把握している AI Provider プラグイン) と, 範囲を限定した grant 付きの能力ブローカーを貸し出します. プラグインは認証情報を保持せず, モデルプロバイダーに自ら接続せず, アクセシビリティ権限も要求しません.

******

### 現在の状態

******

バージョン 1.0.0 はロードマップの P0 開発プレビューです. プラグインの識別情報, AutoJs6 の検出契約 (INFO サービス, Wake Activity, `org.autojs.plugin.AI_AGENT` サービスの仮実装), ホストの状態を表示する起動画面を含みます. エージェントループ, スクリプトカタログ, `ai.agent` API, タスク画面はまだ実装されていません. 進捗と証拠は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) に記録しています. プラグインは AutoJs6 ビルド 5283 以降を必要とする予定です.

******

### 機能

******

1.0.0 では以下の機能を提供する予定です:

- スクリプト選択: `project.json` または `@agent` ヘッダーコメントで登録したスクリプトを, 説明とパラメータースキーマとともにモデルに提示します. エージェントはスクリプトを選び, パラメーターを補い, 必要なら確認を求め, AutoJs6 内で実行して構造化された結果を読み取ります.
- 画面の段階的な操作: エージェントはアクセシビリティのノードツリーをコンパクトなテキストとして観察し (OCR プラグインがあれば画面の文字も読み取り), AutoJs6 の能力ブローカーを通じてタップ, 入力, スクロール, キー操作を行い, 目標を検証できるまで続けます.
- 設計段階からの安全性: 読み取り専用ツールは自動で実行され, 敏感な操作 (支払い, 送信, 削除, ファイル書き込み, shell, 座標ジェスチャー, 敏感として登録されたスクリプト) は確認を必要とし, 各実行にはステップ数, モデル呼び出し回数, 実行時間, トークンの予算があります.
- スクリプト API とユーザーインターフェース: `ai.agent.run(goal, options)` はイベント, 応答, キャンセルを備えた `AgentRun` ハンドルを返します. 単独アプリは履歴, プリセット, 設定メモリ, 設定, リリース履歴を備えたタスク画面を提供します.

******

### 使い方

******

1. AutoJs6 ビルド 5283 以降を導入したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `AI Agent` が認識されていることを確認して有効にします. 公式リリースのパッケージは署名検証を自動的に通過します.
3. ランチャーから AI Agent を開きます. このプレビューでは互換性のある AutoJs6 ホストがインストールされているかどうかだけを表示します. タスク画面, ドロワーの項目, `ai.agent` API はロードマップの後続段階で提供されます.

> このプレビューの起動画面はホストの状態のみを表示します. AutoJs6 のドロワー項目, `ai.agent` API, タスク画面はそれぞれロードマップの P1, P5, P6 で提供されます.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder のエントリーポイントは署名権限 `org.autojs.permission.PLUGIN` で保護されており, AutoJs6 だけがアクセスできます. 起動画面が唯一のそれ以外の公開コンポーネントです.
- プラグインは API キーを保持せず, モデルプロバイダーに自ら接続せず, アクセシビリティ権限も要求しません. モデル呼び出しとデバイス操作は, AutoJs6 が接続中のリンク 1 本のために貸し出し, 切断時に取り消すブローカーを通じて行われ, それぞれ grant (許可メソッド, レート, サイズ, モデルの割り当て) で制限されます.
- プラグインはネットワークを使用しません. このプレビューではプラグイン権限以外の権限を宣言していません. フォアグラウンドサービス, 通知, オーバーレイの権限はそれを必要とする機能とともに追加し, ここに記載します.
- タスク履歴, プリセット, 設定メモリはプラグインの非公開ストレージにのみ保存されます. バックアップとデバイス間の転送は無効です.

プラグインは公式の [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) ページまたは AutoJs6 のプラグインセンターからのみ入手してください. 出所不明のパッケージは, バージョン番号が同じに見えてもホストの検証に失敗したり, リスクを伴う可能性があります.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストおよびプラグインの開発者向けです. ホストはこれらの識別子を使ってプラグインを検出し, 互換性を交渉します:

```text
application id: io.github.supermonster003.autojs6.plugin.ai.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5283 (6.8.0)
```

`AiAgentPluginService` は `:agent` プロセスで `org.autojs.plugin.AI_AGENT` (category `ai-agent`) に応答します. このプレビューでは, ホストの契約モジュールが用意されるまで, ディスクリプター `org.autojs.plugin.ai.agent.api.IAiAgentPlugin` のみを持つ仮の Binder を公開します. `AiAgentPluginInfoService` は `org.autojs.plugin.INFO` に PluginInfo で応答します. `WakeActivity` によりホストはプラグインを有効化できます.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/09/22_

- `ヒント` P0 開発プレビュー: プラグインの識別情報, AutoJs6 の検出契約, ホストの状態を表示する起動画面. エージェントループ, スクリプトカタログ, ai.agent API, タスク画面はまだ実装されていません. ROADMAP.md を参照してください.
- `ヒント` ホスト側の AI Agent 契約, 権限を制限した機能プロキシ, 使用量を制限したモデルプロキシを実装. プラグインのタスク実行機能は引き続き開発中
- `機能` INFO サービス, Wake Activity, `:agent` プロセスで動く `org.autojs.plugin.AI_AGENT` サービスの仮実装, 互換性のある AutoJs6 ホストの有無を表示する起動画面を備えたプラグイン識別情報 `ai-agent`
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴
- `依存関係` 共有プラグイン契約として `common-plugin-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api`, ホストビルド 6.8.0 / 5282, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュ固定

##### さらに詳しいリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルドと検証

******

このセクションはソースからプラグインをビルドしたい開発者向けです. 通常のユーザーは Releases ページのビルド済み APK をインストールするだけで済みます.

デバッグ APK をビルドする:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM ユニットテストを実行し, インストルメンテーションテスト APK をビルドする:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

リリース APK をビルドする:

```powershell
.\gradlew.bat :app:assembleRelease
```

リリース成果物を収集し, ファイル名にバージョンと CRC32 ダイジェストを追加する:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

多言語ドキュメントのソースと生成物が同期していることを検証する (CI でも実施):

```powershell
py .python\generate_markdown.py --check
```

ビルドには JDK 21 以降と Android SDK 37 が必要です. Gradle とプラグインのバージョンは `version.properties` と `io.github.supermonster003.autojs6-platform-versions` で一元管理されます.

******

### ローカライズとドキュメント生成

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/` と `.changelog/` の言語 JSON ファイルが README, プラグインセンターの説明, 変更履歴の唯一のソースです. 常にこれらの JSON ソースを編集して `py .python/generate_markdown.py` を再実行してください. 生成された README, `plugin_instruction.md`, 変更履歴は手で編集しません. `py .python/generate_markdown.py --check` を実行するとすべての生成物を検証できます.

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE) の下で提供されます. サードパーティのコンポーネントとそのライセンスは [サードパーティ通知](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md) に記載しています.

******

### リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: https://docs.autojs6.com
- AutoJs6 ディスカッション #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md
