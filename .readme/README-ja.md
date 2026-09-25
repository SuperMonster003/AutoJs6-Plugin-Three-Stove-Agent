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

1.0.0 は自然言語タスク, 登録済みスクリプトの呼び出し, リスクに応じた確認を伴う端末操作を提供します. 検証済みの事例, モデルの制限, 未実施の端末検証は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) を参照してください. ネイティブツール呼び出し, 画像入力, 動的スクリプト生成は 1.1.0 で対応予定です.

******

### 機能

******

現在の実装は次の機能を提供します:

- スクリプト選択: `project.json` または `@agent` ヘッダーコメントで登録したスクリプトを, 説明とパラメータースキーマとともにモデルに提示します. エージェントはスクリプトを選び, パラメーターを補い, 必要なら確認を求め, AutoJs6 内で実行して構造化された結果を読み取ります.
- 画面の段階的な操作: エージェントはアクセシビリティのノードツリーをコンパクトなテキストとして観察し (OCR プラグインがあれば画面の文字も読み取り), AutoJs6 の能力ブローカーを通じてタップ, 入力, スクロール, キー操作を行い, 目標を検証できるまで続けます.
- 設計段階からの安全性: 読み取り専用ツールは自動で実行され, 敏感な操作 (支払い, 送信, 削除, ファイル書き込み, shell, 座標ジェスチャー, 敏感として登録されたスクリプト) は確認を必要とし, 各実行にはステップ数, モデル呼び出し回数, 実行時間, トークンの予算があります.
- スクリプト API とユーザーインターフェース: `ai.agent.run(goal, options)` はイベント, 応答, キャンセルを備えた `AgentRun` ハンドルを返します. 単独アプリは履歴, プリセット, 設定メモリ, 設定, リリース履歴を備えたタスク画面を提供します.

### 画面例

Android API 37.1 上の実際の英語画面です. 専用のサンプルタスクと応答を固定したデモモデルを使っています. 実モデルでのタスク成功を示す証拠ではなく, 個人のアカウント情報も含みません. [撮影手順](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/README.md).

| タスク画面 | タスク詳細 |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/workbench.png?raw=true" alt="タスク画面" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/detail.png?raw=true" alt="タスク詳細" width="288" /> |
| 操作の確認 | フローティング入力 |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="操作の確認" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/floating.png?raw=true" alt="フローティング入力" width="288" /> |

******

### インストール

******

1. AutoJs6 ビルド 5293 以降を導入したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `AI Agent` が認識されていることを確認して有効にします. 公式リリースのパッケージは署名検証を自動的に通過します.

[3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI) をインストールして有効にし, オンラインモデルを設定するか対応するローカルモデルを取り込んでください. 現在のホスト仲介は 3-Stone AI を選択します. 別の Provider にはホスト側の対応が必要です. モデルは AI Agent > プリセット で選択します. Connected to AutoJs6 はホスト接続状態を示し, モデル選択はプリセット内にあります.

### 互換性

Android 7.0+ (API 24). 接続には AutoJs6 6.8.0 / build 5289+, 完全なタスク API とこの手順には build 5293+ が必要です. Agent の変更を含むホストを使用してください. 画面操作にはホストのユーザー補助サービスが必要です. OCR は任意で, インストールと認可が済み, ホストが利用可能と報告する OCR プラグインが必要です. AI Agent 自体はモデル認証情報や独立したユーザー補助サービスを持ちません.

### 画面から開始

AI Agent を開いて AutoJs6 に接続し, 目標を入力して標準プリセットで開始します. タスクカードで回答や確認を行い, 最近のタスクから詳細を確認できます.

### スクリプトから開始

AI Agent を接続してモデルを設定した後, AutoJs6 で次の JavaScript を実行します. 質問と確認はプラグイン画面で処理します. 保存済み設定を使うには `preset: "your-preset-name"` を指定します.

```javascript
let run = ai.agent.run('Android のバージョンを読み取り, 観察した値を報告してください.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

`result.status` を確認してください. Promise が解決しても結果は completed, partial, failed, blocked, cancelled のいずれかです. `run.cancel()` で停止できます. モデル選択やイベント, 予算, スクリプトでの応答は [ai.agent API](https://docs.autojs6.com/#ai) を参照してください.

### スクリプトの登録

次の例を AutoJs6 作業ディレクトリまたはホスト承認済みディレクトリの `text-counter.js` として保存します. 先頭の `@agent` JSDoc が登録の指定です. 指定したテキストの文字数を数えるよう依頼すると, 必須パラメータが不足する場合は実行前に質問します.

```javascript
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through AI Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

プロジェクトでは次の `project.json` を `main.js` の隣に置けます. main.js は上の例と同様に `ai.agent.context().parameters` を読み, `ai.agent.result(...)` を呼び出します. 登録情報は `agent` オブジェクトに置きます.

```json
{
  "name": "Text counter",
  "main": "main.js",
  "agent": {
    "id": "text-counter",
    "description": "Count Unicode characters in the supplied text",
    "parameters": {
      "type": "object",
      "properties": { "text": { "type": "string" } },
      "required": ["text"],
      "additionalProperties": false
    },
    "risk": "readonly",
    "confirm": "never",
    "timeoutMs": 10000
  }
}
```

パラメータ型は string, number, integer, boolean に対応し, 入れ子のオブジェクトや配列は非対応です. sensitive スクリプトは毎回実行前の確認が必要です. レビュー済みスクリプトだけを登録してください. リスク宣言は JavaScript を隔離しません. [完全な登録形式](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### ツール一覧

この表は同梱の ToolCatalog から生成します. 実際の画面対象によってリスクが上がる場合があり, 慎重モードでは読み取り以外の操作も確認します. 設定, プリセット, タスクオプション, ホスト権限が利用可能なグループを制限します.

| ツール | グループ | リスク | 既定 | 説明 |
| --- | --- | --- | --- | --- |
| `app_launch` | `act` | `NORMAL` | `on` | Open an application by package name or display name. |
| `clipboard_get` | `act` | `READ_ONLY` | `on` | Read clipboard text. |
| `clipboard_set` | `act` | `NORMAL` | `on` | Replace clipboard text. |
| `ui_click` | `act` | `NORMAL` | `on` | Click one observed target. |
| `ui_long_click` | `act` | `NORMAL` | `on` | Long-click one observed target. |
| `ui_press_key` | `act` | `NORMAL` | `on` | Use an Android navigation or notification-panel action. |
| `ui_scroll` | `act` | `NORMAL` | `on` | Scroll one observed target a bounded number of times. |
| `ui_set_text` | `act` | `NORMAL` | `on` | Set or append text on one observed editable target. |
| `files_list` | `files` | `NORMAL` | `off` | List workspace files. |
| `files_read` | `files` | `NORMAL` | `off` | Read bounded workspace file text. |
| `files_stat` | `files` | `NORMAL` | `off` | Read workspace file metadata. |
| `files_write` | `files` | `SENSITIVE` | `off` | Write a workspace file after confirmation. |
| `ui_click_xy` | `gesture` | `SENSITIVE` | `off` | Tap coordinates only with the gesture group enabled and confirmation. |
| `ui_gesture` | `gesture` | `SENSITIVE` | `off` | Follow a bounded coordinate path after confirmation. |
| `ui_swipe` | `gesture` | `SENSITIVE` | `off` | Swipe between coordinates after confirmation. |
| `memory_get` | `memory` | `READ_ONLY` | `on` | Read available preference memory in the current scope. |
| `memory_propose` | `memory` | `SENSITIVE` | `on` | Propose a preference for user-approved storage; never store credentials. |
| `app_current` | `observe` | `READ_ONLY` | `on` | Read the current window and application. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

### プリセットと記憶

タスク画面のプリセットから設定を保存します. 名前はスクリプトと記憶の固定識別子です. 別名にするには複製してください. 組み込みの default は編集できますが削除できません. ホストの一覧からモデルを選ぶか, 自動選択を使用します. 指定モデルが利用できない場合は失敗し, 他のモデルに切り替わりません. タスクの設定はプリセットの制限をさらに縮小できます. 固定とタスクのコンテキストは合計 8 KiB までです. 記憶は全体と現在のプリセット, 片方のみ, または無効を選べます. 編集や削除は待機中のタスクに影響しません. 非公開領域に最大 32 件 / 1 MiB を保存します.

記憶画面で設定を確認, 編集, 削除またはバックアップできます. 上限は 500 件 / 256 KiB で, スコープ, 元のタスクと日時を保持します. memory_propose とインポートは各項目を個別に確認してください. 不明なプリセットは先に作成が必要です. 自動注入は許可された範囲の新しい完全な項目から最大 4 KiB, 同じ key では現在のプリセットを優先します. memory: false は自動注入のみ停止します. 検索と提案も停止するには memory ツールグループか記憶スコープを無効にします. エクスポートは実際の値と出典を含みます. 認証情報は保存しないでください. 識別可能な認証用キーとトークン形式は拒否されます.

### 使い方

- ランチャーの "スクリプトディレクトリ" で追加フォルダーを設定します. 1 行に 1 つの絶対パスを入力し, 保存後にホストが検証して適用します. タスクは承認済みの範囲だけを絞り込めます.
- 最大 200 件 / 32 MiB. 終了済みタスクのうち最も長く閲覧されていないものから削除されます. 再実行は元の目標とプリセットをタスク画面に入力します. 確認して開始ボタンを押すと実行されます. 履歴の消去時も実行中のタスクは保持されます. 診断用の回数, ツール名, 確認結果を保存します. 目標, 引数, 観察内容, スクリプト結果は除去されます. 保存先を選択してください.
- 前面ではタスク画面で回答し, 背景では高優先度の通知から該当するリクエストを開きます. 確認画面にツール, 引数, リスクと残り時間を表示します. 継続許可はこのタスクの同じツールと同じリスクのみで, 支払いと記憶の提案は毎回確認します. 回答の記憶は許可されたスコープ内で別の memory_propose を作成します. 通常の確認は 120 秒, 質問は最大 10 分で, タスクの予算も適用されます. 時間切れは USER_TIMEOUT として返り, モデルが再質問か部分完了を選びます. 古いリクエストでは新しい質問に回答できません. 背景の通知は権限とチャンネル設定に従います.
- タスク画面の設定でツール群, 予算, 慎重モード, 音声入力と標準プリセットを選択できます. 変更は新しいタスクに適用されます. gesture/files/shell は初期状態で無効, OCR にはホストで利用可能な許可済みプラグインが必要です. 空欄の予算は初期値を継承し, 設定はプロトコル上限に従います. プリセットと個別指定は制限を狭める場合のみ有効です. データ管理は件数とバイト数を表示し, 実行中タスクがない場合に確認後カテゴリーを消去できます. プリセット消去は組み込み default に戻します. スクリプトフォルダー, ライセンスとソースへの入口もあります.
- リリース履歴と法的表示はオフラインで読めます. 更新は GitHub Releases で手動確認し, 成功結果を 24 時間保存します. 取り消しとバージョンの無視に対応し, アプリ内履歴またはブラウザーの公開ページを開けます. 自動確認と APK ダウンロードは行いません.
- 設定でフローティングボールを有効にし, 他のアプリの上への表示を許可して保存します. 初期状態では無効で, AutoJs6 の接続中のみ表示し, ロック時や切断時に隠れます. 待機中のフォアグラウンドサービスはありません. ドラッグで移動し, タップで目標とプリセットの入力, 質問や確認への回答, タスクの停止ができます. カードを閉じると確認通知が再び表示されます. テキストの共有, 新規タスクのショートカット, プリセット画面からの固定目標付きショートカットを利用できます. どの入口も編集可能な下書きを開き, 開始ボタンで実行します. 削除されたプリセットを自動置換しません. 音声認識は画面の言語を使用し, 利用できなければ非表示です. 結果は入力欄に戻し, 自動送信しません.

### よくある質問

**なぜ AutoJs6 が必要ですか?**

プラグインはタスクループと画面を担当し, AutoJs6 はモデル接続, ユーザー補助操作, 登録スクリプト実行を担当します. 互換ホストに接続していなくても履歴は読めますが, 新しい端末タスクは開始できません. 切断で実行中タスクは blocked となり, 再接続しても自動再実行しません.

**なぜ支払いは毎回確認が必要ですか?**

支払いは独立した機密操作です. 注文やスクリプト, 同種操作の承認は支払いの承認ではありません. 検出した支払い操作ごとに個別確認が必要で, 時間切れは拒否として扱います. 承認前に店舗, 商品, 住所, 金額を確認してください.

**ローカルモデルの制限は何ですか?**

タスクは指示追従, 有効な決策 JSON, 利用可能な文脈に依存します. 小型モデルは読み込みに成功してもタスクに失敗し得ます. 記録済みの Gemma 4 E2B IT Wi-Fi 例は決策検証に合格していません. 短いタスクから始め, partial/failed を確認してください. 1.0.0 はノード/OCR テキストと JSON ループを使用します. 画像入力, ネイティブツール呼び出し, スクリプト自動生成は 1.1.0 の計画です.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder 契約の入口は org.autojs.permission.PLUGIN の署名権限で保護されます. ランチャー (ショートカットを含む) と text/plain ACTION_SEND 共有先は公開されていますが, サイズ制限付きの目標とプリセットの下書きだけを受け付けます. 外部 Intent はタスク実行, 確認回答, 権限変更を行えません. 設定, 音声結果, タスク制御は非公開です.
- プラグインは API キーを保持せず, モデルプロバイダーに自ら接続せず, アクセシビリティ権限も要求しません. モデル呼び出しとデバイス操作は, AutoJs6 が接続中のリンク 1 本のために貸し出し, 切断時に取り消すブローカーを通じて行われ, それぞれ grant (許可メソッド, レート, サイズ, モデルの割り当て) で制限されます.
- INTERNET は GitHub リリースの手動確認専用です. FOREGROUND_SERVICE と FOREGROUND_SERVICE_SPECIAL_USE は実行中のタスク, POST_NOTIFICATIONS は進行状況と確認に使用します. SYSTEM_ALERT_WINDOW は設定でフローティングボールを有効にするときだけ要求します. ユーザー補助, ストレージ, マイク権限は要求しません.
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
minimum host build: 5289 (6.8.0)
```

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: ホスト認証付き接続でタスクの待機列, 応答, 取消, 照会と非公開の手順履歴を提供; ホスト切断時は停止状態になり, プロセス再起動で自動再開しない.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/09/25_

- `ヒント` 1.0.0 は自然言語タスク, 登録済みスクリプトの呼び出し, リスクに応じた確認を伴う端末操作を提供します. 検証済みの事例, モデルの制限, 未実施の端末検証は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) を参照してください. ネイティブツール呼び出し, 画像入力, 動的スクリプト生成は 1.1.0 で対応予定です.
- `ヒント` Android 7+, タスク API には AutoJs6 6.8.0 / build 5293+, モデル設定済みの有効な 3-Stone AI が必要です. OCR は任意です. 接続プロトコル単体の最低ホストは build 5289+ です.
- `機能` 自然言語タスク画面に質問, 進捗, 停止, 結果を表示し, 任意のフローティング入力, テキスト共有, プリセットショートカット, 音声下書きに対応
- `機能` ai.agent API でタスク作成, イベント, 照会, 応答, 取消に対応し, detached タスクや登録スクリプトの結果/文脈も利用可能
- `機能` project.json / @agent 登録スクリプトの検索, パラメータ検証と既定値, 不足値の質問, 確認, 有界実行, 構造化結果
- `機能` テキストノードと任意の認可済み OCR で画面を観察し, 参照に基づくクリック, 入力, スクロール, キー操作と画面変化/完了証拠の検証
- `機能` 認証情報を保存せず AutoJs6 経由でオンライン/ローカルモデルを利用し, 選択した対象が欠けた場合は勝手に切り替えず失敗を報告
- `機能` ステップ数, モデル呼び出し, 時間, token の予算とツール期限, ステップごと最大 2 回の決策修復再試行, 無効操作の反復防止
- `機能` モデル, 文脈, ツール群, 予算, 慎重モード, スクリプトフォルダ, 記憶範囲の名前付きプリセットと全体設定; gesture/files/shell は初期状態で無効
- `機能` 範囲付き設定記憶の提案/取り込みを個別確認し, 編集, 削除, JSON バックアップに対応; 最大 500 件 / 256 KiB, 自動注入は 4 KiB
- `機能` タスク詳細と時系列, 絞り込み, 再実行下書き, 秘匿化 JSON 出力; 私有履歴は最大 200 タスク / 32 MiB
- `機能` 画面, 通知, フローティングカードでリスクに応じて確認し, 支払いと記憶は毎回個別承認; ホスト切断で停止し, プロセス再起動で自動再開しない
- `機能` 10 言語の設定, オフライン履歴, 法的表示; 手動 GitHub 更新確認に取消, 日次キャッシュ, 無視する版を用意し, APK は自動ダウンロードしない
- `修正` 残りの予算を使用済みと誤認し, タスクを早期終了する問題
- `修正` フォームとフィルターのタッチ領域, 選択項目とスクリプト引数列の折り返し, 大きなフォントと Android 7 でのフローティング操作部のレイアウト
- `修正` 設定メモリ内の全角文字, ゼロ幅文字および一部の認証情報名による認証情報チェックの回避
- `修正` 安全な画面ロックのない端末で, 復帰時の画面状態が安定する前に判定してフローティングボールが再表示されない問題
- `修正` プラグインのプロセス終了で中断したタスクを再起動後に失敗として記録し, 画面ロックの検出後は画面操作を停止
- `修正` ファイルツールは確認やホスト呼び出しの前にパストラバーサル, 絶対パス, 無効な作業パスを拒否; 履歴には拒否したモデルの本文を含めず, 件数を制限した拒否分類を記録
- `修正` 通知からの確認は対象アプリに戻ってから操作を再開し, 画面停止後も応答を処理し, フローティング返信の送信時にカードを閉じてフォーカスを解放
- `修正` Android 13 でウィンドウのビュー生成前にシステムバーのコントローラーを取得すると起動時にクラッシュする問題
- `修正` 最近の履歴をタスク開始時刻で並べて保持し, 再起動時のファイル書き換えによる新しいタスクの削除を防止
- `修正` 質問と確認への応答で interaction の担当を検証し, スクリプトがプラグイン画面の代わりに応答することを防止
- `修正` 取引確認ボタンが支払い操作と認識されない問題, 毎回の確認が必要となりタスク全体の許可を再利用しない
- `修正` 画面外の一致項目の空または反転した境界を引数エラーとせずテキストを保持し座標が使用不可であることを示す
- `修正` ノードの再配置で境界や操作能力が異なる入れ子のコンテナーを同じ対象と誤認する問題
- `修正` ノード対象の修正案内で参照の # 接頭辞と selector 使用時の snapshotId 省略を明示
- `修正` タスク受付時に注文意図ルールを事前読み込みし, ルール初期化の負荷を削減
- `修正` 異なるウィンドウの同一ノードを区別し, クリップボード読み取り後も画面観察を要求, ファイル転送を支払いと誤判定しない
- `修正` 操作後の画面読み取りが応答しない場合に安定待機の期限を超える問題
- `修正` コンソールの行分割や切り詰め前に複数行のパラメーターを秘匿化し, 認証情報のラベルと同じパラメーター文字列による秘匿漏れを防止
- `修正` 終了中のフォアグラウンドサービスが次のタスクの起動要求を誤って拒否する問題
- `改善` 長い履歴を削減する際に未変更のプロンプトと観察の断片を再利用し, 各ステップの処理時間を短縮
- `改善` 確認説明の上限に JSON エスケープ後のサイズを反映し, 大きな引数表でも Binder イベント上限を維持
- `改善` 操作ノードの検査と確認を実行に結び付けるため, 最低ホストを AutoJs6 6.8.0 / ビルド 5289 に設定
- `依存関係` 同じ AutoJs6 6.8.0 / 5289 release ビルドの common-plugin-api, host-capability-api と ai-agent-api (MPL 2.0) を追加し, SHA-256 で固定
- `依存関係` 有界の厳密 JSON 解析と Schema ツリー用に Gson 2.13.2 を追加

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
